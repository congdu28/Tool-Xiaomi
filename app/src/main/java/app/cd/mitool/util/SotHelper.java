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

        // If tracked SOT is 0 (e.g. app freshly installed or first run), fallback to UsageStats if available
        if (accumulated <= 0 && BatteryUsageHelper.hasUsageAccessPermission(context)) {
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
    }
}
