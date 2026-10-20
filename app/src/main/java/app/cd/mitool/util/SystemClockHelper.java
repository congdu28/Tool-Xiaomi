package app.cd.mitool.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
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
     * Check if system status bar clock seconds is currently enabled.
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
            return miuiSeconds == 1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Enable or disable system status bar clock seconds directly in system settings.
     * Returns true if successful, false if permission denied or error.
     */
    public static boolean setSystemClockSeconds(Context context, boolean enable) {
        if (!hasSecureSettingsPermission(context)) {
            return false;
        }

        boolean success = false;
        try {
            // Write standard AOSP SystemUI setting
            Settings.Secure.putString(context.getContentResolver(), "clock_seconds", enable ? "1" : "0");
            success = true;
        } catch (Exception ignored) {
        }

        try {
            // Write MIUI / HyperOS specific status bar setting
            Settings.System.putInt(context.getContentResolver(), "status_bar_show_seconds", enable ? 1 : 0);
            success = true;
        } catch (Exception ignored) {
        }

        return success;
    }

    public static String getAdbCommand(Context context) {
        return "adb shell pm grant " + context.getPackageName() + " android.permission.WRITE_SECURE_SETTINGS";
    }
}
