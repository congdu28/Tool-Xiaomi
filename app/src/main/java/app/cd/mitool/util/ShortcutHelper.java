package app.cd.mitool.util;

import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.concurrent.Executor;

import app.cd.mitool.R;
import app.cd.mitool.ToggleNotificationActivity;
import app.cd.mitool.service.NotificationTileService;
import app.cd.mitool.service.RefreshRateTileService;

public class ShortcutHelper {

    public static final String SHORTCUT_ID_TOGGLE_NOTIF = "mitool_shortcut_toggle_notif";

    /**
     * Request launcher to pin a 1-tap shortcut to the user's Home Screen
     */
    public static boolean pinNotificationToggleShortcut(Context context) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Trình khởi chạy của máy không hỗ trợ tự động ghim phím tắt. Bạn có thể nhấn giữ biểu tượng Mi-Tool ngoài màn hình chính để kéo phím tắt ra!", Toast.LENGTH_LONG).show();
            return false;
        }

        try {
            Intent shortcutIntent = new Intent(context, ToggleNotificationActivity.class);
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(context, SHORTCUT_ID_TOGGLE_NOTIF)
                    .setShortLabel("Bật/Tắt Thông báo")
                    .setLongLabel("Bật/Tắt Thông báo Mi-Tool")
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_notification))
                    .setIntent(shortcutIntent)
                    .build();

            boolean result = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null);
            if (result) {
                Toast.makeText(context, "Đã gửi yêu cầu tạo lối tắt ra Màn hình chính!\n(Nếu chưa thấy icon, hãy bật quyền 'Lối tắt màn hình chính' trong Cài đặt ứng dụng)", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Không thể tạo lối tắt tự động. Hãy nhấn giữ biểu tượng app ngoài màn hình chính!", Toast.LENGTH_LONG).show();
            }
            return result;
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi tạo phím tắt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Request adding Quick Settings Notification Tile directly (Android 13+ / HyperOS)
     */
    public static void requestAddTileToQuickSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                StatusBarManager statusBarManager = context.getSystemService(StatusBarManager.class);
                if (statusBarManager != null) {
                    ComponentName componentName = new ComponentName(context, NotificationTileService.class);
                    Icon icon = Icon.createWithResource(context, R.drawable.ic_tile_notification);
                    Executor executor = ContextCompat.getMainExecutor(context);
                    statusBarManager.requestAddTileService(
                            componentName,
                            context.getString(R.string.tile_notification_label),
                            icon,
                            executor,
                            resultCode -> {
                                if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                    Toast.makeText(context, "Đã thêm nút 'Thông báo Mi-Tool' vào Bảng điều khiển!", Toast.LENGTH_SHORT).show();
                                } else if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                    Toast.makeText(context, "Nút 'Thông báo Mi-Tool' đã có trong Bảng điều khiển của bạn!", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(context, "💡 Cách thêm: Vuốt thanh thông báo xuống -> Chạm biểu tượng Bút chì ✏️ -> Tìm 'Thông báo Mi-Tool' và kéo lên!", Toast.LENGTH_LONG).show();
    }

    /**
     * Request adding Quick Settings Refresh Rate (Hz) Tile directly (Android 13+ / HyperOS)
     */
    public static void requestAddHzTileToQuickSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                StatusBarManager statusBarManager = context.getSystemService(StatusBarManager.class);
                if (statusBarManager != null) {
                    ComponentName componentName = new ComponentName(context, RefreshRateTileService.class);
                    Icon icon = Icon.createWithResource(context, R.drawable.ic_tile_hz);
                    Executor executor = ContextCompat.getMainExecutor(context);
                    statusBarManager.requestAddTileService(
                            componentName,
                            context.getString(R.string.tile_hz_label),
                            icon,
                            executor,
                            resultCode -> {
                                if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                    Toast.makeText(context, "Đã thêm nút 'Tần số quét' vào Bảng điều khiển!", Toast.LENGTH_SHORT).show();
                                } else if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                    Toast.makeText(context, "Nút 'Tần số quét' đã có trong Bảng điều khiển của bạn!", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(context, "💡 Cách thêm: Vuốt thanh thông báo xuống -> Chạm biểu tượng Bút chì ✏️ -> Tìm 'Tần số quét' và kéo lên!", Toast.LENGTH_LONG).show();
    }
}
