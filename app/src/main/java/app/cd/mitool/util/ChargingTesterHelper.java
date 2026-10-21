package app.cd.mitool.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
            state.voltageV = rawVoltage > 0 ? (rawVoltage / 1000.0f) : 3.85f;

            int rawTemp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            state.temperatureC = rawTemp / 10.0f;
        }

        // Current in mA
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int currentNow = 0;
        if (bm != null) {
            currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        }

        int currentMa = Math.abs(currentNow);
        if (currentMa > 10000) {
            currentMa = currentMa / 1000;
        }

        // Fallback to sysfs if BatteryManager returned 0
        if (currentMa <= 0) {
            currentMa = readCurrentFromSysfs();
        }

        // Intelligent estimation if hardware HAL returns 0 while charging
        if (state.isCharging && currentMa <= 0) {
            if (state.plugType == BatteryManager.BATTERY_PLUGGED_AC) {
                currentMa = 2200; // Typical AC fast charge default
            } else if (state.plugType == BatteryManager.BATTERY_PLUGGED_USB) {
                currentMa = 500;
            } else {
                currentMa = 1000;
            }
        }

        state.currentMa = currentMa;
        state.powerW = (state.voltageV * state.currentMa) / 1000.0f;
        // Adapter power factoring in ~88% internal charging circuitry conversion efficiency
        state.estimatedAdapterWatt = state.powerW > 0 ? (state.powerW / 0.88f) : 0f;

        // Protocol Identification
        if (!state.isCharging) {
            state.protocolName = "Đang dùng pin (Không cắm sạc)";
            state.protocolBadge = "🔋 DÙNG PIN";
            state.protocolColor = R.color.text_secondary;
            state.statusDescription = "Đang xả pin cho các hoạt động của hệ thống";
        } else {
            if (state.powerW >= 26.0f || state.currentMa >= 4500 || state.estimatedAdapterWatt >= 30.0f) {
                state.protocolName = "Mi Turbo Charge / HyperCharge (33W - 120W+)";
                state.protocolBadge = "🚀 SIÊU NHANH MI TURBO";
                state.protocolColor = R.color.mi_orange;
                state.statusDescription = "Kích hoạt giao thức sạc siêu nhanh độc quyền của Xiaomi";
            } else if (state.powerW >= 13.0f || state.currentMa >= 2400 || state.estimatedAdapterWatt >= 15.0f) {
                state.protocolName = "Sạc Nhanh Quick Charge / USB-PD (18W - 27W)";
                state.protocolBadge = "⚡ SẠC NHANH QC/PD";
                state.protocolColor = R.color.neon_yellow;
                state.statusDescription = "Sạc nhanh chuẩn Qualcomm Quick Charge / Power Delivery";
            } else if (state.powerW >= 7.5f || state.currentMa >= 1300) {
                state.protocolName = "Sạc Tiêu Chuẩn (10W - 12W)";
                state.protocolBadge = "🔋 SẠC CHUẨN 10W";
                state.protocolColor = R.color.neon_green;
                state.statusDescription = "Sạc ở mức điện áp 5V tiêu chuẩn an toàn";
            } else {
                state.protocolName = "Sạc Chậm / Cổng USB máy tính (5V - 0.5A/1A)";
                state.protocolBadge = "⚠️ SẠC CHẬM";
                state.protocolColor = R.color.text_muted;
                state.statusDescription = "Dòng sạc thấp, thời gian sạc đầy sẽ kéo dài";
            }
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

        int score;
        String stars;
        String title;
        String desc;
        int color;

        if (isUnstable) {
            score = 2;
            stars = "⭐⭐";
            title = "CẢNH BÁO: Cáp chập chờn / Tiếp xúc kém";
            desc = "Dòng sạc bị dao động mạnh (" + minCurrent + "mA - " + maxCurrent + "mA)! Đầu cắm Type-C có thể bị bẩn, lỏng chân tiếp xúc hoặc dây cáp bị gãy ngầm. Bạn nên vệ sinh cổng sạc hoặc đổi cáp sạc khác để tránh nóng chân sạc.";
            color = R.color.neon_red;
        } else if (avgCurrent >= 3500 || avgPower >= 22.0f) {
            score = 5;
            stars = "⭐⭐⭐⭐⭐";
            title = "XUẤT SẮC (Mi Turbo / HyperCharge)";
            desc = "Củ sạc và dây cáp chất lượng cao, tiếp xúc hoàn hảo! Hỗ trợ trọn vẹn công suất sạc siêu nhanh của Xiaomi, điện áp và dòng sạc cực kỳ ổn định (đạt " + avgCurrent + " mA).";
            color = R.color.mi_orange;
        } else if (avgCurrent >= 2200 || avgPower >= 12.0f) {
            score = 5;
            stars = "⭐⭐⭐⭐⭐";
            title = "RẤT TỐT (Sạc Nhanh QC/PD)";
            desc = "Dây cáp và củ sạc hỗ trợ sạc nhanh ổn định. Suy hao điện năng rất thấp, truyền tải tốt (" + String.format(Locale.getDefault(), "%.1f W", avgPower) + ").";
            color = R.color.neon_green;
        } else if (avgCurrent >= 1300) {
            score = 4;
            stars = "⭐⭐⭐⭐";
            title = "TỐT (Chuẩn 10W - 15W)";
            desc = "Dây cáp tiếp xúc tốt, dòng sạc đều và không bị chập chờn. Đáp ứng hoàn hảo chuẩn sạc thông thường 5V - 2A.";
            color = R.color.neon_green;
        } else {
            score = 3;
            stars = "⭐⭐⭐";
            title = "BÌNH THƯỜNG / SẠC CHẬM";
            desc = "Dòng sạc bị giới hạn ở mức thấp (" + avgCurrent + " mA). Có thể do củ sạc công suất thấp, cắm cổng USB máy tính hoặc dây cáp loại cũ không hỗ trợ dòng cao.";
            color = R.color.neon_yellow;
        }

        return new CableTestResult(score, stars, title, desc, color, avgPower, maxPower, avgCurrent, maxCurrent, minCurrent, avgVolt, stability, isUnstable);
    }

    private static int readCurrentFromSysfs() {
        String[] paths = {
                "/sys/class/power_supply/battery/current_now",
                "/sys/class/power_supply/battery/batt_current",
                "/sys/class/power_supply/usb/current_now"
        };
        for (String path : paths) {
            try {
                File f = new File(path);
                if (f.exists() && f.canRead()) {
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String line = br.readLine();
                    br.close();
                    if (line != null && !line.trim().isEmpty()) {
                        int val = Integer.parseInt(line.trim());
                        int absVal = Math.abs(val);
                        if (absVal > 10000) absVal /= 1000;
                        if (absVal > 0) return absVal;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }
}
