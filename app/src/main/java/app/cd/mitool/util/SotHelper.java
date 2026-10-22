package app.cd.mitool.util;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SotHelper {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM", Locale.getDefault());

    /**
     * Called when device is unplugged from charger (ACTION_POWER_DISCONNECTED).
     * Resets Screen On Time session to measure from this unplug moment.
     */
    public static void onPowerDisconnected(Context context, int batteryLevel) {
        if (context == null) return;
        PreferencesHelper prefs = new PreferencesHelper(context);
        long now = System.currentTimeMillis();

        prefs.setSotLastUnplugTime(now);
        prefs.setSotLastUnplugLevel(batteryLevel > 0 ? batteryLevel : 100);
        prefs.setSotAccumulatedMillis(0);

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isInteractive()) {
            prefs.setSotScreenOnStart(now);
        } else {
            prefs.setSotScreenOnStart(0);
        }
        prefs.setSotTrackerActive(true);
    }

    /**
     * Called when device is connected to charger (ACTION_POWER_CONNECTED).
     */
    public static void onPowerConnected(Context context, int batteryLevel) {
        if (context == null) return;
        PreferencesHelper prefs = new PreferencesHelper(context);
        long start = prefs.getSotScreenOnStart();
        if (start > 0) {
            long now = System.currentTimeMillis();
            long elapsed = now - start;
            if (elapsed > 0 && elapsed < 86400000L) {
                prefs.setSotAccumulatedMillis(prefs.getSotAccumulatedMillis() + elapsed);
            }
            prefs.setSotScreenOnStart(now);
        }
    }

    /**
     * Called when screen turns ON (ACTION_SCREEN_ON).
     */
    public static void onScreenOn(Context context) {
        if (context == null) return;
        PreferencesHelper prefs = new PreferencesHelper(context);
        if (prefs.getSotScreenOnStart() <= 0) {
            prefs.setSotScreenOnStart(System.currentTimeMillis());
        }
    }

    /**
     * Called when screen turns OFF (ACTION_SCREEN_OFF).
     */
    public static void onScreenOff(Context context) {
        if (context == null) return;
        PreferencesHelper prefs = new PreferencesHelper(context);
        long start = prefs.getSotScreenOnStart();
        if (start > 0) {
            long now = System.currentTimeMillis();
            long elapsed = now - start;
            if (elapsed > 0 && elapsed < 86400000L) {
                prefs.setSotAccumulatedMillis(prefs.getSotAccumulatedMillis() + elapsed);
            }
            prefs.setSotScreenOnStart(0);
        }
    }

    /**
     * Returns total Screen On Time in milliseconds since last unplug or reset.
     */
    public static long getScreenOnTimeMillis(Context context) {
        if (context == null) return 0;
        PreferencesHelper prefs = new PreferencesHelper(context);

        long accumulated = prefs.getSotAccumulatedMillis();
        long start = prefs.getSotScreenOnStart();
        if (start > 0) {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > 0 && elapsed < 86400000L) {
                accumulated += elapsed;
            }
        }

        // If tracker has not been activated yet (e.g. freshly installed before first unplug/reset)
        // and accumulated is 0, estimate initial SOT from today's UsageStats
        if (!prefs.isSotTrackerActive() && accumulated <= 0 && BatteryUsageHelper.hasUsageAccessPermission(context)) {
            long fallback = getSotFromUsageStats(context, prefs.getSotLastUnplugTime());
            if (fallback > 0) {
                return fallback;
            }
        }

        return Math.max(0, accumulated);
    }

    /**
     * Fallback calculation from UsageStatsManager when background tracker wasn't running yet.
     */
    private static long getSotFromUsageStats(Context context, long sinceTime) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                if (usm == null) return 0;

                long now = System.currentTimeMillis();
                long start = sinceTime;
                if (start <= 0 || (now - start) > (48 * 3600 * 1000L) || start > now) {
                    // Default to today start
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    start = cal.getTimeInMillis();
                }

                List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, now);
                if (stats != null && !stats.isEmpty()) {
                    long totalForeground = 0;
                    for (UsageStats u : stats) {
                        totalForeground += u.getTotalTimeInForeground();
                    }
                    if (totalForeground > 0) {
                        return totalForeground;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /**
     * Format milliseconds into human-readable Vietnamese duration.
     * E.g.: "4 giờ 25 phút", "45 phút", "< 1 phút".
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) return "0 phút";
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return hours + " giờ " + minutes + " phút";
        } else if (minutes > 0) {
            return minutes + " phút";
        } else {
            return "< 1 phút";
        }
    }

    /**
     * Returns formatted total device usage duration since last unplug or reset.
     * E.g. "3 giờ 41 phút", "45 phút", "Vừa mới rút".
     */
    public static String getTotalUsageDuration(Context context) {
        if (context == null) return "Vừa mới rút";
        PreferencesHelper prefs = new PreferencesHelper(context);
        long lastUnplug = prefs.getSotLastUnplugTime();
        long now = System.currentTimeMillis();
        if (lastUnplug <= 0 || lastUnplug > now) {
            return "Vừa mới rút";
        }
        long diff = now - lastUnplug;
        if (diff < 60000L) {
            return "Vừa mới rút";
        } else if (diff > 7 * 86400000L) {
            return "> 7 ngày";
        } else {
            return formatDuration(diff);
        }
    }

    /**
     * Ensure SOT preferences have a valid baseline timestamp when app is first installed/opened.
     */
    public static void ensureInitialized(Context context) {
        if (context == null) return;
        try {
            PreferencesHelper prefs = new PreferencesHelper(context);
            long unplugTime = prefs.getSotLastUnplugTime();
            long now = System.currentTimeMillis();
            if (unplugTime <= 0 || unplugTime > now) {
                int level = BatteryHelper.getBatteryPercentage(context);
                prefs.setSotLastUnplugTime(now);
                prefs.setSotLastUnplugLevel(level > 0 ? level : 100);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Format unplug moment and battery consumption delta.
     */
    public static String getUnplugSummary(Context context, int currentLevel) {
        if (context == null) return "";
        PreferencesHelper prefs = new PreferencesHelper(context);
        long unplugTime = prefs.getSotLastUnplugTime();
        int unplugLevel = prefs.getSotLastUnplugLevel();
        long now = System.currentTimeMillis();

        if (unplugTime <= 0 || unplugTime > now) {
            return "Đang theo dõi pin từ khi cài đặt ứng dụng";
        }

        long diff = now - unplugTime;
        int drainPercent = Math.max(0, unplugLevel - currentLevel);

        if (diff < 60000L) {
            return "Vừa mới rút sạc ở mức " + unplugLevel + "% • Đã tiêu thụ " + drainPercent + "% pin";
        }

        Date date = new Date(unplugTime);
        String timeStr = TIME_FORMAT.format(date);
        String dateStr = DATE_FORMAT.format(date);

        return "Rút sạc lúc " + timeStr + " (" + dateStr + ") ở mức " + unplugLevel + "% • Đã tiêu thụ " + drainPercent + "% pin";
    }

    /**
     * Manually reset SOT session to now.
     */
    public static void resetSot(Context context, int currentLevel) {
        if (context == null) return;
        PreferencesHelper prefs = new PreferencesHelper(context);
        long now = System.currentTimeMillis();
        prefs.setSotLastUnplugTime(now);
        prefs.setSotLastUnplugLevel(currentLevel > 0 ? currentLevel : 100);
        prefs.setSotAccumulatedMillis(0);

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isInteractive()) {
            prefs.setSotScreenOnStart(now);
        } else {
            prefs.setSotScreenOnStart(0);
        }
        prefs.setSotTrackerActive(true);
    }

    public static class BatteryEstimate {
        public final long remainSotMinutes;
        public final long remainMixedMinutes;
        public final float drainRatePerHour;
        public final String formattedSot;
        public final String formattedMixed;
        public final String formattedRate;

        public BatteryEstimate(long remainSotMinutes, long remainMixedMinutes, float drainRatePerHour,
                               String formattedSot, String formattedMixed, String formattedRate) {
            this.remainSotMinutes = remainSotMinutes;
            this.remainMixedMinutes = remainMixedMinutes;
            this.drainRatePerHour = drainRatePerHour;
            this.formattedSot = formattedSot;
            this.formattedMixed = formattedMixed;
            this.formattedRate = formattedRate;
        }
    }

    /**
     * Estimates remaining on-screen and mixed battery usage based on actual consumption since unplug.
     */
    public static BatteryEstimate getBatteryEstimate(Context context) {
        if (context == null) {
            return new BatteryEstimate(0, 0, 0f, "--", "--", "--");
        }
        PreferencesHelper prefs = new PreferencesHelper(context);
        int currentLevel = BatteryHelper.getBatteryPercentage(context);
        if (currentLevel <= 0) {
            return new BatteryEstimate(0, 0, 0f, "0 phút", "0 phút", "0%");
        }

        long lastUnplug = prefs.getSotLastUnplugTime();
        int lastLevel = prefs.getSotLastUnplugLevel();
        if (lastLevel <= 0) lastLevel = 100;

        int drop = Math.max(0, lastLevel - currentLevel);
        long sotMillis = getScreenOnTimeMillis(context);
        long now = System.currentTimeMillis();
        long totalElapsed = Math.max(0, now - lastUnplug);

        double rateSotMin;
        double rateMixedMin;
        float drainRatePerHour = 0f;

        if (drop >= 2 && sotMillis >= 5 * 60 * 1000L && totalElapsed > 0) {
            // Real measured rate based on actual device usage
            rateSotMin = (sotMillis / (double) drop) / 60000.0;
            // Clamp between 3.5 min/1% (heavy gaming) and 10.0 min/1% (reading/dark mode)
            rateSotMin = Math.max(3.5, Math.min(10.0, rateSotMin));

            rateMixedMin = (totalElapsed / (double) drop) / 60000.0;
            rateMixedMin = Math.max(rateSotMin, Math.min(30.0, rateMixedMin));

            drainRatePerHour = (float) ((drop * 3600000.0) / Math.max(60000L, totalElapsed));
        } else {
            // Intelligent Xiaomi standard baseline (~6 min/1% for 10h total SOT)
            rateSotMin = 6.0;
            rateMixedMin = 15.0; // ~25h total standby/mixed
            if (drop > 0 && totalElapsed > 10 * 60 * 1000L) {
                drainRatePerHour = (float) ((drop * 3600000.0) / totalElapsed);
            }
        }

        long remainSotMins = Math.round(currentLevel * rateSotMin);
        long remainMixedMins = Math.round(currentLevel * rateMixedMin);

        String formattedSot = formatDuration(remainSotMins * 60000L);
        String formattedMixed = formatDuration(remainMixedMins * 60000L);
        String formattedRate = drainRatePerHour > 0.05f 
                ? String.format(Locale.getDefault(), "~%.1f%% / giờ", drainRatePerHour) 
                : "Đang tính...";

        return new BatteryEstimate(remainSotMins, remainMixedMins, drainRatePerHour, formattedSot, formattedMixed, formattedRate);
    }
}
