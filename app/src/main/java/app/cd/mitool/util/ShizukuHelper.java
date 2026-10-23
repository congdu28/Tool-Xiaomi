package app.cd.mitool.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class ShizukuHelper {

    public static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";
    public static final int SHIZUKU_REQ_CODE = 8801;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface CommandCallback {
        void onComplete(boolean success, String message);
    }

    public static class ShellResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;
        public final boolean success;

        public ShellResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout != null ? stdout.trim() : "";
            this.stderr = stderr != null ? stderr.trim() : "";
            this.success = (exitCode == 0);
        }
    }

    /**
     * Check if the Shizuku manager app is installed on this device.
     */
    public static boolean isShizukuInstalled(Context context) {
        if (isShizukuRunning()) return true;
        if (context == null) return false;
        try {
            context.getPackageManager().getPackageInfo(SHIZUKU_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            try {
                return context.getPackageManager().getLaunchIntentForPackage(SHIZUKU_PKG) != null;
            } catch (Throwable ignored) {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Check if Shizuku service / binder is running and responsive.
     */
    public static boolean isShizukuRunning() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isShizukuAvailable() {
        return isShizukuRunning();
    }

    /**
     * Check if Shizuku permission has been granted to this application.
     */
    public static boolean hasShizukuPermission() {
        if (!isShizukuRunning()) return false;
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Request Shizuku permission or guide user to start Shizuku.
     */
    public static void requestShizukuPermission(Activity activity) {
        if (activity == null) return;
        if (!isShizukuInstalled(activity)) {
            openShizukuDownload(activity);
            return;
        }
        if (!isShizukuRunning()) {
            openShizukuApp(activity);
            return;
        }
        try {
            if (Shizuku.isPreV11()) {
                openShizukuApp(activity);
            } else {
                Shizuku.requestPermission(SHIZUKU_REQ_CODE);
            }
        } catch (Throwable t) {
            openShizukuApp(activity);
        }
    }

    /**
     * Open installed Shizuku app.
     */
    public static boolean openShizukuApp(Context context) {
        if (context == null) return false;
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(SHIZUKU_PKG);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Open browser to download or learn about Shizuku.
     */
    public static void openShizukuDownload(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Execute shell command synchronously via Shizuku binder process reflection.
     * Note: Must be called from a background thread!
     */
    public static ShellResult executeCommand(String command) {
        if (!hasShizukuPermission()) {
            return new ShellResult(-1, "", "Shizuku chưa chạy hoặc chưa cấp quyền");
        }

        Process process = null;
        try {
            Method newProcessMethod = Shizuku.class.getDeclaredMethod(
                    "newProcess",
                    String[].class,
                    String[].class,
                    String.class
            );
            newProcessMethod.setAccessible(true);
            String[] cmd = new String[]{"sh", "-c", command};
            process = (Process) newProcessMethod.invoke(null, cmd, null, null);

            if (process == null) {
                return new ShellResult(-1, "", "Không thể khởi tạo tiến trình Shizuku");
            }

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            try (InputStream is = process.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            } catch (Throwable ignored) {
            }

            try (InputStream es = process.getErrorStream();
                 BufferedReader errReader = new BufferedReader(new InputStreamReader(es))) {
                String line;
                while ((line = errReader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            } catch (Throwable ignored) {
            }

            int exitCode = process.waitFor();
            return new ShellResult(exitCode, stdout.toString(), stderr.toString());
        } catch (Throwable t) {
            return new ShellResult(-1, "", "Lỗi thực thi: " + t.getMessage());
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * Grant WRITE_SECURE_SETTINGS and WRITE_SETTINGS to this app using Shizuku.
     */
    public static void grantSecureSettings(Context context, CommandCallback callback) {
        EXECUTOR.execute(() -> {
            String pkg = context.getPackageName();
            String cmd = "pm grant " + pkg + " android.permission.WRITE_SECURE_SETTINGS && " +
                         "pm grant " + pkg + " android.permission.WRITE_SETTINGS";
            ShellResult result = executeCommand(cmd);

            // Re-verify permission
            boolean hasPerm = SystemClockHelper.hasSecureSettingsPermission(context);
            MAIN_HANDLER.post(() -> {
                if (hasPerm || result.success) {
                    if (callback != null) callback.onComplete(true, "Đã cấp quyền WRITE_SECURE_SETTINGS thành công!");
                } else {
                    String msg = result.stderr.isEmpty() ? result.stdout : result.stderr;
                    if (callback != null) callback.onComplete(false, "Thất bại: " + msg);
                }
            });
        });
    }

    /**
     * Disable or Enable Joyose package via Shizuku.
     */
    public static void setJoyoseEnabled(boolean enable, CommandCallback callback) {
        EXECUTOR.execute(() -> {
            String cmd;
            if (enable) {
                cmd = "pm enable " + GamingFpsHelper.JOYOSE_PKG;
            } else {
                cmd = "pm disable-user --user 0 " + GamingFpsHelper.JOYOSE_PKG;
            }
            ShellResult result = executeCommand(cmd);
            MAIN_HANDLER.post(() -> {
                if (result.success) {
                    String msg = enable ? "Đã bật lại Joyose thành công!" : "Đã vô hiệu hóa Joyose (mở khóa 120 FPS)!";
                    if (callback != null) callback.onComplete(true, msg);
                } else {
                    String err = !result.stderr.isEmpty() ? result.stderr : result.stdout;
                    if (callback != null) callback.onComplete(false, "Lỗi: " + err);
                }
            });
        });
    }

    /**
     * Automatically unlock 120 FPS via Shizuku in 1-tap:
     * 1. Disable Joyose (removes 89 FPS thermal throttle)
     * 2. Clear PowerKeeper data (resets dynamic throttle limits)
     */
    public static void autoUnlock120Fps(CommandCallback callback) {
        EXECUTOR.execute(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("pm disable-user --user 0 ").append(GamingFpsHelper.JOYOSE_PKG).append("; ")
              .append("pm clear ").append(GamingFpsHelper.POWERKEEPER_PKG);

            ShellResult result = executeCommand(sb.toString());
            MAIN_HANDLER.post(() -> {
                if (result.success) {
                    if (callback != null) {
                        callback.onComplete(true, "⚡ Đã mở khóa 120 FPS! (Đã tắt Joyose & xóa dữ liệu Pin và Hiệu suất)");
                    }
                } else {
                    String err = !result.stderr.isEmpty() ? result.stderr : result.stdout;
                    if (callback != null) {
                        callback.onComplete(false, "Lỗi mở khóa: " + err);
                    }
                }
            });
        });
    }

    /**
     * Restore default Joyose state and reset screen refresh rate to Auto.
     */
    public static void autoRestoreJoyose(CommandCallback callback) {
        EXECUTOR.execute(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("pm enable ").append(GamingFpsHelper.JOYOSE_PKG).append("; ")
              .append("settings put secure min_refresh_rate 60.0; ")
              .append("settings put secure peak_refresh_rate 120.0; ")
              .append("settings put secure user_refresh_rate 0; ")
              .append("settings put system user_refresh_rate 0; ")
              .append("settings put secure miui_refresh_rate 0; ")
              .append("settings put system miui_refresh_rate 0; ")
              .append("settings put system thermal_limit_refresh_rate 0; ")
              .append("settings put global speed_mode 0");

            ShellResult result = executeCommand(sb.toString());
            MAIN_HANDLER.post(() -> {
                if (result.success) {
                    if (callback != null) {
                        callback.onComplete(true, "🔄 Đã khôi phục dịch vụ Joyose & cài đặt màn hình mặc định!");
                    }
                } else {
                    String err = !result.stderr.isEmpty() ? result.stderr : result.stdout;
                    if (callback != null) {
                        callback.onComplete(false, "Lỗi khôi phục: " + err);
                    }
                }
            });
        });
    }

    /**
     * Clear Battery and Performance (PowerKeeper) app cache & data via Shizuku shell.
     */
    public static void clearPowerKeeper(CommandCallback callback) {
        EXECUTOR.execute(() -> {
            String cmd = "pm clear " + GamingFpsHelper.POWERKEEPER_PKG;
            ShellResult result = executeCommand(cmd);
            MAIN_HANDLER.post(() -> {
                if (result.success) {
                    if (callback != null) {
                        callback.onComplete(true, "🗑️ Đã xóa sạch dữ liệu & cache của Pin & Hiệu suất (PowerKeeper)!");
                    }
                } else {
                    String err = !result.stderr.isEmpty() ? result.stderr : result.stdout;
                    if (callback != null) {
                        callback.onComplete(false, "Lỗi xóa cache: " + err);
                    }
                }
            });
        });
    }

    /**
     * Set display refresh rate via Shizuku shell.
     */
    public static void setRefreshRateViaShizuku(int targetRate, CommandCallback callback) {
        EXECUTOR.execute(() -> {
            float rateFloat = (float) targetRate;
            StringBuilder cmd = new StringBuilder();
            if (targetRate <= 0) {
                cmd.append("settings put secure min_refresh_rate 60.0; ")
                   .append("settings put secure peak_refresh_rate 120.0; ")
                   .append("settings put system user_refresh_rate 0; ")
                   .append("settings put system miui_refresh_rate 0; ")
                   .append("settings put secure miui_refresh_rate 0; ")
                   .append("settings put system thermal_limit_refresh_rate 0");
            } else {
                cmd.append("settings put secure peak_refresh_rate ").append(rateFloat).append("; ")
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
            ShellResult result = executeCommand(cmd.toString());
            MAIN_HANDLER.post(() -> {
                if (result.success) {
                    if (callback != null) callback.onComplete(true, "Đã áp dụng tần số quét " + (targetRate <= 0 ? "Tự động" : targetRate + "Hz") + " qua Shizuku!");
                } else {
                    if (callback != null) callback.onComplete(false, "Lỗi: " + result.stderr);
                }
            });
        });
    }

    /**
     * Force stop an application by package name using Shizuku.
     */
    public static void forceStopPackage(String packageName, CommandCallback callback) {
        EXECUTOR.execute(() -> {
            if (packageName == null || packageName.trim().isEmpty()) {
                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onComplete(false, "Tên gói ứng dụng không hợp lệ");
                });
                return;
            }
            String cmd = "am force-stop " + packageName.trim();
            ShellResult result = executeCommand(cmd);
            MAIN_HANDLER.post(() -> {
                if (result.success) {
                    if (callback != null) callback.onComplete(true, "Đã dừng ứng dụng thành công!");
                } else {
                    String err = !result.stderr.isEmpty() ? result.stderr : result.stdout;
                    if (callback != null) callback.onComplete(false, "Lỗi: " + err);
                }
            });
        });
    }

    /**
     * Grant PACKAGE_USAGE_STATS permission via Shizuku shell.
     */
    public static void grantUsageStats(Context context, CommandCallback callback) {
        EXECUTOR.execute(() -> {
            String pkg = context.getPackageName();
            String cmd = "pm grant " + pkg + " android.permission.PACKAGE_USAGE_STATS; " +
                         "appops set " + pkg + " GET_USAGE_STATS allow";
            ShellResult result = executeCommand(cmd);
            MAIN_HANDLER.post(() -> {
                if (result.success) {
                    if (callback != null) callback.onComplete(true, "Đã cấp quyền Đọc dữ liệu pin & thời gian sử dụng qua Shizuku!");
                } else {
                    String err = !result.stderr.isEmpty() ? result.stderr : result.stdout;
                    if (callback != null) callback.onComplete(false, "Lỗi: " + err);
                }
            });
        });
    }

    /**
     * Grant overlay and MIUI popup window permissions via Shizuku shell.
     */
    public static void grantOverlayPermission(Context context, CommandCallback callback) {
        EXECUTOR.execute(() -> {
            String pkg = context.getPackageName();
            String cmd = "appops set " + pkg + " SYSTEM_ALERT_WINDOW allow; " +
                         "appops set " + pkg + " 10021 allow; " + // OP_BACKGROUND_START_ACTIVITY
                         "appops set " + pkg + " 10020 allow; " + // OP_SHOW_WHEN_LOCKED
                         "appops set " + pkg + " 24 allow";      // OP_SYSTEM_ALERT_WINDOW
            ShellResult result = executeCommand(cmd);
            MAIN_HANDLER.post(() -> {
                if (result.success) {
                    if (callback != null) callback.onComplete(true, "Đã cấp quyền hiển thị cửa sổ nổi HUD qua Shizuku!");
                } else {
                    String err = !result.stderr.isEmpty() ? result.stderr : result.stdout;
                    if (callback != null) callback.onComplete(false, "Lỗi cấp quyền HUD: " + err);
                }
            });
        });
    }
}
