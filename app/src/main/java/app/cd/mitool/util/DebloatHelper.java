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

    private static final java.util.concurrent.ExecutorService EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());

    public static void setPackageState(Context context, String pkg, boolean freeze, ShizukuHelper.CommandCallback callback) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            boolean success = changeAndVerify(app, pkg, freeze);
            MAIN.post(() -> {
                if (callback != null) callback.onComplete(success, success
                        ? (freeze ? "Đã đóng băng: " : "Đã kích hoạt lại: ") + pkg
                        : "Chưa thay đổi được " + pkg + ". Kiểm tra quyền Shizuku/Root và trạng thái ứng dụng.");
            });
        });
    }

    private static boolean changeAndVerify(Context context, String pkg, boolean freeze) {
        if (pkg == null || !pkg.matches("[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+")) return false;
        String cmd = freeze ? "pm disable-user --user 0 " + pkg : "pm enable --user 0 " + pkg;
        boolean success = ShizukuHelper.hasShizukuPermission()
                ? ShizukuHelper.executeCommand(cmd).success : GamingFpsHelper.runRootCommand(cmd);
        return success && isInstalled(context, pkg) && isFrozen(context, pkg) == freeze;
    }

    public static void batchFreeze(Context context, List<String> packages, ShizukuHelper.CommandCallback callback) {
        batchChange(context, packages, true, callback);
    }
    public static void batchRestore(Context context, List<String> packages, ShizukuHelper.CommandCallback callback) {
        batchChange(context, packages, false, callback);
    }
    private static void batchChange(Context context, List<String> packages, boolean freeze, ShizukuHelper.CommandCallback callback) {
        Context app = context.getApplicationContext();
        List<String> snapshot = packages == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(new java.util.LinkedHashSet<>(packages));
        EXECUTOR.execute(() -> {
            int count = 0;
            for (String pkg : snapshot) if (changeAndVerify(app, pkg, freeze)) count++;
            final int verified = count;
            MAIN.post(() -> {
                if (callback != null) callback.onComplete(verified == snapshot.size(),
                        (freeze ? "Đã đóng băng " : "Đã khôi phục ") + verified + "/" + snapshot.size()
                        + " ứng dụng (đã kiểm tra trạng thái)."
                        + (verified < snapshot.size() ? " Kiểm tra quyền Shizuku/Root cho các mục chưa hoàn tất." : ""));
            });
        });
    }
}
