package app.cd.mitool.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.service.quicksettings.TileService;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import app.cd.mitool.MainActivity;
import app.cd.mitool.R;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.SotHelper;
import app.cd.mitool.util.VoiceAlertHelper;

/**
 * Real-time Lockscreen Charging & Clock Notification Service.
 * - Shows on Lockscreen (IMPORTANCE_HIGH, VISIBILITY_PUBLIC, completely silent).
 * - Realtime Clock (HH:mm:ss) + Power (W) + Battery % + Temp (°C) + Standard ETA.
 * - Battery alerts remain event driven; seconds rendering is enabled explicitly.
 * - No recurring notification polling while the display is off.
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
    private final app.cd.mitool.util.BatteryAlertPolicy alertPolicy = new app.cd.mitool.util.BatteryAlertPolicy();
    private BatteryHelper.BatteryInfo cachedBattery;
    private long lastBatteryRead;
    private long lastNotificationUpdate;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();

            if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                BatteryHelper.stopAlertTone();
                // When unplugged: Seamlessly switch to battery status, keeping unified notification active
                cachedBattery = BatteryHelper.getBatteryInfo(context);
                lastBatteryRead = android.os.SystemClock.elapsedRealtime();
                updateNotification(true);
            } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                cachedBattery = BatteryHelper.getBatteryInfo(context);
                lastBatteryRead = android.os.SystemClock.elapsedRealtime();
                updateNotification(true);
                startTicker();
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                isScreenOn = false;
                SotHelper.onScreenOff(context);
                if (tickerRunnable != null) {
                    handler.removeCallbacks(tickerRunnable);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                isScreenOn = true;
                SotHelper.onScreenOn(context);
                updateNotification(true);
                startTicker();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean wasPlugged = cachedBattery != null && cachedBattery.isPlugged;
                BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(context, intent);
                cachedBattery = info;
                lastBatteryRead = android.os.SystemClock.elapsedRealtime();
                checkBatteryAlerts(info);
                updateNotification(wasPlugged != info.isPlugged);
                if (wasPlugged != info.isPlugged) startTicker();
                app.cd.mitool.util.BatteryHistoryHelper.recordSnapshot(context, info.level, info.isPlugged);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new PreferencesHelper(this);
        android.os.PowerManager power = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        isScreenOn = power != null && power.isInteractive();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        dateFormat = new SimpleDateFormat("EEEE, dd/MM", new Locale("vi", "VN"));

        // Clean up legacy notification channels
        if (notificationManager != null) {
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
        notifyTileUpdate();

    }

    private void notifyTileUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                TileService.requestListeningState(this, new ComponentName(this, NotificationTileService.class));
            } catch (Exception ignored) {
            }
        }
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
        openApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openApp.putExtra("extra_open_battery_alert", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 4004, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Reuse the latest broadcast/sensor reading across one-second clock updates.
        long elapsed = android.os.SystemClock.elapsedRealtime();
        if (cachedBattery == null || elapsed - lastBatteryRead >= 5000) {
            cachedBattery = BatteryHelper.getBatteryInfo(this);
            lastBatteryRead = elapsed;
        }
        BatteryHelper.BatteryInfo info = cachedBattery;

        String title = prefs.isNotifClockEnabled() ? "🕒 " + timeStr + "  (" + dateStr + ")" : "Mi Tool • Theo dõi pin";
        String summaryText;
        StringBuilder bigText = new StringBuilder();

        if (info.isPlugged) {
            String powerStr = info.powerWatt > 0 ? String.format(Locale.US, "%.1fW", info.powerWatt) : getString(R.string.charging_notif_power_unknown);
            String etaShort = info.estimatedMinutesToFull > 0 ? ("Còn ~" + info.estimatedMinutesToFull + "p") : info.formattedEta;
            summaryText = "⚡ " + info.level + "% • " + powerStr + " • " + String.format(Locale.US, "%.1f°C", info.temperature) + " • " + etaShort;

            // Expanded detail
            String chargingState = getString(info.isFull ? R.string.charging_notif_full
                    : info.isCharging ? R.string.charging_notif_active : R.string.charging_notif_paused);
            bigText.append(chargingState).append(": ").append(info.level).append("% (").append(info.plugTypeName).append(")");
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

        int iconRes = info.isPlugged ? R.drawable.ic_battery_bolt : R.drawable.ic_clock;

        Intent deleteIntent = new Intent(this, NotificationClockService.class);
        deleteIntent.setAction(ACTION_STOP);
        PendingIntent deletePi = PendingIntent.getService(this, 1, deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(summaryText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText.toString()))
                .setSmallIcon(iconRes)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_STATUS);
        if (prefs.isNotifClockEnabled()) builder.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.charging_notif_clock_off), deletePi);
        builder.addAction(android.R.drawable.ic_menu_manage, getString(R.string.charging_notif_open_settings), pendingIntent);
        return builder.build();
    }

    private void updateNotification() { updateNotification(false); }

    private void updateNotification(boolean force) {
        long now = android.os.SystemClock.elapsedRealtime();
        long interval = app.cd.mitool.util.MonitoringPolicy.notificationInterval(isScreenOn,
                prefs.isNotifClockEnabled(), cachedBattery != null && cachedBattery.isPlugged);
        // Battery broadcasts can still deliver alerts while the screen is off, without
        // rebuilding the notification for every sensor change.
        if (!force && now - lastNotificationUpdate < (interval == 0 ? 60000 : interval)) return;
        if (notificationManager != null) {
            notificationManager.notify(NOTIF_ID, buildUnifiedNotification());
            lastNotificationUpdate = now;
        }
    }

    private void startTicker() {
        if (tickerRunnable != null) handler.removeCallbacks(tickerRunnable);
        if (!isScreenOn) return;
        tickerRunnable = new Runnable() {
            @Override public void run() {
                updateNotification();
                long interval = app.cd.mitool.util.MonitoringPolicy.notificationInterval(isScreenOn,
                        prefs.isNotifClockEnabled(), cachedBattery != null && cachedBattery.isPlugged);
                if (interval > 0) handler.postDelayed(this, interval);
            }
        };
        handler.post(tickerRunnable);
    }

    private void checkBatteryAlerts(BatteryHelper.BatteryInfo info) {
        if (!info.isPlugged) VoiceAlertHelper.resetSessionAlerts();
        VoiceAlertHelper.checkLevelAlerts(this, info.level, info.isPlugged);
        VoiceAlertHelper.checkTempAlert(this, info.temperature);
        int alerts = alertPolicy.evaluate(info.isPlugged, info.level, info.temperature, prefs.getBatteryAlertLevel(),
                prefs.isAlert80Enabled(), prefs.isAlert100Enabled(), prefs.isAlertTempEnabled());
        if (alerts != 0) {
            BatteryHelper.playAlertTone(this);
            BatteryHelper.vibrateDevice(this, (alerts & app.cd.mitool.util.BatteryAlertPolicy.HOT) != 0 ? 1500
                    : (alerts & app.cd.mitool.util.BatteryAlertPolicy.FULL) != 0 ? 1200 : 800);
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
        }
        notifyTileUpdate();
        stopSelf();
    }

    public static void sync(Context context) {
        PreferencesHelper prefs = new PreferencesHelper(context);
        boolean needed = app.cd.mitool.util.MonitoringPolicy.requiresService(prefs.isNotifClockEnabled(),
                prefs.isLockscreenNotifEnabled(), prefs.isAlert80Enabled(), prefs.isAlert100Enabled(),
                prefs.isAlertTempEnabled(), prefs.isVoiceAlertMasterEnabled());
        Intent intent = new Intent(context, NotificationClockService.class);
        intent.setAction(ACTION_UPDATE_BATTERY);
        try {
            if (needed) androidx.core.content.ContextCompat.startForegroundService(context, intent);
            else context.stopService(intent);
        } catch (RuntimeException e) {
            android.util.Log.w("MiToolNotification", "System refused battery monitoring start", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) prefs.setNotifClockEnabled(false);
        }
        if (intent != null && ACTION_START.equals(intent.getAction())) prefs.setNotifClockEnabled(true);
        boolean needed = app.cd.mitool.util.MonitoringPolicy.requiresService(prefs.isNotifClockEnabled(),
                prefs.isLockscreenNotifEnabled(), prefs.isAlert80Enabled(), prefs.isAlert100Enabled(),
                prefs.isAlertTempEnabled(), prefs.isVoiceAlertMasterEnabled());
        if (!needed) {
            dismissAndStop();
            return START_NOT_STICKY;
        }
        updateNotification(true);
        startTicker();
        notifyTileUpdate();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BatteryHelper.stopAlertTone();
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
        notifyTileUpdate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
