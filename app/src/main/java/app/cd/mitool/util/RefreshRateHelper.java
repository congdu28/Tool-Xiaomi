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
     * @return true if successfully written to Settings or applied via Root.
     */
    public static boolean setRefreshRate(Context context, int targetRate) {
        ContentResolver cr = context.getContentResolver();
        float rateFloat = (float) targetRate;

        // Try Root command first for instant, permission-free execution
        StringBuilder rootCmd = new StringBuilder();
        if (targetRate <= 0) {
            rootCmd.append("settings put secure min_refresh_rate 60.0; ")
                    .append("settings put secure peak_refresh_rate 120.0; ")
                    .append("settings put system user_refresh_rate 0; ")
                    .append("settings put system miui_refresh_rate 0; ")
                    .append("settings put secure miui_refresh_rate 0; ")
                    .append("settings put system thermal_limit_refresh_rate 0");
        } else {
            rootCmd.append("settings put secure peak_refresh_rate ").append(rateFloat).append("; ")
                    .append("settings put secure min_refresh_rate ").append(rateFloat).append("; ")
                    .append("settings put secure user_refresh_rate ").append(targetRate).append("; ")
                    .append("settings put system user_refresh_rate ").append(targetRate).append("; ")
                    .append("settings put secure miui_refresh_rate ").append(targetRate).append("; ")
                    .append("settings put system miui_refresh_rate ").append(targetRate).append("; ")
                    .append("settings put system min_refresh_rate ").append(rateFloat).append("; ")
                    .append("settings put system peak_refresh_rate ").append(rateFloat).append("; ")
                    .append("settings put system thermal_limit_refresh_rate 0; ")
                    .append("settings put global speed_mode 1");
        }

        if (runRootCommand(rootCmd.toString())) {
            return true;
        }

        // Standard ContentResolver (requires WRITE_SECURE_SETTINGS or WRITE_SETTINGS)
        boolean wroteAny = false;
        try {
            if (targetRate <= 0) {
                try {
                    Settings.Secure.putFloat(cr, "min_refresh_rate", 60.0f);
                    Settings.Secure.putFloat(cr, "peak_refresh_rate", 120.0f);
                    Settings.Secure.putInt(cr, "user_refresh_rate", 0);
                    wroteAny = true;
                } catch (Exception ignored) {
                }
                try {
                    Settings.System.putInt(cr, "user_refresh_rate", 0);
                    Settings.System.putInt(cr, "miui_refresh_rate", 0);
                    Settings.Secure.putInt(cr, "miui_refresh_rate", 0);
                    Settings.System.putInt(cr, "thermal_limit_refresh_rate", 0);
                    wroteAny = true;
                } catch (Exception ignored) {
                }
                return wroteAny;
            }

            // AVOID DisplayModeDirector COLLISION:
            // When increasing rate (e.g. 60 -> 120), peak MUST be set first so min <= peak!
            // When decreasing rate (e.g. 120 -> 60), min MUST be set first so min <= peak!
            float currentPeak = 120.0f;
            try {
                currentPeak = Settings.Secure.getFloat(cr, "peak_refresh_rate", 120.0f);
            } catch (Exception ignored) {
            }

            if (rateFloat >= currentPeak) {
                try {
                    Settings.Secure.putFloat(cr, "peak_refresh_rate", rateFloat);
                    Settings.Secure.putFloat(cr, "min_refresh_rate", rateFloat);
                    Settings.Secure.putInt(cr, "user_refresh_rate", targetRate);
                    wroteAny = true;
                } catch (Exception ignored) {
                }
            } else {
                try {
                    Settings.Secure.putFloat(cr, "min_refresh_rate", rateFloat);
                    Settings.Secure.putFloat(cr, "peak_refresh_rate", rateFloat);
                    Settings.Secure.putInt(cr, "user_refresh_rate", targetRate);
                    wroteAny = true;
                } catch (Exception ignored) {
                }
            }

            // Xiaomi MIUI / HyperOS specific settings
            try {
                Settings.System.putInt(cr, "user_refresh_rate", targetRate);
                Settings.System.putFloat(cr, "min_refresh_rate", rateFloat);
                Settings.System.putFloat(cr, "peak_refresh_rate", rateFloat);
                Settings.System.putInt(cr, "miui_refresh_rate", targetRate);
                Settings.Secure.putInt(cr, "miui_refresh_rate", targetRate);
                Settings.System.putInt(cr, "thermal_limit_refresh_rate", 0);
                Settings.Global.putInt(cr, "speed_mode", 1);
                wroteAny = true;
            } catch (Exception ignored) {
            }

        } catch (Exception ignored) {
        }

        return wroteAny;
    }

    private static boolean runRootCommand(String command) {
        java.io.DataOutputStream os = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new java.io.DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            return ProcessRunner.collect(process, 15000).exitCode == 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ignored) {
                }
            }
            if (process != null) {
                try {
                    process.destroy();
                } catch (Exception ignored) {
                }
            }
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
            return "adb shell \"settings put secure min_refresh_rate 60.0 && settings put secure peak_refresh_rate 120.0 && settings put system user_refresh_rate 0 && settings put system thermal_limit_refresh_rate 0\"";
        }
        return "adb shell \"settings put secure peak_refresh_rate " + targetRate + ".0 && " +
               "settings put secure min_refresh_rate " + targetRate + ".0 && " +
               "settings put system user_refresh_rate " + targetRate + " && " +
               "settings put system thermal_limit_refresh_rate 0\"";
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
