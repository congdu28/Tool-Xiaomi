package app.cd.mitool.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import app.cd.mitool.MainActivity;
import app.cd.mitool.R;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.SotHelper;

/**
 * Home Screen Widget for Screen On Time (SOT) and Battery stats.
 * Supports multiple HyperOS styles, system theme detection (Light/Dark),
 * manual refresh button and periodic auto-updates.
 */
public class SotAppWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH_SOT = "app.cd.mitool.widget.ACTION_REFRESH_SOT";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
        SotWidgetScheduler.scheduleNext(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        SotWidgetScheduler.scheduleNext(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        SotWidgetScheduler.cancel(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_REFRESH_SOT.equals(action)
                || Intent.ACTION_SCREEN_ON.equals(action)
                || Intent.ACTION_SCREEN_OFF.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action)
                || Intent.ACTION_POWER_CONNECTED.equals(action)
                || Intent.ACTION_POWER_DISCONNECTED.equals(action)
                || Intent.ACTION_BATTERY_CHANGED.equals(action)) {

            if (ACTION_REFRESH_SOT.equals(action) && intent.getBooleanExtra("manual_click", false)) {
                BatteryHelper.vibrateDevice(context, 20);
            }

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
        PreferencesHelper prefs = new PreferencesHelper(context);

        int style = prefs.getWidgetStyle();
        int layoutId;
        switch (style) {
            case PreferencesHelper.WIDGET_STYLE_MINIMAL:
                layoutId = R.layout.widget_sot_minimal;
                break;
            case PreferencesHelper.WIDGET_STYLE_DASHBOARD:
                layoutId = R.layout.widget_sot_dashboard;
                break;
            case PreferencesHelper.WIDGET_STYLE_HYPEROS:
            default:
                layoutId = R.layout.widget_sot;
                break;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        // 1. Calculate SOT & Battery
        long sotMillis = SotHelper.getScreenOnTimeMillis(context);
        String sotTimeStr = SotHelper.formatDuration(sotMillis);
        int batteryPct = BatteryHelper.getBatteryPercentage(context);
        float temp = BatteryHelper.getBatteryTemperature(context);
        String totalUsageStr = SotHelper.getTotalUsageDuration(context);
        String summary = SotHelper.getUnplugSummary(context, batteryPct);
        SotHelper.BatteryEstimate estimate = SotHelper.getBatteryEstimate(context);

        views.setTextViewText(R.id.widget_tv_sot_time, sotTimeStr);

        if (style == PreferencesHelper.WIDGET_STYLE_DASHBOARD) {
            views.setTextViewText(R.id.widget_tv_sot_battery, batteryPct + "%");
            views.setTextViewText(R.id.widget_tv_sot_temp, String.format(java.util.Locale.getDefault(), "%.1f°C", temp));
            views.setTextViewText(R.id.widget_tv_sot_total_bar, "🕒 Tổng dùng máy: " + totalUsageStr);
            views.setTextViewText(R.id.widget_tv_sot_total, "Tổng dùng: " + totalUsageStr);
        } else {
            views.setTextViewText(R.id.widget_tv_sot_battery, "🔋 " + batteryPct + "%");
            views.setTextViewText(R.id.widget_tv_sot_temp, String.format(java.util.Locale.getDefault(), "🌡️ %.1f°C", temp));
        }

        // Subtext / Estimate
        String subText;
        if (prefs.isWidgetShowEstimate() && estimate != null && estimate.remainSotMinutes > 0) {
            if (style == PreferencesHelper.WIDGET_STYLE_DASHBOARD) {
                subText = "~" + estimate.formattedSot;
            } else {
                subText = "⚡ Còn ~" + estimate.formattedSot + " • Tổng: " + totalUsageStr;
            }
        } else {
            if (style == PreferencesHelper.WIDGET_STYLE_DASHBOARD) {
                subText = "Tổng: " + totalUsageStr;
            } else {
                subText = "Tổng dùng: " + totalUsageStr;
            }
        }
        views.setTextViewText(R.id.widget_tv_sot_sub, subText);

        // Visibility of temp
        if (views != null) {
            views.setViewVisibility(R.id.widget_tv_sot_temp, prefs.isWidgetShowTemp() ? View.VISIBLE : View.GONE);
        }

        // 2. Theme Adaptation (Light / Dark)
        int themeMode = prefs.getWidgetThemeMode();
        boolean isNight;
        if (themeMode == PreferencesHelper.WIDGET_THEME_DARK) {
            isNight = true;
        } else if (themeMode == PreferencesHelper.WIDGET_THEME_LIGHT) {
            isNight = false;
        } else {
            int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            isNight = (currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        }

        // Apply background card
        views.setInt(R.id.widget_sot_root, "setBackgroundResource",
                isNight ? R.drawable.bg_widget_card : R.drawable.bg_widget_card_light);

        // Apply refresh button tint
        views.setInt(R.id.widget_btn_sot_refresh, "setColorFilter",
                isNight ? 0xFFFFFFFF : 0xFF222222);

        // Apply colors according to style & theme
        if (style == PreferencesHelper.WIDGET_STYLE_DASHBOARD) {
            int boxBg = isNight ? R.drawable.bg_widget_box_dark : R.drawable.bg_widget_box_light;
            views.setInt(R.id.widget_box_1, "setBackgroundResource", boxBg);
            views.setInt(R.id.widget_box_2, "setBackgroundResource", boxBg);
            views.setInt(R.id.widget_box_3, "setBackgroundResource", boxBg);
            views.setInt(R.id.widget_box_4, "setBackgroundResource", boxBg);

            int labelColor = isNight ? 0xFFA0A5B5 : 0xFF666677;
            views.setTextColor(R.id.widget_tv_label_sot, labelColor);
            views.setTextColor(R.id.widget_tv_label_battery, labelColor);
            views.setTextColor(R.id.widget_tv_label_temp, labelColor);
            views.setTextColor(R.id.widget_tv_label_sub, labelColor);

            int subTextColor = isNight ? 0xFFA0A5B5 : 0xFF666677;
            views.setTextColor(R.id.widget_tv_sot_total_bar, subTextColor);
            views.setTextColor(R.id.widget_tv_sot_total, subTextColor);

            views.setTextColor(R.id.widget_tv_sot_title, 0xFFFF6700);
            views.setTextColor(R.id.widget_tv_sot_time, isNight ? 0xFF00E5FF : 0xFF007E8A);
            views.setTextColor(R.id.widget_tv_sot_battery, isNight ? 0xFF00E676 : 0xFF1B5E20);
            views.setTextColor(R.id.widget_tv_sot_temp, isNight ? 0xFFFFD600 : 0xFFE65100);
            views.setTextColor(R.id.widget_tv_sot_sub, isNight ? 0xFFFFFFFF : 0xFF1C1B1F);
        } else {
            int pillBg = isNight ? R.drawable.bg_widget_pill : R.drawable.bg_widget_pill_light;
            views.setInt(R.id.widget_tv_sot_battery, "setBackgroundResource", pillBg);
            views.setInt(R.id.widget_tv_sot_temp, "setBackgroundResource", pillBg);

            views.setTextColor(R.id.widget_tv_sot_title, 0xFFFF6700);
            views.setTextColor(R.id.widget_tv_sot_time, isNight ? 0xFF00E5FF : 0xFF007E8A);
            views.setTextColor(R.id.widget_tv_sot_battery, isNight ? 0xFF00E676 : 0xFF1B5E20);
            views.setTextColor(R.id.widget_tv_sot_temp, isNight ? 0xFF00E5FF : 0xFF007E8A);
            views.setTextColor(R.id.widget_tv_sot_sub, isNight ? 0xFFA0A5B5 : 0xFF555566);
        }

        // 3. Click on widget root -> Open MainActivity into Tab 1 (Pin)
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openAppIntent.putExtra("extra_tab_index", 1);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent openAppPi = PendingIntent.getActivity(
                context,
                1001,
                openAppIntent,
                piFlags
        );
        views.setOnClickPendingIntent(R.id.widget_sot_root, openAppPi);

        // 4. Click on small refresh button -> Refresh SOT widget immediately with haptic
        Intent refreshIntent = new Intent(context, SotAppWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH_SOT);
        refreshIntent.putExtra("manual_click", true);
        PendingIntent refreshPi = PendingIntent.getBroadcast(
                context,
                1002,
                refreshIntent,
                piFlags
        );
        views.setOnClickPendingIntent(R.id.widget_btn_sot_refresh, refreshPi);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
