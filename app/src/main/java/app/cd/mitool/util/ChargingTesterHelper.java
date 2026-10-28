package app.cd.mitool.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.cd.mitool.R;

public class ChargingTesterHelper {

    public static class RealtimeChargingState {
        public boolean isCharging;
        public int plugType;
        public int level;
        public float voltageV;
        public int currentMa;
        public float powerW;
        public float estimatedAdapterWatt;
        public float temperatureC;
        public String protocolName;
        public String protocolBadge;
        public int protocolColor;
        public String statusDescription;

        public RealtimeChargingState() {
            this.protocolName = "Đang dùng pin";
            this.protocolBadge = "🔋 DÙNG PIN";
            this.protocolColor = R.color.text_secondary;
            this.statusDescription = "Chưa cắm sạc";
        }
    }

    public static class CableTestResult {
        public final int score; // 1 to 5
        public final String stars;
        public final String title;
        public final String description;
        public final int color;
        public final float avgPowerW;
        public final float maxPowerW;
        public final int avgCurrentMa;
        public final int maxCurrentMa;
        public final int minCurrentMa;
        public final float avgVoltageV;
        public final int stabilityPercent;
        public final boolean isUnstable;

        public CableTestResult(int score, String stars, String title, String description, int color,
                               float avgPowerW, float maxPowerW, int avgCurrentMa, int maxCurrentMa,
                               int minCurrentMa, float avgVoltageV, int stabilityPercent, boolean isUnstable) {
            this.score = score;
            this.stars = stars;
            this.title = title;
            this.description = description;
            this.color = color;
            this.avgPowerW = avgPowerW;
            this.maxPowerW = maxPowerW;
            this.avgCurrentMa = avgCurrentMa;
            this.maxCurrentMa = maxCurrentMa;
            this.minCurrentMa = minCurrentMa;
            this.avgVoltageV = avgVoltageV;
            this.stabilityPercent = stabilityPercent;
            this.isUnstable = isUnstable;
        }
    }

    public interface CableTestListener {
        void onProgress(int currentSample, int totalSamples, RealtimeChargingState liveData);
        void onComplete(CableTestResult result);
        void onError(String message);
    }

    private static final Handler testHandler = new Handler(Looper.getMainLooper());
    private static Runnable testRunnable = null;
    private static boolean isTestingActive = false;

    /**
     * Reads realtime charging and sensor data, identifying the charging protocol accurately.
     */
    public static RealtimeChargingState getRealtimeChargingState(Context context) {
        RealtimeChargingState state = new RealtimeChargingState();
        if (context == null) return state;

        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            state.level = (level >= 0 && scale > 0) ? Math.round((level / (float) scale) * 100) : 0;

            int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            state.isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;

            state.plugType = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            int rawVoltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            state.voltageV = rawVoltage > 0 ? (rawVoltage / 1000.0f) : 0f;

            int rawTemp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            state.temperatureC = rawTemp / 10.0f;
        }

        // Current in mA
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int currentNow = 0;
        if (bm != null) {
            currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        }

        int currentMa = BatteryMeasurements.milliAmps(currentNow);

        state.currentMa = currentMa;
        state.powerW = (state.voltageV * state.currentMa) / 1000.0f;
        // Adapter power factoring in ~88% internal charging circuitry conversion efficiency
        state.estimatedAdapterWatt = state.powerW > 0 ? (state.powerW / 0.88f) : 0f;

        state.estimatedAdapterWatt = 0; // Battery-side power does not identify adapter output.
        if (!state.isCharging) {
            state.protocolName = "Đang dùng pin";
            state.protocolBadge = "Đang dùng pin";
            state.protocolColor = R.color.text_secondary;
            state.statusDescription = "Số đo phía pin, không phải điện năng tiêu thụ riêng của ứng dụng.";
        } else {
            state.protocolName = "Đang sạc";
            state.protocolBadge = state.currentMa > 0 ? "Số đo phía pin" : "Chưa có số đo";
            state.protocolColor = R.color.neon_green;
            state.statusDescription = "Chưa xác định giao thức hoặc công suất đầu ra của bộ sạc.";
        }

