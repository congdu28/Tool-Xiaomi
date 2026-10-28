package app.cd.mitool.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Drains both pipes concurrently and bounds privileged command execution. */
public final class ProcessRunner {
    public static final class Result {
        public final int exitCode;
        public final String stdout, stderr;
        Result(int code, String out, String err) { exitCode = code; stdout = out; stderr = err; }
    }

    public static Result collect(Process process, long timeoutMs) throws InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Thread stdout = drain(process.getInputStream(), out);
        Thread stderr = drain(process.getErrorStream(), err);
        CountDownLatch exited = new CountDownLatch(1);
        AtomicInteger exitCode = new AtomicInteger(-1);
        AtomicReference<Exception> failure = new AtomicReference<>();
        // Shizuku may marshal "still running" as IllegalArgumentException from exitValue().
        // Wait through the Process contract instead of polling an implementation-specific exception.
        Thread waiter = new Thread(() -> {
            try { exitCode.set(process.waitFor()); }
            catch (Exception e) { failure.set(e); }
            finally { exited.countDown(); }
        }, "shell-wait");
        waiter.setDaemon(true);
        waiter.start();
        try {
            boolean finished = exited.await(Math.max(0, timeoutMs), TimeUnit.MILLISECONDS);
            if (!finished) process.destroy();
            stdout.join(1000);
            stderr.join(1000);
            return new Result(finished && failure.get() == null ? exitCode.get() : -1, snapshot(out),
                    !finished ? "Hết thời gian chờ lệnh (" + timeoutMs / 1000 + " giây)"
                            : failure.get() != null ? "Không thể chờ kết quả lệnh: " + failure.get().getMessage() : snapshot(err));
        } finally {
            process.destroy();
            waiter.interrupt();
            try { process.getInputStream().close(); } catch (Exception ignored) {}
            try { process.getErrorStream().close(); } catch (Exception ignored) {}
        }
    }

    private static String snapshot(ByteArrayOutputStream buffer) {
        synchronized (buffer) { return buffer.toString().trim(); }
    }

    private static Thread drain(InputStream input, ByteArrayOutputStream buffer) {
        Thread thread = new Thread(() -> {
            try {
                byte[] bytes = new byte[4096];
                int size;
                while ((size = input.read(bytes)) != -1) {
                    synchronized (buffer) {
                        if (buffer.size() < 65536) buffer.write(bytes, 0, Math.min(size, 65536 - buffer.size()));
                    }
                }
            } catch (Exception ignored) {}
        }, "shell-output");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
