package app.cd.mitool;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import app.cd.mitool.util.BatteryHelper;

public class LockscreenChargingActivity extends Activity {

    private TextView tvClock;
    private TextView tvDate;
    private TextView tvPercent;
    private TextView tvPlugType;
    private TextView tvEta;
    private TextView tvFullTime;
    private TextView tvPower;
    private TextView tvVoltCurr;
    private TextView tvTemp;
    private ProgressBar progressBattery;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable clockTicker;
    private Runnable autoDismissRunnable;
    private BroadcastReceiver batteryReceiver;

    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configureLockscreenDisplay();
        setContentView(R.layout.activity_lockscreen_charging);

        initViews();
        setupClock();
        setupDismissGesture();
        registerBatteryReceiver();
        updateBatteryData();
        setupAutoDismiss();
    }

    private void configureLockscreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            Window window = getWindow();
            window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }

        // Fullscreen immersive mode
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void initViews() {
        tvClock = findViewById(R.id.tv_lock_clock);
        tvDate = findViewById(R.id.tv_lock_date);
        tvPercent = findViewById(R.id.tv_lock_percent);
        tvPlugType = findViewById(R.id.tv_lock_plug_type);
        tvEta = findViewById(R.id.tv_lock_eta);
        tvFullTime = findViewById(R.id.tv_lock_full_time);
        tvPower = findViewById(R.id.tv_lock_power);
        tvVoltCurr = findViewById(R.id.tv_lock_volt_curr);
        tvTemp = findViewById(R.id.tv_lock_temp);
        progressBattery = findViewById(R.id.progress_lock_battery);

        timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        dateFormat = new SimpleDateFormat("EEEE, dd/MM", new Locale("vi", "VN"));
    }

    private void setupClock() {
        clockTicker = new Runnable() {
            @Override
            public void run() {
                Date now = new Date();
                if (tvClock != null) tvClock.setText(timeFormat.format(now));
                if (tvDate != null) tvDate.setText(dateFormat.format(now));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(clockTicker);
    }

    private void setupDismissGesture() {
        View root = findViewById(R.id.root_lockscreen);
        if (root != null) {
            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    private void setupAutoDismiss() {
        // Automatically finish after 15 seconds to allow phone to sleep and conserve battery
        autoDismissRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    finish();
                }
            }
        };
        handler.postDelayed(autoDismissRunnable, 15000);
    }

    private void updateBatteryData() {
        BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(this);
        tvPercent.setText(info.level + "%");
        progressBattery.setProgress(info.level);
        tvPlugType.setText(info.isCharging ? "⚡ " + info.plugTypeName : "Không sạc");
        tvEta.setText(info.formattedEta);

        if (info.isCharging && info.estimatedMinutesToFull > 0) {
            long fullTimeMillis = System.currentTimeMillis() + (info.estimatedMinutesToFull * 60 * 1000L);
            String fullTimeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(fullTimeMillis));
            tvFullTime.setText("Dự kiến đầy lúc: " + fullTimeStr);
            tvFullTime.setVisibility(View.VISIBLE);
        } else {
            tvFullTime.setVisibility(View.GONE);
        }

        if (info.powerWatt > 0) {
            tvPower.setText(String.format(Locale.US, "%.1fW", info.powerWatt));
        } else {
            tvPower.setText(R.string.copy_message_lockscreenchargingactivity_c7c3317);
        }

        float volt = info.voltageMv > 0 ? (info.voltageMv / 1000.0f) : 0.0f;
        tvVoltCurr.setText(String.format(Locale.US, "%.1fV • %dmA", volt, info.currentMa));
        tvTemp.setText(String.format(Locale.US, "%.1f°C", info.temperature));
    }

    private void registerBatteryReceiver() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                    finish();
                } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                    updateBatteryData();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(batteryReceiver, filter);
            }
        } catch (Exception ignored) {
            try {
                registerReceiver(batteryReceiver, filter);
            } catch (Exception ignored2) {
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            if (clockTicker != null) handler.removeCallbacks(clockTicker);
            if (autoDismissRunnable != null) handler.removeCallbacks(autoDismissRunnable);
        }
        if (batteryReceiver != null) {
            try {
                unregisterReceiver(batteryReceiver);
            } catch (Exception ignored) {
            }
        }
    }
}
