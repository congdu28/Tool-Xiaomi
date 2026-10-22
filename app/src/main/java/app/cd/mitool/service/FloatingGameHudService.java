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
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Choreographer;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

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
    public static final String ACTION_RELOAD_SETTINGS = "app.cd.mitool.RELOAD_GAME_HUD_SETTINGS";

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

    // Compact Mode Views
    private View layoutCompact;
    private ImageView ivCompactIcon;
    private TextView tvCompactFps;
    private TextView tvCompactDot;
    private TextView tvCompactTemp;
    private ImageView btnCloseCompact;

    // Expanded Mode Views
    private View layoutExpanded;
    private View layoutExpHeader;
    private ImageView ivExpIcon;
    private TextView tvExpTitle;
    private TextView tvExpFps;
    private ImageView btnCloseExp;
    private View vExpDivider;

    private View layoutExpRow1;
    private View layoutExpCpu;
    private TextView tvExpCpuLabel;
    private TextView tvExpCpu;
    private View layoutExpRam;
    private TextView tvExpRamLabel;
    private TextView tvExpRam;

    private View layoutExpRow2;
    private View layoutExpTemp;
    private TextView tvExpTempLabel;
    private TextView tvExpTemp;
    private View layoutExpPower;
    private TextView tvExpPowerLabel;
    private TextView tvExpPower;

    private TextView tvExpHint;

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
                    currentFps = (int) Math.round((frameCount * 1000.0) / Math.max(1, diff));
                    if (currentFps > maxDisplayHz + 5) currentFps = maxDisplayHz;
                }
                frameCount = 0;
                lastFpsSampleTime = now;
                updateFpsViews();
            }
            if (isScreenOn) {
                try {
                    Choreographer.getInstance().postFrameCallback(this);
                } catch (Throwable ignored) {
                }
            }
        }
    };

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

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();

            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                isScreenOn = false;
                try {
                    Choreographer.getInstance().removeFrameCallback(frameCallback);
                } catch (Throwable ignored) {
                }
                if (metricsRunnable != null) {
                    metricsHandler.removeCallbacks(metricsRunnable);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                isScreenOn = true;
                lastFpsSampleTime = System.currentTimeMillis();
                frameCount = 0;
                try {
                    Choreographer.getInstance().postFrameCallback(frameCallback);
                } catch (Throwable ignored) {
                }
                if (metricsRunnable != null) {
                    metricsHandler.post(metricsRunnable);
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

        // Detect screen refresh rate safely via DisplayManager
        try {
            DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            if (dm != null) {
                Display d = dm.getDisplay(Display.DEFAULT_DISPLAY);
                if (d != null) {
                    maxDisplayHz = Math.round(d.getRefreshRate());
                    if (maxDisplayHz < 60) maxDisplayHz = 60;
                }
            }
        } catch (Throwable ignored) {
            maxDisplayHz = 120;
        }

        startForegroundNotification();

        if (Settings.canDrawOverlays(this)) {
            initHudView();
            registerScreenStateReceiver();
            startMetricPolling();
        } else {
            stopSelf();
        }
    }

    private void startForegroundNotification() {
        try {
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

            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                } catch (Throwable t) {
                    startForeground(NOTIFICATION_ID, notification);
                }
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Throwable t) {
            try {
                Notification fallbackNotif = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_gamepad)
                        .setContentTitle("🎮 Game HUD FPS")
                        .setContentText("Đang chạy")
                        .build();
                startForeground(NOTIFICATION_ID, fallbackNotif);
            } catch (Throwable ignored) {
            }
        }
    }

    private void initHudView() {
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            ContextThemeWrapper themeWrapper = new ContextThemeWrapper(this, R.style.Theme_ToolXiaomi);
            LayoutInflater inflater = LayoutInflater.from(themeWrapper);
            hudRootView = inflater.inflate(R.layout.layout_floating_game_hud, null);

            // Bind Compact Views
            layoutCompact = hudRootView.findViewById(R.id.layout_hud_compact);
            ivCompactIcon = hudRootView.findViewById(R.id.iv_hud_compact_icon);
            tvCompactFps = hudRootView.findViewById(R.id.tv_hud_compact_fps);
            tvCompactDot = hudRootView.findViewById(R.id.tv_hud_compact_dot);
            tvCompactTemp = hudRootView.findViewById(R.id.tv_hud_compact_temp);
            btnCloseCompact = hudRootView.findViewById(R.id.btn_hud_close_compact);

            // Bind Expanded Views
            layoutExpanded = hudRootView.findViewById(R.id.layout_hud_expanded);
            layoutExpHeader = hudRootView.findViewById(R.id.layout_hud_exp_header);
            ivExpIcon = hudRootView.findViewById(R.id.iv_hud_exp_icon);
            tvExpTitle = hudRootView.findViewById(R.id.tv_hud_exp_title);
            tvExpFps = hudRootView.findViewById(R.id.tv_hud_exp_fps);
            btnCloseExp = hudRootView.findViewById(R.id.btn_hud_close_exp);
            vExpDivider = hudRootView.findViewById(R.id.v_hud_exp_divider);

            layoutExpRow1 = hudRootView.findViewById(R.id.layout_hud_exp_row1);
            layoutExpCpu = hudRootView.findViewById(R.id.layout_hud_exp_cpu);
            tvExpCpuLabel = hudRootView.findViewById(R.id.tv_hud_exp_cpu_label);
            tvExpCpu = hudRootView.findViewById(R.id.tv_hud_exp_cpu);
            layoutExpRam = hudRootView.findViewById(R.id.layout_hud_exp_ram);
            tvExpRamLabel = hudRootView.findViewById(R.id.tv_hud_exp_ram_label);
            tvExpRam = hudRootView.findViewById(R.id.tv_hud_exp_ram);

            layoutExpRow2 = hudRootView.findViewById(R.id.layout_hud_exp_row2);
            layoutExpTemp = hudRootView.findViewById(R.id.layout_hud_exp_temp);
            tvExpTempLabel = hudRootView.findViewById(R.id.tv_hud_exp_temp_label);
            tvExpTemp = hudRootView.findViewById(R.id.tv_hud_exp_temp);
            layoutExpPower = hudRootView.findViewById(R.id.layout_hud_exp_power);
            tvExpPowerLabel = hudRootView.findViewById(R.id.tv_hud_exp_power_label);
            tvExpPower = hudRootView.findViewById(R.id.tv_hud_exp_power);

            tvExpHint = hudRootView.findViewById(R.id.tv_hud_exp_hint);

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
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );

            params.gravity = Gravity.TOP | Gravity.START;
            int savedX = prefs.getGameHudX();
            int savedY = prefs.getGameHudY();

            DisplayMetrics dm = getResources().getDisplayMetrics();
            int screenWidth = dm != null ? dm.widthPixels : 1080;
            int screenHeight = dm != null ? dm.heightPixels : 2400;

            int initX = (savedX != -1) ? savedX : 60;
            int initY = (savedY != -1) ? savedY : 200;
            params.x = Math.max(0, Math.min(initX, screenWidth - 100));
            params.y = Math.max(0, Math.min(initY, screenHeight - 80));

            setupTouchListener();

            View.OnClickListener closeListener = v -> stopSelf();
            if (btnCloseCompact != null) btnCloseCompact.setOnClickListener(closeListener);
            if (btnCloseExp != null) btnCloseExp.setOnClickListener(closeListener);

            applyHudCustomizations();

            windowManager.addView(hudRootView, params);
            prefs.setGameHudEnabled(true);
        } catch (Throwable t) {
            hudRootView = null;
            stopSelf();
        }
    }

    public void applyHudCustomizations() {
        if (hudRootView == null || prefs == null) return;
        try {
            // 1. Background Opacity (Nền HUD độc lập)
            int bgOpacity = prefs.getGameHudBgOpacity(); // 0 to 100
            int bgAlpha = Math.max(0, Math.min(255, Math.round(bgOpacity * 2.55f)));
            int strokeAlpha = Math.max(0, Math.min(255, Math.round(bgOpacity * 1.5f)));

            DisplayMetrics dm = getResources().getDisplayMetrics();
            float density = (dm != null) ? dm.density : 2.5f;

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            gd.setCornerRadius(28 * density);
            gd.setColor(Color.argb(bgAlpha, 0x1E, 0x1E, 0x26));
            if (bgOpacity > 5) {
                gd.setStroke(Math.round(1.5f * density), Color.argb(strokeAlpha, 0x00, 0xE6, 0x76));
            } else {
                gd.setStroke(0, Color.TRANSPARENT);
            }
            hudRootView.setBackground(gd);

            // 2. Text & Icon Opacity (Chữ và Biểu tượng HUD độc lập)
            int textOpacity = prefs.getGameHudTextOpacity(); // 30 to 100
            float textAlpha = Math.max(0.3f, Math.min(1.0f, textOpacity / 100.0f));
            if (layoutCompact != null) layoutCompact.setAlpha(textAlpha);
            if (layoutExpanded != null) layoutExpanded.setAlpha(textAlpha);

            hudRootView.setAlpha(1.0f);
            if (params != null) {
                params.alpha = 1.0f;
            }

            // 2. Metrics visibility
            boolean showFps = prefs.isGameHudShowFps();
            boolean showTemp = prefs.isGameHudShowTemp();
            boolean showCpu = prefs.isGameHudShowCpu();
            boolean showRam = prefs.isGameHudShowRam();
            boolean showPower = prefs.isGameHudShowPower();

            // Compact mode visibility
            if (tvCompactFps != null) tvCompactFps.setVisibility(showFps ? View.VISIBLE : View.GONE);
            if (tvCompactTemp != null) tvCompactTemp.setVisibility(showTemp ? View.VISIBLE : View.GONE);
            if (tvCompactDot != null) tvCompactDot.setVisibility((showFps && showTemp) ? View.VISIBLE : View.GONE);

            // Expanded mode visibility
            if (tvExpFps != null) tvExpFps.setVisibility(showFps ? View.VISIBLE : View.GONE);
            if (layoutExpCpu != null) layoutExpCpu.setVisibility(showCpu ? View.VISIBLE : View.GONE);
            if (layoutExpRam != null) layoutExpRam.setVisibility(showRam ? View.VISIBLE : View.GONE);
            if (layoutExpRow1 != null) layoutExpRow1.setVisibility((showCpu || showRam) ? View.VISIBLE : View.GONE);

            if (layoutExpTemp != null) layoutExpTemp.setVisibility(showTemp ? View.VISIBLE : View.GONE);
            if (layoutExpPower != null) layoutExpPower.setVisibility(showPower ? View.VISIBLE : View.GONE);
            if (layoutExpRow2 != null) layoutExpRow2.setVisibility((showTemp || showPower) ? View.VISIBLE : View.GONE);

            // 3. Scaling / Size (Scale text sizes, paddings, and widths proportionally)
            int scalePercent = prefs.getGameHudScale();
            float factor = Math.max(0.65f, Math.min(1.6f, scalePercent / 100.0f));

            // Scale text sizes
            if (tvCompactFps != null) tvCompactFps.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * factor);
            if (tvCompactTemp != null) tvCompactTemp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f * factor);
            if (tvCompactDot != null) tvCompactDot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * factor);

            if (tvExpTitle != null) tvExpTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * factor);
            if (tvExpFps != null) tvExpFps.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f * factor);
            if (tvExpCpuLabel != null) tvExpCpuLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * factor);
            if (tvExpCpu != null) tvExpCpu.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * factor);
            if (tvExpRamLabel != null) tvExpRamLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * factor);
            if (tvExpRam != null) tvExpRam.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * factor);
            if (tvExpTempLabel != null) tvExpTempLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * factor);
            if (tvExpTemp != null) tvExpTemp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * factor);
            if (tvExpPowerLabel != null) tvExpPowerLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * factor);
            if (tvExpPower != null) tvExpPower.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * factor);
            if (tvExpHint != null) tvExpHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f * factor);

            // Scale capsule padding
            int padH = Math.round(14 * density * factor);
            int padV = Math.round(8 * density * factor);
            hudRootView.setPadding(padH, padV, padH, padV);

            // Scale expanded layout width
            if (layoutExpanded != null && layoutExpanded.getLayoutParams() != null) {
                layoutExpanded.getLayoutParams().width = Math.round(210 * density * factor);
            }

            // Scale icon sizes
            int iconSize = Math.round(16 * density * factor);
            int closeSize = Math.round(20 * density * factor);
            if (ivCompactIcon != null && ivCompactIcon.getLayoutParams() != null) {
                ivCompactIcon.getLayoutParams().width = iconSize;
                ivCompactIcon.getLayoutParams().height = iconSize;
            }
            if (btnCloseCompact != null && btnCloseCompact.getLayoutParams() != null) {
                btnCloseCompact.getLayoutParams().width = closeSize;
                btnCloseCompact.getLayoutParams().height = closeSize;
            }
            if (ivExpIcon != null && ivExpIcon.getLayoutParams() != null) {
                ivExpIcon.getLayoutParams().width = iconSize;
                ivExpIcon.getLayoutParams().height = iconSize;
            }
            if (btnCloseExp != null && btnCloseExp.getLayoutParams() != null) {
                btnCloseExp.getLayoutParams().width = closeSize;
                btnCloseExp.getLayoutParams().height = closeSize;
            }

            if (windowManager != null && params != null) {
                windowManager.updateViewLayout(hudRootView, params);
            }
        } catch (Throwable ignored) {
        }
    }

    private void updateDisplayMode() {
        if (layoutCompact != null && layoutExpanded != null) {
            layoutCompact.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            layoutExpanded.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        }
    }

    private void setupTouchListener() {
        if (hudRootView == null) return;
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
                        initialX = (params != null) ? params.x : 0;
                        initialY = (params != null) ? params.y : 0;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        downTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (params != null && windowManager != null && hudRootView != null) {
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            try {
                                DisplayMetrics dmCurrent = getResources().getDisplayMetrics();
                                if (dmCurrent != null) {
                                    params.x = Math.max(0, Math.min(params.x, dmCurrent.widthPixels - 40));
                                    params.y = Math.max(0, Math.min(params.y, dmCurrent.heightPixels - 40));
                                }
                                windowManager.updateViewLayout(hudRootView, params);
                            } catch (Throwable ignored) {
                            }
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
                            if (windowManager != null && hudRootView != null && params != null) {
                                try {
                                    windowManager.updateViewLayout(hudRootView, params);
                                } catch (Throwable ignored) {
                                }
                            }
                        } else if (params != null) {
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

    private void registerScreenStateReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                        this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
                );
            } else {
                registerReceiver(screenReceiver, filter);
            }
        } catch (Throwable ignored) {
        }
    }

    private void startMetricPolling() {
        lastFpsSampleTime = System.currentTimeMillis();
        frameCount = 0;
        try {
            Choreographer.getInstance().postFrameCallback(frameCallback);
        } catch (Throwable ignored) {
        }

        metricsRunnable = new Runnable() {
            @Override
            public void run() {
                if (isScreenOn) {
                    if (prefs.isGameHudShowFps()) {
                        int hwFps = readHardwareFps();
                        if (hwFps > 0) {
                            currentFps = hwFps;
                            updateFpsViews();
                        }
                    }
                    if (prefs.isGameHudShowCpu()) {
                        measureCpuUsage();
                    }
                    if (prefs.isGameHudShowTemp() || prefs.isGameHudShowPower()) {
                        measureBatteryMetrics();
                    }
                    updateMetricsViews();
                }
                metricsHandler.postDelayed(this, 1000);
            }
        };
        metricsHandler.post(metricsRunnable);
    }

    private void measureBatteryMetrics() {
        try {
            Intent bIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bIntent != null) {
                int tempRaw = bIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                currentTemp = tempRaw > 0 ? (tempRaw / 10.0f) : 36.0f;

                int status = bIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                              status == BatteryManager.BATTERY_STATUS_FULL);

                int voltageMv = bIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000);
                long currentUa = 0;
                if (batteryManager != null) {
                    currentUa = Math.abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
                }
                if (currentUa > 0 && voltageMv > 0) {
                    currentPowerW = (voltageMv / 1000.0f) * (currentUa / 1000000.0f);
                }
            }
        } catch (Throwable ignored) {
        }
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
        if (!prefs.isGameHudShowFps()) return;
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

        if (prefs.isGameHudShowTemp() && tvCompactTemp != null) {
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
            if (prefs.isGameHudShowCpu() && tvExpCpu != null) {
                tvExpCpu.setText(currentCpuUsage + "%");
            }
            if (prefs.isGameHudShowRam() && tvExpRam != null) {
                tvExpRam.setText(getRamUsagePercent() + "%");
            }
            if (prefs.isGameHudShowTemp() && tvExpTemp != null) {
                tvExpTemp.setText(tempText);
            }
            if (prefs.isGameHudShowPower() && tvExpPower != null) {
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
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_RELOAD_SETTINGS.equals(action)) {
                applyHudCustomizations();
                return START_NOT_STICKY;
            }
        }

        if (hudRootView == null && Settings.canDrawOverlays(this)) {
            initHudView();
            registerScreenStateReceiver();
            startMetricPolling();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (windowManager != null && hudRootView != null && params != null) {
            try {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                int maxX = Math.max(0, (dm != null ? dm.widthPixels : 1080) - (hudRootView.getWidth() > 0 ? hudRootView.getWidth() : 120));
                int maxY = Math.max(0, (dm != null ? dm.heightPixels : 2400) - (hudRootView.getHeight() > 0 ? hudRootView.getHeight() : 60));
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
            unregisterReceiver(screenReceiver);
        } catch (Throwable ignored) {
        }

        try {
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        } catch (Throwable ignored) {
        }

        if (metricsHandler != null && metricsRunnable != null) {
            try {
                metricsHandler.removeCallbacks(metricsRunnable);
            } catch (Throwable ignored) {
            }
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
