package app.cd.mitool;

import app.cd.mitool.util.ProcessRunner;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProcessRunnerTest {
    private Process remoteLike(Process delegate, boolean waitFails) {
        return new Process() {
            @Override public java.io.OutputStream getOutputStream() { return delegate.getOutputStream(); }
            @Override public java.io.InputStream getInputStream() { return delegate.getInputStream(); }
            @Override public java.io.InputStream getErrorStream() { return delegate.getErrorStream(); }
            @Override public int waitFor() throws InterruptedException {
                if (waitFails) throw new IllegalStateException("binder unavailable");
                return delegate.waitFor();
            }
            @Override public int exitValue() { throw new IllegalArgumentException("process hasn't exited"); }
            @Override public void destroy() { delegate.destroy(); }
        };
    }
    private Process child(String mode) throws Exception {
        String java = new File(System.getProperty("java.home"), "bin/java").getPath();
        String classes = new File(CommandFixture.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        return new ProcessBuilder(java, "-cp", classes, CommandFixture.class.getName(), mode).start();
    }
    @Test(timeout = 10000) public void drainsBothPipesAndPreservesFailedExitCode() throws Exception {
        ProcessRunner.Result result = ProcessRunner.collect(child("output"), 5000);
        assertEquals(7, result.exitCode);
        assertEquals(65536, result.stdout.length());
        assertEquals(65536, result.stderr.length());
    }
    @Test(timeout = 5000) public void terminatesHungCommands() throws Exception {
        ProcessRunner.Result result = ProcessRunner.collect(child("timeout"), 100);
        assertEquals(-1, result.exitCode);
        assertTrue(result.stderr.contains("Hết thời gian"));
    }
    @Test(timeout = 10000) public void shizukuStyleExitValueDoesNotAbortARunningCommand() throws Exception {
        ProcessRunner.Result result = ProcessRunner.collect(remoteLike(child("output"), false), 5000);
        assertEquals(7, result.exitCode);
        assertEquals(65536, result.stdout.length());
        assertEquals(65536, result.stderr.length());
    }
    @Test(timeout = 5000) public void failedRemoteWaitIsNotReportedAsSuccess() throws Exception {
        ProcessRunner.Result result = ProcessRunner.collect(remoteLike(child("timeout"), true), 1000);
        assertEquals(-1, result.exitCode);
        assertTrue(result.stderr.contains("binder unavailable"));
    }
}
