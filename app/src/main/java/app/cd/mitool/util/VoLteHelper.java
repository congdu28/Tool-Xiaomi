package app.cd.mitool.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

public class VoLteHelper {

    public static final String CODE_VOLTE = "*#*#86583#*#*";
    public static final String CODE_VOWIFI = "*#*#869434#*#*";
    public static final String CODE_TESTING = "*#*#4636#*#*";

    /**
     * Trigger secret code via dialer intent and copy to clipboard.
     */
    public static boolean triggerSecretCode(Context context, String code, String label) {
        if (context == null || code == null) return false;

        // 1. Copy code to clipboard for convenience
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText(label, code));
            }
        } catch (Throwable ignored) {
        }

        // 2. Open phone dialer with prefilled USSD code
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + Uri.encode(code)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Toast.makeText(context, "Đã mở trình gọi điện với mã " + label + " (" + code + ").\nNếu chưa tự kích hoạt, chỉ cần xóa dấu # cuối rồi gõ lại dấu #.", Toast.LENGTH_LONG).show();
            return true;
        } catch (Throwable t) {
            Toast.makeText(context, "Đã sao chép mã: " + code + ". Hãy mở trình gọi điện và dán mã.", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public static boolean openMobileNetworkSettings(Context context) {
        if (context == null) return false;
        String[] actions = {
                Settings.ACTION_NETWORK_OPERATOR_SETTINGS,
                Settings.ACTION_DATA_ROAMING_SETTINGS,
                Settings.ACTION_WIRELESS_SETTINGS
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
