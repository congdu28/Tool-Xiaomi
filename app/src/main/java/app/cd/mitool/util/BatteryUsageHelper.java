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
            long timeSinceUnplug = now - unplugTime;

            if (filterType == FILTER_SINCE_UNPLUG && unplugTime > 0 && timeSinceUnplug > (10 * 60 * 1000L) && timeSinceUnplug < (48 * 3600 * 1000L)) {
                start = unplugTime;
                long mins = timeSinceUnplug / (60 * 1000L);
                if (mins < 60) {
                    rangeLabel = "Kể từ khi rút sạc (" + mins + " phút trước)";
                } else {
                    rangeLabel = "Kể từ khi rút sạc (" + (mins / 60) + "h " + (mins % 60) + "p trước)";
                }
            } else {
                // Today since 00:00
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                start = cal.getTimeInMillis();
                rangeLabel = (filterType == FILTER_SINCE_UNPLUG)
                        ? "Hôm nay (Do vừa rút sạc chưa lâu)"
                        : "Toàn bộ trong ngày hôm nay";
            }

            Map<String, Long> aggregated = new HashMap<>();

            // 1. First attempt: queryAndAggregateUsageStats
            try {
                Map<String, UsageStats> aggMap = usm.queryAndAggregateUsageStats(start, now);
                if (aggMap != null && !aggMap.isEmpty()) {
                    for (Map.Entry<String, UsageStats> e : aggMap.entrySet()) {
                        long time = e.getValue().getTotalTimeInForeground();
                        if (time > 1000) {
                            aggregated.put(e.getKey(), time);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            // 2. Complement / fallback with queryUsageStats
            if (aggregated.isEmpty()) {
                List<UsageStats> statsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, now);
                if (statsList != null) {
                    for (UsageStats stat : statsList) {
                        long time = stat.getTotalTimeInForeground();
                        if (time > 1000) {
                            String pkg = stat.getPackageName();
                            Long cur = aggregated.get(pkg);
                            aggregated.put(pkg, (cur == null ? 0 : cur) + time);
                        }
                    }
                }
            }

            // Exclude our own app
            String myPkg = context.getPackageName();
            aggregated.remove(myPkg);

            PackageManager pm = context.getPackageManager();
            long totalUsageTime = 0;

            List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(aggregated.entrySet());
            Collections.sort(sortedEntries, (o1, o2) -> Long.compare(o2.getValue(), o1.getValue()));

            for (Map.Entry<String, Long> entry : sortedEntries) {
                totalUsageTime += entry.getValue();
            }

            if (totalUsageTime <= 0) totalUsageTime = 1;

            // Calculate estimated battery drop
            int unplugLevel = prefs.getSotLastUnplugLevel();
            int curLevel = BatteryHelper.getBatteryPercentage(context);
            int batteryDrop = Math.max(1, unplugLevel - curLevel);

            int count = 0;
            for (Map.Entry<String, Long> entry : sortedEntries) {
                if (count >= limit) break;
                String pkg = entry.getKey();
                long time = entry.getValue();

                // Skip internal android system processes that have no launchable UI unless significant
                if (time < 3000) continue;

                String appName = pkg;
                Drawable icon = null;
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                    appName = pm.getApplicationLabel(appInfo).toString();
                    icon = pm.getApplicationIcon(appInfo);
                } catch (PackageManager.NameNotFoundException ignored) {
                    continue;
                } catch (Throwable t) {
                    icon = pm.getDefaultActivityIcon();
                }

                int percentage = Math.min(100, Math.max(1, (int) Math.round((time * 100.0) / totalUsageTime)));
                float estBattery = Math.round((batteryDrop * (time / (float) totalUsageTime)) * 10.0f) / 10.0f;
                String formatted = SotHelper.formatDuration(time);

                count++;
                resultList.add(new AppDrainItem(count, pkg, appName, icon, time, percentage, estBattery, formatted));
            }

            return new DrainQueryResult(resultList, rangeLabel, totalUsageTime);

        } catch (Throwable ignored) {
        }

        return new DrainQueryResult(resultList, "Đang tải dữ liệu...", 0);
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
                    callback.onComplete(true, "Đã mở Cài đặt ứng dụng. Hãy chạm 'Buộc dừng'!");
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onComplete(false, "Không thể mở trang ứng dụng: " + e.getMessage());
                }
            }
        }
    }
}
