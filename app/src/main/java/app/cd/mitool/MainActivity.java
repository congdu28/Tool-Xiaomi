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
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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

import android.content.res.ColorStateList;
import android.widget.ProgressBar;
import android.service.quicksettings.TileService;
import java.util.List;
import java.util.Locale;

import app.cd.mitool.service.FloatingClockService;
import app.cd.mitool.service.NotificationClockService;
import app.cd.mitool.service.NotificationTileService;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.GamingFpsHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.RamBoosterHelper;
import app.cd.mitool.util.RefreshRateHelper;
import app.cd.mitool.util.ShortcutHelper;
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
    private MaterialSwitch switchFloatingClock;

    // Floating Clock Sub-Views
    private View layoutFloatingSubOptions;
    private MaterialButton btnPresetTopLeft;
    private MaterialButton btnPresetBeside;
    private MaterialButton btnPresetNotch;
    private MaterialButton btnPresetFree;
    private MaterialSwitch switchClockTransparent;
    private MaterialSwitch switchClockLock;
    private MaterialSwitch switchClockMs;

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

    // Game 120 FPS Unlocker Views
    private TextView tvJoyoseStatus;
    private MaterialButton btnGame120Mode;
    private TextView tvJoyoseCmd;
    private MaterialButton btnCopyDisableJoyose;
    private MaterialButton btnCopyEnableJoyose;
    private MaterialButton btnClearPowerkeeper;
    private MaterialButton btnOpenPerAppHz;
    private MaterialButton btnLaunchAov;

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

        // Default start notification clock service if enabled
        if (prefs.isNotifClockEnabled()) {
            startNotificationClockService();
        }

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
        switchFloatingClock = findViewById(R.id.switch_floating_clock);

        // Floating Sub-Views
        layoutFloatingSubOptions = findViewById(R.id.layout_floating_sub_options);
        btnPresetTopLeft = findViewById(R.id.btn_preset_top_left);
        btnPresetBeside = findViewById(R.id.btn_preset_beside);
        btnPresetNotch = findViewById(R.id.btn_preset_notch);
        btnPresetFree = findViewById(R.id.btn_preset_free);
        switchClockTransparent = findViewById(R.id.switch_clock_transparent);
        switchClockLock = findViewById(R.id.switch_clock_lock);
        switchClockMs = findViewById(R.id.switch_clock_ms);

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

        // Game 120 FPS Views
        tvJoyoseStatus = findViewById(R.id.tv_joyose_status);
        btnGame120Mode = findViewById(R.id.btn_game_120_mode);
        tvJoyoseCmd = findViewById(R.id.tv_joyose_cmd);
        btnCopyDisableJoyose = findViewById(R.id.btn_copy_disable_joyose);
        btnCopyEnableJoyose = findViewById(R.id.btn_copy_enable_joyose);
        btnClearPowerkeeper = findViewById(R.id.btn_clear_powerkeeper);
        btnOpenPerAppHz = findViewById(R.id.btn_open_per_app_hz);
        btnLaunchAov = findViewById(R.id.btn_launch_aov);

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
                    Toast.makeText(MainActivity.this, "Đã bật thông báo trạng thái Mi-Tool!", Toast.LENGTH_SHORT).show();
                } else {
                    stopNotificationClockService();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        TileService.requestListeningState(MainActivity.this, new ComponentName(MainActivity.this, NotificationTileService.class));
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        // Shortcut Buttons for Notification Clock
        MaterialButton btnPinNotifShortcut = findViewById(R.id.btn_pin_notif_shortcut);
        if (btnPinNotifShortcut != null) {
            btnPinNotifShortcut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShortcutHelper.pinNotificationToggleShortcut(MainActivity.this);
                }
            });
        }

        MaterialButton btnAddQsTile = findViewById(R.id.btn_add_qs_tile);
        if (btnAddQsTile != null) {
            btnAddQsTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShortcutHelper.requestAddTileToQuickSettings(MainActivity.this);
                }
            });
        }

        // 2. Floating Overlay Clock
        switchFloatingClock.setChecked(prefs.isClockEnabled());
        if (layoutFloatingSubOptions != null) {
            layoutFloatingSubOptions.setVisibility(prefs.isClockEnabled() ? View.VISIBLE : View.GONE);
        }

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
                    if (layoutFloatingSubOptions != null) {
                        layoutFloatingSubOptions.setVisibility(View.VISIBLE);
                    }
                    startFloatingClockService();
                } else {
                    prefs.setClockEnabled(false);
                    if (layoutFloatingSubOptions != null) {
                        layoutFloatingSubOptions.setVisibility(View.GONE);
                    }
                    stopFloatingClockService();
                }
            }
        });

        if (switchClockTransparent != null) {
            switchClockTransparent.setChecked(prefs.isClockTransparentBg());
            switchClockTransparent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.setClockTransparentBg(isChecked);
                    reloadFloatingClock();
                }
            });
        }

        if (switchClockLock != null) {
            switchClockLock.setChecked(prefs.isClockLockPosition());
            switchClockLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.setClockLockPosition(isChecked);
                    reloadFloatingClock();
                    Toast.makeText(MainActivity.this, isChecked ?
                            "Đã khóa vị trí & bật chạm xuyên thấu!" :
                            "Đã mở khóa, bạn có thể kéo thả di chuyển.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchClockMs != null) {
            switchClockMs.setChecked(prefs.isClockShowMs());
            switchClockMs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.setClockShowMs(isChecked);
                    reloadFloatingClock();
                }
            });
        }

        if (btnPresetTopLeft != null) {
            btnPresetTopLeft.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyFloatingPreset(0);
                }
            });
        }

        if (btnPresetBeside != null) {
            btnPresetBeside.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyFloatingPreset(1);
                }
            });
        }

        if (btnPresetNotch != null) {
            btnPresetNotch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyFloatingPreset(2);
                }
            });
        }

        if (btnPresetFree != null) {
            btnPresetFree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyFloatingPreset(3);
                }
            });
        }

        updateFloatingPresetButtons();
    }

    private void applyFloatingPreset(int index) {
        float density = getResources().getDisplayMetrics().density;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int sbHeight = getStatusBarHeight();
        int centerY = Math.max(0, (sbHeight - (int) (18 * density)) / 2);

        switch (index) {
            case 0: // Top-Left Status Bar Pin
                prefs.setClockStatusBarMode(true);
                prefs.setClockTransparentBg(true);
                prefs.setClockLockPosition(true);
                prefs.setClockX((int) (16 * density));
                prefs.setClockY(centerY);
                if (switchClockTransparent != null) switchClockTransparent.setChecked(true);
                if (switchClockLock != null) switchClockLock.setChecked(true);
                Toast.makeText(this, "Đã đặt ghim góc trái thanh trạng thái (Status Bar Pin)!", Toast.LENGTH_SHORT).show();
                break;
            case 1: // Beside original clock
                prefs.setClockStatusBarMode(false);
                prefs.setClockTransparentBg(true);
                prefs.setClockLockPosition(true);
                prefs.setClockX((int) (75 * density));
                prefs.setClockY(centerY);
                if (switchClockTransparent != null) switchClockTransparent.setChecked(true);
                if (switchClockLock != null) switchClockLock.setChecked(true);
                Toast.makeText(this, "Đã đặt đồng hồ kế bên giờ gốc!", Toast.LENGTH_SHORT).show();
                break;
            case 2: // Next to camera notch
                prefs.setClockStatusBarMode(false);
                prefs.setClockTransparentBg(true);
                prefs.setClockLockPosition(true);
                prefs.setClockX(screenWidth / 2 + (int) (22 * density));
                prefs.setClockY(centerY);
                if (switchClockTransparent != null) switchClockTransparent.setChecked(true);
                if (switchClockLock != null) switchClockLock.setChecked(true);
                Toast.makeText(this, "Đã đặt đồng hồ cạnh nốt ruồi camera!", Toast.LENGTH_SHORT).show();
                break;
            case 3: // Free draggable
                prefs.setClockStatusBarMode(false);
                prefs.setClockLockPosition(false);
                if (switchClockLock != null) switchClockLock.setChecked(false);
                Toast.makeText(this, "Chế độ tự do: Bạn có thể kéo thả đồng hồ đến vị trí mong muốn.", Toast.LENGTH_SHORT).show();
                break;
        }

        updateFloatingPresetButtons();
        if (prefs.isClockEnabled()) {
            reloadFloatingClock();
        } else {
            Toast.makeText(this, "Gạt bật công tắc bên trên để kích hoạt hiển thị.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFloatingPresetButtons() {
        if (btnPresetTopLeft == null || btnPresetBeside == null || btnPresetNotch == null || btnPresetFree == null) return;
        int activeIdx = 3;
        if (prefs.isClockStatusBarMode()) {
            activeIdx = 0;
        } else if (prefs.isClockLockPosition()) {
            float density = getResources().getDisplayMetrics().density;
            int x = prefs.getClockX();
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            if (x >= screenWidth / 3) {
                activeIdx = 2; // Notch
            } else if (x >= (int) (50 * density)) {
                activeIdx = 1; // Beside
            } else {
                activeIdx = 0;
            }
        } else {
            activeIdx = 3; // Free
        }

        int highlightColor = ContextCompat.getColor(this, R.color.mi_orange);
        int normalColor = ContextCompat.getColor(this, R.color.card_highlight);

        btnPresetTopLeft.setBackgroundTintList(ColorStateList.valueOf(activeIdx == 0 ? highlightColor : normalColor));
        btnPresetBeside.setBackgroundTintList(ColorStateList.valueOf(activeIdx == 1 ? highlightColor : normalColor));
        btnPresetNotch.setBackgroundTintList(ColorStateList.valueOf(activeIdx == 2 ? highlightColor : normalColor));
        btnPresetFree.setBackgroundTintList(ColorStateList.valueOf(activeIdx == 3 ? highlightColor : normalColor));
    }

    private void reloadFloatingClock() {
        if (prefs.isClockEnabled()) {
            Intent intent = new Intent(this, FloatingClockService.class);
            intent.setAction(FloatingClockService.ACTION_RELOAD_SETTINGS);
            startService(intent);
        }
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

        // 2. Game 120 FPS Unlocker
        btnGame120Mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean ok = GamingFpsHelper.applyGaming120Mode(MainActivity.this);
                if (ok) {
                    Toast.makeText(MainActivity.this, "⚡ Đã kích hoạt Game Mode 120Hz & Tối Đa Hiệu Năng!", Toast.LENGTH_LONG).show();
                    updateToolsDisplay();
                } else {
                    Toast.makeText(MainActivity.this, "Cần cấp quyền WRITE_SECURE_SETTINGS (Xem Tab 1) để áp dụng đầy đủ.", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnCopyDisableJoyose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GamingFpsHelper.handleDisableJoyose(MainActivity.this);
                updateToolsDisplay();
            }
        });

        btnCopyEnableJoyose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GamingFpsHelper.handleEnableJoyose(MainActivity.this);
                updateToolsDisplay();
            }
        });

        btnClearPowerkeeper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean opened = GamingFpsHelper.openPowerKeeperAppInfo(MainActivity.this);
                if (opened) {
                    Toast.makeText(MainActivity.this, "Bấm 'Xóa dữ liệu' -> 'Xóa tất cả dữ liệu' để reset bảng khóa 89 FPS.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Không tìm thấy Pin và hiệu suất (PowerKeeper).", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnOpenPerAppHz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean opened = GamingFpsHelper.openPerAppRefreshRate(MainActivity.this);
                if (opened) {
                    Toast.makeText(MainActivity.this, "Chọn 'Liên Quân Mobile' trong danh sách và đổi sang '120Hz'.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Không thể mở trang tần số quét ứng dụng.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnLaunchAov.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean launched = GamingFpsHelper.launchLienQuanMobile(MainActivity.this);
                if (!launched) {
                    Toast.makeText(MainActivity.this, "Không tìm thấy Liên Quân Mobile trên máy của bạn.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 3. RAM Booster & Game Turbo
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

    private void applyRefreshRate(final int targetRate) {
        // 1. Check if device hardware actually supports this rate
        List<Integer> supportedRates = RefreshRateHelper.getSupportedRefreshRates(this);
        if (targetRate > 0 && !supportedRates.contains(targetRate)) {
            Toast.makeText(this, "Màn hình thiết bị không hỗ trợ mốc " + targetRate + "Hz! Các mốc hỗ trợ: " + supportedRates, Toast.LENGTH_LONG).show();
            return;
        }

        // 2. Immediate Window Preferred Refresh Rate (Zero delay in foreground)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.preferredRefreshRate = (float) (targetRate > 0 ? targetRate : 0);
                getWindow().setAttributes(lp);
            } catch (Exception ignored) {
            }
        }

        // 3. Save preference & provide instant visual feedback
        prefs.setTargetRefreshRate(targetRate);
        tvCurrentRefreshRate.setText(targetRate == 0 ? "Mặc định (Auto)..." : (targetRate + " Hz (Đang chuyển...)"));
        updateHzButtons();

        // 4. Apply system-wide refresh rate (via Root or ContentResolver)
        boolean success = RefreshRateHelper.setRefreshRate(this, targetRate);
        if (success) {
            String msg = (targetRate == 0) ? "Đã đặt tần số quét Mặc định (Auto)." : ("Đã ép tần số quét " + targetRate + "Hz!");
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

            // 5. Polling checks to sync display text with SurfaceFlinger mode transition
            final Handler h = new Handler(Looper.getMainLooper());
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateHzButtons();
                }
            }, 300);
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateHzButtons();
                }
            }, 800);
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateHzButtons();
                }
            }, 1500);
        } else {
            Toast.makeText(this, "Cần cấp quyền WRITE_SECURE_SETTINGS qua ADB hoặc Root để thay đổi!", Toast.LENGTH_LONG).show();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("ADB Refresh Rate", RefreshRateHelper.getAdbCommand(targetRate)));
                Toast.makeText(this, "Đã sao chép lệnh ADB để ép tần số quét!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateToolsDisplay() {
        updateHzButtons();
        updateGamingFpsDisplay();
        updateRamDisplay();
    }

    private void updateGamingFpsDisplay() {
        if (tvJoyoseStatus == null) return;
        GamingFpsHelper.JoyoseStatus status = GamingFpsHelper.checkJoyoseStatus(this);
        switch (status) {
            case DISABLED_UNLOCKED:
                tvJoyoseStatus.setText("Joyose: ĐÃ TẮT (120 FPS Unlocked)");
                tvJoyoseStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
                break;
            case ACTIVE_LOCKED:
                tvJoyoseStatus.setText("Joyose: ĐANG CHẠY (Khóa 89 FPS)");
                tvJoyoseStatus.setTextColor(ContextCompat.getColor(this, R.color.mi_orange));
                break;
            case NOT_FOUND:
                tvJoyoseStatus.setText("Joyose: Không có / Đã gỡ");
                tvJoyoseStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
                break;
        }

        if (btnLaunchAov != null) {
            boolean installed = GamingFpsHelper.isLienQuanInstalled(this);
            if (installed) {
                btnLaunchAov.setText("⚔️ Mở Liên Quân Mobile (AOV)");
                btnLaunchAov.setEnabled(true);
            } else {
                btnLaunchAov.setText("⚔️ Mở Liên Quân Mobile (Chưa cài đặt)");
            }
        }
    }

    private void updateHzButtons() {
        if (tvCurrentRefreshRate == null) return;
        int activeRate = RefreshRateHelper.getCurrentRefreshRate(this);
        int savedRate = prefs.getTargetRefreshRate();
        tvCurrentRefreshRate.setText(activeRate + " Hz");

        int highlightColor = ContextCompat.getColor(this, R.color.mi_orange);
        int normalColor = ContextCompat.getColor(this, R.color.card_highlight);

        List<Integer> supportedRates = RefreshRateHelper.getSupportedRefreshRates(this);
        boolean supp90 = supportedRates.contains(90);
        boolean supp120 = supportedRates.contains(120);
        boolean supp144 = supportedRates.contains(144);

        btnHzAuto.setBackgroundTintList(ColorStateList.valueOf(savedRate == 0 ? highlightColor : normalColor));
        btnHz60.setBackgroundTintList(ColorStateList.valueOf(savedRate == 60 ? highlightColor : normalColor));

        if (supp90) {
            btnHz90.setEnabled(true);
            btnHz90.setAlpha(1.0f);
            btnHz90.setText("90 Hz");
            btnHz90.setBackgroundTintList(ColorStateList.valueOf(savedRate == 90 ? highlightColor : normalColor));
        } else {
            btnHz90.setEnabled(false);
            btnHz90.setAlpha(0.35f);
            btnHz90.setText("90 Hz (N/A)");
            btnHz90.setBackgroundTintList(ColorStateList.valueOf(normalColor));
        }

        if (supp120) {
            btnHz120.setEnabled(true);
            btnHz120.setAlpha(1.0f);
            btnHz120.setText("120 Hz");
            btnHz120.setBackgroundTintList(ColorStateList.valueOf(savedRate == 120 ? highlightColor : normalColor));
        } else {
            btnHz120.setEnabled(false);
            btnHz120.setAlpha(0.35f);
            btnHz120.setText("120 Hz (N/A)");
            btnHz120.setBackgroundTintList(ColorStateList.valueOf(normalColor));
        }

        if (supp144) {
            btnHz144.setEnabled(true);
            btnHz144.setAlpha(1.0f);
            btnHz144.setText("144 Hz");
            btnHz144.setBackgroundTintList(ColorStateList.valueOf(savedRate == 144 ? highlightColor : normalColor));
        } else {
            btnHz144.setEnabled(false);
            btnHz144.setAlpha(0.35f);
            btnHz144.setText("144 Hz (N/A)");
            btnHz144.setBackgroundTintList(ColorStateList.valueOf(normalColor));
        }
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

    private final Handler batteryTickerHandler = new Handler(Looper.getMainLooper());
    private Runnable batteryTickerRunnable;

    private void startBatteryRealtimeTicker() {
        if (batteryTickerRunnable != null) {
            batteryTickerHandler.removeCallbacks(batteryTickerRunnable);
        }
        batteryTickerRunnable = new Runnable() {
            @Override
            public void run() {
                updateBatteryDisplay();
                batteryTickerHandler.postDelayed(this, 1000); // Live 1-second refresh
            }
        };
        batteryTickerHandler.post(batteryTickerRunnable);
    }

    private void stopBatteryRealtimeTicker() {
        if (batteryTickerRunnable != null) {
            batteryTickerHandler.removeCallbacks(batteryTickerRunnable);
            batteryTickerRunnable = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (switchNotifClock != null) {
            switchNotifClock.setChecked(prefs.isNotifClockEnabled());
        }
        updateFloatingPresetButtons();
        updateBatteryDisplay();
        updateToolsDisplay();
        registerBatteryMonitorReceiver();
        startBatteryRealtimeTicker();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterBatteryMonitorReceiver();
        stopBatteryRealtimeTicker();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBatteryRealtimeTicker();
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

            if (info.isCharging) {
                tvChargeEta.setText("⚡ " + info.formattedEta);
                tvBatteryPower.setText(String.format(Locale.US, "+%.1f W", info.powerWatt));
                tvBatteryPower.setTextColor(ContextCompat.getColor(this, R.color.mi_orange));
            } else {
                tvChargeEta.setText("⏳ " + info.formattedEta);
                tvBatteryPower.setText(String.format(Locale.US, "~%.1f W", info.powerWatt));
                tvBatteryPower.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
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
            intent.setAction(NotificationClockService.ACTION_START);
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
