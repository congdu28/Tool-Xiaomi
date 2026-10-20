package app.cd.mitool;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import android.widget.ProgressBar;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.RamBoosterHelper;
import app.cd.mitool.util.RefreshRateHelper;
import app.cd.mitool.util.SystemClockHelper;
import app.cd.mitool.util.UpdateHelper;
import app.cd.mitool.util.VoiceAlertHelper;
import app.cd.mitool.util.XiaomiHelper;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_NOTIF_PERM = 101;

    private PreferencesHelper prefs;

    // Tabs
    private View tabClockScroll;
    private View tabBatteryScroll;
    private View tabToolsScroll;
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
    private MaterialButton btnGrantWriteSettings;
    private MaterialButton btnOpenMiuiStatusSettings;

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
    private MaterialSwitch switchLockscreenNotif;
    private MaterialSwitch switchLockscreenAmbient;
    private MaterialButton btnPreviewLockscreen;
    private MaterialButton btnOpenLockscreenSettings;

    // Voice Alert Views
    private MaterialSwitch switchVoiceMaster;
    private View layoutVoiceSubOptions;
    private MaterialSwitch switchVoiceConnect;
    private MaterialSwitch switchVoiceDisconnect;
    private MaterialSwitch switchVoice80;
    private MaterialSwitch switchVoice100;
    private MaterialSwitch switchVoiceTemp;
    private MaterialButton btnTestVoice;

    // Battery Cycle & Secret Info
    private TextView tvBatteryCycleCount;
    private MaterialButton btnOpenBatterySecretInfo;

    // Tools Views
    private TextView tvCurrentRefreshRate;
    private MaterialButton btnHzAuto;
    private MaterialButton btnHz60;
    private MaterialButton btnHz90;
    private MaterialButton btnHz120;
    private MaterialButton btnHz144;
    private MaterialButton btnOpenHzSettings;

    private TextView tvRamStats;
    private ProgressBar pbRamUsage;
    private MaterialButton btnBoostRam;
    private MaterialButton btnOpenGameTurbo;

    private MaterialButton btnOpenDeepClean;
    private MaterialButton btnOpenCitTest;
    private MaterialButton btnOpenRadioInfo;
    private MaterialButton btnOpenMsaSettings;
    private MaterialButton btnOpenAdServiceSettings;

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

        // Cancel old legacy notification IDs from previous versions
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(2002);
            nm.cancel(3003);
        }

        setupWindowInsets();
        initViews();
        setupNavigation();
        setupClockOptions();
        setupBatteryControls();
        setupToolsControls();
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
        tabToolsScroll = findViewById(R.id.tab_tools_scroll);
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
        btnGrantWriteSettings = findViewById(R.id.btn_grant_write_settings);
        btnOpenMiuiStatusSettings = findViewById(R.id.btn_open_miui_status_settings);

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
        switchLockscreenNotif = findViewById(R.id.switch_lockscreen_notif);
        switchLockscreenAmbient = findViewById(R.id.switch_lockscreen_ambient);
        btnPreviewLockscreen = findViewById(R.id.btn_preview_lockscreen);
        btnOpenLockscreenSettings = findViewById(R.id.btn_open_lockscreen_settings);

        // Voice Alert Views
        switchVoiceMaster = findViewById(R.id.switch_voice_master);
        layoutVoiceSubOptions = findViewById(R.id.layout_voice_sub_options);
        switchVoiceConnect = findViewById(R.id.switch_voice_connect);
        switchVoiceDisconnect = findViewById(R.id.switch_voice_disconnect);
        switchVoice80 = findViewById(R.id.switch_voice_80);
        switchVoice100 = findViewById(R.id.switch_voice_100);
        switchVoiceTemp = findViewById(R.id.switch_voice_temp);
        btnTestVoice = findViewById(R.id.btn_test_voice);

        // Battery Cycle
        tvBatteryCycleCount = findViewById(R.id.tv_battery_cycle_count);
        btnOpenBatterySecretInfo = findViewById(R.id.btn_open_battery_secret_info);

        // Tools Views
        tvCurrentRefreshRate = findViewById(R.id.tv_current_refresh_rate);
        btnHzAuto = findViewById(R.id.btn_hz_auto);
        btnHz60 = findViewById(R.id.btn_hz_60);
        btnHz90 = findViewById(R.id.btn_hz_90);
        btnHz120 = findViewById(R.id.btn_hz_120);
        btnHz144 = findViewById(R.id.btn_hz_144);
        btnOpenHzSettings = findViewById(R.id.btn_open_hz_settings);

        tvRamStats = findViewById(R.id.tv_ram_stats);
        pbRamUsage = findViewById(R.id.pb_ram_usage);
        btnBoostRam = findViewById(R.id.btn_boost_ram);
        btnOpenGameTurbo = findViewById(R.id.btn_open_game_turbo);

        btnOpenDeepClean = findViewById(R.id.btn_open_deep_clean);
        btnOpenCitTest = findViewById(R.id.btn_open_cit_test);
        btnOpenRadioInfo = findViewById(R.id.btn_open_radio_info);
        btnOpenMsaSettings = findViewById(R.id.btn_open_msa_settings);
        btnOpenAdServiceSettings = findViewById(R.id.btn_open_ad_service_settings);

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
                    tabToolsScroll.setVisibility(View.GONE);
                    tabSettingsScroll.setVisibility(View.GONE);
                    return true;
                } else if (id == R.id.nav_battery) {
                    tabClockScroll.setVisibility(View.GONE);
                    tabBatteryScroll.setVisibility(View.VISIBLE);
                    tabToolsScroll.setVisibility(View.GONE);
                    tabSettingsScroll.setVisibility(View.GONE);
                    updateBatteryDisplay();
                    return true;
                } else if (id == R.id.nav_tools) {
                    tabClockScroll.setVisibility(View.GONE);
                    tabBatteryScroll.setVisibility(View.GONE);
                    tabToolsScroll.setVisibility(View.VISIBLE);
                    tabSettingsScroll.setVisibility(View.GONE);
                    updateToolsDisplay();
                    return true;
                } else if (id == R.id.nav_settings) {
                    tabClockScroll.setVisibility(View.GONE);
                    tabBatteryScroll.setVisibility(View.GONE);
                    tabToolsScroll.setVisibility(View.GONE);
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

        btnGrantWriteSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean opened = SystemClockHelper.openWriteSettingsScreen(MainActivity.this);
                if (opened) {
                    Toast.makeText(MainActivity.this, "Hãy gạt bật 'Cho phép sửa đổi cài đặt hệ thống'", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Đã cấp quyền hoặc không cần cấp.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnOpenMiuiStatusSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean opened = SystemClockHelper.openXiaomiStatusBarSettings(MainActivity.this);
                if (!opened) {
                    Toast.makeText(MainActivity.this, "Không tìm thấy trang cài đặt thanh trạng thái.", Toast.LENGTH_SHORT).show();
                }
            }
        });

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
                if (SystemClockHelper.hasSecureSettingsPermission(MainActivity.this)
                        || SystemClockHelper.canWriteSystemSettings(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "Đã nhận diện quyền hệ thống thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Chưa nhận được quyền. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        switchSystemClock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean hasSecure = SystemClockHelper.hasSecureSettingsPermission(MainActivity.this);
                boolean canWriteSys = SystemClockHelper.canWriteSystemSettings(MainActivity.this);

                if (!hasSecure && !canWriteSys) {
                    if (isChecked) {
                        switchSystemClock.setChecked(false);
                        Toast.makeText(MainActivity.this, "Vui lòng bấm 'Cấp quyền trực tiếp' hoặc cấp qua Shizuku/ADB trước!", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                boolean success = SystemClockHelper.setSystemClockSeconds(MainActivity.this, isChecked);
                if (success) {
                    String msg = isChecked ? "Đã gửi lệnh bật giây vào hệ thống!" : "Đã gửi lệnh tắt giây.";
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    switchSystemClock.setChecked(!isChecked);
                    Toast.makeText(MainActivity.this, "Không thể ghi cài đặt (ROM yêu cầu cấp quyền qua ADB/Shizuku).", Toast.LENGTH_LONG).show();
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
        boolean hasSecure = SystemClockHelper.hasSecureSettingsPermission(this);
        boolean canWriteSys = SystemClockHelper.canWriteSystemSettings(this);

        if (hasSecure) {
            tvSecureSettingsStatus.setText("Đã cấp Secure Settings (ADB)");
            tvSecureSettingsStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            boolean isSecondsOn = SystemClockHelper.isSystemClockSecondsEnabled(this);
            switchSystemClock.setChecked(isSecondsOn);
        } else if (canWriteSys) {
            tvSecureSettingsStatus.setText("Đã cấp Sửa cài đặt hệ thống (Zero-ADB)");
            tvSecureSettingsStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            boolean isSecondsOn = SystemClockHelper.isSystemClockSecondsEnabled(this);
            switchSystemClock.setChecked(isSecondsOn);
        } else {
            tvSecureSettingsStatus.setText(R.string.status_missing);
            tvSecureSettingsStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
            switchSystemClock.setChecked(false);
        }
        // Always keep ADB guide and copy command visible for convenient access
        layoutSecureSettingsGuide.setVisibility(View.VISIBLE);
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

        // Lockscreen Charging Controls
        switchLockscreenNotif.setChecked(prefs.isLockscreenNotifEnabled());
        switchLockscreenNotif.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setLockscreenNotifEnabled(isChecked);
                Toast.makeText(MainActivity.this, isChecked ?
                        "Đã bật ghim thông báo sạc ngoài màn hình khóa!" :
                        "Đã tắt thông báo ngoài màn hình khóa.", Toast.LENGTH_SHORT).show();
            }
        });

        switchLockscreenAmbient.setChecked(prefs.isLockscreenAmbientEnabled());
        switchLockscreenAmbient.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setLockscreenAmbientEnabled(isChecked);
                Toast.makeText(MainActivity.this, isChecked ?
                        "Đã bật màn hình sạc khóa AMOLED!" :
                        "Đã tắt màn hình sạc khóa.", Toast.LENGTH_SHORT).show();
            }
        });

        btnPreviewLockscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent previewIntent = new Intent(MainActivity.this, LockscreenChargingActivity.class);
                startActivity(previewIntent);
            }
        });

        btnOpenLockscreenSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openXiaomiLockscreenSettings();
            }
        });

        // Voice Smart Alert Controls
        switchVoiceMaster.setChecked(prefs.isVoiceAlertMasterEnabled());
        layoutVoiceSubOptions.setVisibility(prefs.isVoiceAlertMasterEnabled() ? View.VISIBLE : View.GONE);
        switchVoiceMaster.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setVoiceAlertMasterEnabled(isChecked);
                layoutVoiceSubOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if (isChecked) {
                    VoiceAlertHelper.init(MainActivity.this);
                    Toast.makeText(MainActivity.this, "Đã bật âm báo sạc thông minh Tiếng Việt!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        switchVoiceConnect.setChecked(prefs.isVoiceAlertConnectEnabled());
        switchVoiceConnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setVoiceAlertConnectEnabled(isChecked);
            }
        });

        switchVoiceDisconnect.setChecked(prefs.isVoiceAlertDisconnectEnabled());
        switchVoiceDisconnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setVoiceAlertDisconnectEnabled(isChecked);
            }
        });

        switchVoice80.setChecked(prefs.isVoiceAlert80Enabled());
        switchVoice80.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setVoiceAlert80Enabled(isChecked);
            }
        });

        switchVoice100.setChecked(prefs.isVoiceAlert100Enabled());
        switchVoice100.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setVoiceAlert100Enabled(isChecked);
            }
        });

        switchVoiceTemp.setChecked(prefs.isVoiceAlertTempEnabled());
        switchVoiceTemp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setVoiceAlertTempEnabled(isChecked);
            }
        });

        btnTestVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VoiceAlertHelper.testVoice(MainActivity.this);
                Toast.makeText(MainActivity.this, "Đang phát âm báo thử nghiệm...", Toast.LENGTH_SHORT).show();
            }
        });

        // Battery Cycle Secret Info
        btnOpenBatterySecretInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean opened = XiaomiHelper.openBatterySecretInfo(MainActivity.this);
                if (!opened) {
                    Toast.makeText(MainActivity.this, "Bấm *#*#6485#*#* trong trình gọi điện để xem MF_02 (Chu kỳ sạc).", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupToolsControls() {
        // 1. Refresh Rate Controls
        btnHzAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyRefreshRate(0);
            }
        });
        btnHz60.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyRefreshRate(60);
            }
        });
        btnHz90.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyRefreshRate(90);
            }
        });
        btnHz120.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyRefreshRate(120);
            }
        });
        btnHz144.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyRefreshRate(144);
            }
        });
        btnOpenHzSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean opened = RefreshRateHelper.openRefreshRateSettings(MainActivity.this);
                if (!opened) {
                    Toast.makeText(MainActivity.this, "Không tìm thấy trang cài đặt màn hình.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 2. RAM Booster & Game Turbo
        btnBoostRam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnBoostRam.setEnabled(false);
                btnBoostRam.setText("Đang dọn dẹp...");
                RamBoosterHelper.boostRam(MainActivity.this, new RamBoosterHelper.BoostCallback() {
                    @Override
                    public void onBoostComplete(long freedMb, RamBoosterHelper.MemoryStats newStats) {
                        btnBoostRam.setEnabled(true);
                        btnBoostRam.setText(R.string.ram_boost_btn);
                        updateRamDisplay();
                        Toast.makeText(MainActivity.this, "Đã giải phóng ~" + freedMb + " MB RAM, máy mượt hơn!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        btnOpenGameTurbo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean opened = XiaomiHelper.openGameTurbo(MainActivity.this);
                if (!opened) {
                    Toast.makeText(MainActivity.this, "Không tìm thấy Không gian trò chơi (Game Turbo) trên máy.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 3. Deep Clean
        btnOpenDeepClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean opened = XiaomiHelper.openDeepClean(MainActivity.this);
                if (!opened) {
                    Toast.makeText(MainActivity.this, "Không thể mở module Dọn dẹp Xiaomi.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 4. CIT Hardware Test & Testing Radio
        btnOpenCitTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean opened = XiaomiHelper.openCITHardwareTest(MainActivity.this);
                if (!opened) {
                    Toast.makeText(MainActivity.this, "Bấm *#*#6484#*#* trong trình gọi điện để mở CIT.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnOpenRadioInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XiaomiHelper.openHiddenBatteryInfo(MainActivity.this);
            }
        });

        // 5. Disable MSA & System Ads
        btnOpenMsaSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean opened = XiaomiHelper.openAuthorizationAndRevocation(MainActivity.this);
                if (opened) {
                    Toast.makeText(MainActivity.this, "Hãy tìm dòng 'msa' và gạt TẮT để loại bỏ quảng cáo ngầm!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Vào Cài đặt -> Mật khẩu & bảo mật -> Ủy quyền và hủy bỏ -> Tắt msa.", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnOpenAdServiceSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean opened = XiaomiHelper.openAdServicesSettings(MainActivity.this);
                if (opened) {
                    Toast.makeText(MainActivity.this, "Hãy gạt TẮT 'Dịch vụ quảng cáo cá nhân hóa'.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Vào Cài đặt -> Quyền riêng tư -> Dịch vụ quảng cáo -> Tắt.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void applyRefreshRate(int targetRate) {
        boolean success = RefreshRateHelper.setRefreshRate(this, targetRate);
        if (success) {
            prefs.setTargetRefreshRate(targetRate);
            String msg = (targetRate == 0) ? "Đã đặt tần số quét Mặc định (Auto)." : ("Đã ép tần số quét " + targetRate + "Hz!");
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            updateHzButtons();
        } else {
            Toast.makeText(this, "Cần cấp quyền WRITE_SECURE_SETTINGS qua ADB hoặc Shizuku để thay đổi!", Toast.LENGTH_LONG).show();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("ADB Refresh Rate", RefreshRateHelper.getAdbCommand(targetRate)));
                Toast.makeText(this, "Đã sao chép lệnh ADB để ép tần số quét!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateToolsDisplay() {
        updateHzButtons();
        updateRamDisplay();
    }

    private void updateHzButtons() {
        int activeRate = RefreshRateHelper.getCurrentRefreshRate(this);
        int savedRate = prefs.getTargetRefreshRate();
        tvCurrentRefreshRate.setText(activeRate + " Hz");

        int highlightColor = ContextCompat.getColor(this, R.color.mi_orange);
        int normalColor = ContextCompat.getColor(this, R.color.card_highlight);

        btnHzAuto.setBackgroundColor(savedRate == 0 ? highlightColor : normalColor);
        btnHz60.setBackgroundColor(savedRate == 60 ? highlightColor : normalColor);
        btnHz90.setBackgroundColor(savedRate == 90 ? highlightColor : normalColor);
        btnHz120.setBackgroundColor(savedRate == 120 ? highlightColor : normalColor);
        btnHz144.setBackgroundColor(savedRate == 144 ? highlightColor : normalColor);
    }

    private void updateRamDisplay() {
        RamBoosterHelper.MemoryStats stats = RamBoosterHelper.getMemoryStats(this);
        tvRamStats.setText(String.format(Locale.getDefault(), "Đã dùng: %.1f GB / %.1f GB (%d%%)",
                stats.usedMb / 1024.0f, stats.totalMb / 1024.0f, stats.percentUsed));
        pbRamUsage.setProgress(stats.percentUsed);
    }

    private void openXiaomiLockscreenSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent channelIntent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                channelIntent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                channelIntent.putExtra(Settings.EXTRA_CHANNEL_ID, NotificationClockService.CHANNEL_ID);
                startActivity(channelIntent);
            } else {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
            }
            Toast.makeText(this, "Hãy chọn 'Trên màn hình khóa' -> 'Hiển thị tất cả nội dung'", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
                Toast.makeText(this, "Hãy chọn 'Trên màn hình khóa' -> 'Hiển thị tất cả nội dung'", Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception ignored) {
                }
            }
        }
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
        updateToolsDisplay();
        registerBatteryMonitorReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterBatteryMonitorReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VoiceAlertHelper.destroy();
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

            int cycles = XiaomiHelper.getBatteryCycleCount(this);
            if (cycles > 0) {
                tvBatteryCycleCount.setText("Số chu kỳ sạc: " + cycles + " lần sạc");
            } else {
                tvBatteryCycleCount.setText("Số chu kỳ sạc: Xem chi tiết tại mã 6485 (mục MF_02)");
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
