package app.cd.mitool.util;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class RefreshRateHelper {

    /**
     * Get list of refresh rates supported by current display (e.g. [60, 90, 120, 144]).
     */
    public static List<Integer> getSupportedRefreshRates(Context context) {
        Set<Integer> rates = new TreeSet<>();
        rates.add(60); // Standard baseline

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) {
                    Display display = wm.getDefaultDisplay();
                    if (display != null) {
                        Display.Mode[] modes = display.getSupportedModes();
                        if (modes != null) {
                            for (Display.Mode mode : modes) {
                                int r = Math.round(mode.getRefreshRate());
                                if (r >= 58 && r <= 62) rates.add(60);
                                else if (r >= 88 && r <= 92) rates.add(90);
                                else if (r >= 118 && r <= 122) rates.add(120);
                                else if (r >= 142 && r <= 146) rates.add(144);
                                else if (r > 30) rates.add(r);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(rates);
    }

    /**
     * Get the active screen refresh rate.
     */
    public static int getCurrentRefreshRate(Context context) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                Display display = wm.getDefaultDisplay();
                if (display != null) {
                    return Math.round(display.getMode().getRefreshRate());
                }
            }
        } catch (Exception ignored) {
        }
        return 60;
    }

    /**
     * Force refresh rate (min == peak == user) to lock FPS and prevent throttling.
     * @param targetRate 0 for Default/Auto, or 60, 90, 120, 144.
     * @return true if successfully written to Settings.
     */
    public static boolean setRefreshRate(Context context, int targetRate) {
        ContentResolver cr = context.getContentResolver();
        try {
            if (targetRate <= 0) {
                // Reset to Auto/Default
                try {
                    Settings.Secure.putFloat(cr, "min_refresh_rate", 60.0f);
                    Settings.Secure.putFloat(cr, "peak_refresh_rate", 120.0f);
                } catch (Exception ignored) {
                }
                try {
                    Settings.System.putInt(cr, "user_refresh_rate", 0);
                    Settings.System.putInt(cr, "min_refresh_rate", 60);
                    Settings.System.putInt(cr, "peak_refresh_rate", 120);
                } catch (Exception ignored) {
                }
                return true;
            }

            float rateFloat = (float) targetRate;

            // Android standard secure settings
            boolean wroteSecure = false;
            try {
                Settings.Secure.putFloat(cr, "min_refresh_rate", rateFloat);
                Settings.Secure.putFloat(cr, "peak_refresh_rate", rateFloat);
                Settings.Secure.putInt(cr, "user_refresh_rate", targetRate);
                wroteSecure = true;
            } catch (Exception ignored) {
            }

            // Xiaomi MIUI / HyperOS system settings
            boolean wroteSystem = false;
            try {
                Settings.System.putInt(cr, "user_refresh_rate", targetRate);
                Settings.System.putInt(cr, "min_refresh_rate", targetRate);
                Settings.System.putInt(cr, "peak_refresh_rate", targetRate);
                wroteSystem = true;
            } catch (Exception ignored) {
            }

            return wroteSecure || wroteSystem;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if device supports higher than 60Hz.
     */
    public static boolean isHighRefreshRateSupported(Context context) {
        List<Integer> rates = getSupportedRefreshRates(context);
        for (int r : rates) {
            if (r > 60) return true;
        }
        return false;
    }

    /**
     * Generate ADB command string to force refresh rate.
     */
    public static String getAdbCommand(int targetRate) {
        if (targetRate <= 0) {
            return "adb shell settings put secure min_refresh_rate 60.0 && adb shell settings put secure peak_refresh_rate 120.0";
        }
        return "adb shell settings put secure min_refresh_rate " + targetRate + ".0 && " +
               "adb shell settings put secure peak_refresh_rate " + targetRate + ".0 && " +
               "adb shell settings put system user_refresh_rate " + targetRate;
    }

    /**
     * Open Xiaomi's Display Refresh Rate settings screen directly.
     */
    public static boolean openRefreshRateSettings(Context context) {
        String[][] attempts = {
            {"com.android.settings", "com.android.settings.SubSettings"},
            {"com.android.settings", "com.android.settings.display.RefreshRateSettings"},
            {"com.android.settings", "com.android.settings.DisplaySettings"}
        };

        for (String[] comp : attempts) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(comp[0], comp[1]));
                intent.putExtra(":settings:show_fragment", "com.android.settings.display.RefreshRateSettings");
                intent.putExtra(":settings:show_fragment_title", "Tốc độ làm mới");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {
            }
        }

        try {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }
}
