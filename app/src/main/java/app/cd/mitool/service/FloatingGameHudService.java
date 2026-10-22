package app.cd.mitool.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Choreographer;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import app.cd.mitool.MainActivity;
import app.cd.mitool.R;
import app.cd.mitool.util.PreferencesHelper;

public class FloatingGameHudService extends Service {

    public static final String ACTION_STOP = "app.cd.mitool.STOP_GAME_HUD";

    private static final String CHANNEL_ID = "mitool_game_hud_channel_v1";
    public static final int NOTIFICATION_ID = 5005;

    private static final String[] HARDWARE_FPS_SYSFS = {
            "/sys/class/drm/card0/sde-crtc-0/measured_fps",
            "/sys/class/drm/card0-DSI-1/measured_fps",
            "/sys/class/graphics/fb0/measured_fps",
            "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/measured_fps",
            "/sys/devices/virtual/graphics/fb0/fps",
            "/proc/cpufreq/cpufreq_fps"
    };

    private WindowManager windowManager;
    private View hudRootView;
    private WindowManager.LayoutParams params;

    private View layoutCompact;
    private View layoutExpanded;
    private TextView tvCompactFps;
    private TextView tvCompactTemp;
    private ImageView btnCloseCompact;

    private TextView tvExpFps;
    private TextView tvExpCpu;
    private TextView tvExpRam;
    private TextView tvExpTemp;
    private TextView tvExpPower;
    private ImageView btnCloseExp;

    private PreferencesHelper prefs;
    private ActivityManager activityManager;
    private BatteryManager batteryManager;

    private boolean isExpanded = false;
    private boolean isScreenOn = true;

    // FPS calculation variables
    private int frameCount = 0;
    private long lastFpsSampleTime = 0;
    private int currentFps = 60;
    private int maxDisplayHz = 120;

    // CPU calculation variables
    private long lastCpuTotal = 0;
    private long lastCpuWork = 0;
    private int currentCpuUsage = 0;

    // Battery / Power variables
    private float currentTemp = 36.0f;
    private float currentPowerW = 0.0f;
    private boolean isCharging = false;

