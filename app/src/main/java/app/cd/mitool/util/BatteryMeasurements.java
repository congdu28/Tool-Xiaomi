package app.cd.mitool.util;

/** BatteryManager current is always in microamps, including small idle readings. */
public final class BatteryMeasurements {
    private BatteryMeasurements() {}

    public static int milliAmps(int microAmps) {
        if (microAmps == Integer.MIN_VALUE) return 0; // Property unavailable.
        return (int) (Math.abs((long) microAmps) / 1000L);
    }

    public static float watts(int voltageMv, int currentMa) {
        if (voltageMv <= 0 || currentMa <= 0) return 0;
        return (voltageMv / 1000.0f) * (currentMa / 1000.0f);
    }
}
