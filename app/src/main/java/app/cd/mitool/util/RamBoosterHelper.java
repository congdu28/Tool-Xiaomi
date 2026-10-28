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
        return new MemoryStats(0, 0, 0, 0);
    }

    /**
     * Synchronously execute RAM clean and return BoostResult (can be called from background threads / widgets).
     */
    public static BoostResult boostRamSync(Context context) {
        MemoryStats snapshot = getMemoryStats(context);
        return new BoostResult(0, snapshot, snapshot, 0, false);
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
