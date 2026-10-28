package app.cd.mitool;

import app.cd.mitool.util.RefreshRatePlan;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshRatePlanTest {
    @Test public void hzChangesNeverEnablePerformanceOrOverrideThermalControls() {
        String command = RefreshRatePlan.shell(RefreshRatePlan.changes(60, null));
        assertFalse(command.contains("speed_mode"));
        assertFalse(command.contains("power_mode"));
        assertFalse(command.contains("thermal"));
        assertTrue(command.indexOf("peak_refresh_rate '60'") < command.indexOf("min_refresh_rate '60'"));
    }
    @Test public void restorePreservesOriginalValuesAndOriginallyAbsentKeys() {
        Map<String, String> original = new LinkedHashMap<>();
        for (String key : RefreshRatePlan.KEYS) original.put(key, null);
        original.put("secure:peak_refresh_rate", "144.0");
        original.put("secure:min_refresh_rate", "1.0");
        Map<String, String> restored = RefreshRatePlan.changes(0, original);
        assertEquals(original, restored);
        String command = RefreshRatePlan.shell(restored);
        assertTrue(command.contains("delete system min_refresh_rate"));
        assertTrue(command.contains("peak_refresh_rate '144.0'"));
        assertFalse(command.contains("120"));
    }
    @Test public void legacyAutoClearsFixedBoundsInsteadOfAssumingSixtyToOneTwenty() {
        String command = RefreshRatePlan.shell(RefreshRatePlan.changes(0, null));
        assertTrue(command.contains("delete secure min_refresh_rate"));
        assertTrue(command.contains("delete secure peak_refresh_rate"));
        assertFalse(command.contains("120"));
        assertFalse(command.contains("60.0"));
    }
}
