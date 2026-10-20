package app.cd.mitool.service;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import app.cd.mitool.R;
import app.cd.mitool.util.PreferencesHelper;

/**
 * Quick Settings Tile service for Xiaomi Control Center / Notification shade.
 * Enables 1-tap toggling of the Mi-Tool realtime status notification from anywhere,
 * including lockscreen and in-game pull down.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class NotificationTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        try {
            PreferencesHelper prefs = new PreferencesHelper(this);
            boolean currentlyEnabled = prefs.isNotifClockEnabled();
            boolean newState = !currentlyEnabled;
            prefs.setNotifClockEnabled(newState);

            if (newState) {
                Intent intent = new Intent(this, NotificationClockService.class);
                intent.setAction(NotificationClockService.ACTION_START);
                ContextCompat.startForegroundService(this, intent);
                Toast.makeText(this, "⚡ Đã bật thông báo trạng thái Mi-Tool", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, NotificationClockService.class);
                intent.setAction(NotificationClockService.ACTION_STOP);
                startService(intent);
                Toast.makeText(this, "✕ Đã tắt thông báo trạng thái Mi-Tool", Toast.LENGTH_SHORT).show();
            }

            updateTileState();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi chuyển trạng thái: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTileState() {
        try {
            Tile tile = getQsTile();
            if (tile == null) return;

            PreferencesHelper prefs = new PreferencesHelper(this);
            boolean enabled = prefs.isNotifClockEnabled();

            tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.tile_notification_label));
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_notification));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(enabled ? "Đang bật" : "Đã tắt");
            }

            tile.updateTile();
        } catch (Exception ignored) {
        }
    }
}
