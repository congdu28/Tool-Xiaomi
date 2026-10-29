package app.cd.mitool;

import app.cd.mitool.util.MonitoringPolicy;
import org.junit.Test;
import static org.junit.Assert.*;

public class MonitoringServicePolicyTest {
    @Test public void turningOffTheClockPreservesIndependentMonitoring() {
        assertFalse(MonitoringPolicy.requiresService(false, false, false, false, false, false));
        assertTrue(MonitoringPolicy.requiresService(true, false, false, false, false, false));
        assertTrue(MonitoringPolicy.requiresService(false, true, false, false, false, false));
        assertTrue(MonitoringPolicy.requiresService(false, false, true, false, false, false));
        assertTrue(MonitoringPolicy.requiresService(false, false, false, true, false, false));
        assertTrue(MonitoringPolicy.requiresService(false, false, false, false, true, false));
        assertTrue(MonitoringPolicy.requiresService(false, false, false, false, false, true));
    }
}
