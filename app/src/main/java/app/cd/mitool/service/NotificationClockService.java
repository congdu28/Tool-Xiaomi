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

/**
 * Ultra-lightweight notification service displaying realtime clock in the Notification Drawer.
 * 100% NO ADB or PC required. Pinned at top of notification shade.
 * Zero CPU / battery drain when screen is OFF (pauses background ticker).
 */
public class NotificationClockService extends Service {

    public static final String ACTION_START = "app.cd.mitool.START_NOTIF_CLOCK";
    public static final String ACTION_STOP = "app.cd.mitool.STOP_NOTIF_CLOCK";

    private static final String CHANNEL_ID = "mitool_notification_clock";
    private static final int NOTIF_ID = 4004;

    private NotificationManager notificationManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tickerRunnable;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private PreferencesHelper prefs;

    private boolean isScreenOn = true;
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                isScreenOn = false;
                handler.removeCallbacks(tickerRunnable);
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                isScreenOn = true;
                if (notificationManager != null) {
                    notificationManager.notify(NOTIF_ID, buildClockNotification());
                }
                handler.removeCallbacks(tickerRunnable);
                handler.postDelayed(tickerRunnable, 1000);
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

        createChannel();
        startForeground(NOTIF_ID, buildClockNotification());
        registerScreenStateReceiver();
        startTicker();
    }

    private void registerScreenStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(screenReceiver, filter);
            }
        } catch (Exception ignored) {
            try {
                registerReceiver(screenReceiver, filter);
            } catch (Exception ignored2) {
            }
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Đồng hồ giây trên thanh thông báo",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Hiển thị giờ, phút, giây trực tiếp trong bảng thông báo kéo xuống");
            channel.setShowBadge(false);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildClockNotification() {
        Date now = new Date();
        String timeStr = timeFormat.format(now);
        String dateStr = dateFormat.format(now);

        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Read battery summary
        BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(this);
        String subtext = "Pin: " + info.level + "% • " + String.format(Locale.US, "%.1f°C", info.temperature);
        if (info.isCharging) {
            subtext = "⚡ Đang sạc (" + info.level + "%) • " + String.format(Locale.US, "%.1f°C", info.temperature);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🕒 " + timeStr + "  (" + dateStr + ")")
                .setContentText(subtext)
                .setSmallIcon(R.drawable.ic_clock)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void startTicker() {
        tickerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isScreenOn) return;
                if (notificationManager != null) {
                    notificationManager.notify(NOTIF_ID, buildClockNotification());
                }
                // Update every second
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(tickerRunnable, 1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
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
            unregisterReceiver(screenReceiver);
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
