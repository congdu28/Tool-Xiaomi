package app.cd.mitool.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import java.io.DataOutputStream;

/**
 * Helper to unlock 120 FPS and bypass Xiaomi's 89-90 FPS thermal / Joyose caps.
 */
public class GamingFpsHelper {

    public static final String JOYOSE_PKG = "com.xiaomi.joyose";
    public static final String POWERKEEPER_PKG = "com.miui.powerkeeper";

    public static final String CMD_DISABLE_JOYOSE = "adb shell pm disable-user --user 0 com.xiaomi.joyose";
    public static final String CMD_ENABLE_JOYOSE = "adb shell pm enable com.xiaomi.joyose";

    public static final String[] LIEN_QUAN_PACKAGES = {
            "com.garena.game.kgvn",          // Liên Quân Mobile (Việt Nam)
            "com.levelinfinite.sgameGlobal", // Honor of Kings (Quốc tế)
            "com.tencent.tmgp.sgame",        // Vương Giả Vinh Diệu (Nội địa)
            "com.garena.game.kgtw",          // Truyền Thuyết Đối Quyết (Đài Loan)
            "com.garena.game.kgth",          // RoV (Thái Lan)
            "com.ngame.allstar.eu"           // Arena of Valor (Âu Mỹ)
    };

    public enum JoyoseStatus {
        ACTIVE_LOCKED,     // Joyose đang chạy ngầm -> Bóp 89-90 FPS
        DISABLED_UNLOCKED, // Joyose đã vô hiệu hóa -> Đã mở khóa 120 FPS
        NOT_FOUND          // Không tìm thấy Joyose trên thiết bị
    }

    /**
     * Check current Joyose status on this device.
     */
    public static JoyoseStatus checkJoyoseStatus(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(JOYOSE_PKG, 0);
            int setting = pm.getApplicationEnabledSetting(JOYOSE_PKG);

            if (setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                return JoyoseStatus.DISABLED_UNLOCKED;
            }

            if (!ai.enabled) {
                return JoyoseStatus.DISABLED_UNLOCKED;
            }

            return JoyoseStatus.ACTIVE_LOCKED;
        } catch (PackageManager.NameNotFoundException e) {
            // Either uninstalled or not Xiaomi
            return JoyoseStatus.NOT_FOUND;
        } catch (Exception e) {
            return JoyoseStatus.NOT_FOUND;
        }
    }

    /**
     * Apply maximum 120Hz lock and speed mode settings to prevent frame rate drops.
     */
    public static boolean applyGaming120Mode(Context context) {
        ContentResolver cr = context.getContentResolver();
        boolean wroteAny = false;

        // 1. Force 120Hz min and peak in Secure Settings
        try {
            Settings.Secure.putFloat(cr, "min_refresh_rate", 120.0f);
            Settings.Secure.putFloat(cr, "peak_refresh_rate", 120.0f);
            Settings.Secure.putInt(cr, "user_refresh_rate", 120);
            Settings.Secure.putInt(cr, "game_mode_refresh_rate", 120);
            wroteAny = true;
        } catch (Exception ignored) {
        }

        // 2. Force 120Hz in System Settings & disable thermal refresh downclock
        try {
            Settings.System.putInt(cr, "user_refresh_rate", 120);
            Settings.System.putInt(cr, "min_refresh_rate", 120);
            Settings.System.putInt(cr, "peak_refresh_rate", 120);
            Settings.System.putInt(cr, "thermal_limit_refresh_rate", 0);
            wroteAny = true;
        } catch (Exception ignored) {
        }

        // 3. Disable FPS limit and smart FPS throttling in Global Settings
        try {
            Settings.Global.putInt(cr, "speed_mode", 1);
            Settings.Global.putInt(cr, "fps_limit", 0);
            Settings.Global.putInt(cr, "smart_fps", 0);
            wroteAny = true;
        } catch (Exception ignored) {
        }

        // 4. High performance mode in System Settings
        try {
            Settings.System.putString(cr, "power_mode", "high");
        } catch (Exception ignored) {
        }

        return wroteAny;
    }

    /**
     * Try disabling Joyose directly with root, or copy ADB command to clipboard.
     * Returns true if root succeeded, false if copied to clipboard.
     */
    public static boolean handleDisableJoyose(Context context) {
        boolean rootSuccess = runRootCommand("pm disable-user --user 0 " + JOYOSE_PKG);
        if (rootSuccess) {
            Toast.makeText(context, "🔥 Đã vô hiệu hóa Joyose bằng quyền Root! Khóa 89 FPS đã được gỡ bỏ.", Toast.LENGTH_LONG).show();
            return true;
        } else {
            copyToClipboard(context, "Lệnh tắt Joyose", CMD_DISABLE_JOYOSE);
            Toast.makeText(context, "📋 Đã sao chép lệnh ADB tắt Joyose! Hãy chạy lệnh này trên máy tính để mở khóa 120 FPS.", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * Try enabling Joyose directly with root, or copy ADB command to clipboard.
     */
    public static boolean handleEnableJoyose(Context context) {
        boolean rootSuccess = runRootCommand("pm enable " + JOYOSE_PKG);
        if (rootSuccess) {
            Toast.makeText(context, "🔄 Đã bật lại Joyose bằng quyền Root.", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            copyToClipboard(context, "Lệnh bật Joyose", CMD_ENABLE_JOYOSE);
            Toast.makeText(context, "📋 Đã sao chép lệnh ADB bật lại Joyose!", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Open Battery & Performance (com.miui.powerkeeper) App Info so user can tap "Clear Data".
     */
    public static boolean openPowerKeeperAppInfo(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + POWERKEEPER_PKG));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    /**
     * Open Xiaomi Per-App Refresh Rate settings screen so user can choose 120Hz for Lien Quan.
     */
    public static boolean openPerAppRefreshRate(Context context) {
        String[][] attempts = {
                {"com.android.settings", "com.android.settings.SubSettings"},
                {"com.android.settings", "com.android.settings.display.AppRefreshRateSettings"},
                {"com.android.settings", "com.android.settings.display.RefreshRateSettings"}
        };
        for (String[] comp : attempts) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(comp[0], comp[1]));
                intent.putExtra(":settings:show_fragment", "com.android.settings.display.AppRefreshRateSettings");
                intent.putExtra(":android:show_fragment", "com.android.settings.display.AppRefreshRateSettings");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {
            }
        }
        return RefreshRateHelper.openRefreshRateSettings(context);
    }

    /**
     * Launch Arena of Valor / Liên Quân Mobile if installed.
     */
    public static boolean launchLienQuanMobile(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String pkg : LIEN_QUAN_PACKAGES) {
            try {
                Intent intent = pm.getLaunchIntentForPackage(pkg);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /**
     * Check if Lien Quan Mobile or Honor of Kings is installed.
     */
    public static boolean isLienQuanInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String pkg : LIEN_QUAN_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                return true;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return false;
    }

    /**
     * Execute shell command with root if available.
     */
    private static boolean runRootCommand(String command) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            int exitCode = process.waitFor();
            return exitCode == 0;
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
     * Copy text to clipboard.
     */
    public static void copyToClipboard(Context context, String label, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                ClipData clip = ClipData.newPlainText(label, text);
                cm.setPrimaryClip(clip);
            }
        } catch (Exception ignored) {
        }
    }
}
