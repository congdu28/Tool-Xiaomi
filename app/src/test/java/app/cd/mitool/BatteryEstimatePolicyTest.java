package app.cd.mitool;

import app.cd.mitool.util.BatteryEstimatePolicy;
import org.junit.Test;
import static org.junit.Assert.*;

public class BatteryEstimatePolicyTest {
    @Test public void missingShortOrChargingSessionsStayUnknown() {
        assertNull(BatteryEstimatePolicy.fromSession(80, 80, 3600000, 600000, false));
        assertNull(BatteryEstimatePolicy.fromSession(80, 70, 60000, 60000, false));
        assertNull(BatteryEstimatePolicy.fromSession(80, 70, 3600000, 600000, true));
        assertNull(BatteryEstimatePolicy.fromSession(80, 70, 3600000, 7200000, false));
    }
    @Test public void projectionsUseMeasuredDropAndBothObservedDurations() {
        BatteryEstimatePolicy.Estimate estimate = BatteryEstimatePolicy.fromSession(52, 50, 1800000, 600000, false);
        assertNotNull(estimate);
        assertEquals(750, estimate.mixedMinutes);
        assertEquals(250, estimate.screenMinutes);
        assertEquals(4, estimate.percentPerHour, 0.001);
    }
    @Test public void observedHeavyDrainIsNotClampedToAnOptimisticBaseline() {
        BatteryEstimatePolicy.Estimate estimate = BatteryEstimatePolicy.fromSession(100, 50, 1800000, 1800000, false);
        assertNotNull(estimate);
        assertEquals(30, estimate.mixedMinutes);
        assertEquals(100, estimate.percentPerHour, 0.001);
    }
}
