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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import app.cd.mitool.MainActivity;
import app.cd.mitool.R;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.VoiceAlertHelper;

/**
 * Real-time Lockscreen Charging & Clock Notification Service.
 * - Shows on Lockscreen (IMPORTANCE_HIGH, VISIBILITY_PUBLIC, completely silent).
 * - Realtime Clock (HH:mm:ss) + Power (W) + Battery % + Temp (°C) + Standard ETA.
 * - Automatically dismissed when charger is unplugged.
 * - 0% CPU consumption when screen is OFF.
 */
public class NotificationClockService extends Service {

    public static final String ACTION_START = "app.cd.mitool.START_NOTIF_CLOCK";
    public static final String ACTION_STOP = "app.cd.mitool.STOP_NOTIF_CLOCK";
    public static final String ACTION_START_CHARGING = "app.cd.mitool.START_CHARGING";
    public static final String ACTION_UPDATE_BATTERY = "app.cd.mitool.UPDATE_BATTERY";

    // New unique channel ID with IMPORTANCE_HIGH to force Android & Xiaomi HyperOS to show on Lockscreen
    public static final String CHANNEL_ID = "mitool_charging_lockscreen_v4";
    public static final int NOTIF_ID = 4004;

    private NotificationManager notificationManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tickerRunnable;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private PreferencesHelper prefs;

    private boolean isScreenOn = true;
    private boolean alerted80 = false;
    private boolean alerted100 = false;
    private boolean alertedTemp = false;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();

