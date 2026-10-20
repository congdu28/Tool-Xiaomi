package app.cd.mitool.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

import app.cd.mitool.MainActivity;
import app.cd.mitool.R;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.view.ChargingHudView;

public class BatteryReceiver extends BroadcastReceiver {

    public static final String CHARGING_CHANNEL_ID = "mitool_charging_notifications";
    public static final int CHARGING_NOTIF_ID = 3003;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        PreferencesHelper prefs = new PreferencesHelper(context);
        String action = intent.getAction();

        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            // Read realtime battery info directly from system API
            BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(context);
            info.isCharging = true;

            // Optional subtle haptic feedback
            BatteryHelper.vibrateDevice(context, 80);

            // 1. Show native system notification with charging ETA
            showChargingNotification(context, info);

            // 2. Show optional dynamic HUD pill if user enabled and has overlay permission
            if (prefs.isChargingHudEnabled() && Settings.canDrawOverlays(context)) {
                try {
                    ChargingHudView.show(context, info);
                } catch (Exception ignored) {
                }
            }

        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            // Dismiss charging notification when unplugged
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(CHARGING_NOTIF_ID);
            }
        }
    }

    private void showChargingNotification(Context context, BatteryHelper.BatteryInfo info) {
        try {
            createNotificationChannel(context);

            Intent openApp = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openApp,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String title = "⚡ " + info.plugTypeName + " • " + info.level + "%";
            String content = info.formattedEta;
            if (info.powerWatt > 0) {
                content += String.format(java.util.Locale.US, " • %.1fW", info.powerWatt);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHARGING_CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(R.drawable.ic_battery_bolt)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(CHARGING_NOTIF_ID, builder.build());
            }
        } catch (Exception ignored) {
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHARGING_CHANNEL_ID,
                    "Thông báo sạc pin hệ thống",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Hiển thị ước lượng thời gian sạc đầy và công suất sạc");
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
