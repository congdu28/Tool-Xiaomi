package app.cd.mitool;

import app.cd.mitool.util.BatteryMeasurements;
import app.cd.mitool.util.BatteryDrainAnalyzer;
import app.cd.mitool.util.MonitoringPolicy;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class BatteryMonitoringTest {
    @Test public void smallIdleCurrentsUseTheSameUnitsAsLargeCurrents() {
        assertEquals(5, BatteryMeasurements.milliAmps(-5000));
        assertEquals(10, BatteryMeasurements.milliAmps(10000));
        assertEquals(450, BatteryMeasurements.milliAmps(-450000));
        assertEquals(0, BatteryMeasurements.milliAmps(Integer.MIN_VALUE));
        assertEquals(0, BatteryMeasurements.milliAmps(0));
        assertEquals(0.02f, BatteryMeasurements.watts(4000, 5), 0.0001f);
        assertEquals(0, BatteryMeasurements.watts(4000, 0), 0);
    }
    @Test public void screenOffNeverSchedulesPollingAndSecondsAreOptIn() {
        assertEquals(0, MonitoringPolicy.notificationInterval(false, true, true));
        assertEquals(0, MonitoringPolicy.notificationInterval(false, false, false));
        assertEquals(60000, MonitoringPolicy.notificationInterval(true, false, false));
        assertEquals(5000, MonitoringPolicy.notificationInterval(true, false, true));
        assertEquals(1000, MonitoringPolicy.notificationInterval(true, true, false));
    }
    @Test public void rapidDrainDependsOnElapsedTimeAndMinimumObservation() {
        assertTrue(MonitoringPolicy.isRapidDrain(6, 30 * 60000L));
        assertFalse(MonitoringPolicy.isRapidDrain(6, 3 * 3600000L));
        assertFalse(MonitoringPolicy.isRapidDrain(6, 60000));
        assertFalse(MonitoringPolicy.isRapidDrain(6, 0));
    }
    @Test public void aggregatesFrequentSamplesRatherThanMissingFastDischarge() {
        List<BatteryDrainAnalyzer.Point> points = new ArrayList<>();
        for (int minute = 0; minute <= 20; minute += 5) points.add(point(minute, 80 - minute, false));
        BatteryDrainAnalyzer.Result result = BatteryDrainAnalyzer.fastest(points);
        assertNotNull(result);
        assertEquals(60, result.percentPerHour, 0.0001);
    }
    @Test public void chargingGapsAndClockJumpsCannotBeAttributedToDischarge() {
        assertNull(BatteryDrainAnalyzer.fastest(Arrays.asList(point(0, 80, false), point(15, 70, true), point(30, 69, false))));
        assertNull(BatteryDrainAnalyzer.fastest(Arrays.asList(point(0, 80, false), point(60, 60, false))));
        assertNull(BatteryDrainAnalyzer.fastest(Arrays.asList(point(20, 80, false), point(0, 70, false))));
        assertNull(BatteryDrainAnalyzer.fastest(Arrays.asList(point(0, 50, false), point(15, 60, false))));
    }
    @Test public void emptyHistoryIsUnknownAndZeroPercentIsAValidReading() {
        assertNull(BatteryDrainAnalyzer.fastest(new ArrayList<>()));
        BatteryDrainAnalyzer.Result result = BatteryDrainAnalyzer.fastest(Arrays.asList(point(0, 3, false), point(15, 0, false)));
        assertNotNull(result);
        assertEquals(12, result.percentPerHour, 0.0001);
    }
    private BatteryDrainAnalyzer.Point point(int minute, int percentage, boolean charging) {
        return new BatteryDrainAnalyzer.Point(minute * 60000L, percentage, charging);
    }
}
