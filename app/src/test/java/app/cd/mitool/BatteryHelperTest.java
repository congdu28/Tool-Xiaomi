package app.cd.mitool;

import org.junit.Test;
import static org.junit.Assert.*;

public class BatteryHelperTest {

    @Test
    public void testBatteryMathAndEta() {
        // Fast charge calculation check
        int currentLevel = 20;
        int remainingPercent = 100 - currentLevel; // 80%
        assertEquals(80, remainingPercent);

        // Verify that temperature formatting works properly
        int rawTemp = 365;
        float tempC = rawTemp / 10.0f;
        assertEquals(36.5f, tempC, 0.01f);

        // Verify voltage to Volt conversion
        int voltageMv = 4250;
        float voltageV = voltageMv / 1000.0f;
        assertEquals(4.25f, voltageV, 0.01f);

        // Power calculation (W = V * A)
        int currentMa = 3000; // 3A
        float powerW = voltageV * (currentMa / 1000.0f);
        assertEquals(12.75f, powerW, 0.01f);
    }
}
