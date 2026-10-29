package app.cd.mitool.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BatteryHelper {

    public static class BatteryInfo {
        public int level; // 0 - 100
        public boolean isCharging;
        public boolean isPlugged;
        public boolean isFull;
        public int plugType; // AC, USB, Wireless, None
        public String plugTypeName;
        public float temperature; // Celsius
        public int voltageMv; // Millivolts
        public int currentMa; // Milliamperes (positive when charging)
        public float powerWatt; // Watt (V * I / 1000)
        public long estimatedMinutesToFull;
        public String formattedEta;
        public String healthDescription;
    }

    public static BatteryInfo getBatteryInfo(Context context) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return getBatteryInfo(context, batteryStatus);
    }

    public static BatteryInfo getBatteryInfo(Context context, Intent intent) {
        BatteryInfo info = new BatteryInfo();
        if (intent == null) {
            info.level = 50;
            info.isCharging = false;
            info.plugTypeName = "Không xác định";
            info.formattedEta = "Không có thông tin";
            return info;
        }

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level >= 0 && scale > 0) {
            info.level = Math.round((level / (float) scale) * 100);
        } else {
            info.level = 0;
        }

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        info.isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        info.isFull = status == BatteryManager.BATTERY_STATUS_FULL || info.level >= 100;

        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        info.isPlugged = plugged > 0;
        info.plugType = plugged;
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                info.plugTypeName = "Củ sạc (AC)";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                info.plugTypeName = "Cổng USB";
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                info.plugTypeName = "Sạc không dây";
                break;
            default:
                info.plugTypeName = info.isCharging ? "Đang sạc" : "Đang dùng pin";
                break;
        }

        int rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        info.temperature = rawTemp / 10.0f;

        info.voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);

        // Health
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                info.healthDescription = "Tốt (Good)";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                info.healthDescription = "Quá nhiệt!";
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                info.healthDescription = "Hỏng";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                info.healthDescription = "Quá áp!";
                break;
            default:
                info.healthDescription = "Bình thường";
                break;
        }

        // Current & Power calculation
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int currentNow = 0;
        long systemRemainingMs = -1;

        if (bm != null) {
            currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && (info.isCharging || status == BatteryManager.BATTERY_STATUS_CHARGING)) {
                try {
                    systemRemainingMs = bm.computeChargeTimeRemaining();
                } catch (Exception ignored) {
                }
            }
        }

        info.currentMa = BatteryMeasurements.milliAmps(currentNow);
        info.powerWatt = BatteryMeasurements.watts(info.voltageMv, info.currentMa);
        // Missing sensor readings stay unavailable; never fabricate discharge/charge power.

        // Calculate estimated minutes
        if (info.isFull && info.isPlugged) {
            info.estimatedMinutesToFull = 0;
            info.formattedEta = "Pin đã sạc đầy 100%";
        } else if (!info.isCharging) {
            info.estimatedMinutesToFull = -1;
            info.formattedEta = info.isPlugged ? "Đã kết nối sạc; hệ thống đang tạm ngừng sạc" : formatRemainingTime(info.level);
        } else if (info.isFull) {
            info.estimatedMinutesToFull = 0;
            info.formattedEta = "Pin đã sạc đầy 100%";
        } else {
            info.estimatedMinutesToFull = calculateEstimatedMinutes(info.level, info.currentMa, info.plugType, systemRemainingMs, info.powerWatt);
            if (info.estimatedMinutesToFull <= 0) {
                info.formattedEta = "Đang ước lượng thời gian sạc...";
            } else {
                long fullTimestamp = System.currentTimeMillis() + (info.estimatedMinutesToFull * 60 * 1000L);
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String fullTimeStr = timeFormat.format(new Date(fullTimestamp));
                info.formattedEta = "Còn ~" + info.estimatedMinutesToFull + " phút (Đầy lúc " + fullTimeStr + ")";
            }
        }

        return info;
    }

    public static String formatRemainingTime(int level) {
        return level <= 0 ? "Cần sạc pin" : "Thời gian dùng còn lại phụ thuộc tác vụ và kết nối";
    }

    private static long calculateEstimatedMinutes(int currentLevel, int currentMa, int plugType, long systemRemainingMs, float powerWatt) {
        // Priority 1: System native API (computeChargeTimeRemaining) - only if realistic (< 4 hours)
        if (systemRemainingMs > 60000 && systemRemainingMs < 4 * 3600 * 1000L) {
            long minutes = Math.round(systemRemainingMs / 60000.0);
            if (minutes > 0 && minutes <= 240) {
                return minutes;
            }
        }

        int percentRemaining = Math.max(0, 100 - currentLevel);
        if (percentRemaining <= 0) return 0;

        double below80 = Math.max(0, Math.min(80, 100) - currentLevel);
        if (currentLevel >= 80) below80 = 0;
        double above80 = percentRemaining - below80;

        double minutesTotal;

        if (plugType == BatteryManager.BATTERY_PLUGGED_AC) {
            // Xiaomi AC Fast Charging (18W, 33W, 67W, 120W)
            double rateBelow80;
            double rateAbove80;

            if (powerWatt >= 30.0f || currentMa >= 4500) {
                // HyperCharge / 67W - 120W (~30-40 min full charge)
                rateBelow80 = 2.4; // 2.4% / min
                rateAbove80 = 1.2; // 1.2% / min
            } else if (powerWatt >= 18.0f || currentMa >= 3000) {
                // Turbo Charge / 33W (~45-55 min full charge)
                rateBelow80 = 1.8; // 1.8% / min
                rateAbove80 = 0.9; // 0.9% / min
            } else if (powerWatt >= 12.0f || currentMa >= 1800) {
                // Quick Charge 18W (~65-75 min full charge)
                rateBelow80 = 1.4; // 1.4% / min
                rateAbove80 = 0.7; // 0.7% / min
            } else {
                // Standard AC / 10W-15W (~85-100 min full charge)
                rateBelow80 = 1.15; // 1.15% / min
                rateAbove80 = 0.6;  // 0.6% / min
            }

            minutesTotal = (below80 / rateBelow80) + (above80 / rateAbove80);

        } else if (plugType == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
            // Wireless charging (~10W - 30W)
            minutesTotal = (below80 / 0.9) + (above80 / 0.45);
        } else {
            // Slow USB computer port (5V 0.5A - 1A)
            minutesTotal = (below80 / 0.45) + (above80 / 0.25);
        }

        return Math.max(1, Math.round(minutesTotal));
    }

    private static Ringtone activeRingtone;

    public static synchronized void playAlertTone(Context context) {
        stopAlertTone();
        activeRingtone = startTone(context, new PreferencesHelper(context).getCustomAlertToneUri());
        android.util.Log.i("MiToolAudio", "ring_started=" + (activeRingtone != null && activeRingtone.isPlaying()));
    }

    private static Ringtone startTone(Context context, String custom) {
        java.util.List<Uri> sources = new java.util.ArrayList<>();
        if (custom != null && !custom.trim().isEmpty()) sources.add(Uri.parse(custom));
        for (int type : new int[]{RingtoneManager.TYPE_NOTIFICATION, RingtoneManager.TYPE_ALARM, RingtoneManager.TYPE_RINGTONE}) {
            Uri uri = RingtoneManager.getDefaultUri(type); if (uri != null) sources.add(uri);
        }
        for (Uri source : sources) {
            try {
                Ringtone tone = RingtoneManager.getRingtone(context.getApplicationContext(), source);
                if (tone == null) continue;
                tone.setAudioAttributes(new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                tone.play();
                if (tone.isPlaying()) return tone;
                tone.stop();
            } catch (RuntimeException ignored) { }
        }
        android.util.Log.w("MiToolAudio", "No ringtone could be started");
        return null;
    }
    private static Ringtone previewRingtone;
    public static synchronized boolean previewTone(Context context) {
        stopPreviewTone();
        previewRingtone = startTone(context, new PreferencesHelper(context).getCustomAlertToneUri());
        return previewRingtone != null;
    }
    public static synchronized void stopPreviewTone() {
        if (previewRingtone != null) { previewRingtone.stop(); previewRingtone = null; }
    }

    public static synchronized void stopAlertTone() {
        try {
            if (activeRingtone != null && activeRingtone.isPlaying()) {
                activeRingtone.stop();
            }
        } catch (Exception ignored) {
        }
    }

    public static synchronized boolean isAlertTonePlaying() {
        try {
            return activeRingtone != null && activeRingtone.isPlaying();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String getRingtoneTitle(Context context, Uri uri) {
        if (uri == null) return "Mặc định hệ thống";
        try {
            Ringtone r = RingtoneManager.getRingtone(context, uri);
            if (r != null) {
                String title = r.getTitle(context);
                if (title != null && !title.trim().isEmpty()) {
                    return title;
                }
            }
        } catch (Exception ignored) {
        }
        return "Âm thanh tùy chọn";
    }

    public static void vibrateDevice(Context context, long durationMs) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(durationMs);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static int getBatteryPercentage(Context context) {
        return getBatteryInfo(context).level;
    }

    public static boolean isCharging(Context context) {
        return getBatteryInfo(context).isCharging;
    }

    public static float getBatteryTemperature(Context context) {
        return getBatteryInfo(context).temperature;
    }

    public static float getBatteryVoltage(Context context) {
        return getBatteryInfo(context).voltageMv / 1000.0f;
    }

    public static int getPlugType(Context context) {
        return getBatteryInfo(context).plugType;
    }

    public static String getEstimatedChargeTime(Context context) {
        return getBatteryInfo(context).formattedEta;
    }

    public static float getBatteryPowerWatts(Context context) {
        return getBatteryInfo(context).powerWatt;
    }

    public static int getBatteryCurrentNow(Context context) {
        return getBatteryInfo(context).currentMa;
    }

    public static class BatteryCapacityData {
        public int designMah;
        public int actualMah;
        public int currentMah;
        public float healthPercent;
        public String healthDescription;
        public int cycleCount;
    }

    private static BatteryCapacityData cachedCapacityData = null;
    private static long lastCapacityDataFetchTime = 0;
    private static final long CAPACITY_CACHE_TTL_MS = 15000; // 15 seconds cache

    public static int getBatteryDesignCapacity(Context context) {
        if (context != null) {
            try {
                Class<?> powerProfileClass = Class.forName("com.android.internal.os.PowerProfile");
                java.lang.reflect.Constructor<?> constructor = powerProfileClass.getConstructor(Context.class);
                Object powerProfileInstance = constructor.newInstance(context);
                java.lang.reflect.Method getBatteryCapacity = powerProfileClass.getMethod("getBatteryCapacity");
                double cap = (double) getBatteryCapacity.invoke(powerProfileInstance);
                if (cap > 500 && cap < 30000) {
                    return (int) Math.round(cap);
                }
            } catch (Throwable ignored) {
            }
        }

        String[] designPaths = {
                "/sys/class/power_supply/battery/charge_full_design",
                "/sys/class/power_supply/bms/charge_full_design",
                "/sys/class/power_supply/battery/fg_design_cap"
        };
        for (String path : designPaths) {
            long val = readSysfsLong(path);
            if (val > 0) {
                if (val > 50000) val /= 1000;
                if (val >= 500 && val <= 30000) return (int) val;
            }
        }

        return 0;
    }

    public static synchronized BatteryCapacityData getBatteryCapacityData(Context context) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (cachedCapacityData != null && now - lastCapacityDataFetchTime < 5 * 60000L) return cachedCapacityData;
        BatteryCapacityData data = new BatteryCapacityData();
        data.designMah = getBatteryDesignCapacity(context);
        data.cycleCount = getBatteryCycleCount();
        for (String path : new String[]{"/sys/class/power_supply/battery/charge_full", "/sys/class/power_supply/bms/charge_full"}) {
            long value = readSysfsLong(path);
            if (value > 50000) value /= 1000;
            if (value >= 500 && value <= 30000) { data.actualMah = (int) value; break; }
        }
        long soh = readSysfsLong("/sys/class/power_supply/battery/soh");
        if (soh <= 0) soh = readSysfsLong("/sys/class/power_supply/bms/soh");
        if (soh > 0 && soh <= 100) {
            data.healthPercent = soh;
            data.healthDescription = "Hệ thống báo SOH: " + soh + "%";
        } else {
            data.healthDescription = "Chưa có số đo độ chai pin";
        }
        if (context != null) {
            BatteryManager manager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (manager != null) {
                int counter = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                if (counter > 0) data.currentMah = counter / 1000;
            }
        }
        cachedCapacityData = data;
        lastCapacityDataFetchTime = now;
        return data;
    }

    public static int getBatteryCycleCount() {
        String[] cyclePaths = {
                "/sys/class/power_supply/battery/cycle_count",
                "/sys/class/power_supply/bms/battery_cycle",
                "/sys/class/power_supply/bms/cycle_count",
                "/sys/class/power_supply/battery/battery_cycle",
                "/sys/class/power_supply/battery/device/cycle_count",
                "/sys/class/power_supply/maxfg/cycle_count",
                "/sys/class/power_supply/battery/count"
        };
        for (String path : cyclePaths) {
            long val = readSysfsLong(path);
            if (val >= 0 && val < 50000) {
                return (int) val;
            }
        }
        return -1;
    }

    private static long readSysfsLong(String path) {
        try {
            java.io.File file = new java.io.File(path);
            if (file.exists() && file.canRead()) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String line = br.readLine();
                    if (line != null && !line.trim().isEmpty()) {
                        return Long.parseLong(line.trim());
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Shizuku fallback if direct permission denied
        try {
            if (ShizukuHelper.hasShizukuPermission()) {
                ShizukuHelper.ShellResult res = ShizukuHelper.executeCommand("cat " + path);
                if (res != null && res.success && res.stdout != null) {
                    String line = res.stdout.trim();
                    if (!line.isEmpty()) {
                        return Long.parseLong(line);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return -1;
    }

    public static boolean isPlayingAlert() {
        return isAlertTonePlaying();
    }

    public static synchronized void playAlertTone(Context context, String uriStr) {
        stopAlertTone(); activeRingtone = startTone(context, uriStr);
    }
}
