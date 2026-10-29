package app.cd.mitool;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import app.cd.mitool.service.NotificationClockService;
import app.cd.mitool.service.NotificationTileService;
import app.cd.mitool.util.PreferencesHelper;

/**
 * Headless translucent activity that toggles the Notification Clock Service in 1 tap,
 * displays a quick toast, and finishes immediately without showing any UI or appearing in Recents.
 */
public class ToggleNotificationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            PreferencesHelper prefs = new PreferencesHelper(this);
            boolean currentlyEnabled = prefs.isNotifClockEnabled();
            boolean newState = !currentlyEnabled;
            prefs.setNotifClockEnabled(newState);

            if (newState) {
                Intent intent = new Intent(this, NotificationClockService.class);
                intent.setAction(NotificationClockService.ACTION_START);
                ContextCompat.startForegroundService(this, intent);
                Toast.makeText(this, R.string.message_4e09d4fa5b, Toast.LENGTH_SHORT).show();
            } else {
                NotificationClockService.sync(this);
                Toast.makeText(this, R.string.message_cb902a4f54, Toast.LENGTH_SHORT).show();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    TileService.requestListeningState(this, new ComponentName(this, NotificationTileService.class));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi bật/tắt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
