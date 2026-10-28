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
            Toast.makeText(context, R.string.message_93e0c3b4fb, Toast.LENGTH_LONG).show();
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
                Toast.makeText(context, R.string.message_b9a86048f2, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, R.string.message_3dfd4b742d, Toast.LENGTH_LONG).show();
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
                                    Toast.makeText(context, R.string.message_5eaf7881a3, Toast.LENGTH_SHORT).show();
                                } else if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                    Toast.makeText(context, R.string.message_4668eb9ffa, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(context, R.string.message_a60251a65d, Toast.LENGTH_LONG).show();
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
                                    Toast.makeText(context, R.string.message_21b9a83ad1, Toast.LENGTH_SHORT).show();
                                } else if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                    Toast.makeText(context, R.string.message_fe0e504af4, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(context, R.string.message_59c02cb5e1, Toast.LENGTH_LONG).show();
    }
}
