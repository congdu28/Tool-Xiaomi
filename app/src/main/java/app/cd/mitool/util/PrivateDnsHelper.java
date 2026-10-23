package app.cd.mitool.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class PrivateDnsHelper {

    // Standard official DNS-over-TLS (DoT) hostnames for Android Private DNS
    public static final String DNS_ADGUARD = "dns.adguard-dns.com";
    public static final String DNS_ADGUARD_ALT = "dns.adguard.com";
    public static final String DNS_CLOUDFLARE = "1dot1dot1dot1.cloudflare-dns.com";
    public static final String DNS_CLOUDFLARE_ALT = "one.one.one.one";
    public static final String DNS_CLOUDFLARE_FAMILY = "family.cloudflare-dns.com";
    public static final String DNS_CLOUDFLARE_SECURITY = "security.cloudflare-dns.com";
    public static final String DNS_GOOGLE = "dns.google";
    public static final String DNS_QUAD9 = "dns.quad9.net";

    public static final String MODE_OFF = "off";
    public static final String MODE_AUTO = "opportunistic";
    public static final String MODE_HOSTNAME = "hostname";

    public static String getCurrentDnsMode(Context context) {
        if (context == null) return MODE_AUTO;
        try {
            String mode = Settings.Global.getString(context.getContentResolver(), "private_dns_mode");
            return (mode != null && !mode.trim().isEmpty()) ? mode.trim() : MODE_AUTO;
        } catch (Throwable t) {
            return MODE_AUTO;
        }
    }

    public static String getCurrentDnsSpecifier(Context context) {
        if (context == null) return "";
        try {
            String specifier = Settings.Global.getString(context.getContentResolver(), "private_dns_specifier");
            return (specifier != null) ? specifier.trim() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static String cleanHostname(String hostname) {
        return DnsConfig.cleanHostname(hostname);
    }

    public static String getDnsDescription(Context context) {
        String mode = getCurrentDnsMode(context);
        String spec = getCurrentDnsSpecifier(context);

        if (MODE_HOSTNAME.equalsIgnoreCase(mode) && !spec.isEmpty()) {
            if (DNS_ADGUARD.equalsIgnoreCase(spec) || DNS_ADGUARD_ALT.equalsIgnoreCase(spec)) {
                return "🟢 Đã cấu hình: AdGuard DNS (" + spec + ")";
            } else if (DNS_CLOUDFLARE.equalsIgnoreCase(spec) || DNS_CLOUDFLARE_ALT.equalsIgnoreCase(spec)) {
                return "🟢 Đã cấu hình: Cloudflare 1.1.1.1 (" + spec + ")";
            } else if (DNS_CLOUDFLARE_FAMILY.equalsIgnoreCase(spec)) {
                return "🟢 Đã cấu hình: Cloudflare Family (Chặn nội dung người lớn & mã độc)";
            } else if (DNS_CLOUDFLARE_SECURITY.equalsIgnoreCase(spec)) {
                return "🟢 Đã cấu hình: Cloudflare Security (Chặn mã độc)";
            } else if (DNS_GOOGLE.equalsIgnoreCase(spec)) {
                return "🟢 Đã cấu hình: Google DNS (" + spec + ")";
            } else if (DNS_QUAD9.equalsIgnoreCase(spec)) {
                return "🟢 Đã cấu hình: Quad9 DNS (" + spec + ")";
            } else {
                return "🟢 Đã cấu hình: " + spec;
            }
        } else if (MODE_OFF.equalsIgnoreCase(mode)) {
            return "❌ Đang TẮT DNS riêng tư";
        } else {
            return "🔄 Chế độ: Tự động (Mặc định nhà mạng)";
        }
    }

    public interface ApplyCallback {
        void onComplete(boolean success, boolean needsPermission, String message);
    }
    private static final java.util.concurrent.ExecutorService EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());

    public static void setPrivateDns(Context context, String mode, String hostname, ApplyCallback callback) {
        Context app = context.getApplicationContext();
        String host = cleanHostname(hostname);
        EXECUTOR.execute(() -> {
            if (android.os.Build.VERSION.SDK_INT < 28) {
                MAIN.post(() -> callback.onComplete(false, false, "DNS riêng tư cần Android 9 trở lên."));
                return;
            }
            if (!(MODE_OFF.equals(mode) || MODE_AUTO.equals(mode) || MODE_HOSTNAME.equals(mode))
                    || (MODE_HOSTNAME.equals(mode) && !DnsConfig.isValidHostname(host))) {
                MAIN.post(() -> callback.onComplete(false, false, "Nhập tên máy chủ DoT hợp lệ, ví dụ one.one.one.one; không dùng địa chỉ IP."));
                return;
            }
            String oldMode = getCurrentDnsMode(app);
            String oldHost = getCurrentDnsSpecifier(app);
            boolean privileged = SystemClockHelper.hasSecureSettingsPermission(app) || ShizukuHelper.hasShizukuPermission();
            boolean wrote = writeSettings(app, mode, host);
            boolean verified = wrote && mode.equals(getCurrentDnsMode(app))
                    && (!MODE_HOSTNAME.equals(mode) || host.equalsIgnoreCase(getCurrentDnsSpecifier(app)));
            if (!verified && (!oldMode.equals(getCurrentDnsMode(app)) || !oldHost.equals(getCurrentDnsSpecifier(app)))) {
                writeSettings(app, oldMode, oldHost);
            }
            MAIN.post(() -> callback.onComplete(verified, !verified && !privileged,
                    verified ? "Đã lưu cấu hình DNS. Khả năng kết nối phụ thuộc mạng và máy chủ DoT."
                    : "Chưa áp dụng được DNS. Cần quyền WRITE_SECURE_SETTINGS, Shizuku hoặc Root; có thể cấu hình thủ công."));
        });
    }
    private static boolean writeSettings(Context context, String mode, String host) {
        if (SystemClockHelper.hasSecureSettingsPermission(context)) {
            try {
                boolean provider = Settings.Global.putString(context.getContentResolver(), "private_dns_specifier", host);
                if (provider && Settings.Global.putString(context.getContentResolver(), "private_dns_mode", mode)) return true;
            } catch (Exception ignored) {}
        }
        String cmd = "settings put global private_dns_specifier " + shellQuote(host)
                + " && settings put global private_dns_mode " + shellQuote(mode);
        if (ShizukuHelper.hasShizukuPermission()) return ShizukuHelper.executeCommand(cmd).success;
        return GamingFpsHelper.runRootCommand(cmd);
    }
    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
    public static void copyHostname(Context context, String hostname) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null && hostname != null && !hostname.isEmpty()) {
            cm.setPrimaryClip(ClipData.newPlainText("Private DNS", cleanHostname(hostname)));
        }
    }
    public static boolean openPrivateDnsSettings(Context context) {
        if (context == null) return false;
        String[] actions = {"android.settings.PRIVATE_DNS_SETTINGS", Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS};
        for (String act : actions) {
            try {
                Intent intent = new Intent(act);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
}
