package app.cd.mitool.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Locale;

import app.cd.mitool.R;
import app.cd.mitool.util.RamBoosterHelper;

/**
 * 1-Tap Home Screen Widget for Instant RAM Boosting and Memory Display.
 */
public class RamBoostAppWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_BOOST_RAM = "app.cd.mitool.widget.ACTION_BOOST_RAM";
    public static final String ACTION_REFRESH_RAM = "app.cd.mitool.widget.ACTION_REFRESH_RAM";

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

        if (ACTION_BOOST_RAM.equals(action)) {
            updateAllWidgets(context);
        } else if (ACTION_REFRESH_RAM.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action)) {
            updateAllWidgets(context);
        }
    }

    public static void updateAllWidgets(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, RamBoostAppWidgetProvider.class);
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
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_ram_boost);

        RamBoosterHelper.MemoryStats stats = RamBoosterHelper.getMemoryStats(context);
        float freeGb = stats.availMb / 1024.0f;
        float totalGb = stats.totalMb / 1024.0f;

        views.setTextViewText(R.id.widget_tv_ram_free, String.format(Locale.getDefault(), "%.1f GB Trống", freeGb));
        views.setTextViewText(R.id.widget_tv_ram_percent, stats.percentUsed + "% Tải");
        views.setTextViewText(R.id.widget_tv_ram_sub, String.format(Locale.getDefault(), "Tổng %.1f GB • Chạm để xem bộ nhớ", totalGb));

        // PendingIntent to perform 1-Tap Boost
        Intent boostIntent = new Intent(context, app.cd.mitool.MainActivity.class);
        boostIntent.putExtra("extra_open_memory", true);
        PendingIntent boostPi = PendingIntent.getActivity(
                context,
                2001,
                boostIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_ram_root, boostPi);
        views.setOnClickPendingIntent(R.id.widget_btn_ram_boost_action, boostPi);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
