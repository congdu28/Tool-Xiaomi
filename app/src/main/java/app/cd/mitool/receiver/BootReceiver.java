package app.cd.mitool.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import app.cd.mitool.service.BatteryMonitorService;
import app.cd.mitool.service.FloatingClockService;
import app.cd.mitool.util.PreferencesHelper;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            PreferencesHelper prefs = new PreferencesHelper(context);

            // Restore floating clock if user enabled it
            if (prefs.isClockEnabled() && Settings.canDrawOverlays(context)) {
                Intent clockIntent = new Intent(context, FloatingClockService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(clockIntent);
                } else {
                    context.startService(clockIntent);
                }
            }

            // Start battery monitor
            Intent batteryIntent = new Intent(context, BatteryMonitorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(batteryIntent);
            } else {
                context.startService(batteryIntent);
            }
        }
    }
}
