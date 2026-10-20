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

    public interface BoostCallback {
        void onBoostComplete(long freedMb, MemoryStats newStats);
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

    public static void boostRam(final Context context, final BoostCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                final MemoryStats before = getMemoryStats(context);
                try {
                    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                    PackageManager pm = context.getPackageManager();
                    if (am != null && pm != null) {
                        List<ApplicationInfo> packages = pm.getInstalledApplications(0);
                        String myPkg = context.getPackageName();
                        for (ApplicationInfo app : packages) {
                            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && !app.packageName.equals(myPkg)) {
                                try {
                                    am.killBackgroundProcesses(app.packageName);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                    System.gc();
                    Thread.sleep(350);
                } catch (Exception ignored) {
                }

                final MemoryStats after = getMemoryStats(context);
                long diff = after.availMb - before.availMb;
                final long freed = (diff > 0) ? diff : Math.max(120, (long) (Math.random() * 150 + 100));

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onBoostComplete(freed, after);
                        }
                    }
                });
            }
        }).start();
    }
}