        return state;
    }

    /**
     * Starts a 10-second cable and charger quality test session.
     */
    public static synchronized void startCableTest(final Context context, final CableTestListener listener) {
        if (context == null || listener == null) return;
        stopCableTest();

        final RealtimeChargingState initial = getRealtimeChargingState(context);
        if (!initial.isCharging) {
            listener.onError("Vui lòng cắm dây sạc vào điện thoại trước khi bấm kiểm tra củ cáp!");
            return;
        }

        isTestingActive = true;
        final int TOTAL_SAMPLES = 10;
        final List<Integer> currentSamples = new ArrayList<>();
        final List<Float> voltageSamples = new ArrayList<>();
        final List<Float> powerSamples = new ArrayList<>();

        testRunnable = new Runnable() {
            int currentStep = 0;

            @Override
            public void run() {
                if (!isTestingActive) return;

                RealtimeChargingState live = getRealtimeChargingState(context);
                if (!live.isCharging) {
                    isTestingActive = false;
                    listener.onError("Dây sạc vừa bị rút ra giữa chừng! Bài kiểm tra đã bị dừng lại.");
                    return;
                }

                if (live.currentMa <= 0 || live.voltageV <= 0) {
                    isTestingActive = false;
                    listener.onError("Thiết bị chưa cung cấp đủ số đo dòng điện/điện áp để đánh giá.");
                    return;
                }
                currentStep++;
                currentSamples.add(live.currentMa);
                voltageSamples.add(live.voltageV);
                powerSamples.add(live.powerW);

                listener.onProgress(currentStep, TOTAL_SAMPLES, live);

                if (currentStep < TOTAL_SAMPLES) {
                    testHandler.postDelayed(this, 1000);
                } else {
                    isTestingActive = false;
                    CableTestResult result = evaluateCableQuality(currentSamples, voltageSamples, powerSamples);
                    listener.onComplete(result);
                }
            }
        };

        testHandler.post(testRunnable);
    }

    public static synchronized void stopCableTest() {
        isTestingActive = false;
        if (testRunnable != null) {
            testHandler.removeCallbacks(testRunnable);
            testRunnable = null;
        }
    }

    public static boolean isTesting() {
        return isTestingActive;
    }

    private static CableTestResult evaluateCableQuality(List<Integer> currents, List<Float> voltages, List<Float> powers) {
        if (currents == null || currents.isEmpty()) {
            return new CableTestResult(1, "⭐", "Không xác định", "Không đủ dữ liệu mẫu để đánh giá.", R.color.text_muted, 0, 0, 0, 0, 0, 0, 0, true);
        }

        int minCurrent = Integer.MAX_VALUE;
        int maxCurrent = 0;
        long totalCurrent = 0;

        for (int c : currents) {
            if (c < minCurrent) minCurrent = c;
            if (c > maxCurrent) maxCurrent = c;
            totalCurrent += c;
        }
        int avgCurrent = Math.round(totalCurrent / (float) currents.size());

        float totalVolt = 0;
        for (float v : voltages) totalVolt += v;
        float avgVolt = totalVolt / voltages.size();

        float minPower = Float.MAX_VALUE;
        float maxPower = 0;
        float totalPower = 0;
        for (float p : powers) {
            if (p < minPower) minPower = p;
            if (p > maxPower) maxPower = p;
            totalPower += p;
        }
        float avgPower = totalPower / powers.size();

        // Calculate stability percentage: (1 - (max - min) / max) * 100
        int stability = 100;
        if (maxCurrent > 0) {
            float variance = (maxCurrent - minCurrent) / (float) maxCurrent;
            stability = Math.max(0, Math.min(100, Math.round((1.0f - variance) * 100)));
        }

        boolean isUnstable = (stability < 60) || (minCurrent < 400 && maxCurrent > 1500);

        return new CableTestResult(0, "Tham khảo", "Kết quả đo dòng điện",
                "Dòng điện thay đổi theo mức pin, nhiệt độ và tác vụ. Kết quả này chưa xác định lỗi cáp hay bộ sạc.",
                R.color.text_secondary, avgPower, maxPower, avgCurrent, maxCurrent, minCurrent, avgVolt, stability, isUnstable);
    }

}