    private final Handler metricsHandler = new Handler(Looper.getMainLooper());
    private Runnable metricsRunnable;

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!isScreenOn) return;
            frameCount++;
            long now = System.currentTimeMillis();
            long diff = now - lastFpsSampleTime;
            if (diff >= 1000) {
                int hwFps = readHardwareFps();
                if (hwFps > 0) {
                    currentFps = hwFps;
                } else {
                    currentFps = (int) Math.round((frameCount * 1000.0) / diff);
                    if (currentFps > maxDisplayHz + 5) currentFps = maxDisplayHz;
                }
                frameCount = 0;
                lastFpsSampleTime = now;
                updateFpsViews();
            }
            if (isScreenOn) {
                Choreographer.getInstance().postFrameCallback(this);
            }
        }
    };

    /**
     * Read actual panel / GPU rendering FPS from hardware sysfs nodes available on Xiaomi/MIUI.
     */
    private int readHardwareFps() {
        for (String path : HARDWARE_FPS_SYSFS) {
            try {
                File file = new File(path);
                if (!file.exists() || !file.canRead()) continue;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                    String line = br.readLine();
                    if (line != null) {
                        line = line.trim();
                        if (line.toLowerCase(Locale.US).contains("fps")) {
                            line = line.replaceAll("[^0-9.]", " ").trim();
                            String[] parts = line.split("\\s+");
                            if (parts.length > 0 && !parts[0].isEmpty()) {
                                float val = Float.parseFloat(parts[0]);
                                int fps = Math.round(val);
                                if (fps >= 10 && fps <= 240) return fps;
                            }
                        } else if (!line.isEmpty()) {
                            try {
                                float val = Float.parseFloat(line);
                                int fps = Math.round(val);
                                if (fps >= 10 && fps <= 240) return fps;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return -1;
    }

    private final BroadcastReceiver screenAndBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();

            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                isScreenOn = false;
                Choreographer.getInstance().removeFrameCallback(frameCallback);
                if (metricsRunnable != null) {
                    metricsHandler.removeCallbacks(metricsRunnable);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                isScreenOn = true;
                lastFpsSampleTime = System.currentTimeMillis();
                frameCount = 0;
                Choreographer.getInstance().postFrameCallback(frameCallback);
                if (metricsRunnable != null) {
                    metricsHandler.post(metricsRunnable);
                }
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                currentTemp = tempRaw > 0 ? (tempRaw / 10.0f) : 36.0f;

                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                              status == BatteryManager.BATTERY_STATUS_FULL);

                int voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000);
                long currentUa = 0;
                if (batteryManager != null) {
                    currentUa = Math.abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
                }
                if (currentUa > 0 && voltageMv > 0) {
                    currentPowerW = (voltageMv / 1000.0f) * (currentUa / 1000000.0f);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new PreferencesHelper(this);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);

        // Detect screen refresh rate
        try {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            if (wm != null && wm.getDefaultDisplay() != null) {
                maxDisplayHz = Math.round(wm.getDefaultDisplay().getRefreshRate());
                if (maxDisplayHz < 60) maxDisplayHz = 60;
            }
        } catch (Throwable ignored) {
            maxDisplayHz = 120;
        }

        startForegroundNotification();

        if (Settings.canDrawOverlays(this)) {
            initHudView();
            registerReceivers();
            startMetricPolling();
        } else {
            stopSelf();
        }
    }

    private void startForegroundNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID,
                    "Game HUD FPS & Hiệu Năng",
                    NotificationManager.IMPORTANCE_LOW
            );
            chan.setShowBadge(false);
            nm.createNotificationChannel(chan);
        }

        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent piOpen = PendingIntent.getActivity(
                this, 5005, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent stopIntent = new Intent(this, FloatingGameHudService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent piStop = PendingIntent.getService(
                this, 5006, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_gamepad)
                .setContentTitle("🎮 Game HUD FPS Đang Chạy")
                .setContentText("Theo dõi FPS thời gian thực & Hiệu năng hệ thống")
                .setContentIntent(piOpen)
                .addAction(R.drawable.ic_close, "Tắt HUD", piStop)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, 0);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void initHudView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        hudRootView = inflater.inflate(R.layout.layout_floating_game_hud, null);

        layoutCompact = hudRootView.findViewById(R.id.layout_hud_compact);
        layoutExpanded = hudRootView.findViewById(R.id.layout_hud_expanded);

        tvCompactFps = hudRootView.findViewById(R.id.tv_hud_compact_fps);
        tvCompactTemp = hudRootView.findViewById(R.id.tv_hud_compact_temp);
        btnCloseCompact = hudRootView.findViewById(R.id.btn_hud_close_compact);

        tvExpFps = hudRootView.findViewById(R.id.tv_hud_exp_fps);
        tvExpCpu = hudRootView.findViewById(R.id.tv_hud_exp_cpu);
        tvExpRam = hudRootView.findViewById(R.id.tv_hud_exp_ram);
        tvExpTemp = hudRootView.findViewById(R.id.tv_hud_exp_temp);
        tvExpPower = hudRootView.findViewById(R.id.tv_hud_exp_power);
        btnCloseExp = hudRootView.findViewById(R.id.btn_hud_close_exp);

        isExpanded = prefs.isGameHudExpanded();
        updateDisplayMode();

        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        int savedX = prefs.getGameHudX();
        int savedY = prefs.getGameHudY();

        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;

        int initX = (savedX != -1) ? savedX : 60;
        int initY = (savedY != -1) ? savedY : 200;
        params.x = Math.max(0, Math.min(initX, screenWidth - 100));
        params.y = Math.max(0, Math.min(initY, screenHeight - 80));

        setupTouchListener();

        View.OnClickListener closeListener = v -> stopSelf();
        if (btnCloseCompact != null) btnCloseCompact.setOnClickListener(closeListener);
        if (btnCloseExp != null) btnCloseExp.setOnClickListener(closeListener);

        try {
            windowManager.addView(hudRootView, params);
            prefs.setGameHudEnabled(true);
        } catch (Throwable t) {
            stopSelf();
        }
    }

    private void updateDisplayMode() {
        if (layoutCompact != null && layoutExpanded != null) {
            layoutCompact.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            layoutExpanded.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        }
    }

    private void setupTouchListener() {
        hudRootView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long downTime = 0;

            private boolean isHit(View view, float rawX, float rawY) {
                if (view == null || view.getVisibility() != View.VISIBLE) return false;
                int[] loc = new int[2];
                view.getLocationOnScreen(loc);
                return rawX >= loc[0] && rawX <= (loc[0] + view.getWidth())
                        && rawY >= loc[1] && rawY <= (loc[1] + view.getHeight());
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isHit(btnCloseCompact, event.getRawX(), event.getRawY())
                        || isHit(btnCloseExp, event.getRawX(), event.getRawY())) {
                    return false;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        downTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        try {
                            android.util.DisplayMetrics dmCurrent = getResources().getDisplayMetrics();
                            params.x = Math.max(0, Math.min(params.x, dmCurrent.widthPixels - 40));
                            params.y = Math.max(0, Math.min(params.y, dmCurrent.heightPixels - 40));
                            windowManager.updateViewLayout(hudRootView, params);
                        } catch (Throwable ignored) {
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - downTime;
                        float diffX = Math.abs(event.getRawX() - initialTouchX);
                        float diffY = Math.abs(event.getRawY() - initialTouchY);

                        if (duration < 250 && diffX < 15 && diffY < 15) {
                            // Single Tap -> Toggle compact/expanded
                            isExpanded = !isExpanded;
                            prefs.setGameHudExpanded(isExpanded);
                            updateDisplayMode();
                            try {
                                windowManager.updateViewLayout(hudRootView, params);
                            } catch (Throwable ignored) {
                            }
                        } else {
                            // Drag ended -> Save new position
                            prefs.setGameHudX(params.x);
                            prefs.setGameHudY(params.y);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void registerReceivers() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.registerReceiver(
                        this, screenAndBatteryReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                );
            } else {
                registerReceiver(screenAndBatteryReceiver, filter);
            }
        } catch (Throwable ignored) {
        }
    }

    private void startMetricPolling() {
        lastFpsSampleTime = System.currentTimeMillis();
        frameCount = 0;
        Choreographer.getInstance().postFrameCallback(frameCallback);

        metricsRunnable = new Runnable() {
            @Override
            public void run() {
                if (isScreenOn) {
                    int hwFps = readHardwareFps();
                    if (hwFps > 0) {
                        currentFps = hwFps;
                        updateFpsViews();
                    }
                    measureCpuUsage();
                    updateMetricsViews();
                }
                metricsHandler.postDelayed(this, 1000);
            }
        };
        metricsHandler.post(metricsRunnable);
    }

    private void measureCpuUsage() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/stat")))) {
            String load = reader.readLine();
            if (load != null && load.startsWith("cpu ")) {
                String[] toks = load.split("\\s+");
                if (toks.length >= 8) {
                    long user = Long.parseLong(toks[1]);
                    long nice = Long.parseLong(toks[2]);
                    long system = Long.parseLong(toks[3]);
                    long idle = Long.parseLong(toks[4]);
                    long iowait = Long.parseLong(toks[5]);
                    long irq = Long.parseLong(toks[6]);
                    long softirq = Long.parseLong(toks[7]);

                    long total = user + nice + system + idle + iowait + irq + softirq;
                    long work = user + nice + system + irq + softirq;

                    if (lastCpuTotal > 0 && total > lastCpuTotal) {
                        long diffTotal = total - lastCpuTotal;
                        long diffWork = work - lastCpuWork;
                        currentCpuUsage = (int) Math.max(0, Math.min(100, (diffWork * 100) / diffTotal));
                    }
                    lastCpuTotal = total;
                    lastCpuWork = work;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private int getRamUsagePercent() {
        try {
            if (activityManager != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(mi);
                if (mi.totalMem > 0) {
                    return (int) (((mi.totalMem - mi.availMem) * 100) / mi.totalMem);
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private void updateFpsViews() {
        String fpsText = currentFps + " FPS";
        int fpsColor = Color.parseColor("#00E676"); // Default Neon Green
        if (currentFps < 55) {
            fpsColor = Color.parseColor("#FF5252"); // Red (lag)
        } else if (currentFps < 85 && maxDisplayHz >= 90) {
            fpsColor = Color.parseColor("#FF9800"); // Orange (throttled)
        }

        if (tvCompactFps != null) {
            tvCompactFps.setText(fpsText);
            tvCompactFps.setTextColor(fpsColor);
        }
        if (tvExpFps != null) {
            tvExpFps.setText(fpsText);
            tvExpFps.setTextColor(fpsColor);
        }
    }

    private void updateMetricsViews() {
        String tempText = String.format(Locale.getDefault(), "%.1f°C", currentTemp);

        if (tvCompactTemp != null) {
            tvCompactTemp.setText(tempText);
            if (currentTemp >= 43.0f) {
                tvCompactTemp.setTextColor(Color.parseColor("#FF5252"));
            } else if (currentTemp >= 40.0f) {
                tvCompactTemp.setTextColor(Color.parseColor("#FF9800"));
            } else {
                tvCompactTemp.setTextColor(Color.parseColor("#00E5FF"));
            }
        }

        if (isExpanded) {
            if (tvExpCpu != null) {
                tvExpCpu.setText(currentCpuUsage + "%");
            }
            if (tvExpRam != null) {
                tvExpRam.setText(getRamUsagePercent() + "%");
            }
            if (tvExpTemp != null) {
                tvExpTemp.setText(tempText);
            }
            if (tvExpPower != null) {
                if (currentPowerW > 0.05f) {
                    String pfx = isCharging ? "⚡ +" : "-";
                    tvExpPower.setText(String.format(Locale.getDefault(), "%s%.1fW", pfx, currentPowerW));
                } else {
                    tvExpPower.setText(isCharging ? "Đang sạc" : "-- W");
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (hudRootView == null && Settings.canDrawOverlays(this)) {
            initHudView();
            registerReceivers();
            startMetricPolling();
        }

        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (windowManager != null && hudRootView != null && params != null) {
            try {
                android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                int maxX = Math.max(0, dm.widthPixels - (hudRootView.getWidth() > 0 ? hudRootView.getWidth() : 120));
                int maxY = Math.max(0, dm.heightPixels - (hudRootView.getHeight() > 0 ? hudRootView.getHeight() : 60));
                if (params.x > maxX) params.x = maxX;
                if (params.y > maxY) params.y = maxY;
                windowManager.updateViewLayout(hudRootView, params);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(screenAndBatteryReceiver);
        } catch (Throwable ignored) {
        }

        Choreographer.getInstance().removeFrameCallback(frameCallback);
        if (metricsHandler != null && metricsRunnable != null) {
            metricsHandler.removeCallbacks(metricsRunnable);
        }

        if (windowManager != null && hudRootView != null) {
            try {
                windowManager.removeView(hudRootView);
            } catch (Throwable ignored) {
            }
            hudRootView = null;
        }

        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIFICATION_ID);
            }
        } catch (Throwable ignored) {
        }

        if (prefs != null) {
            prefs.setGameHudEnabled(false);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
