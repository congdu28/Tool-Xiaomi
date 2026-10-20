package app.cd.mitool.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import app.cd.mitool.MainActivity;
import app.cd.mitool.R;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;

public class BatteryMonitorService extends Service {

    public static final String ACTION_BATTERY_UPDATED = "app.cd.mitool.BATTERY_UPDATED";
    private static final String CHANNEL_ID = "mitool_battery_channel";
    private static final int NOTIFICATION_ID = 2002;

    private PreferencesHelper prefs;
    private BroadcastReceiver batteryReceiver;

    private boolean alerted80 = false;
    private boolean alerted100 = false;
    private boolean alertedTemp = false;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new PreferencesHelper(this);
        createNotificationChannel();
        Notification notification = buildNotification("Đang theo dõi pin & sạc...", "Mi Tool đang hoạt động");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification, 0);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        registerBatteryListener();
    }

    private void registerBatteryListener() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                    processBatteryState(intent);
                }
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void processBatteryState(Intent intent) {
        BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(this, intent);

        // Reset alert flags if unplugged or discharged
        if (!info.isCharging) {
            alerted80 = false;
            alerted100 = false;
        }
        if (info.temperature < 40.0f) {
            alertedTemp = false;
        }

        // Check 80% Alert
        if (info.isCharging && info.level >= 80 && info.level < 85 && prefs.isAlert80Enabled() && !alerted80) {
            alerted80 = true;
            BatteryHelper.playAlertTone(this);
            BatteryHelper.vibrateDevice(this, 800);
        }

        // Check 100% Full Alert
        if (info.isFull && prefs.isAlert100Enabled() && !alerted100) {
            alerted100 = true;
            BatteryHelper.playAlertTone(this);
            BatteryHelper.vibrateDevice(this, 1200);
        }

        // Check Temperature Alert
        if (info.temperature >= 42.0f && prefs.isAlertTempEnabled() && !alertedTemp) {
            alertedTemp = true;
            BatteryHelper.playAlertTone(this);
            BatteryHelper.vibrateDevice(this, 1500);
        }

        // Update Notification
        String notifTitle;
        String notifText;
        if (info.isCharging) {
            notifTitle = "⚡ Đang sạc: " + info.level + "% (" + info.plugTypeName + ")";
            notifText = info.formattedEta;
        } else {
            notifTitle = "Pin: " + info.level + "% • " + info.temperature + "°C";
            notifText = "Tình trạng: " + info.healthDescription;
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(notifTitle, notifText));
        }

        // Send local broadcast to update UI if active
        Intent broadcast = new Intent(ACTION_BATTERY_UPDATED);
        sendBroadcast(broadcast);
    }

    private Notification buildNotification(String title, String text) {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_app_icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Giám sát sạc pin",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Cập nhật thông số sạc và thời gian dự kiến");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (batteryReceiver != null) {
            try {
                unregisterReceiver(batteryReceiver);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
