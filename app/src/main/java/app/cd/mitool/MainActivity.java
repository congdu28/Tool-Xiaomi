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
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationBarView;

import java.util.Locale;

import app.cd.mitool.service.FloatingClockService;
import app.cd.mitool.service.NotificationClockService;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.SystemClockHelper;
import app.cd.mitool.util.UpdateHelper;
import app.cd.mitool.util.XiaomiHelper;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_NOTIF_PERM = 101;

    private PreferencesHelper prefs;

    // Tabs
    private View tabClockScroll;
    private View tabBatteryScroll;
    private View tabSettingsScroll;
    private BottomNavigationView bottomNav;

    // Clock Views
    private MaterialSwitch switchNotifClock;
    private MaterialSwitch switchSystemClock;
    private MaterialSwitch switchFloatingClock;
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
    private MaterialSwitch switchAlert80;
    private MaterialSwitch switchAlert100;
    private MaterialSwitch switchAlertTemp;

    // Settings Views
    private TextView tvSettingsCurrentVersion;
    private MaterialButton btnCheckUpdateNow;
    private View btnPermAutostart;
    private View btnPermBatterySaver;
    private MaterialButton btnOpenBatteryInfo;

    private BroadcastReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PreferencesHelper(this);

        setupWindowInsets();
        initViews();
        setupNavigation();
        setupClockOptions();
        setupBatteryControls();
        setupSettingsControls();
        requestNotificationPermissionIfNeeded();

        // Auto check for updates from GitHub in background
        UpdateHelper.checkForUpdate(this, false);
    }

    private void setupWindowInsets() {
        View root = findViewById(R.id.main_root_container);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, new OnApplyWindowInsetsListener() {
                @NonNull
                @Override
                public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
                    return insets;
                }
            });
        }
    }

    private void initViews() {
        tabClockScroll = findViewById(R.id.tab_clock_scroll);
        tabBatteryScroll = findViewById(R.id.tab_battery_scroll);
        tabSettingsScroll = findViewById(R.id.tab_settings_scroll);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Clock Views
        switchNotifClock = findViewById(R.id.switch_notif_clock);
        switchSystemClock = findViewById(R.id.switch_system_clock);
        switchFloatingClock = findViewById(R.id.switch_floating_clock);
        tvSecureSettingsStatus = findViewById(R.id.tv_secure_settings_status);
        layoutSecureSettingsGuide = findViewById(R.id.layout_secure_settings_guide);
        tvAdbCommand = findViewById(R.id.tv_adb_command);
        btnCopyAdb = findViewById(R.id.btn_copy_adb);
        btnCheckSecurePerm = findViewById(R.id.btn_check_secure_perm);

        // Battery Views
        tvBatteryPercent = findViewById(R.id.tv_battery_percent);
        tvBatteryStatus = findViewById(R.id.tv_battery_status);
        tvBatteryPlugType = findViewById(R.id.tv_battery_plug_type);
        tvChargeEta = findViewById(R.id.tv_charge_eta);
        tvBatteryPower = findViewById(R.id.tv_battery_power);
        tvBatteryTemp = findViewById(R.id.tv_battery_temp);
        tvBatteryVoltage = findViewById(R.id.tv_battery_voltage);
        switchAlert80 = findViewById(R.id.switch_alert_80);
        switchAlert100 = findViewById(R.id.switch_alert_100);
        switchAlertTemp = findViewById(R.id.switch_alert_temp);

        // Settings Views
        tvSettingsCurrentVersion = findViewById(R.id.tv_settings_current_version);
        btnCheckUpdateNow = findViewById(R.id.btn_check_update_now);
        btnPermAutostart = findViewById(R.id.btn_perm_autostart);
        btnPermBatterySaver = findViewById(R.id.btn_perm_battery_saver);
        btnOpenBatteryInfo = findViewById(R.id.btn_open_battery_info);

        tvSettingsCurrentVersion.setText("Phiên bản đang cài: v" + UpdateHelper.getCurrentVersion());
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_clock) {
                    tabClockScroll.setVisibility(View.VISIBLE);
                    tabBatteryScroll.setVisibility(View.GONE);
                    tabSettingsScroll.setVisibility(View.GONE);
                    return true;
                } else if (id == R.id.nav_battery) {
                    tabClockScroll.setVisibility(View.GONE);
                    tabBatteryScroll.setVisibility(View.VISIBLE);
                    tabSettingsScroll.setVisibility(View.GONE);
                    updateBatteryDisplay();
                    return true;
                } else if (id == R.id.nav_settings) {
                    tabClockScroll.setVisibility(View.GONE);
                    tabBatteryScroll.setVisibility(View.GONE);
                    tabSettingsScroll.setVisibility(View.VISIBLE);
                    return true;
                }
                return false;
            }
        });
    }

    private void setupClockOptions() {
        // 1. Notification Clock (NO ADB required)
        switchNotifClock.setChecked(prefs.isNotifClockEnabled());
        switchNotifClock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setNotifClockEnabled(isChecked);
                if (isChecked) {
                    startNotificationClockService();
                    Toast.makeText(MainActivity.this, "Đã bật đồng hồ giây trên thanh thông báo!", Toast.LENGTH_SHORT).show();
                } else {
                    stopNotificationClockService();
                }
            }
        });

        // 2. System Status Bar Clock
        final String adbCmd = SystemClockHelper.getAdbCommand(this);
        tvAdbCommand.setText(adbCmd);

        btnCopyAdb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("ADB Command", adbCmd);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, "Đã sao chép lệnh ADB!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCheckSecurePerm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSystemClockStatus();
                if (SystemClockHelper.hasSecureSettingsPermission(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "Đã nhận diện quyền hệ thống thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Chưa nhận được quyền. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        switchSystemClock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!SystemClockHelper.hasSecureSettingsPermission(MainActivity.this)) {
                    if (isChecked) {
                        switchSystemClock.setChecked(false);
                        Toast.makeText(MainActivity.this, "Cần cấp quyền qua Shizuku hoặc ADB trước!", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                boolean success = SystemClockHelper.setSystemClockSeconds(MainActivity.this, isChecked);
                if (success) {
                    String msg = isChecked ? "Đã bật giây trên thanh trạng thái góc trên!" : "Đã tắt giây thanh trạng thái.";
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    switchSystemClock.setChecked(!isChecked);
                    Toast.makeText(MainActivity.this, "Không thể ghi cài đặt hệ thống.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 3. Floating Overlay Clock
        switchFloatingClock.setChecked(prefs.isClockEnabled());
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
    }

    private void updateSystemClockStatus() {
        boolean hasPerm = SystemClockHelper.hasSecureSettingsPermission(this);
        if (hasPerm) {
            tvSecureSettingsStatus.setText(R.string.status_granted);
            tvSecureSettingsStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            layoutSecureSettingsGuide.setVisibility(View.GONE);

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

    private void setupSettingsControls() {
        btnCheckUpdateNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Đang kiểm tra bản cập nhật mới từ GitHub...", Toast.LENGTH_SHORT).show();
                UpdateHelper.checkForUpdate(MainActivity.this, true);
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

            tvBatteryPlugType.setText("Nguồn: " + info.plugTypeName);
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

    private void startNotificationClockService() {
        try {
            Intent intent = new Intent(this, NotificationClockService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception ignored) {
        }
    }

    private void stopNotificationClockService() {
        try {
            Intent intent = new Intent(this, NotificationClockService.class);
            intent.setAction(NotificationClockService.ACTION_STOP);
            startService(intent);
        } catch (Exception ignored) {
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
