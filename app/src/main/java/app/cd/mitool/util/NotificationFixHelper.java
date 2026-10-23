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
        public boolean isRecommended = false;

        public TargetApp(String packageName, String defaultName, String category) {
            this(packageName, defaultName, category, true, false);
        }

        public TargetApp(String packageName, String defaultName, String category, boolean defaultSelected) {
            this(packageName, defaultName, category, defaultSelected, false);
        }

        public TargetApp(String packageName, String defaultName, String category, boolean defaultSelected, boolean isRecommended) {
            this.packageName = packageName;
            this.defaultName = defaultName;
            this.category = category;
            this.isSelected = defaultSelected;
            this.isRecommended = isRecommended;
        }
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static final List<TargetApp> TARGET_TEMPLATES = Arrays.asList(
            // Dịch vụ cốt lõi Google (Cực kỳ quan trọng cho Gmail & Push FCM)
            new TargetApp("com.google.android.gms", "Dịch Vụ Google Play (GMS)", "Cốt lõi Push & Tài khoản", true, true),
            new TargetApp("com.google.android.gsf", "Khung Dịch Vụ Google (GSF)", "Hạ tầng Google", true, true),
            new TargetApp("com.google.android.gm", "Gmail", "Thư điện tử Google", true, true),

            // Ứng dụng Nhắn tin & Công việc
            new TargetApp("com.zing.zalo", "Zalo", "Nhắn tin & Gọi", true, true),
            new TargetApp("com.facebook.orca", "Messenger", "Facebook Chat", true, true),
            new TargetApp("com.facebook.mlite", "Messenger Lite", "Facebook Chat", true, true),
            new TargetApp("org.telegram.messenger", "Telegram", "Nhắn tin", true, true),
            new TargetApp("org.telegram.plus", "Telegram Plus", "Nhắn tin", true, true),
            new TargetApp("com.whatsapp", "WhatsApp", "Nhắn tin quốc tế", true, true),
            new TargetApp("com.whatsapp.w4b", "WhatsApp Business", "Kinh doanh", true, true),
            new TargetApp("com.microsoft.office.outlook", "Microsoft Outlook", "Email & Lịch", true, true),
            new TargetApp("com.microsoft.teams", "Microsoft Teams", "Công việc", true, true),
            new TargetApp("com.viber.voip", "Viber", "Nhắn tin", true, true),
            new TargetApp("com.skype.raider", "Skype", "Công việc", true, true),
            new TargetApp("com.discord", "Discord", "Cộng đồng & Chat", true, true),
            new TargetApp("jp.naver.line.android", "LINE", "Nhắn tin", true, true),

            // Ngân hàng & Biến động số dư OTT (Chống trễ tiền về)
            new TargetApp("com.VCB", "Vietcombank Digibank", "Ngân hàng", true, true),
            new TargetApp("vn.com.techcombank.bb.app", "Techcombank Mobile", "Ngân hàng", true, true),
            new TargetApp("com.mbmobile", "MB Bank", "Ngân hàng", true, true),
            new TargetApp("com.vnpay.vpbankonline", "VPBank NEO", "Ngân hàng", true, true),
            new TargetApp("com.vietinbank.ipay", "VietinBank iPay", "Ngân hàng", true, true),
            new TargetApp("com.vnpay.bidv", "BIDV SmartBanking", "Ngân hàng", true, true),
            new TargetApp("com.tpb.mb.gprsandroid", "TPBank Mobile", "Ngân hàng", true, true),
            new TargetApp("mobile.acb.com.vn", "ACB ONE", "Ngân hàng", true, true),
            new TargetApp("xyz.be.cake", "Cake by VPBank", "Ngân hàng số", true, true),
            new TargetApp("com.mservice.momopay", "Ví MoMo", "Ví điện tử", true, true),
            new TargetApp("vn.com.vng.zalopay", "ZaloPay", "Ví điện tử", true, true),

            // Mạng xã hội & Mua sắm (Không tự chọn để tránh chạy ngầm hao pin)
            new TargetApp("com.instagram.android", "Instagram", "Mạng xã hội", false, false),
            new TargetApp("com.zhiliaoapp.musically", "TikTok", "Mạng xã hội", false, false),
            new TargetApp("com.ss.android.ugc.trill", "TikTok (Quốc tế)", "Mạng xã hội", false, false),
            new TargetApp("com.shopee.vn", "Shopee", "Đơn hàng & Mua sắm", false, false),
            new TargetApp("com.lazada.android", "Lazada", "Đơn hàng & Mua sắm", false, false),
            new TargetApp("com.grabtaxi.passenger", "Grab", "Đặt xe & Đồ ăn", false, false)
    );

    /**
     * Get ALL installed applications on this device (Recommended GMS/Banks/Chat first, then all user apps).
     * Eliminates the limitation of only showing template apps.
     */
    public static List<TargetApp> getAllInstalledApps(Context context) {
        if (context == null) return new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<TargetApp> resultList = new ArrayList<>();
        java.util.Set<String> processedPackages = new java.util.HashSet<>();
        String myPkg = context.getPackageName();

        // 1. Process recommended template apps first
        for (TargetApp tmpl : TARGET_TEMPLATES) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(tmpl.packageName, 0);
                TargetApp app = new TargetApp(tmpl.packageName, tmpl.defaultName, tmpl.category, tmpl.isSelected, tmpl.isRecommended);
                app.isInstalled = true;
                app.label = pm.getApplicationLabel(ai).toString();
                app.icon = pm.getApplicationIcon(ai);
                app.isOptimized = isIgnoringBatteryOptimizations(context, tmpl.packageName);
                resultList.add(app);
                processedPackages.add(tmpl.packageName);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Not installed
            } catch (Throwable ignored) {
            }
        }

        // 2. Query ALL other installed user-facing apps on this device
        try {
            List<ApplicationInfo> allInstalled = pm.getInstalledApplications(0);
            List<TargetApp> otherApps = new ArrayList<>();

            for (ApplicationInfo ai : allInstalled) {
                String pkg = ai.packageName;
                if (pkg == null || pkg.equals(myPkg) || processedPackages.contains(pkg)) {
                    continue;
                }

                boolean isUserApp = (ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                        || (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
                Intent launchIntent = pm.getLaunchIntentForPackage(pkg);

                // Include all user-installed apps or any app with a launcher activity
                if (isUserApp || launchIntent != null) {
                    try {
                        String label = pm.getApplicationLabel(ai).toString();
                        Drawable icon = pm.getApplicationIcon(ai);
                        TargetApp app = new TargetApp(pkg, label, isUserApp ? "Ứng dụng cài đặt" : "Hệ thống / Mặc định", false, false);
                        app.isInstalled = true;
                        app.label = label;
                        app.icon = icon;
                        app.isOptimized = isIgnoringBatteryOptimizations(context, pkg);
                        otherApps.add(app);
                        processedPackages.add(pkg);
                    } catch (Throwable ignored) {
                    }
                }
            }

            // Sort other apps alphabetically by label
            java.util.Collections.sort(otherApps, (a, b) -> {
                String nameA = (a.label != null) ? a.label : a.packageName;
                String nameB = (b.label != null) ? b.label : b.packageName;
                return nameA.compareToIgnoreCase(nameB);
            });

            resultList.addAll(otherApps);
        } catch (Throwable ignored) {
        }

        return resultList;
    }

    /**
     * Backward-compatible method forwarding to getAllInstalledApps.
     */
    public static List<TargetApp> getInstalledTargetApps(Context context) {
        return getAllInstalledApps(context);
    }

    /**
     * Check if app is whitelisted from Doze / Battery Optimization.
     */
    public static boolean isIgnoringBatteryOptimizations(Context context, String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                try { return pm.isIgnoringBatteryOptimizations(packageName); }
                catch (RuntimeException ignored) { return false; }
            }
        }
        return false;
    }

    /** Apply only to selected installed packages; verify the documented Android controls. */
    public static void applyBatchFix(Context context, List<String> packageNames, FixCallback callback) {
        Context app = context.getApplicationContext();
        java.util.LinkedHashSet<String> selected = new java.util.LinkedHashSet<>();
        if (packageNames != null) selected.addAll(packageNames);
        EXECUTOR.execute(() -> {
            int count = 0;
            int index = 0;
            java.util.List<String> failed = new java.util.ArrayList<>();
            for (String pkg : selected) {
                final int progress = ++index;
                MAIN_HANDLER.post(() -> { if (callback != null) callback.onProgress(progress, selected.size(), pkg); });
                if (applyAndVerify(app, pkg)) count++;
                else failed.add(String.valueOf(pkg));
            }
            final int verified = count;
            MAIN_HANDLER.post(() -> {
                if (verified > 0) new PreferencesHelper(app).setNotifFixLastApplied(System.currentTimeMillis());
                if (callback != null) callback.onComplete(!selected.isEmpty() && verified == selected.size(), verified,
                        "Đã xác minh miễn Doze và quyền chạy nền: " + verified + "/" + selected.size()
                        + ". Tự khởi chạy và giới hạn pin MIUI cần kiểm tra trong Cài đặt."
                        + (failed.isEmpty() ? "" : "\nChưa hoàn tất: " + android.text.TextUtils.join(", ", failed)));
            });
        });
    }

    public static void fixSingleApp(Context context, String packageName, ShizukuHelper.CommandCallback callback) {
        applyBatchFix(context, java.util.Collections.singletonList(packageName), new FixCallback() {
            @Override public void onProgress(int current, int total, String appName) {}
            @Override public void onComplete(boolean success, int count, String message) {
                if (callback != null) callback.onComplete(success, message);
            }
        });
    }

    private static boolean applyAndVerify(Context context, String pkg) {
        if (pkg == null || !pkg.matches("[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+")
                || !ShizukuHelper.hasShizukuPermission()) return false;
        try { context.getPackageManager().getApplicationInfo(pkg, 0); }
        catch (PackageManager.NameNotFoundException e) { return false; }
        ShizukuHelper.ShellResult doze = ShizukuHelper.executeCommand("dumpsys deviceidle whitelist +" + pkg);
        ShizukuHelper.ShellResult background = ShizukuHelper.executeCommand("cmd appops set " + pkg + " RUN_IN_BACKGROUND allow");
        boolean ok = doze.success && background.success;
        if (Build.VERSION.SDK_INT >= 28) {
            ok &= ShizukuHelper.executeCommand("cmd appops set " + pkg + " RUN_ANY_IN_BACKGROUND allow").success;
        }
        ShizukuHelper.ShellResult check = ShizukuHelper.executeCommand("cmd appops get " + pkg + " RUN_IN_BACKGROUND");
        ok &= check.success && check.stdout.contains("allow");
        if (Build.VERSION.SDK_INT >= 28) {
            ShizukuHelper.ShellResult any = ShizukuHelper.executeCommand("cmd appops get " + pkg + " RUN_ANY_IN_BACKGROUND");
            ok &= any.success && any.stdout.contains("allow");
        }
        // Vendor autostart operations differ between ROMs. Never guess numeric AppOps IDs
        // or write undocumented global lists. Leave those controls to the user's MIUI settings.
        return ok && isIgnoringBatteryOptimizations(context, pkg);
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

    public static boolean openAppNotificationSettings(Context context, String packageName) {
        try {
            Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("android.provider.extra.APP_PACKAGE", packageName);
            intent.putExtra("app_package", packageName);
            intent.putExtra("app_uid", context.getPackageManager().getApplicationInfo(packageName, 0).uid);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) { return openAppDetails(context, packageName); }
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

    /**
     * Open Google Play Services FCM Diagnostics screen (*#*#426#*#*).
     */
    public static boolean openFcmDiagnostics(Context context) {
        if (context == null) return false;
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    "com.google.android.gms",
                    "com.google.android.gms.gcm.GcmDiagnostics"
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:*%23*%23426%23*%23*"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Throwable t) {
                Toast.makeText(context, "Không thể mở trang kiểm tra FCM", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }
}
