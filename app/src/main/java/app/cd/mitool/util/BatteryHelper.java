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
            // Value is in microamperes
            currentMa = currentMa / 1000;
        }
        info.currentMa = currentMa;

        // Calculate power (W = V * A)
        if (info.voltageMv > 0 && info.currentMa > 0) {
            info.powerWatt = (info.voltageMv / 1000.0f) * (info.currentMa / 1000.0f);
        } else if (info.isCharging && info.voltageMv > 0) {
            // Intelligent fallback when hardware doesn't report current_now while charging
            float fallbackAmp = (plugged == BatteryManager.BATTERY_PLUGGED_AC) ? 2.0f : 0.5f;
            info.powerWatt = (info.voltageMv / 1000.0f) * fallbackAmp;
        } else if (!info.isCharging && info.voltageMv > 0) {
            // Typical screen-on discharge consumption (~450mA)
            info.powerWatt = (info.voltageMv / 1000.0f) * 0.45f;
        } else {
            info.powerWatt = 0.0f;
        }

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
}
