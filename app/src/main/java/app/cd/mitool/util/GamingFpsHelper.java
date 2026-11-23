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
    public interface ThermalApplyCallback { void onComplete(ThermalApplyResult result); }
    public interface ThermalInspectCallback { void onComplete(ThermalInspectResult result); }

    public static final class ThermalApplyResult {
        public final boolean success;
        public final String headline;
        public final String details;

        public ThermalApplyResult(boolean success, String headline, String details) {
            this.success = success;
            this.headline = headline;
            this.details = details;
        }
    }

    public static final class ThermalInspectResult {
        public final int thermalStatus;
        public final float batteryTempC;
        public final JoyoseStatus joyoseStatus;
        public final boolean hasShizuku;
        public final boolean interventionActive;
        public final String gameConfigSummary;

        public ThermalInspectResult(int thermalStatus, float batteryTempC, JoyoseStatus joyoseStatus,
                                    boolean hasShizuku, boolean interventionActive, String gameConfigSummary) {
            this.thermalStatus = thermalStatus;
            this.batteryTempC = batteryTempC;
            this.joyoseStatus = joyoseStatus;
            this.hasShizuku = hasShizuku;
            this.interventionActive = interventionActive;
            this.gameConfigSummary = gameConfigSummary;
        }
    }

    private static final java.util.concurrent.ExecutorService EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());

    private static ShizukuHelper.ShellResult runPrivilegedCommand(String command, long timeoutMs) {
        if (ShizukuHelper.hasShizukuPermission()) {
            return ShizukuHelper.executeCommand(command, timeoutMs);
        }
        boolean rootOk = runRootCommand(command);
        return new ShizukuHelper.ShellResult(rootOk ? 0 : -1, "", rootOk ? "" : "Chưa có quyền Shizuku hoặc Root");
    }

    private static boolean ensurePrivilegedReady(Activity activity, ActionCallback onNotReady) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return false;
        if (ShizukuHelper.isShizukuRunning() && !ShizukuHelper.hasShizukuPermission()) {
            ShizukuHelper.requestShizukuPermission(activity);
            if (onNotReady != null) onNotReady.onComplete(false, "Cấp quyền Shizuku rồi chọn lại thao tác.");
            return false;
        }
        return true;
    }

    private static void perform(Activity activity, String command, JoyoseStatus expected, String message, ActionCallback callback) {
        if (!ensurePrivilegedReady(activity, callback)) return;
        EXECUTOR.execute(() -> {
            boolean success = runPrivilegedCommand(command, 15000).success;
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

    public static int getThermalStatus(Context context) {
        if (context == null || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return -1;
        try {
            android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null ? pm.getCurrentThermalStatus() : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static String findInstalledLienQuanPackage(Context context) {
        if (context == null) return null;
        PackageManager pm = context.getPackageManager();
        for (String pkg : LIEN_QUAN_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return null;
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        if (context == null || !GameThermalPolicy.validPackage(pkg)) return false;
        try {
            context.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    public static String resolveTargetGamePackage(Context context) {
        if (context == null) return LIEN_QUAN_PACKAGES[0];
        PreferencesHelper prefs = new PreferencesHelper(context);
        String saved = prefs.getGameOptTargetPackage();
        if (isPackageInstalled(context, saved)) return saved;
        String lq = findInstalledLienQuanPackage(context);
        if (lq != null) return lq;
        java.util.Set<String> library = prefs.getCustomGamePackages();
        for (String pkg : library) {
            if (isPackageInstalled(context, pkg)) return pkg;
        }
        java.util.List<String> detected = detectInstalledGames(context);
        if (!detected.isEmpty()) return detected.get(0);
        return LIEN_QUAN_PACKAGES[0];
    }

    public static void inspectThermalState(Context context, String gamePkg, ThermalInspectCallback callback) {
        if (context == null || callback == null) return;
        final Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            int thermalStatus = getThermalStatus(app);
            float tempC = BatteryHelper.getBatteryInfo(app).temperature;
            JoyoseStatus joyoseStatus = checkJoyoseStatus(app);
            boolean hasShizuku = ShizukuHelper.hasShizukuPermission();
            boolean interventionActive = false;
            String summary = "";

            if (hasShizuku && GameThermalPolicy.validPackage(gamePkg) && isPackageInstalled(app, gamePkg)) {
                int sdk = android.os.Build.VERSION.SDK_INT;
                String listCmd = GameThermalPolicy.buildListConfigsCommand(sdk, gamePkg);
                if (listCmd != null) {
                    ShizukuHelper.ShellResult res = ShizukuHelper.executeCommand(listCmd, 10000);
                    GameThermalPolicy.GameConfigVerification v =
                            GameThermalPolicy.parseGameConfigsOutput(res.stdout, res.stderr, 0, GameThermalPolicy.DOWNSCALE_DISABLE);
                    summary = v.summary;
                    String lower = summary.toLowerCase(java.util.Locale.US);
                    interventionActive = v.supported && (lower.contains("fps=") || lower.contains("downscale"));
                } else {
                    String modesCmd = GameThermalPolicy.buildListModesCommand(sdk, gamePkg);
                    if (modesCmd != null) {
                        ShizukuHelper.ShellResult res = ShizukuHelper.executeCommand(modesCmd, 10000);
                        summary = res.stdout.isEmpty() ? res.stderr : res.stdout;
                    }
                }
            }

            final ThermalInspectResult result = new ThermalInspectResult(
                    thermalStatus, tempC, joyoseStatus, hasShizuku, interventionActive, summary);
            MAIN.post(() -> callback.onComplete(result));
        });
    }

    public static void applyThermalOptimization(Activity activity, String gamePkg,
                                                GameThermalPolicy.Config config, ThermalApplyCallback callback) {
        if (!ensurePrivilegedReady(activity, (ok, msg) -> {
            if (callback != null) callback.onComplete(new ThermalApplyResult(false, msg, ""));
        })) return;

        final Context app = activity.getApplicationContext();
        EXECUTOR.execute(() -> {
            ThermalApplyResult result = executeThermalOptimization(app, gamePkg, config);
            MAIN.post(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (callback != null) callback.onComplete(result);
            });
        });
    }

    public static void restoreThermalOptimization(Activity activity, String gamePkg, ThermalApplyCallback callback) {
        if (!ensurePrivilegedReady(activity, (ok, msg) -> {
            if (callback != null) callback.onComplete(new ThermalApplyResult(false, msg, ""));
        })) return;

        final Context app = activity.getApplicationContext();
        EXECUTOR.execute(() -> {
            ThermalApplyResult result = executeThermalRestore(app, gamePkg);
            MAIN.post(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (callback != null) callback.onComplete(result);
            });
        });
    }

    private static ThermalApplyResult executeThermalOptimization(Context app, String gamePkg, GameThermalPolicy.Config config) {
        if (!GameThermalPolicy.validPackage(gamePkg) || !isPackageInstalled(app, gamePkg)) {
            return new ThermalApplyResult(false,
                    "Chưa tìm thấy gói game hợp lệ trên thiết bị.",
                    "Hãy chọn game đã cài (ví dụ Liên Quân Mobile) trước khi áp dụng.");
        }
        if (!ShizukuHelper.hasShizukuPermission() && !runRootCommand("id")) {
            return new ThermalApplyResult(false,
                    "Cần cấp quyền Shizuku (hoặc Root) để can thiệp hệ thống.",
                    "Vui lòng khởi chạy Shizuku và cấp quyền cho Mi Tool.");
        }

        int sdk = android.os.Build.VERSION.SDK_INT;
        PreferencesHelper prefs = new PreferencesHelper(app);
        StringBuilder details = new StringBuilder();
        boolean anyApplied = false;

        // 1. Keep Joyose enabled so Xiaomi's hardware thermal curve remains intact
        if (config.keepJoyoseEnabled) {
            JoyoseStatus joyose = checkJoyoseStatus(app);
            if (joyose == JoyoseStatus.DISABLED_UNLOCKED) {
                boolean enabled = runPrivilegedCommand("pm enable --user 0 " + JOYOSE_PKG, 15000).success
                        && checkJoyoseStatus(app) == JoyoseStatus.ACTIVE_LOCKED;
                details.append(enabled
                        ? "• Joyose: Đã bật lại để bảo vệ nhiệt độ (tránh nóng máy sụt FPS).\n"
                        : "• Joyose: Đang tắt, chưa bật lại được qua shell.\n");
                anyApplied |= enabled;
            } else if (joyose == JoyoseStatus.ACTIVE_LOCKED) {
                details.append("• Joyose: Giữ nguyên hoạt động (bảo vệ nhiệt, không xóa PowerKeeper).\n");
            }
        }

        // 2. GameManager Mode & Interventions (Android 12+ / 13+)
        String modeCmd = GameThermalPolicy.buildGameModeCommand(sdk, gamePkg, config.gameMode);
        if (modeCmd != null) {
            ShizukuHelper.ShellResult modeRes = runPrivilegedCommand(modeCmd, 15000);
            details.append(modeRes.success
                    ? "• Game Mode (" + config.gameMode + "): Đã gửi lệnh cho " + gamePkg + ".\n"
                    : "• Game Mode: ROM từ chối (" + firstLine(modeRes.stderr, modeRes.stdout) + ").\n");
            anyApplied |= modeRes.success;
        } else {
            details.append("• Game Mode: Cần Android 12+ (thiết bị hiện tại API " + sdk + ").\n");
        }

        String setCmd = GameThermalPolicy.buildGameInterventionCommand(sdk, gamePkg, config.targetFps, config.downscale);
        if (setCmd != null) {
            ShizukuHelper.ShellResult setRes = runPrivilegedCommand(setCmd, 15000);
            String listCmd = GameThermalPolicy.buildListConfigsCommand(sdk, gamePkg);
            ShizukuHelper.ShellResult listRes = listCmd != null
                    ? runPrivilegedCommand(listCmd, 10000)
                    : new ShizukuHelper.ShellResult(-1, "", "");
            GameThermalPolicy.GameConfigVerification verified =
                    GameThermalPolicy.parseGameConfigsOutput(listRes.stdout, listRes.stderr, config.targetFps, config.downscale);

            if (setRes.success && verified.activeOverride) {
                anyApplied = true;
                String scaleDesc = GameThermalPolicy.DOWNSCALE_DISABLE.equals(config.downscale)
                        ? "100% gốc" : Math.round(Float.parseFloat(config.downscale) * 100) + "%";
                String fpsDesc = config.targetFps > 0 ? config.targetFps + " FPS" : "Mặc định";
                details.append("• GameManager (Xác minh OK): Trần " + fpsDesc + ", Scale GPU " + scaleDesc + ".\n");
            } else if (setRes.success && verified.supported) {
                details.append("• GameManager: Lệnh đã chạy nhưng ROM/game chưa khớp cấu hình (" + verified.summary + ").\n");
            } else {
                String reason = !setRes.success ? firstLine(setRes.stderr, setRes.stdout) : verified.summary;
                details.append("• GameManager: Game/ROM không áp dụng can thiệp (" + reason + ").\n");
            }
        } else {
            details.append("• Giảm tải GPU / Trần FPS (`cmd game set`): Cần Android 13+ (API " + sdk + ").\n");
        }

        // 3. Optional Display Refresh Rate Alignment (only if panel exposes exact matching step)
        if (config.alignDisplayHz && config.targetFps > 0) {
            int alignedHz = GameThermalPolicy.recommendedDisplayHz(
                    config.targetFps, RefreshRateHelper.getSupportedRefreshRates(app));
            if (alignedHz > 0) {
                boolean hzOk = RefreshRateHelper.setRefreshRate(app, alignedHz);
                if (hzOk) {
                    prefs.setTargetRefreshRate(alignedHz);
                    prefs.setGameOptHzApplied(true);
                    anyApplied = true;
                    details.append("• Tần số quét màn hình: Đã đồng bộ " + alignedHz + " Hz (có lưu snapshot khôi phục).\n");
                } else {
                    details.append("• Tần số quét màn hình: Chưa ghi được " + alignedHz + " Hz.\n");
                }
            } else {
                details.append("• Tần số quét màn hình: Giữ nguyên (tấm nền không có mức " + config.targetFps + " Hz riêng).\n");
            }
        }

        // 4. Optional ART speed-profile pre-compilation (skip if already speed-profile/speed to avoid compile heat)
        if (config.precompileProfile) {
            String inspectCmd = GameThermalPolicy.buildDexoptInspectCommand(gamePkg);
            ShizukuHelper.ShellResult inspectRes = runPrivilegedCommand(inspectCmd, 15000);
            if (inspectRes.success && GameThermalPolicy.isAlreadySpeedProfileCompiled(inspectRes.stdout)) {
                anyApplied = true;
                details.append("• Biên dịch ART: Đã đạt chuẩn speed-profile/speed sẵn (bỏ qua biên dịch lại để tránh nóng máy).\n");
            } else {
                String compileCmd = GameThermalPolicy.buildCompileSpeedProfileCommand(gamePkg);
                ShizukuHelper.ShellResult compileRes = runPrivilegedCommand(compileCmd, 45000);
                if (compileRes.success) {
                    anyApplied = true;
                    details.append("• Biên dịch ART (speed-profile): Hoàn tất, giảm giật khung hình do JIT khi giao tranh.\n");
                } else {
                    details.append("• Biên dịch ART: Chưa hoàn tất (" + firstLine(compileRes.stderr, compileRes.stdout) + ").\n");
                }
            }
        }

        // 5. Safe background cached process trim
        if (config.trimCachedProcesses) {
            ShizukuHelper.ShellResult trimRes = runPrivilegedCommand(GameThermalPolicy.buildTrimCachedCommand(), 10000);
            if (trimRes.success) {
                anyApplied = true;
                details.append("• Tiến trình đệm: Đã dọn cache nền an toàn (`am kill-all`, không ngắt thông báo).\n");
            } else {
                details.append("• Tiến trình đệm: Không chạy được `am kill-all`.\n");
            }
        }

        String summaryText = details.toString().trim();
        prefs.setGameOptTargetPackage(gamePkg);
        prefs.setGameOptLastSummary(summaryText);

        String headline = anyApplied
                ? "Đã áp dụng tối ưu chống nóng cho " + gamePkg + ". Hãy khởi động lại game nếu game đang mở."
                : "Chưa áp dụng được can thiệp nào lên " + gamePkg + ". Kiểm tra quyền Shizuku và hỗ trợ của ROM.";
        return new ThermalApplyResult(anyApplied, headline, summaryText);
    }

    private static ThermalApplyResult executeThermalRestore(Context app, String gamePkg) {
        if (!ShizukuHelper.hasShizukuPermission() && !runRootCommand("id")) {
            return new ThermalApplyResult(false,
                    "Cần quyền Shizuku hoặc Root để khôi phục cấu hình game.",
                    "");
        }
        int sdk = android.os.Build.VERSION.SDK_INT;
        PreferencesHelper prefs = new PreferencesHelper(app);
        StringBuilder details = new StringBuilder();
        boolean ok = false;

        if (GameThermalPolicy.validPackage(gamePkg)) {
            String resetCmd = GameThermalPolicy.buildGameResetCommand(sdk, gamePkg);
            if (resetCmd != null) {
                ShizukuHelper.ShellResult resetRes = runPrivilegedCommand(resetCmd, 15000);
                if (resetRes.success) {
                    ok = true;
                    details.append("• GameManager: Đã khôi phục mặc định cho " + gamePkg + ".\n");
                } else {
                    details.append("• GameManager: Chưa đặt lại được (" + firstLine(resetRes.stderr, resetRes.stdout) + ").\n");
                }
            }
        }

        if (checkJoyoseStatus(app) == JoyoseStatus.DISABLED_UNLOCKED) {
            boolean joyoseOk = runPrivilegedCommand("pm enable --user 0 " + JOYOSE_PKG, 15000).success
                    && checkJoyoseStatus(app) == JoyoseStatus.ACTIVE_LOCKED;
            ok |= joyoseOk;
            details.append(joyoseOk
                    ? "• Joyose: Đã bật lại kiểm soát nhiệt mặc định.\n"
                    : "• Joyose: Chưa bật lại được.\n");
        }

        if (prefs.isGameOptHzApplied()) {
            boolean hzRestored = RefreshRateHelper.setRefreshRate(app, 0);
            if (hzRestored) {
                prefs.setTargetRefreshRate(0);
                prefs.setGameOptHzApplied(false);
                ok = true;
                details.append("• Tần số quét: Đã khôi phục cấu hình trước khi tối ưu game.\n");
            } else {
                details.append("• Tần số quét: Chưa khôi phục được Hz.\n");
            }
        }

        String summary = details.toString().trim();
        prefs.setGameOptLastSummary(summary.isEmpty() ? "Đã khôi phục cấu hình mặc định." : summary);
        return new ThermalApplyResult(ok,
                ok ? "Đã khôi phục cấu hình mặc định cho game & màn hình." : "Không có thay đổi nào được khôi phục.",
                summary);
    }

    private static String firstLine(String primary, String secondary) {
        String s = (primary != null && !primary.trim().isEmpty()) ? primary.trim()
                : (secondary != null ? secondary.trim() : "");
        if (s.isEmpty()) return "lỗi không rõ";
        int nl = s.indexOf('\n');
        return nl > 0 ? s.substring(0, nl).trim() : s;
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
