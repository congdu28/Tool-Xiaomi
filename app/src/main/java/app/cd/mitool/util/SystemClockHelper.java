package app.cd.mitool.util;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.ContextCompat;

public class SystemClockHelper {

    /**
     * Check if app has been granted WRITE_SECURE_SETTINGS permission via ADB or Shizuku.
     */
    public static boolean hasSecureSettingsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if app has permission to write to Settings.System.
     * This can be granted directly by the user on the device without any ADB/PC.
     */
    public static boolean canWriteSystemSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(context);
        }
        return true;
    }

    /**
     * Open system settings screen to allow user to toggle "Modify system settings"
     * (Zero ADB / Zero PC required).
     */
    public static boolean openWriteSettingsScreen(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return true;
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    /**
     * Attempts to open Xiaomi's native Status Bar / Notification Settings Activity directly.
     */
    public static boolean openXiaomiStatusBarSettings(Context context) {
        String[][] activities = {
                {"com.android.settings", "com.android.settings.Settings$StatusBarSettingsActivity"},
                {"com.android.settings", "com.android.settings.Settings$SystemDashboardActivity"},
                {"com.android.systemui", "com.android.systemui.tuner.TunerActivity"},
                {"com.miui.notification", "com.miui.notification.ui.NotificationCenterActivity"}
        };

        for (String[] act : activities) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(act[0], act[1]));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {
            }
        }

        // Generic fallback to Notification Settings
        try {
            Intent intent = new Intent("android.settings.NOTIFICATION_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception ignored2) {
            }
        }

        return false;
    }

    /**
     * Check if system status bar clock seconds is currently enabled in any provider.
     */
    public static boolean isSystemClockSecondsEnabled(Context context) {
        try {
            // Check AOSP standard "clock_seconds" in Secure settings
            String clockSeconds = Settings.Secure.getString(context.getContentResolver(), "clock_seconds");
            if ("1".equals(clockSeconds)) {
                return true;
            }

            // Check MIUI specific setting in System settings
            int miuiSeconds = Settings.System.getInt(context.getContentResolver(), "status_bar_show_seconds", 0);
            if (miuiSeconds == 1) {
                return true;
            }

            // Check Oppo / ColorOS / Alternate ROM key
            int formatWithSec = Settings.System.getInt(context.getContentResolver(), "time_format_with_second", 0);
            return formatWithSec == 1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Enable or disable system status bar clock seconds.
     * Tries Secure Settings if granted (ADB/Shizuku) and System Settings if granted (Zero-ADB).
     * Returns true if at least one write succeeded.
     */
    public static boolean setSystemClockSeconds(Context context, boolean enable) {
        boolean success = false;
        String valStr = enable ? "1" : "0";
        int valInt = enable ? 1 : 0;

        // 1. Try WRITE_SECURE_SETTINGS if granted (ADB / Shizuku)
        if (hasSecureSettingsPermission(context)) {
            try {
                Settings.Secure.putString(context.getContentResolver(), "clock_seconds", valStr);
                Settings.Secure.putInt(context.getContentResolver(), "clock_seconds", valInt);
                Settings.Secure.putString(context.getContentResolver(), "status_bar_clock_seconds", valStr);
                Settings.Secure.putInt(context.getContentResolver(), "status_bar_clock_seconds", valInt);
                success = true;
            } catch (Exception ignored) {
            }
            try {
                Settings.Global.putString(context.getContentResolver(), "clock_seconds", valStr);
                Settings.Global.putInt(context.getContentResolver(), "clock_seconds", valInt);
            } catch (Exception ignored) {
            }
        }

        // 2. Try WRITE_SETTINGS (System settings) if granted (Zero ADB / on-device toggle)
        if (canWriteSystemSettings(context)) {
            try {
                Settings.System.putInt(context.getContentResolver(), "status_bar_show_seconds", valInt);
                Settings.System.putInt(context.getContentResolver(), "status_bar_show_second", valInt);
                Settings.System.putInt(context.getContentResolver(), "show_clock_second", valInt);
                Settings.System.putInt(context.getContentResolver(), "time_format_with_second", valInt);
                Settings.System.putString(context.getContentResolver(), "clock_seconds", valStr);
                Settings.System.putInt(context.getContentResolver(), "clock_seconds", valInt);
                success = true;
            } catch (Exception ignored) {
            }
        }

        return success;
    }

    public static String getAdbCommand(Context context) {
        return "adb shell pm grant " + context.getPackageName() + " android.permission.WRITE_SECURE_SETTINGS";
    }

    /**
     * Hide or show native system clock in status bar using icon_blacklist.
     */
    public static boolean setHideSystemClock(Context context, boolean hide) {
        if (!hasSecureSettingsPermission(context)) return false;
        try {
            String currentBlacklist = Settings.Secure.getString(context.getContentResolver(), "icon_blacklist");
            if (currentBlacklist == null) currentBlacklist = "";

            java.util.Set<String> set = new java.util.LinkedHashSet<>();
            for (String item : currentBlacklist.split(",")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) set.add(trimmed);
            }

            if (hide) {
                set.add("clock");
            } else {
                set.remove("clock");
            }

            StringBuilder sb = new StringBuilder();
            for (String s : set) {
                if (sb.length() > 0) sb.append(",");
                sb.append(s);
            }

            Settings.Secure.putString(context.getContentResolver(), "icon_blacklist", sb.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if native clock is currently hidden via icon_blacklist.
     */
    public static boolean isSystemClockHidden(Context context) {
        try {
            String currentBlacklist = Settings.Secure.getString(context.getContentResolver(), "icon_blacklist");
            return currentBlacklist != null && currentBlacklist.contains("clock");
        } catch (Exception e) {
            return false;
        }
    }
}
