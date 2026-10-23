package app.cd.mitool;

import app.cd.mitool.util.ProcessRunner;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProcessRunnerTest {
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
}
