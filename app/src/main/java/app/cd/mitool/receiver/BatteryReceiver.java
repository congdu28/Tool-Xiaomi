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
import app.cd.mitool.view.ChargingHudView;

public class BatteryReceiver extends BroadcastReceiver {

    public static final int OLD_CHARGING_NOTIF_ID = 3003;
    public static final int OLD_BATTERY_NOTIF_ID = 2002;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        PreferencesHelper prefs = new PreferencesHelper(context);
        String action = intent.getAction();

        // Ensure old duplicate notifications from previous app versions are wiped clean
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(OLD_CHARGING_NOTIF_ID);
            nm.cancel(OLD_BATTERY_NOTIF_ID);
        }

        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            // Read realtime battery info directly from system API
            BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(context);
            info.isCharging = true;

            // Optional subtle haptic feedback
            BatteryHelper.vibrateDevice(context, 80);

            // 1. Update or start single unified notification (shows Clock + Charging W, ETA, Temp)
            if (prefs.isLockscreenNotifEnabled() || prefs.isNotifClockEnabled()) {
                Intent serviceIntent = new Intent(context, NotificationClockService.class);
                serviceIntent.setAction(NotificationClockService.ACTION_UPDATE_BATTERY);
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

            // 3. Show optional dynamic HUD pill if user enabled and has overlay permission
            if (prefs.isChargingHudEnabled() && Settings.canDrawOverlays(context)) {
                try {
                    ChargingHudView.show(context, info);
                } catch (Exception ignored) {
                }
            }

        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            if (!prefs.isNotifClockEnabled()) {
                // If user didn't want clock constantly, dismiss unified notification when unplugged
                Intent stopIntent = new Intent(context, NotificationClockService.class);
                stopIntent.setAction(NotificationClockService.ACTION_STOP);
                try {
                    context.startService(stopIntent);
                } catch (Exception ignored) {
                }
            } else {
                // If clock is enabled, update notification to show battery status
                Intent updateIntent = new Intent(context, NotificationClockService.class);
                updateIntent.setAction(NotificationClockService.ACTION_UPDATE_BATTERY);
                try {
                    context.startService(updateIntent);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
