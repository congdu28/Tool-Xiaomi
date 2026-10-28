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
        info.plugType = plugged;
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                info.plugTypeName = "Củ sạc (AC Nhanh)";
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
        if (info.isFull && !info.isCharging) {
            info.estimatedMinutesToFull = -1;
            info.formattedEta = "Pin đầy 100% (" + formatRemainingTime(100) + ")";
        } else if (!info.isCharging) {
            info.estimatedMinutesToFull = -1;
            info.formattedEta = formatRemainingTime(info.level);
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
        if (level <= 0) return "Cần cắm sạc ngay";
        int totalMinutes = Math.round(level * 5.5f);
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (hours > 0) {
            return "Còn ~" + hours + "h " + mins + "p sử dụng";
        } else {
            return "Còn ~" + mins + " phút sử dụng";
        }
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
        try {
            stopAlertTone();
            PreferencesHelper prefs = new PreferencesHelper(context);
            String customUriStr = prefs.getCustomAlertToneUri();
            Uri alertUri = null;
            if (customUriStr != null && !customUriStr.trim().isEmpty()) {
                alertUri = Uri.parse(customUriStr);
            }
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if (alertUri == null) {
                    alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                }
            }
            activeRingtone = RingtoneManager.getRingtone(context.getApplicationContext(), alertUri);
            if (activeRingtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    activeRingtone.setAudioAttributes(new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                } else {
                    activeRingtone.setStreamType(android.media.AudioManager.STREAM_ALARM);
                }
                activeRingtone.play();
            }
        } catch (Exception ignored) {
        }
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

        return 5000;
    }

    public static BatteryCapacityData getBatteryCapacityData(Context context) {
        long now = System.currentTimeMillis();
        if (cachedCapacityData != null && (now - lastCapacityDataFetchTime) < CAPACITY_CACHE_TTL_MS) {
            return cachedCapacityData;
        }

        BatteryCapacityData data = new BatteryCapacityData();
        data.designMah = getBatteryDesignCapacity(context);
        data.cycleCount = getBatteryCycleCount();

        int level = (context != null) ? getBatteryPercentage(context) : 50;
        long actualVal = -1;

        // 1. If Shizuku is granted, parse dumpsys battery for Xiaomi HyperOS official stats
        if (ShizukuHelper.hasShizukuPermission()) {
            try {
                ShizukuHelper.ShellResult res = ShizukuHelper.executeCommand("dumpsys battery");
                if (res != null && res.success && res.stdout != null) {
                    String[] lines = res.stdout.split("\n");
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("mSavedBatteryAsoc:")) {
                            String v = trimmed.substring("mSavedBatteryAsoc:".length()).trim();
                            try {
                                int asoc = Integer.parseInt(v);
                                if (asoc > 0 && asoc <= 100) {
                                    data.healthPercent = asoc;
                                }
                            } catch (Exception ignored) {}
                        } else if (trimmed.startsWith("mSavedBatteryUsage:")) {
                            String v = trimmed.substring("mSavedBatteryUsage:".length()).trim();
                            try {
                                int usage = Integer.parseInt(v);
                                if (usage >= 0 && usage < 10000) {
                                    data.cycleCount = usage;
                                }
                            } catch (Exception ignored) {}
                        } else if (trimmed.startsWith("Charge counter:")) {
                            String v = trimmed.substring("Charge counter:".length()).trim();
                            try {
                                long counter = Long.parseLong(v);
                                if (counter > 50000) counter /= 1000;
                                if (counter > 50 && counter < 30000 && level > 5) {
                                    actualVal = Math.round((counter / (float) level) * 100);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // 2. Sysfs charge_full & CIT nodes
        if (actualVal <= 0) {
            String[] fullPaths = {
                    "/sys/class/power_supply/battery/charge_full",
                    "/sys/class/power_supply/bms/charge_full",
                    "/sys/class/power_supply/battery/fg_fullcap",
                    "/sys/class/power_supply/battery/charge_counter_ext"
            };
            for (String path : fullPaths) {
                long val = readSysfsLong(path);
                if (val > 0) {
                    if (val > 50000) val /= 1000;
                    if (val >= 500 && val <= 30000) {
                        actualVal = val;
                        break;
                    }
                }
            }
        }

        // 3. BatteryManager CHARGE_COUNTER
        if (actualVal <= 0 && context != null && level > 5) {
            try {
                BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                if (bm != null) {
                    int counter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                    if (counter > 0) {
                        int cMah = counter > 50000 ? counter / 1000 : counter;
                        if (cMah > 50) {
                            actualVal = Math.round((cMah / (float) level) * 100);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // 4. Sysfs State Of Health (SOH) if healthPercent not yet parsed from ASOC
        if (data.healthPercent <= 0) {
            long soh = readSysfsLong("/sys/class/power_supply/battery/soh");
            if (soh <= 0) soh = readSysfsLong("/sys/class/power_supply/bms/soh");

            if (soh > 0 && soh <= 100) {
                data.healthPercent = (float) soh;
            }
        }

        // Finalize healthPercent and actualMah
        if (data.healthPercent > 0) {
            if (actualVal <= 0) {
                actualVal = Math.round(data.designMah * (data.healthPercent / 100.0f));
            }
        } else if (actualVal > 0) {
            data.healthPercent = Math.min(100.0f, (actualVal / (float) data.designMah) * 100.0f);
        } else {
            if (data.cycleCount > 0) {
                float estLoss = (data.cycleCount / 100.0f) * 2.2f;
                data.healthPercent = Math.max(70.0f, Math.min(100.0f, 100.0f - estLoss));
                actualVal = Math.round(data.designMah * (data.healthPercent / 100.0f));
            } else {
                data.healthPercent = 96.0f;
                actualVal = Math.round(data.designMah * 0.96f);
            }
        }

        data.actualMah = (int) actualVal;
        data.currentMah = Math.round(data.actualMah * (level / 100.0f));

        if (data.healthPercent >= 90) {
            data.healthDescription = "Tuyệt vời (" + Math.round(data.healthPercent) + "%)";
        } else if (data.healthPercent >= 80) {
            data.healthDescription = "Tốt (" + Math.round(data.healthPercent) + "%)";
        } else {
            data.healthDescription = "Đã chai (" + Math.round(data.healthPercent) + "%)";
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
        try {
            stopAlertTone();
            Uri alertUri = null;
            if (uriStr != null && !uriStr.trim().isEmpty()) {
                alertUri = Uri.parse(uriStr);
            }
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            activeRingtone = RingtoneManager.getRingtone(context.getApplicationContext(), alertUri);
            if (activeRingtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    activeRingtone.setAudioAttributes(new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                } else {
                    activeRingtone.setStreamType(android.media.AudioManager.STREAM_ALARM);
                }
                activeRingtone.play();
            }
        } catch (Exception ignored) {
        }
    }
}
