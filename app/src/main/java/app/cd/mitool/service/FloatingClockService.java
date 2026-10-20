package app.cd.mitool.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import app.cd.mitool.MainActivity;
import app.cd.mitool.R;
import app.cd.mitool.util.PreferencesHelper;

public class FloatingClockService extends Service {

    public static final String ACTION_RELOAD_SETTINGS = "app.cd.mitool.RELOAD_CLOCK_SETTINGS";
    public static final String ACTION_STOP = "app.cd.mitool.STOP_CLOCK";

    private static final String CHANNEL_ID = "mitool_clock_service";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    private TextView tvClock;
    private TextView tvBattery;

    private PreferencesHelper prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    private SimpleDateFormat sdfStandard;
    private SimpleDateFormat sdfMs;

    private int lastBatteryLevel = -1;
    private long lastBatteryCheckTime = 0;
    private boolean isScreenOn = true;

    private final android.content.BroadcastReceiver screenReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                isScreenOn = false;
                if (handler != null && clockRunnable != null) {
                    handler.removeCallbacks(clockRunnable);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                isScreenOn = true;
                if (clockRunnable != null) {
                    handler.removeCallbacks(clockRunnable);
                    handler.post(clockRunnable);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new PreferencesHelper(this);
        sdfStandard = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        sdfMs = new SimpleDateFormat("HH:mm:ss.SS", Locale.getDefault());

        startForegroundServiceNotification();

        if (Settings.canDrawOverlays(this)) {
            initFloatingView();
            registerScreenStateReceiver();
            startClockTicker();
        } else {
            stopSelf();
        }
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

    private void startForegroundServiceNotification() {
        createNotificationChannel();

        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Đồng hồ giây đang hiển thị")
                .setContentText("Chạm để mở cài đặt Mi Tool")
                .setSmallIcon(R.drawable.ic_app_icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification, 0);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Đồng hồ giây nổi",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Duy trì dịch vụ đồng hồ giây");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void initFloatingView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.layout_floating_clock, null);

        tvClock = floatingView.findViewById(R.id.tv_floating_clock);
        tvBattery = floatingView.findViewById(R.id.tv_floating_battery);

        applyViewCustomizations();

        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        if (prefs.isClockStatusBarMode() || prefs.isClockLockPosition()) {
            flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                flags,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        if (prefs.isClockStatusBarMode()) {
            params.x = prefs.getClockX();
            int sbHeight = getStatusBarHeight();
            params.y = Math.max(0, (sbHeight - (int) (18 * getResources().getDisplayMetrics().density)) / 2);
        } else {
            params.x = prefs.getClockX();
            params.y = prefs.getClockY();
        }

        setupTouchListener();

        windowManager.addView(floatingView, params);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        if (result <= 0) {
            result = (int) (28 * getResources().getDisplayMetrics().density);
        }
        return result;
    }

    private void setupTouchListener() {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (prefs.isClockLockPosition()) {
                    return false;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        prefs.setClockX(params.x);
                        prefs.setClockY(params.y);
                        return true;
                }
                return false;
            }
        });
    }

    private void applyViewCustomizations() {
        if (tvClock == null || floatingView == null) return;

        View root = floatingView.findViewById(R.id.clock_root);
        if (root != null) {
            if (prefs.isClockTransparentBg() || prefs.isClockStatusBarMode()) {
                root.setBackgroundColor(Color.TRANSPARENT);
                root.setPadding(0, 0, 0, 0);
            } else {
                root.setBackgroundResource(R.drawable.bg_floating_capsule);
                int pHoriz = (int) (10 * getResources().getDisplayMetrics().density);
                int pVert = (int) (5 * getResources().getDisplayMetrics().density);
                root.setPadding(pHoriz, pVert, pHoriz, pVert);
            }
        }

        // Size
        float textSize = 15f;
        int sizePref = prefs.getClockSize();
        if (sizePref == 0) textSize = 12f;
        else if (sizePref == 1) textSize = 15f;
        else if (sizePref == 2) textSize = 18f;
        tvClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

        // Color
        int color = Color.WHITE;
        int colorPref = prefs.getClockColor();
        if (colorPref == 1) color = Color.parseColor("#00E676"); // Neon green
        else if (colorPref == 2) color = Color.parseColor("#FF6900"); // Xiaomi orange
        tvClock.setTextColor(color);

        // Show battery
        if (tvBattery != null) {
            tvBattery.setVisibility((prefs.isClockShowBattery() && !prefs.isClockStatusBarMode()) ? View.VISIBLE : View.GONE);
        }
    }

    private void startClockTicker() {
        if (clockRunnable != null) {
            handler.removeCallbacks(clockRunnable);
        }
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                boolean showMs = prefs.isClockShowMs();
                long delay = showMs ? 45 : (1000 - (System.currentTimeMillis() % 1000));
                handler.postDelayed(this, Math.max(15, delay));
            }
        };
        handler.post(clockRunnable);
    }

    private void updateTime() {
        Date now = new Date();
        boolean showMs = prefs.isClockShowMs();
        String timeStr = showMs ? sdfMs.format(now) : sdfStandard.format(now);
        if (tvClock != null) {
            tvClock.setText(timeStr);
        }

        // Periodically refresh battery if enabled (every 5 seconds)
        if (prefs.isClockShowBattery() && tvBattery != null) {
            long currentMillis = System.currentTimeMillis();
            if (currentMillis - lastBatteryCheckTime > 5000 || lastBatteryLevel == -1) {
                lastBatteryCheckTime = currentMillis;
                Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batteryStatus != null) {
                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    if (level >= 0 && scale > 0) {
                        lastBatteryLevel = Math.round((level / (float) scale) * 100);
                        tvBattery.setText(lastBatteryLevel + "%");
                    }
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_RELOAD_SETTINGS.equals(action)) {
                applyViewCustomizations();
                if (floatingView != null && windowManager != null && params != null) {
                    int windowFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                    if (prefs.isClockStatusBarMode() || prefs.isClockLockPosition()) {
                        windowFlags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                    }
                    params.flags = windowFlags;
                    if (prefs.isClockStatusBarMode()) {
                        params.x = prefs.getClockX();
                        int sbHeight = getStatusBarHeight();
                        params.y = Math.max(0, (sbHeight - (int) (18 * getResources().getDisplayMetrics().density)) / 2);
                    } else {
                        params.x = prefs.getClockX();
                        params.y = prefs.getClockY();
                    }
                    try {
                        windowManager.updateViewLayout(floatingView, params);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && clockRunnable != null) {
            handler.removeCallbacks(clockRunnable);
        }
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {
        }
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
