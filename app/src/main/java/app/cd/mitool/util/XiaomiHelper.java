package app.cd.mitool.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class XiaomiHelper {

    /**
     * Check if device is running Xiaomi MIUI or HyperOS.
     */
    public static boolean isXiaomiDevice() {
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer != null && (manufacturer.equalsIgnoreCase("Xiaomi") || manufacturer.equalsIgnoreCase("Redmi") || manufacturer.equalsIgnoreCase("POCO"))) {
            return true;
        }
        return getSystemProperty("ro.miui.ui.version.name") != null || getSystemProperty("ro.mi.os.version.name") != null;
    }

    private static String getSystemProperty(String propName) {
        String line = null;
        BufferedReader input = null;
        try {
            java.lang.Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
        return (line != null && !line.trim().isEmpty()) ? line.trim() : null;
    }

    /**
     * Check if overlay permission (SYSTEM_ALERT_WINDOW) is granted.
     */
    public static boolean canDrawOverlays(Context context) {
        return Settings.canDrawOverlays(context);
    }

    /**
     * Open system Overlay permission screen for this app.
     */
    public static void openOverlaySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Intent fallback = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fallback);
        }
    }

    /**
     * Open Xiaomi Autostart management screen.
     */
    public static void openAutostartSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(new ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setComponent(new ComponentName("com.miui.securitycenter",
                        "com.miui.powercenter.autostart.AutoStartManagementActivity"));
                context.startActivity(intent);
            } catch (Exception e2) {
                openAppDetailsSettings(context);
                Toast.makeText(context, "Vui lòng tìm mục 'Tự khởi chạy' (Autostart) trong cài đặt ứng dụng.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Open Xiaomi Battery Saver ("Không giới hạn" / No restrictions) screen.
     */
    public static void openBatterySaverSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(new ComponentName("com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"));
            intent.putExtra("package_name", context.getPackageName());
            intent.putExtra("package_label", "Mi Tool");
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e2) {
                openAppDetailsSettings(context);
                Toast.makeText(context, "Vui lòng chọn 'Tiết kiệm pin' -> 'Không giới hạn'.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Open Xiaomi Lock Screen / Background pop-up permission screen.
     */
    public static void openLockScreenPermSettings(Context context) {
        try {
            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            openAppDetailsSettings(context);
            Toast.makeText(context, "Vui lòng bật quyền 'Hiển thị trên màn hình khóa' và 'Cửa sổ bật lên'.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Open Xiaomi hidden battery info testing screen (*#*#4636#*#*).
     */
    public static void openHiddenBatteryInfo(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.TestingSettings"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e2) {
                Toast.makeText(context, "Bạn có thể bấm *#*#4636#*#* trong bàn phím quay số để xem.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static void openAppDetailsSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
