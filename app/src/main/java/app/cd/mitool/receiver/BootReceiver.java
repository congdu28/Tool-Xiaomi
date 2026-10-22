package app.cd.mitool.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import app.cd.mitool.service.FloatingClockService;
import app.cd.mitool.service.NotificationClockService;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.widget.SotAppWidgetProvider;
import app.cd.mitool.widget.SotWidgetScheduler;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            PreferencesHelper prefs = new PreferencesHelper(context);

            // Restore floating clock if user enabled it
            if (prefs.isClockEnabled() && Settings.canDrawOverlays(context)) {
                try {
                    Intent clockIntent = new Intent(context, FloatingClockService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(clockIntent);
                    } else {
                        context.startService(clockIntent);
                    }
                } catch (Exception ignored) {
                }
            }

            // Restore unified single notification service if clock or battery alerts enabled
            if (prefs.isNotifClockEnabled() || prefs.isAlert80Enabled() || prefs.isAlert100Enabled() || prefs.isAlertTempEnabled()) {
                try {
                    Intent notifIntent = new Intent(context, NotificationClockService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(notifIntent);
                    } else {
                        context.startService(notifIntent);
                    }
                } catch (Exception ignored) {
                }
            }

            // Restore SOT widget scheduler & refresh widgets
            try {
                SotWidgetScheduler.scheduleNext(context);
                SotAppWidgetProvider.updateAllWidgets(context);
            } catch (Exception ignored) {
            }

            // Periodic background check for app update (every 2h via AlarmManager)
            try {
                app.cd.mitool.util.UpdateHelper.schedulePeriodicCheck(context);
                app.cd.mitool.util.UpdateHelper.checkForUpdate(context.getApplicationContext(), false);
            } catch (Exception ignored) {
            }
        }
    }
}
