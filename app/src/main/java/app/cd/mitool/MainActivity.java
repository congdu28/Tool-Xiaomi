package app.cd.mitool;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

import app.cd.mitool.service.BatteryMonitorService;
import app.cd.mitool.service.FloatingClockService;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.XiaomiHelper;
import app.cd.mitool.view.ChargingHudView;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_NOTIF_PERM = 101;

    private PreferencesHelper prefs;

    // Clock Views
    private MaterialSwitch switchClock;
    private MaterialSwitch switchClockMs;
    private MaterialSwitch switchClockBattery;
    private MaterialSwitch switchClockLock;
    private RadioGroup rgClockSize;
    private RadioGroup rgClockColor;

    // Battery Views
    private TextView tvBatteryPercent;
    private TextView tvBatteryStatus;
    private TextView tvBatteryPlugType;
    private TextView tvChargeEta;
    private TextView tvBatteryPower;
    private TextView tvBatteryTemp;
    private TextView tvBatteryVoltage;
    private MaterialSwitch switchChargingHud;
    private MaterialButton btnTestHud;

    private MaterialSwitch switchAlert80;
    private MaterialSwitch switchAlert100;
    private MaterialSwitch switchAlertTemp;

    // Xiaomi Guide Views
    private TextView tvStatusOverlay;
    private View btnPermOverlay;
    private View btnPermAutostart;
    private View btnPermBatterySaver;
    private View btnPermLockscreen;
    private MaterialButton btnOpenBatteryInfo;

    private BroadcastReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PreferencesHelper(this);

        initViews();
        setupClockControls();
        setupBatteryControls();
        setupXiaomiControls();
        requestNotificationPermissionIfNeeded();

        // Ensure background battery monitor is running
        startBatteryService();
    }

    private void initViews() {
        switchClock = findViewById(R.id.switch_floating_clock);
        switchClockMs = findViewById(R.id.switch_clock_ms);
        switchClockBattery = findViewById(R.id.switch_clock_battery);
        switchClockLock = findViewById(R.id.switch_clock_lock);
        rgClockSize = findViewById(R.id.rg_clock_size);
        rgClockColor = findViewById(R.id.rg_clock_color);

        tvBatteryPercent = findViewById(R.id.tv_battery_percent);
        tvBatteryStatus = findViewById(R.id.tv_battery_status);
        tvBatteryPlugType = findViewById(R.id.tv_battery_plug_type);
        tvChargeEta = findViewById(R.id.tv_charge_eta);
        tvBatteryPower = findViewById(R.id.tv_battery_power);
        tvBatteryTemp = findViewById(R.id.tv_battery_temp);
        tvBatteryVoltage = findViewById(R.id.tv_battery_voltage);
        switchChargingHud = findViewById(R.id.switch_charging_hud);
        btnTestHud = findViewById(R.id.btn_test_hud);

        switchAlert80 = findViewById(R.id.switch_alert_80);
        switchAlert100 = findViewById(R.id.switch_alert_100);
        switchAlertTemp = findViewById(R.id.switch_alert_temp);

        tvStatusOverlay = findViewById(R.id.tv_status_overlay);
        btnPermOverlay = findViewById(R.id.btn_perm_overlay);
        btnPermAutostart = findViewById(R.id.btn_perm_autostart);
        btnPermBatterySaver = findViewById(R.id.btn_perm_battery_saver);
        btnPermLockscreen = findViewById(R.id.btn_perm_lockscreen);
        btnOpenBatteryInfo = findViewById(R.id.btn_open_battery_info);
    }

    private void setupClockControls() {
        switchClock.setChecked(prefs.isClockEnabled());
        switchClockMs.setChecked(prefs.isClockShowMs());
        switchClockBattery.setChecked(prefs.isClockShowBattery());
        switchClockLock.setChecked(prefs.isClockLockPosition());

        int size = prefs.getClockSize();
        if (size == 0) ((RadioButton) findViewById(R.id.rb_size_small)).setChecked(true);
        else if (size == 2) ((RadioButton) findViewById(R.id.rb_size_large)).setChecked(true);
        else ((RadioButton) findViewById(R.id.rb_size_medium)).setChecked(true);

        int color = prefs.getClockColor();
        if (color == 1) ((RadioButton) findViewById(R.id.rb_color_green)).setChecked(true);
        else if (color == 2) ((RadioButton) findViewById(R.id.rb_color_orange)).setChecked(true);
        else ((RadioButton) findViewById(R.id.rb_color_white)).setChecked(true);

        switchClock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        switchClock.setChecked(false);
                        Toast.makeText(MainActivity.this, "Vui lòng cấp quyền 'Hiển thị trên ứng dụng khác' trước!", Toast.LENGTH_LONG).show();
                        XiaomiHelper.openOverlaySettings(MainActivity.this);
                        return;
                    }
                    prefs.setClockEnabled(true);
                    startClockService();
                } else {
                    prefs.setClockEnabled(false);
                    stopClockService();
                }
            }
        });

        switchClockMs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setClockShowMs(isChecked);
                notifyClockSettingsChanged();
            }
        });

        switchClockBattery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setClockShowBattery(isChecked);
                notifyClockSettingsChanged();
            }
        });

        switchClockLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setClockLockPosition(isChecked);
                notifyClockSettingsChanged();
            }
        });

        rgClockSize.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_size_small) prefs.setClockSize(0);
                else if (checkedId == R.id.rb_size_large) prefs.setClockSize(2);
                else prefs.setClockSize(1);
                notifyClockSettingsChanged();
            }
        });

        rgClockColor.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_color_green) prefs.setClockColor(1);
                else if (checkedId == R.id.rb_color_orange) prefs.setClockColor(2);
                else prefs.setClockColor(0);
                notifyClockSettingsChanged();
            }
        });
    }

    private void setupBatteryControls() {
        switchChargingHud.setChecked(prefs.isChargingHudEnabled());
        switchChargingHud.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !Settings.canDrawOverlays(MainActivity.this)) {
                    switchChargingHud.setChecked(false);
                    Toast.makeText(MainActivity.this, "Cần cấp quyền 'Hiển thị trên ứng dụng khác' để hiện HUD sạc!", Toast.LENGTH_LONG).show();
                    XiaomiHelper.openOverlaySettings(MainActivity.this);
                    return;
                }
                prefs.setChargingHudEnabled(isChecked);
            }
        });

        btnTestHud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Settings.canDrawOverlays(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "Vui lòng cấp quyền 'Hiển thị trên ứng dụng khác' trước!", Toast.LENGTH_SHORT).show();
                    XiaomiHelper.openOverlaySettings(MainActivity.this);
                    return;
                }
                BatteryHelper.BatteryInfo demoInfo = BatteryHelper.getBatteryInfo(MainActivity.this);
                demoInfo.isCharging = true;
                demoInfo.plugTypeName = "Củ sạc Turbo (67W)";
                if (demoInfo.estimatedMinutesToFull <= 0) {
                    demoInfo.estimatedMinutesToFull = 25;
                    demoInfo.formattedEta = "Còn ~25 phút (Đầy lúc 21:15)";
                }
                demoInfo.powerWatt = 31.4f;
                ChargingHudView.show(MainActivity.this, demoInfo);
            }
        });

        switchAlert80.setChecked(prefs.isAlert80Enabled());
        switchAlert80.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setAlert80Enabled(isChecked);
            }
        });

        switchAlert100.setChecked(prefs.isAlert100Enabled());
        switchAlert100.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setAlert100Enabled(isChecked);
            }
        });

        switchAlertTemp.setChecked(prefs.isAlertTempEnabled());
        switchAlertTemp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setAlertTempEnabled(isChecked);
            }
        });
    }

    private void setupXiaomiControls() {
        btnPermOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XiaomiHelper.openOverlaySettings(MainActivity.this);
            }
        });

        btnPermAutostart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XiaomiHelper.openAutostartSettings(MainActivity.this);
            }
        });

        btnPermBatterySaver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XiaomiHelper.openBatterySaverSettings(MainActivity.this);
            }
        });

        btnPermLockscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XiaomiHelper.openLockScreenPermSettings(MainActivity.this);
            }
        });

        btnOpenBatteryInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XiaomiHelper.openHiddenBatteryInfo(MainActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateOverlayPermissionStatus();
        updateBatteryDisplay();
        registerBatteryMonitorReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterBatteryMonitorReceiver();
    }

    private void updateOverlayPermissionStatus() {
        boolean hasOverlay = Settings.canDrawOverlays(this);
        if (hasOverlay) {
            tvStatusOverlay.setText(R.string.status_granted);
            tvStatusOverlay.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
        } else {
            tvStatusOverlay.setText(R.string.status_missing);
            tvStatusOverlay.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
        }
    }

    private void updateBatteryDisplay() {
        BatteryHelper.BatteryInfo info = BatteryHelper.getBatteryInfo(this);

        tvBatteryPercent.setText(info.level + "%");

        if (info.isFull) {
            tvBatteryStatus.setText(R.string.battery_status_full);
            tvBatteryStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
        } else if (info.isCharging) {
            tvBatteryStatus.setText(R.string.battery_status_charging);
            tvBatteryStatus.setTextColor(ContextCompat.getColor(this, R.color.mi_orange));
        } else {
            tvBatteryStatus.setText(R.string.battery_status_discharging);
            tvBatteryStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }

        tvBatteryPlugType.setText("Nguồn sạc: " + info.plugTypeName);
        tvChargeEta.setText("⚡ " + info.formattedEta);

        if (info.powerWatt > 0) {
            tvBatteryPower.setText(String.format(Locale.US, "%.1f W", info.powerWatt));
        } else {
            tvBatteryPower.setText("-- W");
        }

        tvBatteryTemp.setText(String.format(Locale.US, "%.1f °C", info.temperature));
        if (info.temperature >= 42.0f) {
            tvBatteryTemp.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
        } else if (info.temperature >= 38.0f) {
            tvBatteryTemp.setTextColor(ContextCompat.getColor(this, R.color.neon_yellow));
        } else {
            tvBatteryTemp.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
        }

        if (info.voltageMv > 0) {
            tvBatteryVoltage.setText(String.format(Locale.US, "%.2f V", info.voltageMv / 1000.0f));
        } else {
            tvBatteryVoltage.setText("-- V");
        }
    }

    private void registerBatteryMonitorReceiver() {
        if (batteryReceiver == null) {
            batteryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateBatteryDisplay();
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(BatteryMonitorService.ACTION_BATTERY_UPDATED);
            registerReceiver(batteryReceiver, filter);
        }
    }

    private void unregisterBatteryMonitorReceiver() {
        if (batteryReceiver != null) {
            try {
                unregisterReceiver(batteryReceiver);
            } catch (Exception ignored) {
            }
            batteryReceiver = null;
        }
    }

    private void startClockService() {
        Intent intent = new Intent(this, FloatingClockService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopClockService() {
        Intent intent = new Intent(this, FloatingClockService.class);
        intent.setAction(FloatingClockService.ACTION_STOP);
        startService(intent);
    }

    private void notifyClockSettingsChanged() {
        if (prefs.isClockEnabled()) {
            Intent intent = new Intent(this, FloatingClockService.class);
            intent.setAction(FloatingClockService.ACTION_RELOAD_SETTINGS);
            startService(intent);
        }
    }

    private void startBatteryService() {
        Intent intent = new Intent(this, BatteryMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF_PERM);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
