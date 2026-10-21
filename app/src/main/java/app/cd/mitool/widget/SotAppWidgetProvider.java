package app.cd.mitool.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import app.cd.mitool.MainActivity;
import app.cd.mitool.R;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.SotHelper;

/**
 * Home Screen Widget for Screen On Time (SOT) and Battery stats.
 */
public class SotAppWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH_SOT = "app.cd.mitool.widget.ACTION_REFRESH_SOT";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_REFRESH_SOT.equals(action)
                || Intent.ACTION_SCREEN_ON.equals(action)
                || Intent.ACTION_SCREEN_OFF.equals(action)
                || Intent.ACTION_POWER_CONNECTED.equals(action)
                || Intent.ACTION_POWER_DISCONNECTED.equals(action)
                || Intent.ACTION_BATTERY_CHANGED.equals(action)) {
            updateAllWidgets(context);
        }
    }

    public static void updateAllWidgets(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, SotAppWidgetProvider.class);
            int[] allWidgetIds = manager.getAppWidgetIds(thisWidget);
            if (allWidgetIds != null && allWidgetIds.length > 0) {
                for (int widgetId : allWidgetIds) {
                    updateWidget(context, manager, widgetId);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_sot);

        // Calculate SOT & Battery
        long sotMillis = SotHelper.getScreenOnTimeMillis(context);
        String sotTimeStr = SotHelper.formatDuration(sotMillis);
        int batteryPct = BatteryHelper.getBatteryPercentage(context);
        float temp = BatteryHelper.getBatteryTemperature(context);
        String summary = SotHelper.getUnplugSummary(context, batteryPct);
        SotHelper.BatteryEstimate estimate = SotHelper.getBatteryEstimate(context);

        views.setTextViewText(R.id.widget_tv_sot_time, sotTimeStr);
        views.setTextViewText(R.id.widget_tv_sot_battery, "🔋 " + batteryPct + "%");
        views.setTextViewText(R.id.widget_tv_sot_temp, String.format(java.util.Locale.getDefault(), "🌡️ %.1f°C", temp));

        String subText;
        if (estimate != null && estimate.remainSotMinutes > 0) {
            subText = "⚡ Còn ~" + estimate.formattedSot + " • " + summary;
        } else {
            subText = summary;
        }
        views.setTextViewText(R.id.widget_tv_sot_sub, subText);

        // Click on widget root -> Open MainActivity into Tab 1 (Pin)
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openAppIntent.putExtra("extra_tab_index", 1);
        PendingIntent openAppPi = PendingIntent.getActivity(
                context,
                1001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_sot_root, openAppPi);

        // Click on refresh button -> Refresh SOT widget immediately
        Intent refreshIntent = new Intent(context, SotAppWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH_SOT);
        PendingIntent refreshPi = PendingIntent.getBroadcast(
                context,
                1002,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_btn_sot_refresh, refreshPi);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
