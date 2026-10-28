package app.cd.mitool;

import app.cd.mitool.util.RamCleanupPolicy;
import org.junit.Test;
import static org.junit.Assert.*;

public class RamCleanupPolicyTest {
    @Test public void numericProcessesAreGroupedByUidWithoutCountingDuplicates() {
        var processes = RamCleanupPolicy.parseProcesses("UID PID NAME\n10123 42 com.test\n10123 43 com.test:remote\n10123 42 com.test\n0 1 init\n");
        assertEquals(2, processes.get(10123).size());
        assertEquals(1, processes.get(0).size());
    }
    @Test public void malformedOrMissingProcessRowsDoNotInventApps() {
        assertTrue(RamCleanupPolicy.parseProcesses(null).isEmpty());
        assertTrue(RamCleanupPolicy.parseProcesses("UID PID NAME\nu0_a1 4 app\n10000 -2 app\n10000 abc app\n").isEmpty());
    }
    @Test public void commandTargetsRejectShellSyntax() {
        assertTrue(RamCleanupPolicy.validPackage("com.example_app.test"));
        for (String value : new String[]{"com.test;id", "com.test app", "com.test\nwhoami", "--user", "com.test:remote", "", null})
            assertFalse(RamCleanupPolicy.validPackage(value));
    }
    @Test public void protectedSharedAndSystemUidsCannotBeStopped() {
        assertTrue(RamCleanupPolicy.canStop(10123, 0, false, false, false));
        assertFalse(RamCleanupPolicy.canStop(10123, 0, true, false, false));
        assertFalse(RamCleanupPolicy.canStop(10123, 0, false, true, false));
        assertFalse(RamCleanupPolicy.canStop(10123, 0, false, false, true));
        assertFalse(RamCleanupPolicy.canStop(1000, 0, false, false, false));
    }
    @Test public void anotherUserProfileIsNeverTargeted() {
        assertFalse(RamCleanupPolicy.canStop(1010123, 0, false, false, false));
        assertTrue(RamCleanupPolicy.canStop(1010123, 10, false, false, false));
    }
}
