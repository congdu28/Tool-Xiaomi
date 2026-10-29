package app.cd.mitool.util;

/** No recurring refresh while the display is off. Alerts still use battery broadcasts. */
public final class MonitoringPolicy {
    private MonitoringPolicy() {}

    public static boolean requiresService(boolean clock, boolean chargingNotification, boolean targetAlert,
                                          boolean fullAlert, boolean temperatureAlert, boolean voice) {
        return clock || chargingNotification || targetAlert || fullAlert || temperatureAlert || voice;
    }

    public static long notificationInterval(boolean screenOn, boolean secondsClock, boolean charging) {
        if (!screenOn) return 0;
        if (secondsClock) return 1000;
        return charging ? 5000 : 60000;
    }

    public static boolean isRapidDrain(int drop, long elapsedMs) {
        // A coarse observation of measured discharge, never evidence of a background app.
        return elapsedMs >= 15 * 60000L && elapsedMs <= 4 * 3600000L
                && drop >= 3 && drop * 3600000.0 / elapsedMs >= 10;
    }
}
