package app.cd.mitool.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

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
        try {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
            boolean finished = false;
            do {
                try { process.exitValue(); finished = true; break; }
                catch (IllegalThreadStateException running) { Thread.sleep(20); }
            } while (System.nanoTime() < deadline);
            if (!finished) process.destroy();
            stdout.join(1000);
            stderr.join(1000);
            return new Result(finished ? process.exitValue() : -1, snapshot(out),
                    finished ? snapshot(err) : "Hết thời gian chờ lệnh (" + timeoutMs / 1000 + " giây)");
        } finally {
            process.destroy();
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
