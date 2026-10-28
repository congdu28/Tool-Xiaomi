package app.cd.mitool.util;

/** Linear projection of the observed unplugged session; no device-specific baseline. */
public final class BatteryEstimatePolicy {
    private BatteryEstimatePolicy() {}
    public static final class Estimate {
        public final long screenMinutes, mixedMinutes;
        public final float percentPerHour;
        Estimate(long screen, long mixed, float rate) { screenMinutes = screen; mixedMinutes = mixed; percentPerHour = rate; }
    }
    public static Estimate fromSession(int startLevel, int currentLevel, long elapsed, long screenOn, boolean charging) {
        if (charging || startLevel > 100 || currentLevel < 0 || currentLevel > 100
                || startLevel - currentLevel < 2 || elapsed < 30 * 60000L || elapsed > 48 * 3600000L
                || screenOn < 0 || screenOn > elapsed) return null;
        int drop = startLevel - currentLevel;
        return new Estimate(Math.round(currentLevel * (screenOn / 60000.0) / drop),
                Math.round(currentLevel * (elapsed / 60000.0) / drop), (float) (drop * 3600000.0 / elapsed));
    }
}
