package app.cd.mitool.util;

import android.app.ActivityManager;
import android.content.Context;


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

}