            if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                // When unplugged: Seamlessly switch to battery status, keeping unified notification active
                BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(context);
                VoiceAlertHelper.onPowerDisconnected(context, info.level);
                updateNotification();
            } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(context);
                VoiceAlertHelper.onPowerConnected(context, info.plugType, info.powerWatt, info.level);
                updateNotification();
                startTicker();
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                isScreenOn = false;
                if (tickerRunnable != null) {
                    handler.removeCallbacks(tickerRunnable);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                isScreenOn = true;
                updateNotification();
                startTicker();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(context, intent);
                checkBatteryAlerts(info);
                updateNotification();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new PreferencesHelper(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        dateFormat = new SimpleDateFormat("EEEE, dd/MM", new Locale("vi", "VN"));

        // Clean up legacy notification channels & old notification IDs
        if (notificationManager != null) {
            notificationManager.cancel(1001);
            notificationManager.cancel(2002);
            notificationManager.cancel(3003);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    notificationManager.deleteNotificationChannel("mitool_notification_clock");
                    notificationManager.deleteNotificationChannel("mitool_charging_notifications");
                    notificationManager.deleteNotificationChannel("mitool_battery_channel");
                } catch (Exception ignored) {
                }
            }
        }

        createChannel();
        startForeground(NOTIF_ID, buildUnifiedNotification());
        registerStateReceivers();
        startTicker();
    }

    private void registerStateReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(stateReceiver, filter);
            }
        } catch (Exception ignored) {
            try {
                registerReceiver(stateReceiver, filter);
            } catch (Exception ignored2) {
            }
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Thông báo sạc & Màn hình khóa",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Hiển thị thông số sạc, công suất W, nhiệt độ và ETA trực tiếp ngoài màn hình khóa");
            channel.setShowBadge(false);
            channel.setSound(null, null); // completely silent, no annoying beeps every second
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildUnifiedNotification() {
        Date now = new Date();
        String timeStr = timeFormat.format(now);
        String dateStr = dateFormat.format(now);

        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Read realtime battery info
        BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(this);

        String title = "🕒 " + timeStr + "  (" + dateStr + ")";
        String summaryText;
        StringBuilder bigText = new StringBuilder();

        if (info.isCharging) {
            String powerStr = info.powerWatt > 0 ? String.format(Locale.US, "%.1fW", info.powerWatt) : "⚡ Sạc nhanh";
            String etaShort = info.estimatedMinutesToFull > 0 ? ("Còn ~" + info.estimatedMinutesToFull + "p") : info.formattedEta;
            summaryText = "⚡ " + info.level + "% • " + powerStr + " • " + String.format(Locale.US, "%.1f°C", info.temperature) + " • " + etaShort;

            // Expanded detail
            bigText.append("⚡ Đang sạc: ").append(info.level).append("% (").append(info.plugTypeName).append(")");
            if (info.powerWatt > 0) {
                bigText.append(" • Công suất: ").append(String.format(Locale.US, "%.1fW", info.powerWatt));
            }
            bigText.append("\n");

            if (info.estimatedMinutesToFull > 0) {
                long fullTimestamp = System.currentTimeMillis() + (info.estimatedMinutesToFull * 60 * 1000L);
                String fullTimeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(fullTimestamp));
                bigText.append("⏳ Dự kiến sạc đầy: Còn ~").append(info.estimatedMinutesToFull)
                        .append(" phút nữa (Đầy lúc ").append(fullTimeStr).append(")\n");
            } else {
                bigText.append("⏳ ").append(info.formattedEta).append("\n");
            }

            bigText.append("🌡️ Nhiệt độ pin: ").append(String.format(Locale.US, "%.1f°C", info.temperature));
            if (info.voltageMv > 0) {
                bigText.append(" • ").append(String.format(Locale.US, "%.2fV", info.voltageMv / 1000.0f));
            }
            if (info.currentMa > 0) {
                bigText.append(" • ").append(info.currentMa).append("mA");
            }
        } else {
            summaryText = "🔋 Pin: " + info.level + "% • " + String.format(Locale.US, "%.1f°C", info.temperature) + " • " + info.healthDescription;

            bigText.append("🔋 Dung lượng pin: ").append(info.level).append("% (Đang dùng pin)\n");
            bigText.append("🌡️ Nhiệt độ pin: ").append(String.format(Locale.US, "%.1f°C", info.temperature));
            if (info.voltageMv > 0) {
                bigText.append(" • Điện áp: ").append(String.format(Locale.US, "%.2fV", info.voltageMv / 1000.0f));
            }
            bigText.append("\n🩺 Tình trạng pin: ").append(info.healthDescription);
        }

        int iconRes = info.isCharging ? R.drawable.ic_battery_bolt : R.drawable.ic_clock;

        Intent deleteIntent = new Intent(this, NotificationClockService.class);
        deleteIntent.setAction(ACTION_STOP);
        PendingIntent deletePi = PendingIntent.getService(this, 1, deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(summaryText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText.toString()))
                .setSmallIcon(iconRes)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePi)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "✕ Tắt thông báo", deletePi)
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build();
    }

    private void updateNotification() {
        if (notificationManager != null) {
            notificationManager.notify(NOTIF_ID, buildUnifiedNotification());
        }
    }

    private void startTicker() {
        if (tickerRunnable != null) {
            handler.removeCallbacks(tickerRunnable);
        }
        tickerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isScreenOn) {
                    updateNotification();
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(tickerRunnable);
    }

    private void checkBatteryAlerts(BatteryHelper.BatteryInfo info) {
        if (!info.isCharging) {
            alerted80 = false;
            alerted100 = false;
            VoiceAlertHelper.resetSessionAlerts();
        }
        if (info.temperature < 40.0f) {
            alertedTemp = false;
        }

        // Voice smart announcements
        VoiceAlertHelper.checkLevelAlerts(this, info.level, info.isCharging);
        VoiceAlertHelper.checkTempAlert(this, info.temperature);

        if (info.isCharging && info.level >= 80 && info.level < 85 && prefs.isAlert80Enabled() && !alerted80) {
            alerted80 = true;
            BatteryHelper.playAlertTone(this);
            BatteryHelper.vibrateDevice(this, 800);
        }

        if (info.isFull && prefs.isAlert100Enabled() && !alerted100) {
            alerted100 = true;
            BatteryHelper.playAlertTone(this);
            BatteryHelper.vibrateDevice(this, 1200);
        }

        if (info.temperature >= 42.0f && prefs.isAlertTempEnabled() && !alertedTemp) {
            alertedTemp = true;
            BatteryHelper.playAlertTone(this);
            BatteryHelper.vibrateDevice(this, 1500);
        }
    }

    private void dismissAndStop() {
        if (handler != null && tickerRunnable != null) {
            handler.removeCallbacks(tickerRunnable);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        if (notificationManager != null) {
            notificationManager.cancel(NOTIF_ID);
            notificationManager.cancelAll();
        }
        if (prefs != null) {
            prefs.setNotifClockEnabled(false);
        }
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                dismissAndStop();
                return START_NOT_STICKY;
            } else if (ACTION_START_CHARGING.equals(action) || ACTION_START.equals(action)) {
                updateNotification();
                startTicker();
            } else if (ACTION_UPDATE_BATTERY.equals(action)) {
                updateNotification();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && tickerRunnable != null) {
            handler.removeCallbacks(tickerRunnable);
        }
        try {
            unregisterReceiver(stateReceiver);
        } catch (Exception ignored) {
        }
        if (notificationManager != null) {
            notificationManager.cancel(NOTIF_ID);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
