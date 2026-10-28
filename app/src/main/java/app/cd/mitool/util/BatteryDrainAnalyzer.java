package app.cd.mitool.util;

import java.util.List;

public final class BatteryDrainAnalyzer {
    private BatteryDrainAnalyzer() {}
    public static class Point {
        public final long timestamp;
        public final int level;
        public final boolean isCharging;
        public Point(long time, int percentage, boolean charging) {
            timestamp = time; level = percentage; isCharging = charging;
        }
    }
    public static final class Result {
        public final Point start, end;
        public final double percentPerHour;
        Result(Point from, Point to) {
            start = from; end = to;
            percentPerHour = (from.level - to.level) * 3600000.0 / (to.timestamp - from.timestamp);
        }
    }
    public static Result fastest(List<? extends Point> points) {
        Result best = null;
        for (int i = 0; i < points.size(); i++) {
            Point start = points.get(i);
            if (start.isCharging || start.level < 0 || start.level > 100) continue;
            for (int j = i + 1; j < points.size(); j++) {
                Point prev = points.get(j - 1), end = points.get(j);
                long gap = end.timestamp - prev.timestamp;
                long duration = end.timestamp - start.timestamp;
                // Do not infer a continuous discharge across recharges, time jumps or missing data.
                if (end.isCharging || end.level < 0 || end.level > prev.level || gap <= 0
                        || gap > 30 * 60000L || duration > 4 * 3600000L) break;
                if (duration < 15 * 60000L) continue;
                Result candidate = new Result(start, end);
                if (best == null || candidate.percentPerHour > best.percentPerHour) best = candidate;
            }
        }
        return best;
    }
}
