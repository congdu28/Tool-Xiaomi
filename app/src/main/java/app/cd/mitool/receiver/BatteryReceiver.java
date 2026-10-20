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

            // Voice announcement
            VoiceAlertHelper.onPowerConnected(context, info.plugType, info.powerWatt, info.level);

            // 1. Launch / Update realtime charging notification on lockscreen
            if (prefs.isLockscreenNotifEnabled() || prefs.isNotifClockEnabled()) {
                Intent serviceIntent = new Intent(context, NotificationClockService.class);
                serviceIntent.setAction(NotificationClockService.ACTION_START_CHARGING);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                } catch (Exception ignored) {
                }
            }

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

        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            // Read realtime battery info
            BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(context);
            // Voice announcement
            VoiceAlertHelper.onPowerDisconnected(context, info.level);

            // Unplugged: Seamlessly switch notification to normal battery status without dismissing
            Intent updateIntent = new Intent(context, NotificationClockService.class);
            updateIntent.setAction(NotificationClockService.ACTION_UPDATE_BATTERY);
            try {
                context.startService(updateIntent);
            } catch (Exception ignored) {
            }
        }
    }
}
