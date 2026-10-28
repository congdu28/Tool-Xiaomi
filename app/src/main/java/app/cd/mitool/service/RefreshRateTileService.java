package app.cd.mitool.service;

import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.List;

import app.cd.mitool.R;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.RefreshRateHelper;
import app.cd.mitool.util.ShizukuHelper;

/**
 * Quick Settings Tile service for toggling Screen Refresh Rate (60Hz / 120Hz).
 * Allows users to toggle high refresh rate from Control Center / Notification shade.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class RefreshRateTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        try {
            int currentRate = RefreshRateHelper.getCurrentRefreshRate(this);
            int targetRate;

            if (currentRate >= 90) {
                // Currently high refresh rate -> switch to 60Hz (battery saver)
                targetRate = 60;
            } else {
                // Currently 60Hz or auto -> switch to highest available (120Hz / 144Hz)
                List<Integer> supported = RefreshRateHelper.getSupportedRefreshRates(this);
                if (supported.contains(144) && !supported.contains(120)) {
                    targetRate = 144;
                } else if (supported.contains(120)) {
                    targetRate = 120;
                } else if (supported.contains(90)) {
                    targetRate = 90;
                } else {
                    targetRate = java.util.Collections.max(supported);
                }
            }

            RefreshRateHelper.applyAsync(this, targetRate, (success, message) -> {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                updateTileState();
            });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi chuyển tần số quét: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTileState() {
        try {
            Tile tile = getQsTile();
            if (tile == null) return;

            int currentRate = RefreshRateHelper.getCurrentRefreshRate(this);
            boolean isHighRate = currentRate >= 90;

            tile.setState(isHighRate ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.tile_hz_label));
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_hz));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(currentRate + "Hz");
            }

            tile.updateTile();
        } catch (Exception ignored) {
        }
    }
}
