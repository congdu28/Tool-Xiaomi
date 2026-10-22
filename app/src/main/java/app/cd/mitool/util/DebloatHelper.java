package app.cd.mitool.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

public class DebloatHelper {

    public static class BloatwareApp {
        public final String pkg;
        public final String name;
        public final String desc;
        public final boolean recommended;

        public BloatwareApp(String pkg, String name, String desc, boolean recommended) {
            this.pkg = pkg;
            this.name = name;
            this.desc = desc;
            this.recommended = recommended;
        }
    }

    public static final List<BloatwareApp> SAFE_BLOATWARE_LIST = new ArrayList<>();

    static {
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.msa.global",
                "Dịch vụ Quảng cáo MSA",
                "Tiến trình ngầm tải và hiển thị quảng cáo trong ứng dụng hệ thống Xiaomi",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.systemAdSolution",
                "Hệ thống Quảng cáo MIUI",
                "Dịch vụ phân phối và đẩy quảng cáo nền tảng Xiaomi",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.analytics",
                "Xiaomi Analytics",
                "Dịch vụ phân tích, thu thập dữ liệu hành vi người dùng chạy nền liên tục",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.videoplayer",
                "Mi Video",
                "Trình xem video mặc định liên tục đẩy thông báo tin tức và quảng cáo",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.player",
                "Mi Music",
                "Trình phát nhạc mặc định kèm dịch vụ trực tuyến đẩy quảng cáo",
                false));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.mi.globalbrowser",
                "Mi Browser",
                "Trình duyệt mặc định Xiaomi có nhiều banner tin tức và gợi ý ngầm",
                false));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.personalassistant",
                "App Vault (Màn hình phụ)",
                "Trang tiện ích vuốt sang trái màn hình chính ngốn RAM và hao pin",
                false));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.bugreport",
                "Xiaomi Bug Reporter",
                "Tiến trình tự động quét và gửi nhật ký lỗi chạy ngầm",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.yellowpage",
                "Trang vàng Xiaomi (YellowPage)",
                "Dịch vụ tra cứu doanh nghiệp Trung Quốc không sử dụng tại Việt Nam",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.xiaomi.glgm",
                "Xiaomi Game Center",
                "Cửa hàng game mặc định liên tục đẩy thông báo mời chơi trò chơi",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.xiaomi.mipicks",
                "GetApps (Kho ứng dụng Xiaomi)",
                "Kho ứng dụng thay thế Play Store liên tục gợi ý cài đặt ngầm",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.hybrid",
                "Quick Apps (Ứng dụng tức thì)",
                "Nền tảng ứng dụng nhanh đẩy banner quảng cáo và ngốn tài nguyên",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.hybrid.accessory",
                "Quick Apps Accessory",
                "Tiến trình phụ trợ cho Quick Apps chạy ngầm",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.cleanmaster",
                "Clean Master Ngầm",
                "Công cụ dọn rác bên thứ ba tích hợp có quảng cáo ngầm",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.translation.kingsoft",
                "Dịch thuật Kingsoft",
                "Dịch vụ dịch thuật tích hợp chạy nền không cần thiết",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.facebook.services",
                "Facebook Services",
                "Dịch vụ nền Facebook cài sẵn trong ROM",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.facebook.appmanager",
                "Facebook App Manager",
                "Trình quản lý ứng dụng Facebook chạy ngầm",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.facebook.system",
                "Facebook System Installer",
                "Trình cài đặt tự động ngầm của Facebook",
                true));
    }

    public static boolean isInstalled(Context context, String pkg) {
        if (context == null || pkg == null) return false;
        try {
            PackageManager pm = context.getPackageManager();
            pm.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isFrozen(Context context, String pkg) {
        if (context == null || pkg == null) return false;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES);
            if (!ai.enabled) return true;
            int setting = pm.getApplicationEnabledSetting(pkg);
            return setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setPackageState(Context context, String pkg, boolean freeze, ShizukuHelper.CommandCallback callback) {
        String primaryCmd = freeze
                ? "pm disable-user --user 0 " + pkg
                : "pm enable " + pkg;
        String fallbackCmd = freeze
                ? "pm disable " + pkg
                : "cmd package install-existing " + pkg;

        if (ShizukuHelper.hasShizukuPermission()) {
            new Thread(() -> {
                ShizukuHelper.ShellResult res = ShizukuHelper.executeCommand(primaryCmd);
                if (!res.success) {
                    res = ShizukuHelper.executeCommand(fallbackCmd);
                }
                boolean success = res.success || res.stdout.contains("new state") || res.stdout.contains("enabled");
                if (callback != null) {
                    callback.onComplete(success, success
                            ? (freeze ? "Đã đóng băng an toàn: " + pkg : "Đã kích hoạt lại: " + pkg)
                            : "Thất bại: " + (res.stderr.isEmpty() ? res.stdout : res.stderr));
                }
            }).start();
        } else if (GamingFpsHelper.runRootCommand(primaryCmd) || GamingFpsHelper.runRootCommand(fallbackCmd)) {
            if (callback != null) {
                callback.onComplete(true, freeze ? "Đã đóng băng (Root): " + pkg : "Đã kích hoạt lại (Root): " + pkg);
            }
        } else {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + pkg));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                if (callback != null) {
                    callback.onComplete(false, "Đang mở Thông tin ứng dụng. Hãy chọn 'Tắt' hoặc 'Gỡ cài đặt'.");
                }
            } catch (Throwable t) {
                if (callback != null) {
                    callback.onComplete(false, "Không thể mở trang cài đặt ứng dụng.");
                }
            }
        }
    }

    public static void batchFreeze(Context context, List<String> pkgs, ShizukuHelper.CommandCallback callback) {
        if (pkgs == null || pkgs.isEmpty()) {
            if (callback != null) callback.onComplete(true, "Không có ứng dụng rác nào cần đóng băng.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String pkg : pkgs) {
            sb.append("pm disable-user --user 0 ").append(pkg).append("; ");
        }
        String cmd = sb.toString();

        if (ShizukuHelper.hasShizukuPermission()) {
            new Thread(() -> {
                ShizukuHelper.ShellResult res = ShizukuHelper.executeCommand(cmd);
                if (callback != null) {
                    callback.onComplete(true, "Đã gửi lệnh đóng băng " + pkgs.size() + " ứng dụng rác qua Shizuku!");
                }
            }).start();
        } else if (GamingFpsHelper.runRootCommand(cmd)) {
            if (callback != null) {
                callback.onComplete(true, "Đã đóng băng " + pkgs.size() + " ứng dụng rác qua Root!");
            }
        } else {
            if (callback != null) {
                callback.onComplete(false, "Cần cấp quyền Shizuku (Tab 1) hoặc Root để đóng băng hàng loạt!");
            }
        }
    }

    public static void batchRestore(Context context, List<String> pkgs, ShizukuHelper.CommandCallback callback) {
        if (pkgs == null || pkgs.isEmpty()) {
            if (callback != null) callback.onComplete(true, "Không có ứng dụng nào cần khôi phục.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String pkg : pkgs) {
            sb.append("pm enable ").append(pkg).append("; cmd package install-existing ").append(pkg).append("; ");
        }
        String cmd = sb.toString();

        if (ShizukuHelper.hasShizukuPermission()) {
            new Thread(() -> {
                ShizukuHelper.ShellResult res = ShizukuHelper.executeCommand(cmd);
                if (callback != null) {
                    callback.onComplete(true, "Đã gửi lệnh khôi phục " + pkgs.size() + " ứng dụng qua Shizuku!");
                }
            }).start();
        } else if (GamingFpsHelper.runRootCommand(cmd)) {
            if (callback != null) {
                callback.onComplete(true, "Đã khôi phục " + pkgs.size() + " ứng dụng qua Root!");
            }
        } else {
            if (callback != null) {
                callback.onComplete(false, "Cần cấp quyền Shizuku (Tab 1) hoặc Root để khôi phục hàng loạt!");
            }
        }
    }
}
