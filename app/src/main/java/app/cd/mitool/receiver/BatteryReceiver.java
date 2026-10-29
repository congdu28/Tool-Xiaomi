package app.cd.mitool.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import app.cd.mitool.LockscreenChargingActivity;
import app.cd.mitool.service.NotificationClockService;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.SotHelper;
import app.cd.mitool.util.VoiceAlertHelper;
import app.cd.mitool.view.ChargingHudView;

public class BatteryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        PreferencesHelper prefs = new PreferencesHelper(context);
        String action = intent.getAction();

        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            // Read realtime battery info
            BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(context);
            info.isCharging = true;

            // Subtle haptic feedback
            BatteryHelper.vibrateDevice(context, 80);

            // Battery history record
            app.cd.mitool.util.BatteryHistoryHelper.recordSnapshot(context, info.level, true);

            // Screen On Time (SOT) tracking
            SotHelper.onPowerConnected(context, info.level);
            app.cd.mitool.widget.SotAppWidgetProvider.updateAllWidgets(context);
            app.cd.mitool.widget.RamBoostAppWidgetProvider.updateAllWidgets(context);

            // Voice announcement


            // Reconcile all explicitly enabled monitoring features, without enabling the clock.
            NotificationClockService.sync(context, action);

            // 2. Launch AMOLED Lockscreen Charging Screen if enabled
            if (prefs.isLockscreenAmbientEnabled()) {
                try {
                    Intent lockscreenIntent = new Intent(context, LockscreenChargingActivity.class);
                    lockscreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    context.startActivity(lockscreenIntent);
                } catch (Exception ignored) {
                }
            }

            // 3. Dynamic HUD pill if enabled
            if (prefs.isChargingHudEnabled() && Settings.canDrawOverlays(context)) {
                try {
                    ChargingHudView.show(context, info);
                } catch (Exception ignored) {
                }
            }

            // 4. Periodic background update check when plugged in (every 6h)
            long lastCheck = prefs.getLastUpdateCheckTime();
            if (System.currentTimeMillis() - lastCheck > 6 * 3600 * 1000L) {
                app.cd.mitool.util.UpdateHelper.checkForUpdate(context.getApplicationContext(), false);
            }

        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            // Read realtime battery info
            BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(context);

            // Battery history record
            app.cd.mitool.util.BatteryHistoryHelper.recordSnapshot(context, info.level, false);

            // Screen On Time (SOT) tracking - reset on unplug
            SotHelper.onPowerDisconnected(context, info.level);
            app.cd.mitool.widget.SotAppWidgetProvider.updateAllWidgets(context);
            app.cd.mitool.widget.RamBoostAppWidgetProvider.updateAllWidgets(context);

            // Voice announcement


            NotificationClockService.sync(context, action);
        }
    }
}
