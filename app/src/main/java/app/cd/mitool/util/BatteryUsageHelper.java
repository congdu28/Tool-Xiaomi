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

    public static class AppDrainItem {
        public final String packageName;
        public final String appName;
        public final Drawable icon;
        public final long foregroundTimeMillis;
        public int percentage;
        public final String formattedTime;
        public boolean isStopped = false;

        public AppDrainItem(String packageName, String appName, Drawable icon, long foregroundTimeMillis, int percentage, String formattedTime) {
            this.packageName = packageName;
            this.appName = appName;
            this.icon = icon;
            this.foregroundTimeMillis = foregroundTimeMillis;
            this.percentage = percentage;
            this.formattedTime = formattedTime;
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
     * Retrieve top apps consuming battery/foreground time since given timestamp.
     * Note: Should be called off main thread.
     */
    public static List<AppDrainItem> getTopDrainApps(Context context, long sinceTime, int limit) {
        List<AppDrainItem> result = new ArrayList<>();
        if (context == null || !hasUsageAccessPermission(context)) {
            return result;
        }

        try {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return result;

            long now = System.currentTimeMillis();
            long start = sinceTime;
            // Fallback to start of today if unplug time is invalid or too old (> 48h)
            if (start <= 0 || (now - start) > (48 * 3600 * 1000L) || start > now) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                start = cal.getTimeInMillis();
            }

            List<UsageStats> statsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, now);
            if (statsList == null || statsList.isEmpty()) {
                return result;
            }

            // Aggregate by package
            Map<String, Long> aggregated = new HashMap<>();
            for (UsageStats stat : statsList) {
                long time = stat.getTotalTimeInForeground();
                if (time > 1000) { // At least 1 second
                    String pkg = stat.getPackageName();
                    Long current = aggregated.get(pkg);
                    aggregated.put(pkg, (current == null ? 0 : current) + time);
                }
            }

            // Exclude our own app and pure system components with no UI
            String myPkg = context.getPackageName();
            aggregated.remove(myPkg);

            PackageManager pm = context.getPackageManager();
            long totalUsageTime = 0;

            List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(aggregated.entrySet());
            // Sort descending
            Collections.sort(sortedEntries, new Comparator<Map.Entry<String, Long>>() {
                @Override
                public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                    return Long.compare(o2.getValue(), o1.getValue());
                }
            });

            for (Map.Entry<String, Long> entry : sortedEntries) {
                totalUsageTime += entry.getValue();
            }

            if (totalUsageTime <= 0) totalUsageTime = 1;

            int count = 0;
            for (Map.Entry<String, Long> entry : sortedEntries) {
                if (count >= limit) break;
                String pkg = entry.getKey();
                long time = entry.getValue();

                // Only consider apps with at least 5 seconds of usage
                if (time < 5000) continue;

                String appName = pkg;
                Drawable icon = null;
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                    appName = pm.getApplicationLabel(appInfo).toString();
                    icon = pm.getApplicationIcon(appInfo);
                } catch (PackageManager.NameNotFoundException ignored) {
                    // Uninstalled or hidden package, continue
                    continue;
                } catch (Throwable t) {
                    // Fallback icon
                    icon = pm.getDefaultActivityIcon();
                }

                int percentage = Math.min(100, Math.max(1, (int) Math.round((time * 100.0) / totalUsageTime)));
                String formatted = SotHelper.formatDuration(time);

                result.add(new AppDrainItem(pkg, appName, icon, time, percentage, formatted));
                count++;
            }

        } catch (Throwable ignored) {
        }

        return result;
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
