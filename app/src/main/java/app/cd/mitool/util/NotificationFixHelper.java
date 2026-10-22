package app.cd.mitool.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationFixHelper {

    public interface FixCallback {
        void onProgress(int current, int total, String appName);
        void onComplete(boolean success, int optimizedCount, String message);
    }

    public static class TargetApp {
        public final String packageName;
        public final String defaultName;
        public final String category;
        public String label;
        public Drawable icon;
        public boolean isInstalled;
        public boolean isOptimized; // true if ignoring battery optimizations
        public boolean isSelected = true;

        public TargetApp(String packageName, String defaultName, String category) {
            this.packageName = packageName;
            this.defaultName = defaultName;
            this.category = category;
        }
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static final List<TargetApp> TARGET_TEMPLATES = Arrays.asList(
            new TargetApp("com.zing.zalo", "Zalo", "Nhắn tin & Gọi"),
            new TargetApp("com.facebook.orca", "Messenger", "Facebook Chat"),
            new TargetApp("com.facebook.mlite", "Messenger Lite", "Facebook Chat"),
            new TargetApp("org.telegram.messenger", "Telegram", "Nhắn tin"),
            new TargetApp("org.telegram.plus", "Telegram Plus", "Nhắn tin"),
            new TargetApp("com.whatsapp", "WhatsApp", "Nhắn tin quốc tế"),
            new TargetApp("com.whatsapp.w4b", "WhatsApp Business", "Kinh doanh"),
            new TargetApp("com.google.android.gm", "Gmail", "Thư điện tử"),
            new TargetApp("com.microsoft.office.outlook", "Microsoft Outlook", "Email & Lịch"),
            new TargetApp("com.microsoft.teams", "Microsoft Teams", "Công việc"),
            new TargetApp("com.viber.voip", "Viber", "Nhắn tin"),
            new TargetApp("com.skype.raider", "Skype", "Công việc"),
            new TargetApp("com.discord", "Discord", "Cộng đồng & Chat"),
            new TargetApp("jp.naver.line.android", "LINE", "Nhắn tin"),
            new TargetApp("com.instagram.android", "Instagram", "Mạng xã hội"),
            new TargetApp("com.zhiliaoapp.musically", "TikTok", "Mạng xã hội"),
            new TargetApp("com.ss.android.ugc.trill", "TikTok (Quốc tế)", "Mạng xã hội"),
            new TargetApp("com.shopee.vn", "Shopee", "Đơn hàng & Mua sắm"),
            new TargetApp("com.lazada.android", "Lazada", "Đơn hàng & Mua sắm"),
            new TargetApp("com.grabtaxi.passenger", "Grab", "Đặt xe & Đồ ăn")
    );

    /**
     * Get list of target apps currently installed on this device.
     */
    public static List<TargetApp> getInstalledTargetApps(Context context) {
        PackageManager pm = context.getPackageManager();
        List<TargetApp> installedList = new ArrayList<>();

        for (TargetApp tmpl : TARGET_TEMPLATES) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(tmpl.packageName, 0);
                TargetApp app = new TargetApp(tmpl.packageName, tmpl.defaultName, tmpl.category);
                app.isInstalled = true;
                app.label = pm.getApplicationLabel(ai).toString();
                app.icon = pm.getApplicationIcon(ai);
                app.isOptimized = isIgnoringBatteryOptimizations(context, tmpl.packageName);
                installedList.add(app);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Not installed
            } catch (Throwable ignored) {
            }
        }

        return installedList;
    }

    /**
     * Check if app is whitelisted from Doze / Battery Optimization.
     */
    public static boolean isIgnoringBatteryOptimizations(Context context, String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return pm.isIgnoringBatteryOptimizations(packageName);
            }
        }
        return false;
    }

    /**
     * Batch optimize multiple applications via Shizuku.
     */
    public static void applyBatchFix(Context context, List<String> packageNames, FixCallback callback) {
        if (!ShizukuHelper.hasShizukuPermission()) {
            if (callback != null) {
                callback.onComplete(false, 0, "Cần cấp quyền Shizuku để tự động tối ưu hóa!");
            }
            return;
        }

        EXECUTOR.execute(() -> {
            int successCount = 0;
            int total = packageNames.size();

            for (int i = 0; i < total; i++) {
                String pkg = packageNames.get(i);
                final int index = i + 1;
                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onProgress(index, total, pkg);
                });

                StringBuilder cmd = new StringBuilder();
                // 1. Whitelist from Doze
                cmd.append("dumpsys deviceidle whitelist +").append(pkg).append(" ; ");
                // 2. Allow running in background (AppOps)
                cmd.append("cmd appops set ").append(pkg).append(" RUN_IN_BACKGROUND allow ; ");
                cmd.append("cmd appops set ").append(pkg).append(" RUN_ANY_IN_BACKGROUND allow ; ");
                // 3. MIUI OP_AUTO_START (10008) and AUTO_START
                cmd.append("cmd appops set ").append(pkg).append(" 10008 allow ; ");
                cmd.append("cmd appops set ").append(pkg).append(" AUTO_START allow ; ");
                cmd.append("cmd appops set ").append(pkg).append(" BOOT_COMPLETED allow ; ");
                cmd.append("cmd appops set ").append(pkg).append(" START_FOREGROUND allow ; ");
                // 4. Force Active Standby Bucket
                cmd.append("am set-standby-bucket ").append(pkg).append(" active");

                ShizukuHelper.ShellResult result = ShizukuHelper.executeCommand(cmd.toString());
                if (result.success || result.exitCode == 0) {
                    successCount++;
                }
            }

            final int finalSuccess = successCount;
            MAIN_HANDLER.post(() -> {
                PreferencesHelper prefs = new PreferencesHelper(context);
                prefs.setNotifFixLastApplied(System.currentTimeMillis());
                if (callback != null) {
                    callback.onComplete(true, finalSuccess, "Đã tối ưu thành công " + finalSuccess + "/" + total + " ứng dụng!");
                }
            });
        });
    }

    /**
     * Fix a single app via Shizuku.
     */
    public static void fixSingleApp(Context context, String packageName, ShizukuHelper.CommandCallback callback) {
        if (!ShizukuHelper.hasShizukuPermission()) {
            if (callback != null) callback.onComplete(false, "Cần cấp quyền Shizuku");
            return;
        }

        EXECUTOR.execute(() -> {
            StringBuilder cmd = new StringBuilder();
            cmd.append("dumpsys deviceidle whitelist +").append(packageName).append(" ; ");
            cmd.append("cmd appops set ").append(packageName).append(" RUN_IN_BACKGROUND allow ; ");
            cmd.append("cmd appops set ").append(packageName).append(" RUN_ANY_IN_BACKGROUND allow ; ");
            cmd.append("cmd appops set ").append(packageName).append(" 10008 allow ; ");
            cmd.append("cmd appops set ").append(packageName).append(" AUTO_START allow ; ");
            cmd.append("cmd appops set ").append(packageName).append(" BOOT_COMPLETED allow ; ");
            cmd.append("cmd appops set ").append(packageName).append(" START_FOREGROUND allow ; ");
            cmd.append("am set-standby-bucket ").append(packageName).append(" active");

            ShizukuHelper.ShellResult result = ShizukuHelper.executeCommand(cmd.toString());
            MAIN_HANDLER.post(() -> {
                if (callback != null) {
                    callback.onComplete(result.success, result.success ? "Đã tối ưu hóa " + packageName : "Lỗi: " + result.stderr);
                }
            });
        });
    }

    /**
     * Open MIUI Autostart Management Activity.
     */
    public static boolean openMiuiAutostart(Context context) {
        if (context == null) return false;
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.securityscan.MainActivity"
                ));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Throwable t) {
                Toast.makeText(context, "Không thể mở mục Tự khởi chạy MIUI", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }

    /**
     * Open MIUI PowerKeeper hidden background activity for a specific app.
     */
    public static boolean openAppBatterySaver(Context context, String packageName) {
        if (context == null) return false;
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            ));
            intent.putExtra("package_name", packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            // Fallback to standard ignore battery optimization dialog
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return true;
                }
            } catch (Throwable ignored2) {
            }

            // Fallback to app details
            return openAppDetails(context, packageName);
        }
    }

    /**
     * Open general MIUI Battery Saver / PowerKeeper management page.
     */
    public static boolean openMiuiBatterySaverList(Context context) {
        if (context == null) return false;
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity"
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            try {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Throwable t) {
                Toast.makeText(context, "Không thể mở trang Tiết kiệm pin", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }

    /**
     * Open system application details settings.
     */
    public static boolean openAppDetails(Context context, String packageName) {
        if (context == null || packageName == null) return false;
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable t) {
            Toast.makeText(context, "Không thể mở thông tin ứng dụng", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
