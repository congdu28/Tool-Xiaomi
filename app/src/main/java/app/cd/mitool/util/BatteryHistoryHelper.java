package app.cd.mitool.util;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BatteryHistoryHelper {

    public static class BatteryPoint extends BatteryDrainAnalyzer.Point {
        public BatteryPoint(long timestamp, int level, boolean isCharging) {
            super(timestamp, level, isCharging);
        }
    }

    public static class DrainAnalysis {
        public final String summary;
        public final int maxDropInPeriod;
        public final String maxDropTimeSpan;
        public final boolean hasAbnormalDrain;

        public DrainAnalysis(String summary, int maxDropInPeriod, String maxDropTimeSpan, boolean hasAbnormalDrain) {
            this.summary = summary;
            this.maxDropInPeriod = maxDropInPeriod;
            this.maxDropTimeSpan = maxDropTimeSpan;
            this.hasAbnormalDrain = hasAbnormalDrain;
        }
    }

    private static final String PREF_KEY_HISTORY = "pref_battery_history_measured_v3";
    private static final long MAX_AGE_MS = 36 * 3600 * 1000L; // Retain up to 36 hours
    private static final long MIN_INTERVAL_MS = 15 * 60 * 1000L; // Record at least every 15 mins

    private static long lastRecordedElapsed = -1;
    private static int lastRecordedLevel = -1;
    private static boolean lastRecordedCharging;

    /**
     * Records a battery level snapshot.
     */
    public static synchronized void recordSnapshot(Context context, int level, boolean isCharging) {
        if (context == null || level < 0 || level > 100) return;
        long elapsedNow = android.os.SystemClock.elapsedRealtime();
        if (lastRecordedElapsed >= 0 && elapsedNow >= lastRecordedElapsed
                && elapsedNow - lastRecordedElapsed < MIN_INTERVAL_MS
                && Math.abs(lastRecordedLevel - level) < 2 && lastRecordedCharging == isCharging) return;
        PreferencesHelper prefs = new PreferencesHelper(context);
        long now = System.currentTimeMillis();

        List<BatteryPoint> list = getRawHistory(prefs);
        if (!list.isEmpty()) {
            BatteryPoint last = list.get(list.size() - 1);
            long elapsed = now - last.timestamp;
            if (elapsed < 0) { list.clear(); elapsed = MIN_INTERVAL_MS; }
            int levelDiff = Math.abs(last.level - level);
            // Don't duplicate if under interval and no meaningful change
            if (elapsed < MIN_INTERVAL_MS && levelDiff < 2 && last.isCharging == isCharging) {
                lastRecordedElapsed = elapsedNow - Math.max(0, elapsed);
                lastRecordedLevel = last.level;
                lastRecordedCharging = last.isCharging;
                return;
            }
        }

        list.add(new BatteryPoint(now, level, isCharging));

        // Purge points older than 36 hours
        long cutoff = now - MAX_AGE_MS;
        List<BatteryPoint> pruned = new ArrayList<>();
        for (BatteryPoint p : list) {
            if (p.timestamp >= cutoff) {
                pruned.add(p);
            }
        }

        saveRawHistory(prefs, pruned);
        lastRecordedElapsed = elapsedNow;
        lastRecordedLevel = level;
        lastRecordedCharging = isCharging;
    }

    /** Returns recorded observations only. v3 intentionally excludes old synthetic history. */
    public static synchronized List<BatteryPoint> getHistory(Context context) {
        if (context == null) return new ArrayList<>();
        BatteryHelper.BatteryInfo battery = BatteryHelper.getBatteryInfo(context);
        recordSnapshot(context, battery.level, battery.isCharging);
        long now = System.currentTimeMillis();
        List<BatteryPoint> result = new ArrayList<>();
        for (BatteryPoint point : getRawHistory(new PreferencesHelper(context))) {
            if (point.timestamp >= now - 24 * 3600000L && point.timestamp <= now) result.add(point);
        }
        return result;
    }

    /**
     * Analyzes battery drain across the points to detect intervals of steep battery drop.
     */
    public static DrainAnalysis analyzeDrain(List<BatteryPoint> points) {
        if (points == null || points.size() < 2) {
            return new DrainAnalysis("Đang thu thập dữ liệu sụt pin...", 0, "", false);
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        BatteryDrainAnalyzer.Result analysis = BatteryDrainAnalyzer.fastest(points);
        if (analysis == null) return new DrainAnalysis("Chưa đủ khoảng đo liên tục để phân tích hao pin.", 0, "", false);
        BatteryDrainAnalyzer.Point peakStart = analysis.start, peakEnd = analysis.end;
        double peakRate = analysis.percentPerHour;
        int drop = peakStart.level - peakEnd.level;
        String span = timeFormat.format(new Date(peakStart.timestamp)) + " - " + timeFormat.format(new Date(peakEnd.timestamp));
        boolean rapid = MonitoringPolicy.isRapidDrain(drop, peakEnd.timestamp - peakStart.timestamp);
        return new DrainAnalysis((rapid ? "⚠️ Tụt pin nhanh: " : "Khoảng đo hao pin cao nhất: ")
                + span + String.format(Locale.getDefault(), " (-%d%%, ~%.1f%%/giờ). ", drop, peakRate)
                + "Chưa xác định nguyên nhân hoặc ứng dụng gây hao pin.", drop, span, rapid);
    }

    private static List<BatteryPoint> getRawHistory(PreferencesHelper prefs) {
        List<BatteryPoint> list = new ArrayList<>();
        try {
            String jsonStr = prefs.getCustomString(PREF_KEY_HISTORY, "[]");
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                long t = obj.getLong("t");
                int l = obj.getInt("l");
                boolean c = obj.optBoolean("c", false);
                list.add(new BatteryPoint(t, l, c));
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    private static void saveRawHistory(PreferencesHelper prefs, List<BatteryPoint> list) {
        try {
            JSONArray array = new JSONArray();
            for (BatteryPoint p : list) {
                JSONObject obj = new JSONObject();
                obj.put("t", p.timestamp);
                obj.put("l", p.level);
                obj.put("c", p.isCharging);
                array.put(obj);
            }
            prefs.setCustomString(PREF_KEY_HISTORY, array.toString());
        } catch (Throwable ignored) {
        }
    }
}
