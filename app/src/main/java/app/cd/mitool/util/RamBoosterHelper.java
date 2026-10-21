package app.cd.mitool.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

public class RamBoosterHelper {

    public static class MemoryStats {
        public long totalMb;
        public long availMb;
        public long usedMb;
        public int percentUsed;

        public MemoryStats(long totalMb, long availMb, long usedMb, int percentUsed) {
            this.totalMb = totalMb;
            this.availMb = availMb;
            this.usedMb = usedMb;
            this.percentUsed = percentUsed;
        }
    }

    public static class BoostResult {
        public final long freedMb;
        public final MemoryStats beforeStats;
        public final MemoryStats afterStats;
        public final int killedCount;
        public final boolean usedShizuku;

        public BoostResult(long freedMb, MemoryStats beforeStats, MemoryStats afterStats, int killedCount, boolean usedShizuku) {
            this.freedMb = freedMb;
            this.beforeStats = beforeStats;
            this.afterStats = afterStats;
            this.killedCount = killedCount;
            this.usedShizuku = usedShizuku;
        }
    }

    public interface BoostCallback {
        void onBoostComplete(long freedMb, MemoryStats newStats);
    }

    public interface DetailedBoostCallback {
        void onBoostComplete(BoostResult result);
    }

    public static MemoryStats getMemoryStats(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                long totalMb = mi.totalMem / (1024 * 1024);
                long availMb = mi.availMem / (1024 * 1024);
                long usedMb = Math.max(0, totalMb - availMb);
                int percent = (totalMb > 0) ? (int) ((usedMb * 100) / totalMb) : 0;
                return new MemoryStats(totalMb, availMb, usedMb, percent);
            }
        } catch (Exception ignored) {
        }
        return new MemoryStats(4096, 2048, 2048, 50);
    }

    /**
     * Synchronously execute RAM clean and return BoostResult (can be called from background threads / widgets).
     */
    public static BoostResult boostRamSync(Context context) {
        final MemoryStats before = getMemoryStats(context);
        int killedCount = 0;
        boolean usedShizuku = false;

        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            PackageManager pm = context.getPackageManager();

            // 1. Shizuku Deep Boost if available
            if (ShizukuHelper.hasShizukuPermission()) {
                try {
                    ShizukuHelper.executeCommand("am kill-all && pm trim-caches 999999999");
                    usedShizuku = true;
                } catch (Throwable ignored) {
                }
            }

            // 2. Standard killBackgroundProcesses on non-system third-party packages
            if (am != null && pm != null) {
                List<ApplicationInfo> packages = pm.getInstalledApplications(0);
                String myPkg = context.getPackageName();
                for (ApplicationInfo app : packages) {
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && !app.packageName.equals(myPkg)) {
                        try {
                            am.killBackgroundProcesses(app.packageName);
                            killedCount++;
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            // 3. JVM Garbage collection
            System.gc();
            Thread.sleep(300);
        } catch (Exception ignored) {
        }

        final MemoryStats after = getMemoryStats(context);
        long diff = after.availMb - before.availMb;
        long freed = Math.max(0, diff);

        return new BoostResult(freed, before, after, killedCount, usedShizuku);
    }

    public static void boostRam(final Context context, final BoostCallback callback) {
        boostRamDetailed(context, result -> {
            if (callback != null) {
                callback.onBoostComplete(result.freedMb, result.afterStats);
            }
        });
    }

    public static void boostRamDetailed(final Context context, final DetailedBoostCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            final BoostResult result = boostRamSync(context);
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onBoostComplete(result);
                }
            });
        }).start();
    }
}
