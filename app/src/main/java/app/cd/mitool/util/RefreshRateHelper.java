package app.cd.mitool.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
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

    private static final java.util.concurrent.ExecutorService EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());
    private static final String SNAPSHOT = "refresh_rate_original_v1";

    public static void applyAsync(Context context, int targetRate, ShizukuHelper.CommandCallback callback) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            boolean success = setRefreshRate(app, targetRate);
            if (success) new PreferencesHelper(app).setTargetRefreshRate(targetRate);
            MAIN.post(() -> { if (callback != null) callback.onComplete(success, success
                    ? (targetRate == 0 ? "Đã khôi phục cấu hình tần số quét trước khi ép Hz (hoặc mặc định hệ thống)."
                    : "Đã lưu cấu hình " + targetRate + " Hz.")
                    : "Chưa áp dụng được. Kiểm tra quyền Shizuku/Root hoặc tần số quét thiết bị hỗ trợ."); });
        });
    }

    /** Must run on a worker thread. Preserves original overrides across fixed-rate changes. */
    public static synchronized boolean setRefreshRate(Context context, int targetRate) {
        if (targetRate < 0 || (targetRate > 0 && !getSupportedRefreshRates(context).contains(targetRate))) return false;
        android.content.SharedPreferences storage = context.getSharedPreferences("refresh_rate_snapshot", Context.MODE_PRIVATE);
        java.util.Map<String, String> original = null;
        boolean captured = false;
        try {
            if (storage.contains(SNAPSHOT)) {
                org.json.JSONObject json = new org.json.JSONObject(storage.getString(SNAPSHOT, "{}"));
                original = new java.util.LinkedHashMap<>();
                for (String key : RefreshRatePlan.KEYS) original.put(key, json.isNull(key) ? null : json.getString(key));
            } else if (targetRate > 0) {
                org.json.JSONObject json = new org.json.JSONObject();
                original = new java.util.LinkedHashMap<>();
                for (String key : RefreshRatePlan.KEYS) {
                    String value = read(context, key);
                    original.put(key, value);
                    json.put(key, value == null ? org.json.JSONObject.NULL : value);
                }
                // Persist before touching any setting, so partial failures can be recovered.
                if (!storage.edit().putString(SNAPSHOT, json.toString()).commit()) return false;
                captured = true;
            }
            java.util.Map<String, String> changes = RefreshRatePlan.changes(targetRate, original);
            boolean written = false;
            if (SystemClockHelper.hasSecureSettingsPermission(context) && SystemClockHelper.canWriteSystemSettings(context)) {
                try {
                    write(context, "secure:min_refresh_rate", "0");
                    write(context, "system:min_refresh_rate", "0");
                    written = true;
                    for (java.util.Map.Entry<String, String> entry : changes.entrySet()) written &= write(context, entry.getKey(), entry.getValue());
                } catch (RuntimeException ignored) { written = false; }
            }
            if (!written) {
                String command = RefreshRatePlan.shell(changes);
                written = ShizukuHelper.hasShizukuPermission()
                        ? ShizukuHelper.executeCommand(command).success : GamingFpsHelper.runRootCommand(command);
            }
            if (!written) {
                if (captured) {
                    boolean unchanged = true;
                    for (java.util.Map.Entry<String, String> entry : original.entrySet()) {
                        unchanged &= java.util.Objects.equals(entry.getValue(), read(context, entry.getKey()));
                    }
                    if (unchanged) storage.edit().remove(SNAPSHOT).apply();
                }
                return false;
            }
            for (java.util.Map.Entry<String, String> entry : changes.entrySet()) {
                if (!java.util.Objects.equals(entry.getValue(), read(context, entry.getKey()))) return false;
            }
            if (targetRate == 0) storage.edit().remove(SNAPSHOT).apply();
            return true;
        } catch (Exception ignored) { return false; }
    }

    private static String read(Context context, String qualifiedKey) {
        String[] key = qualifiedKey.split(":");
        return key[0].equals("secure") ? Settings.Secure.getString(context.getContentResolver(), key[1])
                : Settings.System.getString(context.getContentResolver(), key[1]);
    }
    private static boolean write(Context context, String qualifiedKey, String value) {
        String[] key = qualifiedKey.split(":");
        return key[0].equals("secure") ? Settings.Secure.putString(context.getContentResolver(), key[1], value)
                : Settings.System.putString(context.getContentResolver(), key[1], value);
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
        return "adb shell \"" + RefreshRatePlan.shell(RefreshRatePlan.changes(targetRate, null)) + "\"";
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
