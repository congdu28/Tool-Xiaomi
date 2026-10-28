package app.cd.mitool.util;

import app.cd.mitool.R;
import android.app.Activity;
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

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

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
            return JoyoseStatus.NOT_FOUND;
        } catch (Exception e) {
            return JoyoseStatus.NOT_FOUND;
        }
    }

    public interface ActionCallback { void onComplete(boolean success, String message); }
    private static final java.util.concurrent.ExecutorService EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());

    private static void perform(Activity activity, String command, JoyoseStatus expected, String message, ActionCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (ShizukuHelper.isShizukuRunning() && !ShizukuHelper.hasShizukuPermission()) {
            ShizukuHelper.requestShizukuPermission(activity);
            if (callback != null) callback.onComplete(false, "Cấp quyền Shizuku rồi chọn lại thao tác.");
            return;
        }
        EXECUTOR.execute(() -> {
            boolean success = ShizukuHelper.hasShizukuPermission()
                    ? ShizukuHelper.executeCommand(command).success : runRootCommand(command);
            if (success && expected != null) success = checkJoyoseStatus(activity) == expected;
            final boolean verified = success;
            MAIN.post(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (callback != null) callback.onComplete(verified, verified ? message
                        : "Chưa xác nhận được thay đổi. Kiểm tra quyền Shizuku/Root và khả năng hỗ trợ của ROM.");
            });
        });
    }
    public static void autoUnlock120Fps(Activity activity, ActionCallback callback) {
        perform(activity, "pm disable-user --user 0 " + JOYOSE_PKG + " && pm clear " + POWERKEEPER_PKG,
                JoyoseStatus.DISABLED_UNLOCKED, "Đã tắt Joyose và xóa dữ liệu Pin và Hiệu suất. Chưa xác định FPS của trò chơi.", callback);
    }
    public static void autoRestoreJoyose(Activity activity, ActionCallback callback) { enableJoyose(activity, callback); }
    public static void disableJoyose(Activity activity, ActionCallback callback) {
        perform(activity, "pm disable-user --user 0 " + JOYOSE_PKG, JoyoseStatus.DISABLED_UNLOCKED, "Đã xác nhận Joyose được tắt.", callback);
    }
    public static void enableJoyose(Activity activity, ActionCallback callback) {
        perform(activity, "pm enable --user 0 " + JOYOSE_PKG, JoyoseStatus.ACTIVE_LOCKED, "Đã xác nhận Joyose được bật. Dữ liệu đã xóa không được phục hồi.", callback);
    }
    public static void clearPowerKeeper(Activity activity, ActionCallback callback) {
        perform(activity, "pm clear " + POWERKEEPER_PKG, null, "Lệnh xóa dữ liệu Pin và Hiệu suất đã hoàn tất.", callback);
    }

    /**
     * Guide user to start or install Shizuku when automation is requested.
     */
    public static void showShizukuSetupDialog(Activity activity, String fallbackCommand) {
        if (activity == null || activity.isFinishing()) return;
        boolean installed = ShizukuHelper.isShizukuInstalled(activity);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(installed ? "🚀 Cần Khởi Chạy Shizuku" : "📥 Cài Đặt Shizuku")
                .setMessage(installed
                        ? "Dịch vụ Shizuku chưa chạy.\n\nĐể kích hoạt tính năng tự động hóa 1-chạm không cần máy tính, bạn chỉ cần mở ứng dụng Shizuku và bấm 'Bắt đầu' (Start) qua Ghép đôi không dây (Wireless Debugging).\n\nHoặc bạn có thể sao chép lệnh ADB để chạy thủ công từ máy tính."
                        : "Ứng dụng cần công cụ Shizuku để tự động hóa 1-chạm mà không cần máy tính.\n\nShizuku là giải pháp ADB on-device an toàn, mã nguồn mở, không cần root.")
                .setPositiveButton(installed ? "🚀 Mở Shizuku" : "📥 Tải Shizuku", (dialog, which) -> {
                    if (installed) {
                        ShizukuHelper.openShizukuApp(activity);
                    } else {
                        ShizukuHelper.openShizukuDownload(activity);
                    }
                })
                .setNegativeButton(R.string.action_close, null);

        if (fallbackCommand != null && !fallbackCommand.isEmpty()) {
            builder.setNeutralButton(R.string.copy_action_60dfab3, (dialog, which) -> {
                copyToClipboard(activity, "Lệnh ADB", fallbackCommand);
                Toast.makeText(activity, R.string.message_2c0b7fb956, Toast.LENGTH_SHORT).show();
            });
        }

        builder.show();
    }

    /**
     * Try disabling Joyose directly with root, or Shizuku 1-tap, or copy ADB command to clipboard.
     * Preserved for backward compatibility.
     */
    public static boolean handleDisableJoyose(Context context) {
        if (context instanceof Activity) {
            disableJoyose((Activity) context, (success, message) -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
            return true;
        }
        boolean rootSuccess = runRootCommand("pm disable-user --user 0 " + JOYOSE_PKG);
        if (rootSuccess) {
            Toast.makeText(context, R.string.message_71e4bd4b2a, Toast.LENGTH_LONG).show();
            return true;
        } else if (ShizukuHelper.hasShizukuPermission()) {
            ShizukuHelper.setJoyoseEnabled(false, (success, message) -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
            return true;
        } else {
            copyToClipboard(context, "Lệnh tắt Joyose", CMD_DISABLE_JOYOSE);
            Toast.makeText(context, R.string.message_6e2403a5fe, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * Try enabling Joyose directly with root, or Shizuku 1-tap, or copy ADB command to clipboard.
     * Preserved for backward compatibility.
     */
    public static boolean handleEnableJoyose(Context context) {
        if (context instanceof Activity) {
            enableJoyose((Activity) context, (success, message) -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
            return true;
        }
        boolean rootSuccess = runRootCommand("pm enable " + JOYOSE_PKG);
        if (rootSuccess) {
            Toast.makeText(context, R.string.message_64be67e4a1, Toast.LENGTH_SHORT).show();
            return true;
        } else if (ShizukuHelper.hasShizukuPermission()) {
            ShizukuHelper.setJoyoseEnabled(true, (success, message) -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
            return true;
        } else {
            copyToClipboard(context, "Lệnh bật Joyose", CMD_ENABLE_JOYOSE);
            Toast.makeText(context, R.string.message_7d53463ea4, Toast.LENGTH_SHORT).show();
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
    public static boolean runRootCommand(String command) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
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

    public static class GameEntry {
        public String packageName;
        public String title;
        public android.graphics.drawable.Drawable icon;

        public GameEntry(String packageName, String title, android.graphics.drawable.Drawable icon) {
            this.packageName = packageName;
            this.title = title;
            this.icon = icon;
        }
    }

    public static final String[] POPULAR_GAME_PACKAGES = {
            "com.garena.game.kgvn",              // Liên Quân Mobile (Việt Nam)
            "com.riotgames.league.wildriftvn",     // LMHT: Tốc Chiến (VN)
            "com.riotgames.league.wildrift",       // Wild Rift (Global)
            "com.vng.pubgmobile",                // PUBG Mobile VN
            "com.tencent.ig",                    // PUBG Mobile Global
            "com.dts.freefireth",                // Free Fire VN/SEA
            "com.dts.freefiremax",               // Free Fire MAX
            "com.miHoYo.GenshinImpact",          // Genshin Impact Global
            "com.miHoYo.Yuanshen",               // Genshin Impact CN
            "com.miHoYo.hkrpgoversea",           // Honkai: Star Rail
            "com.HoYoverse.Nap",                 // Zenless Zone Zero
            "com.levelinfinite.sgameGlobal",     // Honor of Kings
            "com.tencent.tmgp.sgame",            // Vương Giả Vinh Diệu
            "com.vng.codmvn",                    // Call of Duty Mobile VN
            "com.activision.callofduty.shooter", // COD Mobile Global
            "com.roblox.client",                 // Roblox
            "com.haegin.playtogether",           // Play Together
            "com.carxtech.sr"                    // CarX Street
    };

    /**
     * Detect installed games on this device (popular games + apps marked as CATEGORY_GAME).
     */
    public static java.util.List<String> detectInstalledGames(Context context) {
        java.util.List<String> detected = new java.util.ArrayList<>();
        if (context == null) return detected;
        PackageManager pm = context.getPackageManager();

        // 1. Check popular game packages
        for (String pkg : POPULAR_GAME_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                if (!detected.contains(pkg)) {
                    detected.add(pkg);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        // 2. Scan installed applications for CATEGORY_GAME
        try {
            java.util.List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo ai : installed) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                    continue; // Skip non-updated system apps
                }
                boolean isGame = false;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    if (ai.category == ApplicationInfo.CATEGORY_GAME) {
                        isGame = true;
                    }
                }
                if ((ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
                    isGame = true;
                }
                if (isGame && !detected.contains(ai.packageName)) {
                    detected.add(ai.packageName);
                }
            }
        } catch (Throwable ignored) {
        }

        return detected;
    }

    /**
     * Resolve GameEntry (Icon, Title) for a given package name.
     */
    public static GameEntry getGameEntry(Context context, String packageName) {
        if (context == null || packageName == null) return null;
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            CharSequence label = pm.getApplicationLabel(ai);
            android.graphics.drawable.Drawable icon = pm.getApplicationIcon(ai);
            return new GameEntry(packageName, label != null ? label.toString() : packageName, icon);
        } catch (PackageManager.NameNotFoundException e) {
            return new GameEntry(packageName, packageName, ContextCompat.getDrawable(context, app.cd.mitool.R.drawable.ic_gamepad));
        } catch (Throwable t) {
            return new GameEntry(packageName, packageName, null);
        }
    }

    /**
     * Launch game by package name.
     */
    public static boolean launchGame(Context context, String packageName) {
        if (context == null || packageName == null) return false;
        PackageManager pm = context.getPackageManager();
        try {
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
