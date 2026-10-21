package app.cd.mitool.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

public class PrivateDnsHelper {

    public static final String DNS_ADGUARD = "dns.adguard-dns.com";
    public static final String DNS_CLOUDFLARE_FAMILY = "family.cloudflare-dns.com";
    public static final String DNS_CLOUDFLARE = "one.one.one.one";
    public static final String DNS_GOOGLE = "dns.google";

    public static final String MODE_OFF = "off";
    public static final String MODE_AUTO = "opportunistic";
    public static final String MODE_HOSTNAME = "hostname";

    public static String getCurrentDnsMode(Context context) {
        if (context == null) return MODE_AUTO;
        try {
            String mode = Settings.Global.getString(context.getContentResolver(), "private_dns_mode");
            return mode != null ? mode : MODE_AUTO;
        } catch (Throwable t) {
            return MODE_AUTO;
        }
    }

    public static String getCurrentDnsSpecifier(Context context) {
        if (context == null) return "";
        try {
            String specifier = Settings.Global.getString(context.getContentResolver(), "private_dns_specifier");
            return specifier != null ? specifier : "";
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * Apply Private DNS configuration via WRITE_SECURE_SETTINGS, Shizuku, Root, or fallback.
     */
    public static boolean setPrivateDns(Context context, String mode, String hostname) {
        if (context == null) return false;
        ContentResolver cr = context.getContentResolver();

        boolean wrote = false;
        // 1. Try ContentResolver if WRITE_SECURE_SETTINGS is granted
        if (SystemClockHelper.hasSecureSettingsPermission(context)) {
            try {
                Settings.Global.putString(cr, "private_dns_mode", mode);
                if (hostname != null && !hostname.isEmpty()) {
                    Settings.Global.putString(cr, "private_dns_specifier", hostname);
                }
                wrote = true;
            } catch (Throwable ignored) {
            }
        }

        // 2. Try Shizuku if not written yet
        if (!wrote && ShizukuHelper.hasShizukuPermission()) {
            String cmd = "settings put global private_dns_mode " + mode;
            if (hostname != null && !hostname.isEmpty()) {
                cmd += " && settings put global private_dns_specifier " + hostname;
            }
            final String finalCmd = cmd;
            new Thread(() -> ShizukuHelper.executeCommand(finalCmd)).start();
            wrote = true;
        }

        // 3. Try Root if not written yet
        if (!wrote) {
            String cmd = "settings put global private_dns_mode " + mode;
            if (hostname != null && !hostname.isEmpty()) {
                cmd += " && settings put global private_dns_specifier " + hostname;
            }
            wrote = GamingFpsHelper.runRootCommand(cmd);
        }

        // 4. Fallback: Copy hostname & open settings screen
        if (!wrote) {
            if (hostname != null && !hostname.isEmpty()) {
                ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Private DNS", hostname));
                }
            }
            openPrivateDnsSettings(context);
            Toast.makeText(context, "Đã sao chép '" + hostname + "'! Hãy dán vào ô 'Tên máy chủ nhà cung cấp DNS riêng tư'.", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    public static boolean openPrivateDnsSettings(Context context) {
        if (context == null) return false;
        String[] actions = {
                "android.settings.NETWORK_PROVIDER_SETTINGS",
                Settings.ACTION_WIRELESS_SETTINGS,
                Settings.ACTION_WIFI_SETTINGS
        };
        for (String act : actions) {
            try {
                Intent intent = new Intent(act);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }
}
