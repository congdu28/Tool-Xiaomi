package app.cd.mitool.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import app.cd.mitool.MainActivity;
import app.cd.mitool.R;
import app.cd.mitool.util.PreferencesHelper;

/**
 * Quick Settings Tile service for toggling Floating Clock (Đồng hồ giây).
 * Allows users to easily toggle the floating clock overlay from Quick Settings / Control Center.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class FloatingClockTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        try {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Vui lòng mở Mi Tool và cấp quyền Hiển thị trên ứng dụng khác!", Toast.LENGTH_LONG).show();
                Intent appIntent = new Intent(this, MainActivity.class);
                appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= 34) {
                    PendingIntent pi = PendingIntent.getActivity(this, 0, appIntent, PendingIntent.FLAG_IMMUTABLE);
                    startActivityAndCollapse(pi);
                } else {
                    startActivityAndCollapse(appIntent);
                }
                return;
            }

            PreferencesHelper prefs = new PreferencesHelper(this);
            boolean currentState = prefs.isFloatingClockEnabled();
            boolean newState = !currentState;
            prefs.setFloatingClockEnabled(newState);

            if (newState) {
                Intent serviceIntent = new Intent(this, FloatingClockService.class);
                serviceIntent.setAction(FloatingClockService.ACTION_RELOAD_SETTINGS);
                ContextCompat.startForegroundService(this, serviceIntent);
                Toast.makeText(this, "⏰ Đã bật Đồng hồ giây nổi", Toast.LENGTH_SHORT).show();
            } else {
                Intent stopIntent = new Intent(this, FloatingClockService.class);
                stopIntent.setAction(FloatingClockService.ACTION_STOP);
                startService(stopIntent);
                Toast.makeText(this, "⏰ Đã tắt Đồng hồ giây nổi", Toast.LENGTH_SHORT).show();
            }

            updateTileState();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi bật/tắt đồng hồ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTileState() {
        try {
            Tile tile = getQsTile();
            if (tile == null) return;

            PreferencesHelper prefs = new PreferencesHelper(this);
            boolean isEnabled = prefs.isFloatingClockEnabled();

            tile.setState(isEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.tile_clock_label));
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_clock));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(isEnabled ? "Đang bật" : "Đang tắt");
            }

            tile.updateTile();
        } catch (Exception ignored) {
        }
    }
}
