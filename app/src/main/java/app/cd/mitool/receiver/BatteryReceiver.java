package app.cd.mitool.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import app.cd.mitool.service.BatteryMonitorService;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.view.ChargingHudView;

public class BatteryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        PreferencesHelper prefs = new PreferencesHelper(context);
        String action = intent.getAction();

        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            // Trigger haptic feedback
            BatteryHelper.vibrateDevice(context, 100);

            // Display Charging HUD overlay if enabled
            if (prefs.isChargingHudEnabled() && Settings.canDrawOverlays(context)) {
                BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(context);
                info.isCharging = true;
                ChargingHudView.show(context, info);
            }

            // Ensure monitor service is active
            startBatteryService(context);
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            // Optionally update state
        }
    }

    private void startBatteryService(Context context) {
        Intent serviceIntent = new Intent(context, BatteryMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
