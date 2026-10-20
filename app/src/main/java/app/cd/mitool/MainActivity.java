package app.cd.mitool;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

import app.cd.mitool.service.FloatingClockService;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.SystemClockHelper;
import app.cd.mitool.util.XiaomiHelper;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_NOTIF_PERM = 101;

    private PreferencesHelper prefs;

    // System Clock Views
    private MaterialSwitch switchSystemClock;
    private TextView tvSecureSettingsStatus;
    private View layoutSecureSettingsGuide;
    private TextView tvAdbCommand;
    private MaterialButton btnCopyAdb;
    private MaterialButton btnCheckSecurePerm;

    // Battery Views
    private TextView tvBatteryPercent;
    private TextView tvBatteryStatus;
    private TextView tvBatteryPlugType;
    private TextView tvChargeEta;
    private TextView tvBatteryPower;
    private TextView tvBatteryTemp;
    private TextView tvBatteryVoltage;
    private MaterialSwitch switchChargingNotif;
    private MaterialSwitch switchAlert80;
    private MaterialSwitch switchAlert100;
    private MaterialSwitch switchAlertTemp;

    // Optional Floating Clock Views
    private MaterialSwitch switchFloatingClock;
    private MaterialSwitch switchClockMs;
    private MaterialSwitch switchClockLock;

    // Xiaomi Shortcuts
    private View btnPermAutostart;
    private View btnPermBatterySaver;
    private MaterialButton btnOpenBatteryInfo;

    private BroadcastReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PreferencesHelper(this);

        initViews();
        setupSystemClockControls();
        setupBatteryControls();
        setupFloatingClockControls();
        setupXiaomiControls();
        requestNotificationPermissionIfNeeded();
    }

    private void initViews() {
        // System Clock
        switchSystemClock = findViewById(R.id.switch_system_clock);
        tvSecureSettingsStatus = findViewById(R.id.tv_secure_settings_status);
        layoutSecureSettingsGuide = findViewById(R.id.layout_secure_settings_guide);
        tvAdbCommand = findViewById(R.id.tv_adb_command);
        btnCopyAdb = findViewById(R.id.btn_copy_adb);
        btnCheckSecurePerm = findViewById(R.id.btn_check_secure_perm);

        // Battery
        tvBatteryPercent = findViewById(R.id.tv_battery_percent);
        tvBatteryStatus = findViewById(R.id.tv_battery_status);
        tvBatteryPlugType = findViewById(R.id.tv_battery_plug_type);
        tvChargeEta = findViewById(R.id.tv_charge_eta);
        tvBatteryPower = findViewById(R.id.tv_battery_power);
        tvBatteryTemp = findViewById(R.id.tv_battery_temp);
        tvBatteryVoltage = findViewById(R.id.tv_battery_voltage);
        switchChargingNotif = findViewById(R.id.switch_charging_notif);
        switchAlert80 = findViewById(R.id.switch_alert_80);
        switchAlert100 = findViewById(R.id.switch_alert_100);
        switchAlertTemp = findViewById(R.id.switch_alert_temp);

        // Optional Floating Clock
        switchFloatingClock = findViewById(R.id.switch_floating_clock);
        switchClockMs = findViewById(R.id.switch_clock_ms);
        switchClockLock = findViewById(R.id.switch_clock_lock);

        // Xiaomi Shortcuts
        btnPermAutostart = findViewById(R.id.btn_perm_autostart);
        btnPermBatterySaver = findViewById(R.id.btn_perm_battery_saver);
        btnOpenBatteryInfo = findViewById(R.id.btn_open_battery_info);
    }

    private void setupSystemClockControls() {
        final String adbCmd = SystemClockHelper.getAdbCommand(this);
        tvAdbCommand.setText(adbCmd);

        btnCopyAdb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("ADB Command", adbCmd);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, "Đã sao chép lệnh ADB vào khay nhớ tạm!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCheckSecurePerm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSystemClockStatus();
                if (SystemClockHelper.hasSecureSettingsPermission(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.permission_not_granted, Toast.LENGTH_SHORT).show();
                }
            }
        });

        switchSystemClock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!SystemClockHelper.hasSecureSettingsPermission(MainActivity.this)) {
                    if (isChecked) {
                        switchSystemClock.setChecked(false);
                        Toast.makeText(MainActivity.this, "Cần cấp quyền Secure Settings qua ADB hoặc Shizuku trước!", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                boolean success = SystemClockHelper.setSystemClockSeconds(MainActivity.this, isChecked);
                if (success) {
                    String msg = isChecked ? "Đã bật hiển thị giây trên thanh trạng thái hệ thống!" : "Đã tắt hiển thị giây.";
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    switchSystemClock.setChecked(!isChecked);
                    Toast.makeText(MainActivity.this, "Không thể ghi cài đặt hệ thống!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateSystemClockStatus() {
        boolean hasPerm = SystemClockHelper.hasSecureSettingsPermission(this);
        if (hasPerm) {
            tvSecureSettingsStatus.setText(R.string.status_granted);
            tvSecureSettingsStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            layoutSecureSettingsGuide.setVisibility(View.GONE);

            // Reflect real system state
            boolean isSecondsOn = SystemClockHelper.isSystemClockSecondsEnabled(this);
            switchSystemClock.setChecked(isSecondsOn);
        } else {
            tvSecureSettingsStatus.setText(R.string.status_missing);
            tvSecureSettingsStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
            layoutSecureSettingsGuide.setVisibility(View.VISIBLE);
            switchSystemClock.setChecked(false);
        }
    }

    private void setupBatteryControls() {
        switchChargingNotif.setChecked(prefs.isChargingHudEnabled());
        switchChargingNotif.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setChargingHudEnabled(isChecked);
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

    private void setupFloatingClockControls() {
        switchFloatingClock.setChecked(prefs.isClockEnabled());
        switchClockMs.setChecked(prefs.isClockShowMs());
        switchClockLock.setChecked(prefs.isClockLockPosition());

        switchFloatingClock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        switchFloatingClock.setChecked(false);
                        Toast.makeText(MainActivity.this, "Cần cấp quyền 'Hiển thị trên ứng dụng khác' cho đồng hồ nổi!", Toast.LENGTH_SHORT).show();
                        XiaomiHelper.openOverlaySettings(MainActivity.this);
                        return;
                    }
                    prefs.setClockEnabled(true);
                    startFloatingClockService();
                } else {
                    prefs.setClockEnabled(false);
                    stopFloatingClockService();
                }
            }
        });

        switchClockMs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setClockShowMs(isChecked);
                notifyFloatingClockSettings();
            }
        });

        switchClockLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setClockLockPosition(isChecked);
                notifyFloatingClockSettings();
            }
        });
    }

    private void setupXiaomiControls() {
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
        updateSystemClockStatus();
        updateBatteryDisplay();
        registerBatteryMonitorReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterBatteryMonitorReceiver();
    }

    private void updateBatteryDisplay() {
        try {
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
        } catch (Exception ignored) {
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
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(batteryReceiver, filter);
            }
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

    private void startFloatingClockService() {
        try {
            Intent intent = new Intent(this, FloatingClockService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception ignored) {
        }
    }

    private void stopFloatingClockService() {
        try {
            Intent intent = new Intent(this, FloatingClockService.class);
            intent.setAction(FloatingClockService.ACTION_STOP);
            startService(intent);
        } catch (Exception ignored) {
        }
    }

    private void notifyFloatingClockSettings() {
        if (prefs.isClockEnabled()) {
            try {
                Intent intent = new Intent(this, FloatingClockService.class);
                intent.setAction(FloatingClockService.ACTION_RELOAD_SETTINGS);
                startService(intent);
            } catch (Exception ignored) {
            }
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
