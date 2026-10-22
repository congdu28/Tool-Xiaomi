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
 * Quick Settings Tile service for toggling Mini Game HUD (Lớp phủ FPS nổi).
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class GameHudTileService extends TileService {

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
                Toast.makeText(this, "Vui lòng cấp quyền Hiển thị trên ứng dụng khác cho Mi Tool!", Toast.LENGTH_LONG).show();
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
            boolean currentState = prefs.isGameHudEnabled();
            boolean newState = !currentState;
            prefs.setGameHudEnabled(newState);

            if (newState) {
                Intent serviceIntent = new Intent(this, FloatingGameHudService.class);
                ContextCompat.startForegroundService(this, serviceIntent);
                Toast.makeText(this, "🎮 Đã bật Mini Game HUD (FPS)", Toast.LENGTH_SHORT).show();
            } else {
                Intent stopIntent = new Intent(this, FloatingGameHudService.class);
                stopIntent.setAction(FloatingGameHudService.ACTION_STOP);
                startService(stopIntent);
                Toast.makeText(this, "🎮 Đã tắt Mini Game HUD", Toast.LENGTH_SHORT).show();
            }

            updateTileState();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTileState() {
        try {
            Tile tile = getQsTile();
            if (tile == null) return;

            PreferencesHelper prefs = new PreferencesHelper(this);
            boolean isEnabled = prefs.isGameHudEnabled();

            tile.setState(isEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.tile_game_hud_label));
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_gamepad));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(isEnabled ? "Đang bật" : "Đang tắt");
            }

            tile.updateTile();
        } catch (Exception ignored) {
        }
    }
}
