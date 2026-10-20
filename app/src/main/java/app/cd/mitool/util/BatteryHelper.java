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

        // Convert current from microampere to milliampere if needed
        int currentMa = Math.abs(currentNow);
        if (currentMa > 10000) {
            // value is in microamperes
            currentMa = currentMa / 1000;
        }
        info.currentMa = currentMa;

        // Calculate power (W = V * A)
        if (info.voltageMv > 0 && info.currentMa > 0) {
            info.powerWatt = (info.voltageMv / 1000.0f) * (info.currentMa / 1000.0f);
        } else {
            info.powerWatt = 0.0f;
        }

        // Calculate estimated minutes to full
        if (info.isFull) {
            info.estimatedMinutesToFull = 0;
            info.formattedEta = "Pin đã đầy 100%";
        } else if (!info.isCharging) {
            info.estimatedMinutesToFull = -1;
            info.formattedEta = "Đang không sạc";
        } else {
            info.estimatedMinutesToFull = calculateEstimatedMinutes(info.level, info.currentMa, info.plugType, systemRemainingMs);
            if (info.estimatedMinutesToFull <= 0) {
                info.formattedEta = "Đang ước lượng...";
            } else {
                long fullTimestamp = System.currentTimeMillis() + (info.estimatedMinutesToFull * 60 * 1000);
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String fullTimeStr = timeFormat.format(new Date(fullTimestamp));
                info.formattedEta = "Còn ~" + info.estimatedMinutesToFull + " phút (Đầy lúc " + fullTimeStr + ")";
            }
        }

        return info;
    }

    private static long calculateEstimatedMinutes(int currentLevel, int currentMa, int plugType, long systemRemainingMs) {
        // Priority 1: System native API
        if (systemRemainingMs > 0) {
            long minutes = systemRemainingMs / (60 * 1000);
            if (minutes > 0) {
                return minutes;
            }
        }

        // Priority 2: Smart model calculation based on battery physics
        int percentRemaining = Math.max(0, 100 - currentLevel);
        if (percentRemaining == 0) return 0;

        // Standard modern Xiaomi capacity ~5000mAh
        double capacityRemainingMah = (percentRemaining / 100.0) * 5000.0;

        if (currentMa >= 300) {
            // Constant Current (CC) phase up to 80%, Constant Voltage (CV) phase above 80%
            double totalHours;
            if (currentLevel < 80) {
                double fastMah = ((80 - currentLevel) / 100.0) * 5000.0;
                double slowMah = (20.0 / 100.0) * 5000.0;
                double fastHours = fastMah / currentMa;
                double slowHours = slowMah / (currentMa * 0.45); // Current tapers down near end
                totalHours = fastHours + slowHours;
            } else {
                double slowMah = (percentRemaining / 100.0) * 5000.0;
                totalHours = slowMah / (currentMa * 0.45);
            }
            return Math.max(1, Math.round(totalHours * 60));
        }

        // Priority 3: Fallback estimation based on plug type
        double ratePerMinute;
        if (plugType == BatteryManager.BATTERY_PLUGGED_AC) {
            ratePerMinute = 1.3; // ~1.3% per minute for typical fast charging
        } else if (plugType == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
            ratePerMinute = 0.8;
        } else {
            ratePerMinute = 0.45; // Slow USB
        }

        return Math.max(1, Math.round(percentRemaining / ratePerMinute));
    }

    public static void playAlertTone(Context context) {
        try {
            Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(context, notificationUri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception ignored) {
        }
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
}
