package app.cd.mitool.util;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatteryUsageHelper {

    public static final int FILTER_SINCE_UNPLUG = 0;
    public static final int FILTER_TODAY = 1;

    public static class AppDrainItem {
        public int rank;
        public final String packageName;
        public final String appName;
        public final Drawable icon;
        public final long foregroundTimeMillis;
        public int percentage;
        public float estimatedBatteryDrain; // estimated % of battery consumed
        public final String formattedTime;
        public boolean isStopped = false;

        public AppDrainItem(int rank, String packageName, String appName, Drawable icon, long foregroundTimeMillis, int percentage, float estimatedBatteryDrain, String formattedTime) {
            this.rank = rank;
            this.packageName = packageName;
            this.appName = appName;
            this.icon = icon;
            this.foregroundTimeMillis = foregroundTimeMillis;
            this.percentage = percentage;
            this.estimatedBatteryDrain = estimatedBatteryDrain;
            this.formattedTime = formattedTime;
        }
    }

    public static class DrainQueryResult {
        public final List<AppDrainItem> items;
        public final String timeRangeLabel;
        public final long totalForegroundMillis;

        public DrainQueryResult(List<AppDrainItem> items, String timeRangeLabel, long totalForegroundMillis) {
            this.items = items;
            this.timeRangeLabel = timeRangeLabel;
            this.totalForegroundMillis = totalForegroundMillis;
        }
    }

    /**
     * Check if user granted PACKAGE_USAGE_STATS permission to our app.
     */
    public static boolean hasUsageAccessPermission(Context context) {
        if (context == null) return false;
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) return false;
            int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.getPackageName()
            );
            return (mode == AppOpsManager.MODE_ALLOWED);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Open system settings for granting Usage Access.
     */
    public static void openUsageAccessSettings(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Query top battery draining apps with specified filter (FILTER_SINCE_UNPLUG or FILTER_TODAY).
     */
    public static DrainQueryResult getTopDrainAppsDetailed(Context context, int filterType, int limit) {
        List<AppDrainItem> resultList = new ArrayList<>();
        if (context == null || !hasUsageAccessPermission(context)) {
            return new DrainQueryResult(resultList, "Chưa cấp quyền đọc dữ liệu", 0);
        }

        try {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return new DrainQueryResult(resultList, "Không tìm thấy UsageStatsManager", 0);

            PreferencesHelper prefs = new PreferencesHelper(context);
            long now = System.currentTimeMillis();
            long start;
            String rangeLabel;

            long unplugTime = prefs.getSotLastUnplugTime();

            if (filterType == FILTER_SINCE_UNPLUG) {
                // If unplugTime is missing or invalid, fallback to current session start or last 1 hour
                if (unplugTime <= 0 || unplugTime > now) {
                    long sot = SotHelper.getScreenOnTimeMillis(context);
                    unplugTime = (sot > 0) ? (now - sot) : (now - (60 * 60 * 1000L));
                    prefs.setSotLastUnplugTime(unplugTime);
                }
                start = unplugTime;
                long elapsed = now - start;
                long mins = elapsed / (60 * 1000L);
                if (mins < 1) {
                    rangeLabel = "Kể từ khi rút sạc (Vừa mới rút)";
                } else if (mins < 60) {
                    rangeLabel = "Kể từ khi rút sạc (" + mins + " phút trước)";
                } else {
                    rangeLabel = "Kể từ khi rút sạc (" + (mins / 60) + "h " + (mins % 60) + "p trước)";
                }
            } else {
                // Today since 00:00:00
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTimeInMillis();
                rangeLabel = "Toàn bộ trong ngày hôm nay (từ 00:00)";
            }

            // Collect accurate foreground time via UsageEvents first
            Map<String, Long> aggregated = getForegroundTimes(usm, start, now);

            // Exclude our own app
            String myPkg = context.getPackageName();
            aggregated.remove(myPkg);

            PackageManager pm = context.getPackageManager();

            // Candidate apps with valid launchable activity
            Map<String, Long> validAppTimes = new HashMap<>();
            Map<String, String> appLabels = new HashMap<>();
            Map<String, Drawable> appIcons = new HashMap<>();

            for (Map.Entry<String, Long> entry : aggregated.entrySet()) {
                String pkg = entry.getKey();
                long time = entry.getValue();

                // Only consider apps used for at least 3 seconds
                if (time < 3000) continue;

                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                    // Filter out pure system daemons without launcher intent unless significant (> 1 min)
                    if (pm.getLaunchIntentForPackage(pkg) == null && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        if (time < 60000L) continue;
                    }
                    String label = pm.getApplicationLabel(appInfo).toString();
                    Drawable icon = pm.getApplicationIcon(appInfo);
                    validAppTimes.put(pkg, time);
                    appLabels.put(pkg, label);
                    appIcons.put(pkg, icon);
                } catch (PackageManager.NameNotFoundException ignored) {
                } catch (Throwable t) {
                    validAppTimes.put(pkg, time);
                    appLabels.put(pkg, pkg);
                    appIcons.put(pkg, pm.getDefaultActivityIcon());
                }
            }

            // Calculate total foreground time across all valid apps
            long totalValidTime = 0;
            for (long time : validAppTimes.values()) {
                totalValidTime += time;
            }
            if (totalValidTime <= 0) totalValidTime = 1;

            // Sort descending
            List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(validAppTimes.entrySet());
            Collections.sort(sortedEntries, (o1, o2) -> Long.compare(o2.getValue(), o1.getValue()));

            // Calculate estimated battery drop
            int unplugLevel = prefs.getSotLastUnplugLevel();
            int curLevel = BatteryHelper.getBatteryPercentage(context);
            int batteryDrop = Math.max(0, unplugLevel - curLevel);

            int count = 0;
            for (Map.Entry<String, Long> entry : sortedEntries) {
                if (count >= limit) break;
                String pkg = entry.getKey();
                long time = entry.getValue();

                String appName = appLabels.get(pkg);
                if (appName == null) appName = pkg;
                Drawable icon = appIcons.get(pkg);

                int percentage = Math.min(100, Math.max(1, (int) Math.round((time * 100.0) / totalValidTime)));

                float estBattery;
                if (filterType == FILTER_SINCE_UNPLUG) {
                    if (batteryDrop > 0) {
                        estBattery = Math.round((batteryDrop * (time / (float) totalValidTime)) * 10.0f) / 10.0f;
                    } else {
                        estBattery = 0.0f;
                    }
                } else {
                    // For whole day today: calculate estimated battery consumed based on screen time
                    // Typical Xiaomi modern display uses ~11% battery per 1 hour of screen-on
                    float hours = time / (3600 * 1000.0f);
                    estBattery = Math.round((hours * 11.0f) * 10.0f) / 10.0f;
                }
                String formatted = SotHelper.formatDuration(time);

                count++;
                resultList.add(new AppDrainItem(count, pkg, appName, icon, time, percentage, estBattery, formatted));
            }

            return new DrainQueryResult(resultList, rangeLabel, totalValidTime);

        } catch (Throwable ignored) {
        }

        return new DrainQueryResult(resultList, "Đang tải dữ liệu...", 0);
    }

    /**
     * Compute foreground time within precise time window [start, now] using UsageEvents.
     * Falls back to queryUsageStats if events are empty.
     */
    private static Map<String, Long> getForegroundTimes(UsageStatsManager usm, long start, long now) {
        Map<String, Long> appTimes = new HashMap<>();
        if (usm == null || start >= now) return appTimes;

        try {
            android.app.usage.UsageEvents events = usm.queryEvents(start, now);
            if (events != null && events.hasNextEvent()) {
                android.app.usage.UsageEvents.Event event = new android.app.usage.UsageEvents.Event();
                Map<String, Long> openTimestamps = new HashMap<>();

                while (events.hasNextEvent()) {
                    events.getNextEvent(event);
                    String pkg = event.getPackageName();
                    if (pkg == null) continue;
                    int type = event.getEventType();
                    long ts = event.getTimeStamp();

                    if (type == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
                            || type == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        openTimestamps.put(pkg, Math.max(start, ts));
                    } else if (type == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED
                            || type == android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED
                            || type == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND) {
                        Long startTs = openTimestamps.remove(pkg);
                        if (startTs != null && ts > startTs) {
                            long duration = Math.min(ts - startTs, now - start);
                            Long current = appTimes.get(pkg);
                            appTimes.put(pkg, (current == null ? 0 : current) + duration);
                        }
                    }
                }

                // Any app currently still open in foreground at 'now'
                for (Map.Entry<String, Long> entry : openTimestamps.entrySet()) {
                    long duration = Math.min(now - entry.getValue(), now - start);
                    if (duration > 0) {
                        Long current = appTimes.get(entry.getKey());
                        appTimes.put(entry.getKey(), (current == null ? 0 : current) + duration);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Fallback: If queryEvents returned nothing, query aggregate stats
        if (appTimes.isEmpty()) {
            try {
                List<UsageStats> statsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, now);
                if (statsList != null) {
                    for (UsageStats stat : statsList) {
                        long time = stat.getTotalTimeInForeground();
                        if (time > 1000) {
                            String pkg = stat.getPackageName();
                            Long cur = appTimes.get(pkg);
                            appTimes.put(pkg, (cur == null ? 0 : cur) + time);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return appTimes;
    }

    /**
     * Convenience method to get drain apps list with filter type.
     */
    public static List<AppDrainItem> getDrainApps(Context context, int filterType, int limit) {
        return getTopDrainAppsDetailed(context, filterType, limit).items;
    }

    /**
     * Legacy convenience overload.
     */
    public static List<AppDrainItem> getTopDrainApps(Context context, long sinceTime, int limit) {
        return getTopDrainAppsDetailed(context, FILTER_SINCE_UNPLUG, limit).items;
    }

    /**
     * Force stop an application either via Shizuku (instant background kill) or via System Settings.
     */
    public static void forceStopApp(Context context, String packageName, ShizukuHelper.CommandCallback callback) {
        if (context == null || packageName == null || packageName.isEmpty()) return;

        if (ShizukuHelper.hasShizukuPermission()) {
            ShizukuHelper.forceStopPackage(packageName, callback);
        } else {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                if (callback != null) {
                    callback.onComplete(false, "Đã mở Cài đặt ứng dụng. Hãy chạm 'Buộc dừng'!");
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onComplete(false, "Không thể mở trang ứng dụng: " + e.getMessage());
                }
            }
        }
    }
}
