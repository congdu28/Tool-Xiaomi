package app.cd.mitool.util;

/** One alert per charging session, with temperature hysteresis and no narrow level window. */
public final class BatteryAlertPolicy {
    public static final int TARGET = 1, FULL = 2, HOT = 4;
    private boolean targetSent, fullSent, hotSent;
    private int previousTarget = -1;
    public int evaluate(boolean plugged, int level, float temperature, int target,
                        boolean targetEnabled, boolean fullEnabled, boolean hotEnabled) {
        if (!plugged) { targetSent = false; fullSent = false; }
        if (target != previousTarget) { targetSent = false; previousTarget = target; }
        if (temperature < 40) hotSent = false;
        int alerts = 0;
        if (plugged && level >= 0 && level <= 100) {
            if (fullEnabled && level >= 100 && !fullSent) { fullSent = true; alerts |= FULL; }
            if (targetEnabled && level >= target && !targetSent) {
                targetSent = true;
                if ((alerts & FULL) == 0) alerts |= TARGET;
            }
        }
        if (hotEnabled && temperature >= 42 && !hotSent) { hotSent = true; alerts |= HOT; }
        return alerts;
    }
}
