package app.cd.mitool.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import app.cd.mitool.util.PreferencesHelper;

public class SotWidgetScheduler {

    private static final int REQ_CODE = 9021;

    public static void scheduleNext(Context context) {
        if (context == null) return;
        try {
            PreferencesHelper prefs = new PreferencesHelper(context);
            int intervalMinutes = prefs.getWidgetRefreshInterval();
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Intent intent = new Intent(context, SotAppWidgetProvider.class);
            intent.setAction(SotAppWidgetProvider.ACTION_REFRESH_SOT);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getBroadcast(context, REQ_CODE, intent, flags);

            if (intervalMinutes <= 0) {
                am.cancel(pi);
                return;
            }

            long intervalMs = intervalMinutes * 60 * 1000L;
            long triggerAt = SystemClock.elapsedRealtime() + intervalMs;

            am.cancel(pi);
            am.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    triggerAt,
                    intervalMs,
                    pi
            );
        } catch (Throwable ignored) {
        }
    }

    public static void cancel(Context context) {
        if (context == null) return;
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Intent intent = new Intent(context, SotAppWidgetProvider.class);
            intent.setAction(SotAppWidgetProvider.ACTION_REFRESH_SOT);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getBroadcast(context, REQ_CODE, intent, flags);
            am.cancel(pi);
        } catch (Throwable ignored) {
        }
    }
}
