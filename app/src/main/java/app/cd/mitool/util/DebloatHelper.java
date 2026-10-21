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
                "Chạy ngầm tải và hiển thị quảng cáo trong ứng dụng hệ thống",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.systemAdSolution",
                "Giải pháp Quảng cáo MIUI",
                "Phân phối và đẩy banner quảng cáo nền tảng Xiaomi",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.analytics",
                "Xiaomi Analytics",
                "Thu thập dữ liệu hành vi người dùng và nhật ký chạy nền",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.videoplayer",
                "Mi Video",
                "Trình xem video mặc định kèm nhiều thông báo tin tức và quảng cáo rác",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.player",
                "Mi Music",
                "Trình phát nhạc mặc định kèm dịch vụ trực tuyến đẩy quảng cáo",
                false));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.mi.globalbrowser",
                "Mi Browser",
                "Trình duyệt mặc định Xiaomi có nhiều banner tin tức và gợi ý rác",
                false));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.personalassistant",
                "App Vault (Màn hình phụ)",
                "Trang tiện ích kéo sang trái màn hình chính ngốn RAM và hao pin",
                false));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.bugreport",
                "Xiaomi Bug Reporter",
                "Tiến trình tự động quét và gửi báo cáo lỗi chạy nền liên tục",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.yellowpage",
                "Trang vàng Xiaomi (YellowPage)",
                "Tra cứu danh bạ doanh nghiệp Trung Quốc không dùng tại VN",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.xiaomi.glgm",
                "Xiaomi Game Center",
                "Cửa hàng trò chơi mặc định liên tục đẩy thông báo mời chơi game",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.xiaomi.mipicks",
                "GetApps (Kho ứng dụng)",
                "Kho ứng dụng thay thế Play Store liên tục gợi ý cài app ngầm",
                true));
        SAFE_BLOATWARE_LIST.add(new BloatwareApp(
                "com.miui.hybrid",
                "Quick Apps (Ứng dụng tức thì)",
                "Nền tảng ứng dụng nhanh đẩy banner quảng cáo và tiêu tốn tài nguyên",
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
        String cmd = freeze
                ? "pm disable-user --user 0 " + pkg
                : "pm enable " + pkg;

        if (ShizukuHelper.hasShizukuPermission()) {
            new Thread(() -> {
                ShizukuHelper.ShellResult res = ShizukuHelper.executeCommand(cmd);
                if (callback != null) {
                    callback.onComplete(res.success, res.success
                            ? (freeze ? "Đã đóng băng " + pkg : "Đã bật lại " + pkg)
                            : "Thất bại: " + res.stderr);
                }
            }).start();
        } else if (GamingFpsHelper.runRootCommand(cmd)) {
            if (callback != null) {
                callback.onComplete(true, freeze ? "Đã đóng băng (Root) " + pkg : "Đã bật lại (Root) " + pkg);
            }
        } else {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + pkg));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                if (callback != null) {
                    callback.onComplete(false, "Đang mở Cài đặt ứng dụng. Hãy bấm 'Tắt' hoặc 'Gỡ cài đặt'.");
                }
            } catch (Throwable t) {
                if (callback != null) {
                    callback.onComplete(false, "Không thể mở trang ứng dụng.");
                }
            }
        }
    }

    public static void batchFreeze(Context context, List<String> pkgs, ShizukuHelper.CommandCallback callback) {
        if (pkgs == null || pkgs.isEmpty()) {
            if (callback != null) callback.onComplete(true, "Không có ứng dụng nào được chọn.");
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
            if (callback != null) callback.onComplete(true, "Không có ứng dụng nào được chọn.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String pkg : pkgs) {
            sb.append("pm enable ").append(pkg).append("; ");
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
