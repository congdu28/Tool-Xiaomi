package app.cd.mitool;

import android.Manifest;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Icon;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.cd.mitool.service.FloatingClockService;
import app.cd.mitool.service.FloatingClockTileService;
import app.cd.mitool.service.FloatingGameHudService;
import app.cd.mitool.service.GameHudTileService;
import app.cd.mitool.service.NotificationClockService;
import app.cd.mitool.service.RefreshRateTileService;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.BatteryUsageHelper;
import app.cd.mitool.util.DebloatHelper;
import app.cd.mitool.util.DeviceInfoHelper;
import app.cd.mitool.util.GamingFpsHelper;
import app.cd.mitool.util.NotificationFixHelper;
import app.cd.mitool.util.PreferencesHelper;
import android.widget.CheckBox;
import app.cd.mitool.util.PrivateDnsHelper;
import app.cd.mitool.util.RamBoosterHelper;
import app.cd.mitool.util.RefreshRateHelper;
import app.cd.mitool.util.ShizukuHelper;
import app.cd.mitool.util.BatteryHistoryHelper;
import app.cd.mitool.util.ChargingTesterHelper;
import app.cd.mitool.util.ShortcutHelper;
import app.cd.mitool.util.SotHelper;
import app.cd.mitool.util.SystemClockHelper;
import app.cd.mitool.util.UpdateHelper;
import app.cd.mitool.util.VoiceAlertHelper;
import app.cd.mitool.util.VoLteHelper;
import app.cd.mitool.util.XiaomiHelper;
import app.cd.mitool.view.BatteryDrainChartView;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_NOTIF_PERM = 101;
    private static final int REQ_PICK_RINGTONE = 7701;

    private PreferencesHelper prefs;

    // Navigation & Tabs
    private View tabDeviceScroll;
    private View tabToolsScroll;
    private View tabBatteryScroll;
    private View tabSettingsScroll;
    private BottomNavigationView bottomNav;

    // Device Specs Views (Tab 1: Home)
    private TextView tvDeviceModelHeader;
    private TextView tvDeviceOsHeader;
    private TextView tvDeviceShizukuBadge;
    private TextView tvSpecModel;
    private TextView tvSpecCodename;
    private TextView tvSpecBrand;
    private TextView tvSpecOs;
    private TextView tvSpecAndroid;
    private TextView tvSpecSecurity;
    private TextView tvSpecCpuVendor;
    private TextView tvSpecCpuModel;
    private TextView tvSpecCpuPlatform;
    private TextView tvSpecCpuCores;
    private TextView tvSpecCpuArch;
    private TextView tvSpecDisplaySize;
    private TextView tvSpecDisplayRes;
    private TextView tvSpecDisplayHz;
    private TextView tvSpecDisplayDpi;
    private TextView tvSpecDisplayTech;
    private TextView tvSpecRamText;
    private ProgressBar pbSpecRam;
    private TextView tvSpecZramText;
    private ProgressBar pbSpecZram;
    private TextView tvSpecStorageText;
    private TextView tvSpecStorageSub;
    private ProgressBar pbSpecStorage;
    private TextView tvSpecBatteryPercent;
    private TextView tvSpecBatteryCapacity;
    private TextView tvSpecBatteryCycles;
    private TextView tvSpecBatteryTempVoltage;
    private TextView tvSpecBatteryHealth;
    private MaterialButton btnQuickGameMode;
    private MaterialButton btnQuickBoostRam;
    private MaterialButton btnQuickDns;
    private MaterialButton btnQuickDebloat;

    // Grouped Feature Cards (Tab 2: Tools)
    private View cardToolHz;
    private View cardToolGameFps;
    private View cardToolDebloater;
    private View cardToolPrivateDns;
    private View cardToolVolte;
    private View cardToolRam;
    private View cardToolClock;
    private View cardToolBatteryAlert;
    private View cardToolMiuiUtils;
    private View cardToolNotifFix;

    private TextView tvToolHzSub;
    private TextView tvToolGameFpsSub;
    private TextView tvToolDebloaterSub;
    private TextView tvToolPrivateDnsSub;
    private TextView tvToolVolteSub;
    private TextView tvToolRamSub;
    private TextView tvToolClockSub;
    private TextView tvToolBatteryAlertSub;
    private TextView tvToolMiuiUtilsSub;
    private TextView tvToolNotifFixSub;

    // Tab 3: Battery Monitoring Views
    private TextView tvBatteryPercent;
    private TextView tvBatteryPlugType;
    private TextView tvChargeEta;
    private TextView tvBatteryPower;
    private TextView tvBatteryTemp;
    private TextView tvBatteryVoltage;
    private TextView tvBatteryDesignCapacity;
    private TextView tvBatteryActualCapacity;
    private TextView tvBatteryCycleCount;
    private MaterialButton btnOpenBatterySecretInfo;

    // Charging Tester & Protocol Views
    private TextView tvChargingProtocolBadge;
    private TextView tvChargingCurrentLabel;
    private TextView tvChargingCurrentVal;
    private MaterialButton btnTestCable;
    private ProgressBar pbCableTest;
    private LinearLayout layoutCableTestResult;
    private TextView tvCableRatingStars;
    private TextView tvCableRatingDesc;
    private TextView tvCableRatingMetrics;

    // SOT & Battery Drain Monitor Views
    private TextView tvSotDuration;
    private TextView tvSotTotalUsed;
    private TextView tvSotUnplugInfo;
    private TextView tvSotEstimateScreenOn;
    private TextView tvSotEstimateMixed;
    private TextView tvSotDrainRate;
    private BatteryDrainChartView chartBatteryDrain;
    private TextView tvDrainChartSummary;
    private MaterialButton btnResetSot;
    private MaterialButton btnRefreshDrainApps;
    private MaterialButton btnDrainFilterUnplug;
    private MaterialButton btnDrainFilterToday;
    private View layoutDrainPermNeeded;
    private MaterialButton btnDrainGrantShizuku;
    private MaterialButton btnDrainOpenSettings;
    private LinearLayout layoutDrainAppsContainer;
    private TextView tvDrainAppsEmpty;
    private TextView tvDrainAppsSubtitle;
    private int currentDrainFilter = BatteryUsageHelper.FILTER_SINCE_UNPLUG;

    // Tab 4: Settings Views
    private TextView tvThemeCurrentBadge;
    private MaterialButton btnThemeSystem;
    private MaterialButton btnThemeLight;
    private MaterialButton btnThemeDark;
    private TextView tvSettingsCurrentVersion;
    private TextView tvSettingsUpdateBadge;
    private TextView tvSettingsLastCheck;
    private MaterialButton btnCheckUpdateNow;
    private View btnPermAutostart;
    private View btnPermBatterySaver;
    private TextView tvShizukuStatus;
    private TextView tvShizukuBadge;
    private MaterialButton btnShizukuGrantAuto;
    private MaterialButton btnShizukuOpen;

    private BroadcastReceiver batteryReceiver;
    private final Handler batteryTickerHandler = new Handler(Looper.getMainLooper());
    private Runnable batteryTickerRunnable;

    private final Shizuku.OnRequestPermissionResultListener shizukuPermissionListener =
            new Shizuku.OnRequestPermissionResultListener() {
                @Override
                public void onRequestPermissionResult(int requestCode, int grantResult) {
                    if (requestCode == ShizukuHelper.SHIZUKU_REQ_CODE) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(MainActivity.this, "Đã cấp quyền Shizuku thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Từ chối quyền Shizuku", Toast.LENGTH_SHORT).show();
                        }
                        updateShizukuDisplay();
                        updateDeviceSpecsDisplay();
                        updateToolsCardSummaries();
                    }
                }
            };

    private final Shizuku.OnBinderReceivedListener shizukuBinderReceivedListener =
            new Shizuku.OnBinderReceivedListener() {
                @Override
                public void onBinderReceived() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateShizukuDisplay();
                            updateDeviceSpecsDisplay();
                            updateToolsCardSummaries();
                        }
                    });
                }
            };

    private final Shizuku.OnBinderDeadListener shizukuBinderDeadListener =
            new Shizuku.OnBinderDeadListener() {
                @Override
                public void onBinderDead() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateShizukuDisplay();
                            updateDeviceSpecsDisplay();
                            updateToolsCardSummaries();
                        }
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = new PreferencesHelper(this);
        applyAppTheme(prefs.getAppThemeMode());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Cancel old legacy notification IDs from previous versions
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(2002);
            nm.cancel(3003);
        }

        setupWindowInsets();
        initViews();
        setupNavigation();
        int activeTabId = savedInstanceState != null ? savedInstanceState.getInt("active_tab_id", R.id.nav_home) : R.id.nav_home;
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(activeTabId);
        }
        setupDeviceSpecs();
        setupToolsCards();
        SotHelper.ensureInitialized(this);
        setupBatteryControls();
        setupSettingsControls();
        setupShizukuControls();

        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener);
            Shizuku.addBinderReceivedListenerSticky(shizukuBinderReceivedListener);
            Shizuku.addBinderDeadListener(shizukuBinderDeadListener);
        } catch (Throwable ignored) {
        }
        requestNotificationPermissionIfNeeded();

        // Default start notification clock service if enabled
        if (prefs.isNotifClockEnabled()) {
            startNotificationClockService();
        }

        // Auto check for updates from GitHub in background
        UpdateHelper.checkForUpdate(this, false);

        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        if (intent.getIntExtra("extra_tab_index", -1) == 1) {
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_battery);
            }
        }
        if (intent.getBooleanExtra(UpdateHelper.EXTRA_OPEN_UPDATE_DIALOG, false)) {
            String tag = intent.getStringExtra(UpdateHelper.EXTRA_UPDATE_TAG);
            String body = intent.getStringExtra(UpdateHelper.EXTRA_UPDATE_BODY);
            String url = intent.getStringExtra(UpdateHelper.EXTRA_UPDATE_URL);
            if (tag != null && url != null) {
                UpdateHelper.showUpdateBottomSheet(this, tag, body, url);
            }
        }
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
        tabDeviceScroll = findViewById(R.id.tab_device_scroll);
        tabToolsScroll = findViewById(R.id.tab_tools_scroll);
        tabBatteryScroll = findViewById(R.id.tab_battery_scroll);
        tabSettingsScroll = findViewById(R.id.tab_settings_scroll);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Device Specs Views
        tvDeviceModelHeader = findViewById(R.id.tv_device_model_header);
        tvDeviceOsHeader = findViewById(R.id.tv_device_os_header);
        tvDeviceShizukuBadge = findViewById(R.id.tv_device_shizuku_badge);
        tvSpecModel = findViewById(R.id.tv_spec_model);
        tvSpecCodename = findViewById(R.id.tv_spec_codename);
        tvSpecBrand = findViewById(R.id.tv_spec_brand);
        tvSpecOs = findViewById(R.id.tv_spec_os);
        tvSpecAndroid = findViewById(R.id.tv_spec_android);
        tvSpecSecurity = findViewById(R.id.tv_spec_security);
        tvSpecCpuVendor = findViewById(R.id.tv_spec_cpu_vendor);
        tvSpecCpuModel = findViewById(R.id.tv_spec_cpu_model);
        tvSpecCpuPlatform = findViewById(R.id.tv_spec_cpu_platform);
        tvSpecCpuCores = findViewById(R.id.tv_spec_cpu_cores);
        tvSpecCpuArch = findViewById(R.id.tv_spec_cpu_arch);
        tvSpecDisplaySize = findViewById(R.id.tv_spec_display_size);
        tvSpecDisplayRes = findViewById(R.id.tv_spec_display_res);
        tvSpecDisplayHz = findViewById(R.id.tv_spec_display_hz);
        tvSpecDisplayDpi = findViewById(R.id.tv_spec_display_dpi);
        tvSpecDisplayTech = findViewById(R.id.tv_spec_display_tech);
        tvSpecRamText = findViewById(R.id.tv_spec_ram_text);
        pbSpecRam = findViewById(R.id.pb_spec_ram);
        tvSpecZramText = findViewById(R.id.tv_spec_zram_text);
        pbSpecZram = findViewById(R.id.pb_spec_zram);
        tvSpecStorageText = findViewById(R.id.tv_spec_storage_text);
        tvSpecStorageSub = findViewById(R.id.tv_spec_storage_sub);
        pbSpecStorage = findViewById(R.id.pb_spec_storage);
        tvSpecBatteryPercent = findViewById(R.id.tv_spec_battery_percent);
        tvSpecBatteryCapacity = findViewById(R.id.tv_spec_battery_capacity);
        tvSpecBatteryCycles = findViewById(R.id.tv_spec_battery_cycles);
        tvSpecBatteryTempVoltage = findViewById(R.id.tv_spec_battery_temp_voltage);
        tvSpecBatteryHealth = findViewById(R.id.tv_spec_battery_health);

        btnQuickGameMode = findViewById(R.id.btn_quick_game_mode);
        btnQuickBoostRam = findViewById(R.id.btn_quick_boost_ram);
        btnQuickDns = findViewById(R.id.btn_quick_dns);
        btnQuickDebloat = findViewById(R.id.btn_quick_debloat);

        // Grouped Feature Cards
        cardToolHz = findViewById(R.id.card_tool_hz);
        cardToolGameFps = findViewById(R.id.card_tool_game_fps);
        cardToolDebloater = findViewById(R.id.card_tool_debloater);
        cardToolPrivateDns = findViewById(R.id.card_tool_private_dns);
        cardToolVolte = findViewById(R.id.card_tool_volte);
        cardToolRam = findViewById(R.id.card_tool_ram);
        cardToolClock = findViewById(R.id.card_tool_clock);
        cardToolBatteryAlert = findViewById(R.id.card_tool_battery_alert);
        cardToolMiuiUtils = findViewById(R.id.card_tool_miui_utils);
        cardToolNotifFix = findViewById(R.id.card_tool_notif_fix);

        tvToolHzSub = findViewById(R.id.tv_tool_hz_sub);
        tvToolGameFpsSub = findViewById(R.id.tv_tool_game_fps_sub);
        tvToolDebloaterSub = findViewById(R.id.tv_tool_debloater_sub);
        tvToolPrivateDnsSub = findViewById(R.id.tv_tool_private_dns_sub);
        tvToolVolteSub = findViewById(R.id.tv_tool_volte_sub);
        tvToolRamSub = findViewById(R.id.tv_tool_ram_sub);
        tvToolClockSub = findViewById(R.id.tv_tool_clock_sub);
        tvToolBatteryAlertSub = findViewById(R.id.tv_tool_battery_alert_sub);
        tvToolMiuiUtilsSub = findViewById(R.id.tv_tool_miui_utils_sub);
        tvToolNotifFixSub = findViewById(R.id.tv_tool_notif_fix_sub);

        // Battery Views
        tvBatteryPercent = findViewById(R.id.tv_battery_percent);
        tvBatteryPlugType = findViewById(R.id.tv_battery_plug_type);
        tvChargeEta = findViewById(R.id.tv_charge_eta);
        tvBatteryPower = findViewById(R.id.tv_battery_power);
        tvBatteryTemp = findViewById(R.id.tv_battery_temp);
        tvBatteryVoltage = findViewById(R.id.tv_battery_voltage);
        tvBatteryDesignCapacity = findViewById(R.id.tv_battery_design_capacity);
        tvBatteryActualCapacity = findViewById(R.id.tv_battery_actual_capacity);
        tvBatteryCycleCount = findViewById(R.id.tv_battery_cycle_count);
        btnOpenBatterySecretInfo = findViewById(R.id.btn_open_battery_secret_info);

        // Charging Tester & Protocol Views
        tvChargingProtocolBadge = findViewById(R.id.tv_charging_protocol_badge);
        tvChargingCurrentLabel = findViewById(R.id.tv_charging_current_label);
        tvChargingCurrentVal = findViewById(R.id.tv_charging_current_val);
        btnTestCable = findViewById(R.id.btn_test_cable);
        pbCableTest = findViewById(R.id.pb_cable_test);
        layoutCableTestResult = findViewById(R.id.layout_cable_test_result);
        tvCableRatingStars = findViewById(R.id.tv_cable_rating_stars);
        tvCableRatingDesc = findViewById(R.id.tv_cable_rating_desc);
        tvCableRatingMetrics = findViewById(R.id.tv_cable_rating_metrics);

        // SOT & Battery Drain Views
        tvSotDuration = findViewById(R.id.tv_sot_duration);
        tvSotTotalUsed = findViewById(R.id.tv_sot_total_used);
        tvSotUnplugInfo = findViewById(R.id.tv_sot_unplug_info);
        tvSotEstimateScreenOn = findViewById(R.id.tv_sot_estimate_screen_on);
        tvSotEstimateMixed = findViewById(R.id.tv_sot_estimate_mixed);
        tvSotDrainRate = findViewById(R.id.tv_sot_drain_rate);
        chartBatteryDrain = findViewById(R.id.chart_battery_drain);
        tvDrainChartSummary = findViewById(R.id.tv_drain_chart_summary);
        btnResetSot = findViewById(R.id.btn_reset_sot);
        btnRefreshDrainApps = findViewById(R.id.btn_refresh_drain_apps);
        btnDrainFilterUnplug = findViewById(R.id.btn_drain_filter_unplug);
        btnDrainFilterToday = findViewById(R.id.btn_drain_filter_today);
        layoutDrainPermNeeded = findViewById(R.id.layout_drain_perm_needed);
        btnDrainGrantShizuku = findViewById(R.id.btn_drain_grant_shizuku);
        btnDrainOpenSettings = findViewById(R.id.btn_drain_open_settings);
        layoutDrainAppsContainer = findViewById(R.id.layout_drain_apps_container);
        tvDrainAppsEmpty = findViewById(R.id.tv_drain_apps_empty);
        tvDrainAppsSubtitle = findViewById(R.id.tv_drain_apps_subtitle);

        // Settings Views
        tvThemeCurrentBadge = findViewById(R.id.tv_theme_current_badge);
        btnThemeSystem = findViewById(R.id.btn_theme_system);
        btnThemeLight = findViewById(R.id.btn_theme_light);
        btnThemeDark = findViewById(R.id.btn_theme_dark);
        tvSettingsCurrentVersion = findViewById(R.id.tv_settings_current_version);
        tvSettingsUpdateBadge = findViewById(R.id.tv_settings_update_badge);
        tvSettingsLastCheck = findViewById(R.id.tv_settings_last_check);
        btnCheckUpdateNow = findViewById(R.id.btn_check_update_now);
        btnPermAutostart = findViewById(R.id.btn_perm_autostart);
        btnPermBatterySaver = findViewById(R.id.btn_perm_battery_saver);

        // Shizuku Views
        tvShizukuStatus = findViewById(R.id.tv_shizuku_status);
        tvShizukuBadge = findViewById(R.id.tv_shizuku_badge);
        btnShizukuGrantAuto = findViewById(R.id.btn_shizuku_grant_auto);
        btnShizukuOpen = findViewById(R.id.btn_shizuku_open);

        updateSettingsVersionDisplay();
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    tabDeviceScroll.setVisibility(View.VISIBLE);
                    tabToolsScroll.setVisibility(View.GONE);
                    tabBatteryScroll.setVisibility(View.GONE);
                    tabSettingsScroll.setVisibility(View.GONE);
                    updateDeviceSpecsDisplay();
                    return true;
                } else if (id == R.id.nav_tools) {
                    tabDeviceScroll.setVisibility(View.GONE);
                    tabToolsScroll.setVisibility(View.VISIBLE);
                    tabBatteryScroll.setVisibility(View.GONE);
                    tabSettingsScroll.setVisibility(View.GONE);
                    updateToolsCardSummaries();
                    return true;
                } else if (id == R.id.nav_battery) {
                    tabDeviceScroll.setVisibility(View.GONE);
                    tabToolsScroll.setVisibility(View.GONE);
                    tabBatteryScroll.setVisibility(View.VISIBLE);
                    tabSettingsScroll.setVisibility(View.GONE);
                    updateBatteryDisplay();
                    return true;
                } else if (id == R.id.nav_settings) {
                    tabDeviceScroll.setVisibility(View.GONE);
                    tabToolsScroll.setVisibility(View.GONE);
                    tabBatteryScroll.setVisibility(View.GONE);
                    tabSettingsScroll.setVisibility(View.VISIBLE);
                    updateShizukuDisplay();
                    return true;
                }
                return false;
            }
        });
    }

    // =========================================================================
    // TAB 1: DEVICE SPECS & DASHBOARD
    // =========================================================================

    private void setupDeviceSpecs() {
        if (btnQuickGameMode != null) {
            btnQuickGameMode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean ok = GamingFpsHelper.applyGaming120Mode(MainActivity.this);
                    if (ok) {
                        Toast.makeText(MainActivity.this, "⚡ Đã kích hoạt Game Mode 120Hz & Tối Đa Hiệu Năng!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Cần cấp quyền WRITE_SECURE_SETTINGS (Xem Tab Cài Đặt)", Toast.LENGTH_SHORT).show();
                    }
                    updateDeviceSpecsDisplay();
                    updateToolsCardSummaries();
                }
            });
        }

        if (btnQuickBoostRam != null) {
            btnQuickBoostRam.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    btnQuickBoostRam.setEnabled(false);
                    btnQuickBoostRam.setText("Đang dọn...");
                    RamBoosterHelper.boostRam(MainActivity.this, new RamBoosterHelper.BoostCallback() {
                        @Override
                        public void onBoostComplete(long freedMb, RamBoosterHelper.MemoryStats newStats) {
                            btnQuickBoostRam.setEnabled(true);
                            btnQuickBoostRam.setText("🚀 Dọn Dẹp RAM");
                            updateDeviceSpecsDisplay();
                            updateToolsCardSummaries();
                            Toast.makeText(MainActivity.this, "Đã giải phóng ~" + freedMb + " MB RAM!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }

        if (btnQuickDns != null) {
            btnQuickDns.setOnClickListener(v -> showPrivateDnsBottomSheet());
        }

        if (btnQuickDebloat != null) {
            btnQuickDebloat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleDebloatFreezeRecommended();
                }
            });
        }

        updateDeviceSpecsDisplay();
    }

    private void updateDeviceSpecsDisplay() {
        if (tvDeviceModelHeader == null) return;
        try {
            // Model & OS
            String model = DeviceInfoHelper.getDeviceMarketName();
            String codename = DeviceInfoHelper.getDeviceCodename();
            String os = DeviceInfoHelper.getOsVersionDisplay();
            String androidVer = DeviceInfoHelper.getAndroidVersionDisplay();
            String security = DeviceInfoHelper.getSecurityPatch();

            tvDeviceModelHeader.setText(model);
            tvDeviceOsHeader.setText(os + " • " + androidVer);

            tvSpecModel.setText(model);
            tvSpecCodename.setText(codename);
            if (tvSpecBrand != null) {
                String brand = Build.MANUFACTURER;
                tvSpecBrand.setText(brand != null ? brand.toUpperCase(Locale.getDefault()) : "Xiaomi");
            }
            tvSpecOs.setText(os);
            tvSpecAndroid.setText(androidVer);
            tvSpecSecurity.setText(security);

            // Shizuku badge
            if (ShizukuHelper.hasShizukuPermission()) {
                tvDeviceShizukuBadge.setText("🟢 Shizuku: Đã cấp");
                tvDeviceShizukuBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            } else if (ShizukuHelper.isShizukuRunning()) {
                tvDeviceShizukuBadge.setText("🟡 Shizuku: Chờ cấp");
                tvDeviceShizukuBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_yellow));
            } else {
                tvDeviceShizukuBadge.setText("🔴 Shizuku: Chưa chạy");
                tvDeviceShizukuBadge.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            }

            // CPU / SoC
            if (tvSpecCpuVendor != null) {
                tvSpecCpuVendor.setText(DeviceInfoHelper.getCpuVendor());
            }
            tvSpecCpuModel.setText(DeviceInfoHelper.getCpuModel());
            if (tvSpecCpuPlatform != null) {
                tvSpecCpuPlatform.setText(DeviceInfoHelper.getCpuPlatform());
            }
            tvSpecCpuCores.setText(DeviceInfoHelper.getCpuFrequencySummary());
            tvSpecCpuArch.setText(DeviceInfoHelper.getCpuArchitecture());

            // Display
            DeviceInfoHelper.DisplayStats ds = DeviceInfoHelper.getDisplayStats(this);
            if (tvSpecDisplaySize != null) {
                tvSpecDisplaySize.setText(String.format(Locale.getDefault(), "%.2f inch • Tỉ lệ %s", ds.screenInches, ds.aspectRatio));
            }
            tvSpecDisplayRes.setText(ds.resolutionStandard);
            tvSpecDisplayHz.setText(ds.refreshRate + " Hz (" + (ds.refreshRate > 60 ? "Mượt mà" : "Tiết kiệm pin") + ")");
            tvSpecDisplayDpi.setText(ds.dpi + " DPI (Mật độ cao)");
            if (tvSpecDisplayTech != null) {
                tvSpecDisplayTech.setText(ds.displayTechnology);
            }

            // RAM & Storage
            DeviceInfoHelper.MemoryStats ram = DeviceInfoHelper.getRamStats(this);
            tvSpecRamText.setText(String.format(Locale.getDefault(), "%.1f GB / %.1f GB (Còn trống %.1f GB • %d%%)", ram.usedGb, ram.totalGb, ram.freeGb, ram.percentUsed));
            pbSpecRam.setProgress(ram.percentUsed);

            // ZRAM (Swap / Extension)
            DeviceInfoHelper.ZramStats zram = DeviceInfoHelper.getZramStats();
            if (tvSpecZramText != null && pbSpecZram != null) {
                if (zram.isEnabled && zram.totalGb > 0) {
                    tvSpecZramText.setText(String.format(Locale.getDefault(), "%.1f GB / %.1f GB (Đã dùng %d%%)", zram.usedGb, zram.totalGb, zram.percentUsed));
                    pbSpecZram.setProgress(zram.percentUsed);
                } else {
                    tvSpecZramText.setText("Chưa kích hoạt Mở rộng RAM");
                    pbSpecZram.setProgress(0);
                }
            }

            // ROM Storage
            DeviceInfoHelper.MemoryStats rom = DeviceInfoHelper.getInternalStorageStats();
            tvSpecStorageText.setText(String.format(Locale.getDefault(), "%.1f GB / %.1f GB", rom.usedGb, rom.totalGb));
            if (tvSpecStorageSub != null) {
                tvSpecStorageSub.setText(String.format(Locale.getDefault(), "Còn trống: %.1f GB • Đã dùng %d%%", rom.freeGb, rom.percentUsed));
            }
            pbSpecStorage.setProgress(rom.percentUsed);

            // Battery Overview
            BatteryHelper.BatteryInfo bInfo = BatteryHelper.getBatteryInfo(this);
            BatteryHelper.BatteryCapacityData bCap = BatteryHelper.getBatteryCapacityData(this);

            tvSpecBatteryPercent.setText(bInfo.level + "% (" + (bInfo.isCharging ? bInfo.plugTypeName : "Đang dùng pin") + ")");

            if (tvSpecBatteryCapacity != null) {
                tvSpecBatteryCapacity.setText(String.format(Locale.getDefault(), "%d / %d mAh (%d%%)", bCap.designMah, bCap.actualMah, Math.round(bCap.healthPercent)));
            }

            if (tvSpecBatteryCycles != null) {
                if (bCap.cycleCount > 0) {
                    tvSpecBatteryCycles.setText(bCap.cycleCount + " Chu kỳ sạc");
                } else {
                    tvSpecBatteryCycles.setText("Tự động phân tích hệ thống");
                }
            }

            tvSpecBatteryTempVoltage.setText(String.format(Locale.getDefault(), "%.1f °C • %.2f V", bInfo.temperature, bInfo.voltageMv / 1000.0f));
            tvSpecBatteryHealth.setText(bCap.healthDescription);
        } catch (Throwable t) {
            android.util.Log.e("MiTool", "Error updating device specs display: " + t.getMessage(), t);
        }
    }

    // =========================================================================
    // TAB 2: GROUPED FEATURE CARDS & BOTTOM SHEETS
    // =========================================================================

    private void setupToolsCards() {
        if (cardToolHz != null) cardToolHz.setOnClickListener(v -> showHzBottomSheet());
        if (cardToolGameFps != null) cardToolGameFps.setOnClickListener(v -> showGameFpsBottomSheet());
        if (cardToolDebloater != null) cardToolDebloater.setOnClickListener(v -> showDebloaterDialog());
        if (cardToolPrivateDns != null) cardToolPrivateDns.setOnClickListener(v -> showPrivateDnsBottomSheet());
        if (cardToolVolte != null) cardToolVolte.setOnClickListener(v -> showVoLteBottomSheet());
        if (cardToolRam != null) cardToolRam.setOnClickListener(v -> showRamBottomSheet());
        if (cardToolClock != null) cardToolClock.setOnClickListener(v -> showClockBottomSheet());
        if (cardToolBatteryAlert != null) cardToolBatteryAlert.setOnClickListener(v -> showBatteryAlertBottomSheet());
        if (cardToolMiuiUtils != null) cardToolMiuiUtils.setOnClickListener(v -> showMiuiUtilsBottomSheet());
        if (cardToolNotifFix != null) cardToolNotifFix.setOnClickListener(v -> showNotificationFixBottomSheet());

        updateToolsCardSummaries();
    }

    private void updateToolsCardSummaries() {
        // Hz Card
        if (tvToolHzSub != null) {
            int currentHz = RefreshRateHelper.getCurrentRefreshRate(this);
            int targetHz = prefs.getTargetRefreshRate();
            if (targetHz == 0) {
                tvToolHzSub.setText("Chế độ: Tự động (Auto) • Màn hình: " + currentHz + " Hz");
                tvToolHzSub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            } else {
                tvToolHzSub.setText("Chế độ: Cố định " + targetHz + " Hz • Màn hình: " + currentHz + " Hz");
                tvToolHzSub.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
            }
        }

        // Game FPS Card
        if (tvToolGameFpsSub != null) {
            GamingFpsHelper.JoyoseStatus joyose = GamingFpsHelper.checkJoyoseStatus(this);
            if (joyose == GamingFpsHelper.JoyoseStatus.DISABLED_UNLOCKED) {
                tvToolGameFpsSub.setText("Joyose: ĐÃ TẮT (120 FPS Unlocked) • Không bị khóa 89 FPS");
                tvToolGameFpsSub.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            } else if (joyose == GamingFpsHelper.JoyoseStatus.ACTIVE_LOCKED) {
                tvToolGameFpsSub.setText("Joyose: ĐANG CHẠY (Khóa 89 FPS) • Chạm để vô hiệu hóa");
                tvToolGameFpsSub.setTextColor(ContextCompat.getColor(this, R.color.mi_orange));
            } else {
                tvToolGameFpsSub.setText("Joyose: Không có / Đã gỡ bỏ");
                tvToolGameFpsSub.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
            }
        }

        // Debloater Card
        if (tvToolDebloaterSub != null) {
            int installed = 0;
            int frozen = 0;
            for (DebloatHelper.BloatwareApp app : DebloatHelper.SAFE_BLOATWARE_LIST) {
                if (DebloatHelper.isInstalled(this, app.pkg)) {
                    installed++;
                    if (DebloatHelper.isFrozen(this, app.pkg)) frozen++;
                }
            }
            if (frozen > 0) {
                tvToolDebloaterSub.setText("Đã đóng băng " + frozen + "/" + installed + " app rác • Máy mượt, tiết kiệm pin");
                tvToolDebloaterSub.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            } else {
                tvToolDebloaterSub.setText("Phát hiện " + installed + " app rác • Chạm để dọn dẹp an toàn");
                tvToolDebloaterSub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        }

        // Private DNS Card
        if (tvToolPrivateDnsSub != null) {
            String mode = PrivateDnsHelper.getCurrentDnsMode(this);
            String spec = PrivateDnsHelper.getCurrentDnsSpecifier(this);
            if (PrivateDnsHelper.MODE_HOSTNAME.equalsIgnoreCase(mode) && !spec.isEmpty()) {
                String label = spec;
                if (PrivateDnsHelper.DNS_ADGUARD.equalsIgnoreCase(spec) || PrivateDnsHelper.DNS_ADGUARD_ALT.equalsIgnoreCase(spec)) {
                    label = "AdGuard DNS (Chặn QC)";
                } else if (PrivateDnsHelper.DNS_CLOUDFLARE_FAMILY.equalsIgnoreCase(spec)) {
                    label = "Cloudflare Family (Chặn 18+)";
                } else if (PrivateDnsHelper.DNS_CLOUDFLARE.equalsIgnoreCase(spec) || PrivateDnsHelper.DNS_CLOUDFLARE_ALT.equalsIgnoreCase(spec)) {
                    label = "Cloudflare 1.1.1.1 (Tăng tốc)";
                } else if (PrivateDnsHelper.DNS_CLOUDFLARE_SECURITY.equalsIgnoreCase(spec)) {
                    label = "Cloudflare Security (Chặn độc)";
                } else if (PrivateDnsHelper.DNS_GOOGLE.equalsIgnoreCase(spec)) {
                    label = "Google DNS (Ổn định)";
                } else if (PrivateDnsHelper.DNS_QUAD9.equalsIgnoreCase(spec)) {
                    label = "Quad9 DNS (Bảo mật)";
                }
                tvToolPrivateDnsSub.setText("Đang bật: " + label);
                tvToolPrivateDnsSub.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            } else if (PrivateDnsHelper.MODE_OFF.equalsIgnoreCase(mode)) {
                tvToolPrivateDnsSub.setText("Đang tắt DNS riêng tư");
                tvToolPrivateDnsSub.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
            } else {
                tvToolPrivateDnsSub.setText("Chế độ Tự động (Mặc định nhà mạng)");
                tvToolPrivateDnsSub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        }

        // RAM Card
        if (tvToolRamSub != null) {
            DeviceInfoHelper.MemoryStats ram = DeviceInfoHelper.getRamStats(this);
            tvToolRamSub.setText(String.format(Locale.getDefault(), "Đã dùng: %.1f GB / %.1f GB (%d%%) • Chạm để dọn dẹp", ram.usedGb, ram.totalGb, ram.percentUsed));
        }

        // Clock Card
        if (tvToolClockSub != null) {
            boolean notif = prefs.isNotifClockEnabled();
            boolean floatClock = prefs.isFloatingClockEnabled();
            if (notif && floatClock) {
                tvToolClockSub.setText("Đang bật: Cả thông báo & Capsule nổi");
            } else if (notif) {
                tvToolClockSub.setText("Đang bật: Đồng hồ thanh thông báo");
            } else if (floatClock) {
                tvToolClockSub.setText("Đang bật: Đồng hồ nổi capsule");
            } else {
                tvToolClockSub.setText("Đang tắt • Hỗ trợ săn sale & hiển thị giây");
            }
        }

        // Battery Alert Card
        if (tvToolBatteryAlertSub != null) {
            int level = prefs.getBatteryAlertLevel();
            boolean alertOn = prefs.isBatteryAlertLevelEnabled();
            boolean voiceOn = prefs.isVoiceAlertMasterEnabled();
            if (voiceOn && alertOn) {
                tvToolBatteryAlertSub.setText("Đang bật: Cả Giọng nói TTS & Chuông sạc (" + level + "%)");
                tvToolBatteryAlertSub.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            } else if (voiceOn) {
                tvToolBatteryAlertSub.setText("Đang bật: Thông báo giọng nói Tiếng Việt");
                tvToolBatteryAlertSub.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            } else if (alertOn) {
                tvToolBatteryAlertSub.setText("Đang bật: Chuông báo sạc đạt " + level + "%");
                tvToolBatteryAlertSub.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            } else {
                tvToolBatteryAlertSub.setText("Giọng nói sạc, chuông báo & HUD Ambient");
                tvToolBatteryAlertSub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        }

        // Notification Fixer Card
        if (tvToolNotifFixSub != null) {
            long lastFix = prefs.getNotifFixLastApplied();
            if (lastFix > 0) {
                tvToolNotifFixSub.setText("Đã tối ưu chạy nền • Chống trễ tin nhắn Zalo, Messenger");
                tvToolNotifFixSub.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            } else {
                tvToolNotifFixSub.setText("Chống kill app Zalo, Messenger, Telegram... Chạm để tối ưu");
                tvToolNotifFixSub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        }
    }

    private void configureBottomSheet(BottomSheetDialog bs) {
        if (bs == null) return;
        bs.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                BottomSheetDialog d = (BottomSheetDialog) dialog;
                FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    bottomSheet.setBackgroundResource(android.R.color.transparent);
                    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setSkipCollapsed(true);
                }
            }
        });
    }

    // --- BottomSheet 1: Refresh Rate ---
    private void showHzBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_hz, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        TextView tvHz = view.findViewById(R.id.bs_tv_current_hz);
        MaterialButton btnAuto = view.findViewById(R.id.bs_btn_hz_auto);
        MaterialButton btn60 = view.findViewById(R.id.bs_btn_hz_60);
        MaterialButton btn90 = view.findViewById(R.id.bs_btn_hz_90);
        MaterialButton btn120 = view.findViewById(R.id.bs_btn_hz_120);
        MaterialButton btn144 = view.findViewById(R.id.bs_btn_hz_144);
        MaterialButton btnCustom = view.findViewById(R.id.bs_btn_hz_custom);
        MaterialButton btnAddTile = view.findViewById(R.id.bs_btn_add_hz_tile);
        MaterialButton btnSettings = view.findViewById(R.id.bs_btn_open_hz_settings);

        int currentHz = RefreshRateHelper.getCurrentRefreshRate(this);
        if (tvHz != null) tvHz.setText(currentHz + " Hz");

        List<Integer> supp = RefreshRateHelper.getSupportedRefreshRates(this);
        if (btn90 != null && !supp.contains(90)) {
            btn90.setEnabled(false);
            btn90.setAlpha(0.35f);
        }
        if (btn120 != null && !supp.contains(120)) {
            btn120.setEnabled(false);
            btn120.setAlpha(0.35f);
        }
        if (btn144 != null && !supp.contains(144)) {
            btn144.setEnabled(false);
            btn144.setAlpha(0.35f);
        }

        updateHzButtonsHighlight(view, prefs.getTargetRefreshRate());

        if (btnAuto != null) {
            btnAuto.setOnClickListener(v -> {
                applyRefreshRate(0);
                updateHzButtonsHighlight(view, 0);
                scheduleHzRefresh(tvHz);
            });
        }
        if (btn60 != null) {
            btn60.setOnClickListener(v -> {
                applyRefreshRate(60);
                updateHzButtonsHighlight(view, 60);
                scheduleHzRefresh(tvHz);
            });
        }
        if (btn90 != null) {
            btn90.setOnClickListener(v -> {
                applyRefreshRate(90);
                updateHzButtonsHighlight(view, 90);
                scheduleHzRefresh(tvHz);
            });
        }
        if (btn120 != null) {
            btn120.setOnClickListener(v -> {
                applyRefreshRate(120);
                updateHzButtonsHighlight(view, 120);
                scheduleHzRefresh(tvHz);
            });
        }
        if (btn144 != null) {
            btn144.setOnClickListener(v -> {
                applyRefreshRate(144);
                updateHzButtonsHighlight(view, 144);
                scheduleHzRefresh(tvHz);
            });
        }
        if (btnCustom != null) {
            btnCustom.setOnClickListener(v -> showCustomHzDialog(view, tvHz));
        }

        if (btnAddTile != null) {
            btnAddTile.setOnClickListener(v -> {
                requestAddHzTile();
                bs.dismiss();
            });
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                RefreshRateHelper.openRefreshRateSettings(MainActivity.this);
                bs.dismiss();
            });
        }

        bs.show();
    }

    private void scheduleHzRefresh(final TextView tvHz) {
        if (tvHz == null) return;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            int hz = RefreshRateHelper.getCurrentRefreshRate(MainActivity.this);
            tvHz.setText(hz + " Hz");
        }, 500);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            int hz = RefreshRateHelper.getCurrentRefreshRate(MainActivity.this);
            tvHz.setText(hz + " Hz");
        }, 1200);
    }

    private void updateHzButtonsHighlight(View view, int selectedRate) {
        if (view == null) return;
        MaterialButton btnAuto = view.findViewById(R.id.bs_btn_hz_auto);
        MaterialButton btn60 = view.findViewById(R.id.bs_btn_hz_60);
        MaterialButton btn90 = view.findViewById(R.id.bs_btn_hz_90);
        MaterialButton btn120 = view.findViewById(R.id.bs_btn_hz_120);
        MaterialButton btn144 = view.findViewById(R.id.bs_btn_hz_144);
        MaterialButton btnCustom = view.findViewById(R.id.bs_btn_hz_custom);
        TextView tvMode = view.findViewById(R.id.bs_tv_hz_current_mode);

        MaterialButton[] allBtns = new MaterialButton[]{btnAuto, btn60, btn90, btn120, btn144, btnCustom};
        int activeColor = ContextCompat.getColor(this, R.color.neon_cyan);
        int normalColor = ContextCompat.getColor(this, R.color.text_primary);
        int strokeW = (int) (1.5f * getResources().getDisplayMetrics().density);

        for (MaterialButton b : allBtns) {
            if (b != null) {
                b.setStrokeWidth(0);
                b.setTextColor(normalColor);
            }
        }

        MaterialButton targetBtn = btnAuto;
        String modeText = "Chế độ đã chọn: Tự động (Auto - Mặc định hệ thống)";

        if (selectedRate == 0) {
            targetBtn = btnAuto;
            modeText = "Chế độ đã chọn: Tự động (Auto - Mặc định hệ thống)";
        } else if (selectedRate == 60) {
            targetBtn = btn60;
            modeText = "Chế độ đã chọn: Cố định 60 Hz";
        } else if (selectedRate == 90) {
            targetBtn = btn90;
            modeText = "Chế độ đã chọn: Cố định 90 Hz";
        } else if (selectedRate == 120) {
            targetBtn = btn120;
            modeText = "Chế độ đã chọn: Cố định 120 Hz";
        } else if (selectedRate == 144) {
            targetBtn = btn144;
            modeText = "Chế độ đã chọn: Cố định 144 Hz";
        } else if (selectedRate > 0) {
            targetBtn = btnCustom;
            if (btnCustom != null) btnCustom.setText(selectedRate + " Hz");
            modeText = "Chế độ đã chọn: Tùy chỉnh " + selectedRate + " Hz";
        }

        if (targetBtn != null) {
            targetBtn.setStrokeWidth(strokeW);
            targetBtn.setStrokeColor(ColorStateList.valueOf(activeColor));
            targetBtn.setTextColor(activeColor);
        }

        if (tvMode != null) {
            tvMode.setText(modeText);
        }
    }

    private void showCustomHzDialog(final View bsView, final TextView tvHz) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        int currentTarget = prefs.getTargetRefreshRate();
        input.setText(currentTarget > 0 ? String.valueOf(currentTarget) : "120");
        input.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        input.setHintTextColor(ContextCompat.getColor(this, R.color.text_muted));

        FrameLayout container = new FrameLayout(this);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, pad / 2);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("✏️ Tùy chỉnh Tần số quét (Hz)")
                .setMessage("Nhập số Hz mong muốn (hoặc 0 để quay về chế độ Tự động):")
                .setView(container)
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    String str = input.getText().toString().trim();
                    try {
                        int rate = Integer.parseInt(str);
                        applyRefreshRate(rate);
                        updateHzButtonsHighlight(bsView, rate);
                        scheduleHzRefresh(tvHz);
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Số Hz không hợp lệ!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Về Tự Động (0Hz)", (dialog, which) -> {
                    applyRefreshRate(0);
                    updateHzButtonsHighlight(bsView, 0);
                    scheduleHzRefresh(tvHz);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // --- BottomSheet 2: Game 120 FPS ---
    private void showGameFpsBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_game_fps, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        TextView tvStatus = view.findViewById(R.id.bs_tv_joyose_status);
        MaterialButton btnGame120 = view.findViewById(R.id.bs_btn_game_120_mode);
        MaterialButton btnDisableJoyose = view.findViewById(R.id.bs_btn_copy_disable_joyose);
        MaterialButton btnEnableJoyose = view.findViewById(R.id.bs_btn_copy_enable_joyose);
        MaterialButton btnClearPk = view.findViewById(R.id.bs_btn_clear_powerkeeper);
        MaterialButton btnPerAppHz = view.findViewById(R.id.bs_btn_open_per_app_hz);
        MaterialButton btnAov = view.findViewById(R.id.bs_btn_launch_aov);

        GamingFpsHelper.JoyoseStatus status = GamingFpsHelper.checkJoyoseStatus(this);
        if (status == GamingFpsHelper.JoyoseStatus.DISABLED_UNLOCKED) {
            tvStatus.setText("Joyose: ĐÃ TẮT (120 FPS Unlocked)");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
        } else if (status == GamingFpsHelper.JoyoseStatus.ACTIVE_LOCKED) {
            tvStatus.setText("Joyose: ĐANG CHẠY (Khóa 89 FPS)");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.mi_orange));
        } else {
            tvStatus.setText("Joyose: Không có / Đã gỡ bỏ");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
        }

        btnGame120.setOnClickListener(v -> {
            boolean ok = GamingFpsHelper.applyGaming120Mode(MainActivity.this);
            Toast.makeText(MainActivity.this, ok ? "⚡ Đã kích hoạt Game Mode 120Hz!" : "Cần cấp quyền WRITE_SECURE_SETTINGS", Toast.LENGTH_SHORT).show();
            updateToolsCardSummaries();
            updateDeviceSpecsDisplay();
            bs.dismiss();
        });

        btnDisableJoyose.setOnClickListener(v -> {
            GamingFpsHelper.handleDisableJoyose(MainActivity.this);
            updateToolsCardSummaries();
            bs.dismiss();
        });

        btnEnableJoyose.setOnClickListener(v -> {
            GamingFpsHelper.handleEnableJoyose(MainActivity.this);
            updateToolsCardSummaries();
            bs.dismiss();
        });

        btnClearPk.setOnClickListener(v -> {
            GamingFpsHelper.openPowerKeeperAppInfo(MainActivity.this);
            bs.dismiss();
        });

        btnPerAppHz.setOnClickListener(v -> {
            GamingFpsHelper.openPerAppRefreshRate(MainActivity.this);
            bs.dismiss();
        });

        btnAov.setOnClickListener(v -> {
            GamingFpsHelper.launchLienQuanMobile(MainActivity.this);
            bs.dismiss();
        });

        // Mini Game HUD controls
        MaterialSwitch switchGameHud = view.findViewById(R.id.bs_switch_game_hud);
        MaterialButton btnToggleGameHud = view.findViewById(R.id.bs_btn_toggle_game_hud);
        MaterialButton btnAddHudTile = view.findViewById(R.id.bs_btn_add_hud_qs_tile);

        if (switchGameHud != null) {
            switchGameHud.setChecked(prefs.isGameHudEnabled());
            switchGameHud.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                        switchGameHud.setChecked(false);
                        Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(overlayIntent);
                        Toast.makeText(MainActivity.this, "Vui lòng cấp quyền Hiển thị trên ứng dụng khác để mở HUD nổi", Toast.LENGTH_LONG).show();
                        return;
                    }
                    prefs.setGameHudEnabled(true);
                    startFloatingGameHudService();
                    Toast.makeText(MainActivity.this, "🎮 Đã bật Mini Game HUD", Toast.LENGTH_SHORT).show();
                } else {
                    prefs.setGameHudEnabled(false);
                    stopFloatingGameHudService();
                    Toast.makeText(MainActivity.this, "🎮 Đã tắt Mini Game HUD", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnToggleGameHud != null) {
            btnToggleGameHud.setOnClickListener(v -> {
                boolean cur = prefs.isGameHudEnabled();
                if (!cur) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                        Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(overlayIntent);
                        Toast.makeText(MainActivity.this, "Vui lòng cấp quyền Hiển thị trên ứng dụng khác để mở HUD nổi", Toast.LENGTH_LONG).show();
                        return;
                    }
                    prefs.setGameHudEnabled(true);
                    startFloatingGameHudService();
                    if (switchGameHud != null) switchGameHud.setChecked(true);
                    Toast.makeText(MainActivity.this, "🎮 Đã bật Mini Game HUD", Toast.LENGTH_SHORT).show();
                } else {
                    prefs.setGameHudEnabled(false);
                    stopFloatingGameHudService();
                    if (switchGameHud != null) switchGameHud.setChecked(false);
                    Toast.makeText(MainActivity.this, "🎮 Đã tắt Mini Game HUD", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnAddHudTile != null) {
            btnAddHudTile.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    StatusBarManager sbm = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
                    if (sbm != null) {
                        sbm.requestAddTileService(
                                new ComponentName(MainActivity.this, GameHudTileService.class),
                                getString(R.string.tile_game_hud_label),
                                Icon.createWithResource(MainActivity.this, R.drawable.ic_gamepad),
                                getMainExecutor(),
                                result -> Toast.makeText(MainActivity.this,
                                        result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ? "Đã thêm phím Game HUD thành công!" : "Đã hủy hoặc phím đã có sẵn",
                                        Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }
                }
                Toast.makeText(MainActivity.this, "Bạn hãy vuốt thanh điều khiển xuống, chọn biểu tượng Cây bút / Chỉnh sửa rồi kéo phím 'Game HUD FPS' vào bảng điều khiển!", Toast.LENGTH_LONG).show();
            });
        }

        bs.show();
    }

    // --- BottomSheet 3: Private DNS ---
    private void showPrivateDnsBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_private_dns, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        TextView tvStatus = view.findViewById(R.id.bs_tv_private_dns_status);
        MaterialButton btnAdguard = view.findViewById(R.id.bs_btn_dns_adguard);
        MaterialButton btnFamily = view.findViewById(R.id.bs_btn_dns_cloudflare_family);
        MaterialButton btnCf = view.findViewById(R.id.bs_btn_dns_cloudflare);
        MaterialButton btnGoogle = view.findViewById(R.id.bs_btn_dns_google);
        MaterialButton btnQuad9 = view.findViewById(R.id.bs_btn_dns_quad9);
        MaterialButton btnCustom = view.findViewById(R.id.bs_btn_dns_custom);
        MaterialButton btnAuto = view.findViewById(R.id.bs_btn_dns_auto);
        MaterialButton btnOff = view.findViewById(R.id.bs_btn_dns_off);
        MaterialButton btnOpenSettings = view.findViewById(R.id.bs_btn_open_dns_settings);

        tvStatus.setText(PrivateDnsHelper.getDnsDescription(this));
        String currentMode = PrivateDnsHelper.getCurrentDnsMode(this);
        if (PrivateDnsHelper.MODE_HOSTNAME.equalsIgnoreCase(currentMode)) {
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
        } else if (PrivateDnsHelper.MODE_OFF.equalsIgnoreCase(currentMode)) {
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
        } else {
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
        }

        btnAdguard.setOnClickListener(v -> {
            applyPrivateDns(PrivateDnsHelper.MODE_HOSTNAME, PrivateDnsHelper.DNS_ADGUARD, "AdGuard DNS (Chặn QC)");
            bs.dismiss();
        });

        btnFamily.setOnClickListener(v -> {
            applyPrivateDns(PrivateDnsHelper.MODE_HOSTNAME, PrivateDnsHelper.DNS_CLOUDFLARE_FAMILY, "Cloudflare Family (Chặn Web Độc)");
            bs.dismiss();
        });

        btnCf.setOnClickListener(v -> {
            applyPrivateDns(PrivateDnsHelper.MODE_HOSTNAME, PrivateDnsHelper.DNS_CLOUDFLARE, "Cloudflare 1.1.1.1");
            bs.dismiss();
        });

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> {
                applyPrivateDns(PrivateDnsHelper.MODE_HOSTNAME, PrivateDnsHelper.DNS_GOOGLE, "Google DNS");
                bs.dismiss();
            });
        }

        if (btnQuad9 != null) {
            btnQuad9.setOnClickListener(v -> {
                applyPrivateDns(PrivateDnsHelper.MODE_HOSTNAME, PrivateDnsHelper.DNS_QUAD9, "Quad9 DNS (Bảo mật)");
                bs.dismiss();
            });
        }

        if (btnCustom != null) {
            btnCustom.setOnClickListener(v -> showCustomDnsInputDialog(bs));
        }

        if (btnAuto != null) {
            btnAuto.setOnClickListener(v -> {
                applyPrivateDns(PrivateDnsHelper.MODE_AUTO, "", "DNS Tự Động (Nhà mạng)");
                bs.dismiss();
            });
        }

        btnOff.setOnClickListener(v -> {
            applyPrivateDns(PrivateDnsHelper.MODE_OFF, "", "Tắt DNS Riêng Tư");
            bs.dismiss();
        });

        btnOpenSettings.setOnClickListener(v -> {
            PrivateDnsHelper.openPrivateDnsSettings(MainActivity.this);
            bs.dismiss();
        });

        bs.show();
    }

    private void showCustomDnsInputDialog(BottomSheetDialog parentBs) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Ví dụ: xxxxxx.dns.nextdns.io");
        input.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        input.setHintTextColor(ContextCompat.getColor(this, R.color.text_muted));
        input.setSingleLine(true);
        String current = PrivateDnsHelper.getCurrentDnsSpecifier(this);
        if (!current.isEmpty()) {
            input.setText(current);
            input.setSelection(current.length());
        }

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        input.setLayoutParams(params);
        container.addView(input);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Nhập DNS DoT Tùy Chọn")
                .setMessage("Nhập tên miền máy chủ DNS riêng tư hỗ trợ DoT (DNS-over-TLS). Tự động loại bỏ https:// hoặc cổng thừa.")
                .setView(container)
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    String host = input.getText() != null ? input.getText().toString().trim() : "";
                    String cleaned = PrivateDnsHelper.cleanHostname(host);
                    if (cleaned.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Vui lòng nhập tên máy chủ DNS hợp lệ!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    applyPrivateDns(PrivateDnsHelper.MODE_HOSTNAME, cleaned, "Tùy chọn: " + cleaned);
                    if (parentBs != null && parentBs.isShowing()) {
                        parentBs.dismiss();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // --- BottomSheet 4: VoLTE & VoWiFi ---
    private void showVoLteBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_volte, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        MaterialButton btnVolte = view.findViewById(R.id.bs_btn_unlock_volte);
        MaterialButton btnVowifi = view.findViewById(R.id.bs_btn_unlock_vowifi);
        MaterialButton btnTesting = view.findViewById(R.id.bs_btn_open_radio_testing);
        MaterialButton btnSim = view.findViewById(R.id.bs_btn_open_sim_settings);

        btnVolte.setOnClickListener(v -> {
            VoLteHelper.triggerSecretCode(MainActivity.this, VoLteHelper.CODE_VOLTE, "Mở khóa VoLTE");
            bs.dismiss();
        });

        btnVowifi.setOnClickListener(v -> {
            VoLteHelper.triggerSecretCode(MainActivity.this, VoLteHelper.CODE_VOWIFI, "Mở khóa VoWiFi");
            bs.dismiss();
        });

        btnTesting.setOnClickListener(v -> {
            VoLteHelper.triggerSecretCode(MainActivity.this, VoLteHelper.CODE_TESTING, "Kiểm tra sóng");
            bs.dismiss();
        });

        btnSim.setOnClickListener(v -> {
            VoLteHelper.openMobileNetworkSettings(MainActivity.this);
            bs.dismiss();
        });

        bs.show();
    }

    // --- BottomSheet 5: RAM Booster ---
    private void showRamBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_ram, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        TextView tvShizukuStatus = view.findViewById(R.id.bs_tv_shizuku_status);
        TextView tvLevelDesc = view.findViewById(R.id.bs_tv_ram_level_desc);
        TextView tvPercent = view.findViewById(R.id.bs_tv_ram_percent);
        ProgressBar pbUsage = view.findViewById(R.id.bs_pb_ram_usage);
        TextView tvUsed = view.findViewById(R.id.bs_tv_ram_used);
        TextView tvFree = view.findViewById(R.id.bs_tv_ram_free);
        TextView tvTotal = view.findViewById(R.id.bs_tv_ram_total);

        View layoutResult = view.findViewById(R.id.bs_layout_boost_result);
        TextView tvResultTitle = view.findViewById(R.id.bs_tv_boost_result_title);
        TextView tvResultDesc = view.findViewById(R.id.bs_tv_boost_result_desc);

        MaterialButton btnBoost = view.findViewById(R.id.bs_btn_boost_ram);
        MaterialButton btnTurbo = view.findViewById(R.id.bs_btn_open_game_turbo);
        MaterialButton btnRunningApps = view.findViewById(R.id.bs_btn_open_running_apps);

        // Update Shizuku Status Banner
        boolean hasShizuku = ShizukuHelper.isShizukuAvailable() && ShizukuHelper.hasShizukuPermission();
        if (tvShizukuStatus != null) {
            if (hasShizuku) {
                tvShizukuStatus.setText("⚡ Shizuku Deep Boost: Đã sẵn sàng (Dọn sâu cấp hệ thống)");
                tvShizukuStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            } else {
                tvShizukuStatus.setText("ℹ️ Chế độ tiêu chuẩn (Kết nối Shizuku để dọn sâu toàn bộ tiến trình ngầm)");
                tvShizukuStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
            }
        }

        Runnable updateRamUi = () -> {
            RamBoosterHelper.MemoryStats stats = RamBoosterHelper.getMemoryStats(MainActivity.this);
            float usedGb = stats.usedMb / 1024.0f;
            float freeGb = stats.availMb / 1024.0f;
            float totalGb = stats.totalMb / 1024.0f;
            int pct = stats.percentUsed;

            if (tvPercent != null) tvPercent.setText(pct + "%");
            if (pbUsage != null) {
                pbUsage.setProgress(pct);
                int tintColor;
                if (pct < 60) tintColor = R.color.neon_green;
                else if (pct < 80) tintColor = R.color.neon_yellow;
                else tintColor = R.color.neon_red;
                pbUsage.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, tintColor)));
            }
            if (tvLevelDesc != null) {
                if (pct < 60) {
                    tvLevelDesc.setText("Hoạt động tối ưu & mượt mà");
                    tvLevelDesc.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.neon_green));
                } else if (pct < 80) {
                    tvLevelDesc.setText("Mức tải bộ nhớ bình thường");
                    tvLevelDesc.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.neon_yellow));
                } else {
                    tvLevelDesc.setText("Bộ nhớ căng thẳng - Cần dọn dẹp");
                    tvLevelDesc.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.neon_red));
                }
            }
            if (tvUsed != null) tvUsed.setText(String.format(Locale.getDefault(), "%.1f GB", usedGb));
            if (tvFree != null) tvFree.setText(String.format(Locale.getDefault(), "%.1f GB", freeGb));
            if (tvTotal != null) tvTotal.setText(String.format(Locale.getDefault(), "%.1f GB", totalGb));
        };

        updateRamUi.run();

        btnBoost.setOnClickListener(v -> {
            btnBoost.setEnabled(false);
            btnBoost.setText("⏳ Đang dọn dẹp & giải phóng RAM...");
            RamBoosterHelper.boostRamDetailed(MainActivity.this, result -> {
                btnBoost.setEnabled(true);
                btnBoost.setText("🚀 Dọn Dẹp & Tăng Tốc RAM Ngay");
                updateRamUi.run();
                updateToolsCardSummaries();
                updateDeviceSpecsDisplay();
                app.cd.mitool.widget.RamBoostAppWidgetProvider.updateAllWidgets(MainActivity.this);

                if (layoutResult != null && tvResultTitle != null && tvResultDesc != null) {
                    layoutResult.setVisibility(View.VISIBLE);
                    if (result.freedMb > 0) {
                        tvResultTitle.setText("🎉 Đã giải phóng +" + result.freedMb + " MB RAM!");
                        tvResultTitle.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.neon_green));
                        String desc = "Đã dọn dẹp bộ nhớ đệm và đóng " + result.killedCount + " tiến trình chạy nền.";
                        if (result.usedShizuku) {
                            desc += " (Đã dọn cấp hệ thống qua Shizuku Deep Boost)";
                        }
                        tvResultDesc.setText(desc);
                    } else {
                        tvResultTitle.setText("⚡ Bộ nhớ RAM đã ở trạng thái tối ưu!");
                        tvResultTitle.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.neon_cyan));
                        tvResultDesc.setText("Hệ thống đã dọn dẹp bộ nhớ đệm và tối ưu các tiến trình chạy nền.");
                    }
                }
                Toast.makeText(MainActivity.this, result.freedMb > 0 ? "Đã giải phóng ~" + result.freedMb + " MB RAM!" : "RAM đang ở trạng thái mượt mà!", Toast.LENGTH_SHORT).show();
            });
        });

        btnTurbo.setOnClickListener(v -> {
            XiaomiHelper.openGameTurbo(MainActivity.this);
            bs.dismiss();
        });

        if (btnRunningApps != null) {
            btnRunningApps.setOnClickListener(v -> {
                try {
                    Intent it = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                    startActivity(it);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Không thể mở Cài đặt ứng dụng", Toast.LENGTH_SHORT).show();
                }
            });
        }

        bs.show();
    }

    // --- BottomSheet 6: Clock Options ---
    private void showClockBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_clock, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        MaterialSwitch swNotif = view.findViewById(R.id.bs_switch_notif_clock);
        MaterialSwitch swFloat = view.findViewById(R.id.bs_switch_floating_clock);
        View subOptions = view.findViewById(R.id.bs_layout_floating_sub_options);

        MaterialButton btnTopLeft = view.findViewById(R.id.bs_btn_preset_top_left);
        MaterialButton btnBeside = view.findViewById(R.id.bs_btn_preset_beside);
        MaterialButton btnNotch = view.findViewById(R.id.bs_btn_preset_notch);
        MaterialButton btnFree = view.findViewById(R.id.bs_btn_preset_free);

        MaterialSwitch swTrans = view.findViewById(R.id.bs_switch_clock_transparent);
        MaterialSwitch swLock = view.findViewById(R.id.bs_switch_clock_lock);
        MaterialSwitch swMs = view.findViewById(R.id.bs_switch_clock_ms);

        swNotif.setChecked(prefs.isNotifClockEnabled());
        swNotif.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.setNotifClockEnabled(isChecked);
            if (isChecked) startNotificationClockService();
            else stopNotificationClockService();
            updateToolsCardSummaries();
        });

        boolean floatOn = prefs.isFloatingClockEnabled();
        swFloat.setChecked(floatOn);
        subOptions.setVisibility(floatOn ? View.VISIBLE : View.GONE);
        swFloat.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                if (!XiaomiHelper.canDrawOverlays(MainActivity.this)) {
                    XiaomiHelper.openOverlaySettings(MainActivity.this);
                    swFloat.setChecked(false);
                    return;
                }
                prefs.setFloatingClockEnabled(true);
                startFloatingClockService();
                subOptions.setVisibility(View.VISIBLE);
            } else {
                prefs.setFloatingClockEnabled(false);
                stopFloatingClockService();
                subOptions.setVisibility(View.GONE);
            }
            updateToolsCardSummaries();
        });

        int activePreset = prefs.getFloatingClockPreset();
        updatePresetButtonsHighlight(btnTopLeft, btnBeside, btnNotch, btnFree, activePreset);

        btnTopLeft.setOnClickListener(v -> {
            applyFloatingPreset(0);
            updatePresetButtonsHighlight(btnTopLeft, btnBeside, btnNotch, btnFree, 0);
        });
        btnBeside.setOnClickListener(v -> {
            applyFloatingPreset(1);
            updatePresetButtonsHighlight(btnTopLeft, btnBeside, btnNotch, btnFree, 1);
        });
        btnNotch.setOnClickListener(v -> {
            applyFloatingPreset(2);
            updatePresetButtonsHighlight(btnTopLeft, btnBeside, btnNotch, btnFree, 2);
        });
        btnFree.setOnClickListener(v -> {
            applyFloatingPreset(3);
            updatePresetButtonsHighlight(btnTopLeft, btnBeside, btnNotch, btnFree, 3);
        });

        MaterialButton btnAddClockTile = view.findViewById(R.id.bs_btn_add_clock_tile);
        if (btnAddClockTile != null) {
            btnAddClockTile.setOnClickListener(v -> requestAddClockTile());
        }

        swTrans.setChecked(prefs.isFloatingClockTransparent());
        swTrans.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.setFloatingClockTransparent(isChecked);
            if (prefs.isFloatingClockEnabled()) startFloatingClockService();
        });

        swLock.setChecked(prefs.isFloatingClockLocked());
        swLock.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.setFloatingClockLocked(isChecked);
            if (prefs.isFloatingClockEnabled()) startFloatingClockService();
        });

        swMs.setChecked(prefs.isFloatingClockMs());
        swMs.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.setFloatingClockMs(isChecked);
            if (prefs.isFloatingClockEnabled()) startFloatingClockService();
        });

        bs.show();
    }

    // --- BottomSheet 7: Battery Alerts & Ambient HUD ---
    private void showBatteryAlertBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_battery_alert, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        // Voice Alert Views
        MaterialSwitch swVoiceMaster = view.findViewById(R.id.bs_switch_voice_master);
        View layoutVoiceSub = view.findViewById(R.id.bs_layout_voice_sub_options);
        MaterialButton btnTestVoice = view.findViewById(R.id.bs_btn_test_voice);
        MaterialSwitch swVoiceConnect = view.findViewById(R.id.bs_switch_voice_connect);
        MaterialSwitch swVoiceDisconnect = view.findViewById(R.id.bs_switch_voice_disconnect);
        MaterialSwitch swVoice80 = view.findViewById(R.id.bs_switch_voice_80);
        TextView tvVoice80Label = view.findViewById(R.id.bs_tv_voice_80_label);
        MaterialSwitch swVoice100 = view.findViewById(R.id.bs_switch_voice_100);
        MaterialSwitch swVoiceTemp = view.findViewById(R.id.bs_switch_voice_temp);

        // Ringtone & Threshold Views
        TextView tvTitle = view.findViewById(R.id.bs_tv_alert_80_title);
        MaterialSwitch swAlert80 = view.findViewById(R.id.bs_switch_alert_80);
        View sliderLayout = view.findViewById(R.id.bs_layout_alert_level_slider);
        TextView tvLevelValue = view.findViewById(R.id.bs_tv_alert_level_value);
        SeekBar sbLevel = view.findViewById(R.id.bs_sb_battery_alert_level);

        TextView tvTone = view.findViewById(R.id.bs_tv_selected_audio_tone);
        MaterialButton btnPick = view.findViewById(R.id.bs_btn_pick_audio);
        MaterialButton btnTest = view.findViewById(R.id.bs_btn_test_audio);
        MaterialButton btnReset = view.findViewById(R.id.bs_btn_reset_audio);

        // Extra Toggles
        MaterialSwitch swAmbient = view.findViewById(R.id.bs_switch_lockscreen_ambient);
        MaterialSwitch swAlert100 = view.findViewById(R.id.bs_switch_alert_100);
        MaterialSwitch swAlertTemp = view.findViewById(R.id.bs_switch_alert_temp);

        // Setup Voice Alerts
        boolean voiceMaster = prefs.isVoiceAlertMasterEnabled();
        if (swVoiceMaster != null) {
            swVoiceMaster.setChecked(voiceMaster);
            if (layoutVoiceSub != null) layoutVoiceSub.setVisibility(voiceMaster ? View.VISIBLE : View.GONE);
            swVoiceMaster.setOnCheckedChangeListener((btn, isChecked) -> {
                prefs.setVoiceAlertMasterEnabled(isChecked);
                if (layoutVoiceSub != null) layoutVoiceSub.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if (isChecked) {
                    VoiceAlertHelper.init(MainActivity.this);
                    Toast.makeText(MainActivity.this, "Đã bật thông báo giọng nói Tiếng Việt!", Toast.LENGTH_SHORT).show();
                }
                updateToolsCardSummaries();
            });
        }

        if (btnTestVoice != null) {
            btnTestVoice.setOnClickListener(v -> VoiceAlertHelper.testVoice(MainActivity.this));
        }

        if (swVoiceConnect != null) {
            swVoiceConnect.setChecked(prefs.isVoiceAlertConnectEnabled());
            swVoiceConnect.setOnCheckedChangeListener((btn, isChecked) -> prefs.setVoiceAlertConnectEnabled(isChecked));
        }
        if (swVoiceDisconnect != null) {
            swVoiceDisconnect.setChecked(prefs.isVoiceAlertDisconnectEnabled());
            swVoiceDisconnect.setOnCheckedChangeListener((btn, isChecked) -> prefs.setVoiceAlertDisconnectEnabled(isChecked));
        }
        if (swVoice80 != null) {
            swVoice80.setChecked(prefs.isVoiceAlert80Enabled());
            swVoice80.setOnCheckedChangeListener((btn, isChecked) -> prefs.setVoiceAlert80Enabled(isChecked));
        }
        if (swVoice100 != null) {
            swVoice100.setChecked(prefs.isVoiceAlert100Enabled());
            swVoice100.setOnCheckedChangeListener((btn, isChecked) -> prefs.setVoiceAlert100Enabled(isChecked));
        }
        if (swVoiceTemp != null) {
            swVoiceTemp.setChecked(prefs.isVoiceAlertTempEnabled());
            swVoiceTemp.setOnCheckedChangeListener((btn, isChecked) -> prefs.setVoiceAlertTempEnabled(isChecked));
        }

        // Setup Ringtone & Threshold
        int currentLevel = prefs.getBatteryAlertLevel();
        boolean enabled = prefs.isBatteryAlertLevelEnabled();
        if (swAlert80 != null) {
            swAlert80.setChecked(enabled);
            swAlert80.setOnCheckedChangeListener((btn, isChecked) -> {
                prefs.setBatteryAlertLevelEnabled(isChecked);
                if (sliderLayout != null) sliderLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                updateToolsCardSummaries();
            });
        }

        if (tvTitle != null) tvTitle.setText("Ngưỡng chuông báo bảo vệ pin (" + currentLevel + "%)");
        if (tvVoice80Label != null) tvVoice80Label.setText("Khi sạc đạt ngưỡng bảo vệ (" + currentLevel + "%)");
        if (tvLevelValue != null) tvLevelValue.setText(currentLevel + "%");
        if (sbLevel != null) sbLevel.setProgress(Math.max(0, Math.min(45, currentLevel - 50)));
        if (sliderLayout != null) sliderLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);

        if (sbLevel != null) {
            sbLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int target = 50 + progress;
                    if (tvLevelValue != null) tvLevelValue.setText(target + "%");
                    if (tvTitle != null) tvTitle.setText("Ngưỡng chuông báo bảo vệ pin (" + target + "%)");
                    if (tvVoice80Label != null) tvVoice80Label.setText("Khi sạc đạt ngưỡng bảo vệ (" + target + "%)");
                    if (fromUser) {
                        prefs.setBatteryAlertLevel(target);
                        updateToolsCardSummaries();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (tvTone != null) tvTone.setText(prefs.getCustomAlertToneTitle());

        if (btnPick != null) {
            btnPick.setOnClickListener(v -> {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION | RingtoneManager.TYPE_RINGTONE);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chọn âm báo bảo vệ pin");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                startActivityForResult(intent, REQ_PICK_RINGTONE);
                bs.dismiss();
            });
        }

        if (btnTest != null) {
            btnTest.setOnClickListener(v -> {
                if (BatteryHelper.isPlayingAlert()) {
                    BatteryHelper.stopAlertTone();
                    btnTest.setText("🔊 Nghe thử");
                } else {
                    BatteryHelper.playAlertTone(MainActivity.this, prefs.getCustomAlertToneUri());
                    btnTest.setText("⏹️ Dừng");
                }
            });
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                prefs.setCustomAlertToneUri(null);
                prefs.setCustomAlertToneTitle("Mặc định hệ thống");
                if (tvTone != null) tvTone.setText("Mặc định hệ thống");
                Toast.makeText(MainActivity.this, "Đã đặt lại âm báo về mặc định", Toast.LENGTH_SHORT).show();
            });
        }

        if (swAmbient != null) {
            swAmbient.setChecked(prefs.isLockscreenAmbientEnabled());
            swAmbient.setOnCheckedChangeListener((btn, isChecked) -> prefs.setLockscreenAmbientEnabled(isChecked));
        }

        if (swAlert100 != null) {
            swAlert100.setChecked(prefs.isAlert100Enabled());
            swAlert100.setOnCheckedChangeListener((btn, isChecked) -> prefs.setAlert100Enabled(isChecked));
        }

        if (swAlertTemp != null) {
            swAlertTemp.setChecked(prefs.isAlertTempEnabled());
            swAlertTemp.setOnCheckedChangeListener((btn, isChecked) -> prefs.setAlertTempEnabled(isChecked));
        }

        MaterialButton btnViewSot = view.findViewById(R.id.bs_btn_view_sot_stats);
        if (btnViewSot != null) {
            btnViewSot.setOnClickListener(v -> {
                bs.dismiss();
                BottomNavigationView bnv = findViewById(R.id.bottom_navigation);
                if (bnv != null) {
                    bnv.setSelectedItemId(R.id.nav_battery);
                }
            });
        }

        bs.setOnDismissListener(dialog -> BatteryHelper.stopAlertTone());
        bs.show();
    }

    // --- BottomSheet: Widget SOT & Battery Customization ---
    private void showWidgetCustomizeDialog() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_customize_widget, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        PreferencesHelper prefs = new PreferencesHelper(this);

        RadioButton rbStyleHyperos = view.findViewById(R.id.rb_style_hyperos);
        RadioButton rbStyleMinimal = view.findViewById(R.id.rb_style_minimal);
        RadioButton rbStyleDashboard = view.findViewById(R.id.rb_style_dashboard);

        int currentStyle = prefs.getWidgetStyle();
        if (currentStyle == PreferencesHelper.WIDGET_STYLE_MINIMAL) {
            rbStyleMinimal.setChecked(true);
        } else if (currentStyle == PreferencesHelper.WIDGET_STYLE_HYPEROS) {
            rbStyleHyperos.setChecked(true);
        } else {
            rbStyleDashboard.setChecked(true);
        }

        RadioButton rbThemeSystem = view.findViewById(R.id.rb_theme_system);
        RadioButton rbThemeDark = view.findViewById(R.id.rb_theme_dark);
        RadioButton rbThemeLight = view.findViewById(R.id.rb_theme_light);

        int currentTheme = prefs.getWidgetThemeMode();
        if (currentTheme == PreferencesHelper.WIDGET_THEME_DARK) {
            rbThemeDark.setChecked(true);
        } else if (currentTheme == PreferencesHelper.WIDGET_THEME_LIGHT) {
            rbThemeLight.setChecked(true);
        } else {
            rbThemeSystem.setChecked(true);
        }

        RadioButton rbInterval15 = view.findViewById(R.id.rb_interval_15);
        RadioButton rbInterval30 = view.findViewById(R.id.rb_interval_30);
        RadioButton rbInterval60 = view.findViewById(R.id.rb_interval_60);
        RadioButton rbIntervalEvent = view.findViewById(R.id.rb_interval_event);

        int currentInterval = prefs.getWidgetRefreshInterval();
        if (currentInterval == 15) {
            rbInterval15.setChecked(true);
        } else if (currentInterval == 60) {
            rbInterval60.setChecked(true);
        } else if (currentInterval == 0) {
            rbIntervalEvent.setChecked(true);
        } else {
            rbInterval30.setChecked(true);
        }

        MaterialSwitch swShowTemp = view.findViewById(R.id.sw_widget_show_temp);
        MaterialSwitch swShowEstimate = view.findViewById(R.id.sw_widget_show_estimate);

        if (swShowTemp != null) swShowTemp.setChecked(prefs.isWidgetShowTemp());
        if (swShowEstimate != null) swShowEstimate.setChecked(prefs.isWidgetShowEstimate());

        View btnSave = view.findViewById(R.id.btn_widget_save);
        View btnClose = view.findViewById(R.id.btn_widget_close);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> bs.dismiss());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                int selectedStyle = PreferencesHelper.WIDGET_STYLE_HYPEROS;
                if (rbStyleMinimal.isChecked()) {
                    selectedStyle = PreferencesHelper.WIDGET_STYLE_MINIMAL;
                } else if (rbStyleDashboard.isChecked()) {
                    selectedStyle = PreferencesHelper.WIDGET_STYLE_DASHBOARD;
                }
                prefs.setWidgetStyle(selectedStyle);

                int selectedTheme = PreferencesHelper.WIDGET_THEME_SYSTEM;
                if (rbThemeDark.isChecked()) {
                    selectedTheme = PreferencesHelper.WIDGET_THEME_DARK;
                } else if (rbThemeLight.isChecked()) {
                    selectedTheme = PreferencesHelper.WIDGET_THEME_LIGHT;
                }
                prefs.setWidgetThemeMode(selectedTheme);

                int selectedInterval = 30;
                if (rbInterval15.isChecked()) {
                    selectedInterval = 15;
                } else if (rbInterval60.isChecked()) {
                    selectedInterval = 60;
                } else if (rbIntervalEvent.isChecked()) {
                    selectedInterval = 0;
                }
                prefs.setWidgetRefreshInterval(selectedInterval);

                if (swShowTemp != null) prefs.setWidgetShowTemp(swShowTemp.isChecked());
                if (swShowEstimate != null) prefs.setWidgetShowEstimate(swShowEstimate.isChecked());

                app.cd.mitool.widget.SotWidgetScheduler.scheduleNext(this);
                app.cd.mitool.widget.SotAppWidgetProvider.updateAllWidgets(this);

                Toast.makeText(this, "Đã lưu tùy biến & cập nhật Widget!", Toast.LENGTH_SHORT).show();
                bs.dismiss();
            });
        }

        bs.show();
    }

    // --- BottomSheet 8: Hidden MIUI / HyperOS Utils ---
    private void showMiuiUtilsBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_miui_utils, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        MaterialButton btnDeepClean = view.findViewById(R.id.bs_btn_open_deep_clean);
        MaterialButton btnCit = view.findViewById(R.id.bs_btn_open_cit_test);
        MaterialButton btnRadio = view.findViewById(R.id.bs_btn_open_radio_info);
        MaterialButton btnMsa = view.findViewById(R.id.bs_btn_open_msa_settings);
        MaterialButton btnAd = view.findViewById(R.id.bs_btn_open_ad_service_settings);

        btnDeepClean.setOnClickListener(v -> {
            XiaomiHelper.openDeepClean(MainActivity.this);
            bs.dismiss();
        });

        btnCit.setOnClickListener(v -> {
            XiaomiHelper.openCITHardwareTest(MainActivity.this);
            bs.dismiss();
        });

        btnRadio.setOnClickListener(v -> {
            XiaomiHelper.openHiddenBatteryInfo(MainActivity.this);
            bs.dismiss();
        });

        btnMsa.setOnClickListener(v -> {
            XiaomiHelper.openAuthorizationAndRevocation(MainActivity.this);
            bs.dismiss();
        });

        btnAd.setOnClickListener(v -> {
            XiaomiHelper.openAdServicesSettings(MainActivity.this);
            bs.dismiss();
        });

        bs.show();
    }

    // --- BottomSheet 9: Notification Fixer ---
    private void showNotificationFixBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_notif_fix, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        ImageView ivShizukuIcon = view.findViewById(R.id.bs_iv_notif_shizuku_icon);
        TextView tvShizukuStatus = view.findViewById(R.id.bs_tv_notif_shizuku_status);
        TextView tvShizukuDesc = view.findViewById(R.id.bs_tv_notif_shizuku_desc);
        MaterialButton btnGrantShizuku = view.findViewById(R.id.bs_btn_grant_shizuku);
        MaterialButton btnBatchFix = view.findViewById(R.id.bs_btn_batch_fix);
        MaterialButton btnOpenAutostart = view.findViewById(R.id.bs_btn_open_autostart);
        MaterialButton btnOpenPowerKeeper = view.findViewById(R.id.bs_btn_open_powerkeeper);
        MaterialButton btnOpenFcmDiag = view.findViewById(R.id.bs_btn_open_fcm_diag);
        TextView tvSelectedCount = view.findViewById(R.id.bs_tv_notif_selected_count);
        TextView tvSelectAll = view.findViewById(R.id.bs_tv_select_all);
        EditText etSearch = view.findViewById(R.id.bs_et_search_apps);
        ImageView ivClearSearch = view.findViewById(R.id.bs_iv_clear_search);
        TextView chipFilterAll = view.findViewById(R.id.bs_chip_filter_all);
        TextView chipFilterRecommended = view.findViewById(R.id.bs_chip_filter_recommended);
        TextView chipFilterSelected = view.findViewById(R.id.bs_chip_filter_selected);
        TextView chipFilterUnoptimized = view.findViewById(R.id.bs_chip_filter_unoptimized);
        ProgressBar pbLoading = view.findViewById(R.id.bs_pb_loading_apps);
        LinearLayout layoutApps = view.findViewById(R.id.bs_layout_notif_apps);
        TextView tvEmpty = view.findViewById(R.id.bs_tv_notif_empty);

        boolean hasShizuku = ShizukuHelper.hasShizukuPermission();
        if (hasShizuku) {
            if (ivShizukuIcon != null) {
                ivShizukuIcon.setImageResource(R.drawable.ic_shield_check);
                ivShizukuIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.neon_green)));
            }
            if (tvShizukuStatus != null) {
                tvShizukuStatus.setText("⚡ Shizuku: Đã cấp quyền (Tự động 1-chạm)");
                tvShizukuStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            }
            if (tvShizukuDesc != null) {
                tvShizukuDesc.setText("Đã sẵn sàng mở khóa FCM Push Hub (GMS) và tự động cấu hình ứng dụng.");
            }
            if (btnGrantShizuku != null) {
                btnGrantShizuku.setVisibility(View.GONE);
            }
            if (btnBatchFix != null) {
                btnBatchFix.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.neon_green)));
                btnBatchFix.setTextColor(0xFF0D1117);
            }
        } else {
            if (ivShizukuIcon != null) {
                ivShizukuIcon.setImageResource(R.drawable.ic_shield_alert);
                ivShizukuIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.mi_orange)));
            }
            if (tvShizukuStatus != null) {
                tvShizukuStatus.setText("⚠️ Shizuku: Chưa cấp quyền");
                tvShizukuStatus.setTextColor(ContextCompat.getColor(this, R.color.mi_orange));
            }
            if (tvShizukuDesc != null) {
                tvShizukuDesc.setText("Cấp quyền Shizuku để tối ưu 1-chạm hoặc dùng các phím tắt hệ thống bên dưới.");
            }
            if (btnGrantShizuku != null) {
                btnGrantShizuku.setVisibility(View.VISIBLE);
                btnGrantShizuku.setOnClickListener(v -> ShizukuHelper.requestShizukuPermission(MainActivity.this));
            }
            if (btnBatchFix != null) {
                btnBatchFix.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_highlight)));
                btnBatchFix.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            }
        }

        final List<NotificationFixHelper.TargetApp> apps = new ArrayList<>();
        final int[] activeFilter = {0}; // 0: all, 1: recommended, 2: selected, 3: unoptimized
        final String[] currentQuery = {""};

        Runnable updateSelectedCounter = () -> {
            int selected = 0;
            for (NotificationFixHelper.TargetApp a : apps) {
                if (a.isSelected) selected++;
            }
            if (tvSelectedCount != null) {
                tvSelectedCount.setText("(Đã chọn " + selected + "/" + apps.size() + ")");
            }
            if (tvSelectAll != null) {
                tvSelectAll.setText(selected == apps.size() && !apps.isEmpty() ? "Bỏ chọn tất cả" : "Chọn tất cả");
            }
        };

        Runnable applyFilter = () -> {
            String q = currentQuery[0].trim().toLowerCase();
            int visibleCount = 0;
            int mode = activeFilter[0];

            for (int i = 0; i < layoutApps.getChildCount(); i++) {
                View itemView = layoutApps.getChildAt(i);
                NotificationFixHelper.TargetApp app = (NotificationFixHelper.TargetApp) itemView.getTag();
                if (app == null) continue;

                boolean matchMode = true;
                if (mode == 1) {
                    matchMode = app.isRecommended;
                } else if (mode == 2) {
                    matchMode = app.isSelected;
                } else if (mode == 3) {
                    matchMode = !app.isOptimized;
                }

                boolean matchQuery = true;
                if (!q.isEmpty()) {
                    String name = (app.label != null) ? app.label.toLowerCase() : "";
                    String pkg = (app.packageName != null) ? app.packageName.toLowerCase() : "";
                    matchQuery = name.contains(q) || pkg.contains(q);
                }

                boolean visible = matchMode && matchQuery;
                itemView.setVisibility(visible ? View.VISIBLE : View.GONE);
                if (visible) visibleCount++;
            }

            if (tvEmpty != null) {
                tvEmpty.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
            }
        };

        Runnable updateChipStyles = () -> {
            TextView[] chips = {chipFilterAll, chipFilterRecommended, chipFilterSelected, chipFilterUnoptimized};
            for (int i = 0; i < chips.length; i++) {
                if (chips[i] == null) continue;
                if (i == activeFilter[0]) {
                    chips[i].setBackgroundResource(R.drawable.bg_badge_cyan);
                    chips[i].setTextColor(ContextCompat.getColor(MainActivity.this, R.color.neon_cyan));
                } else {
                    chips[i].setBackgroundResource(R.drawable.bg_card_rounded);
                    chips[i].setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_secondary));
                }
            }
        };

        if (chipFilterAll != null) chipFilterAll.setOnClickListener(v -> { activeFilter[0] = 0; updateChipStyles.run(); applyFilter.run(); });
        if (chipFilterRecommended != null) chipFilterRecommended.setOnClickListener(v -> { activeFilter[0] = 1; updateChipStyles.run(); applyFilter.run(); });
        if (chipFilterSelected != null) chipFilterSelected.setOnClickListener(v -> { activeFilter[0] = 2; updateChipStyles.run(); applyFilter.run(); });
        if (chipFilterUnoptimized != null) chipFilterUnoptimized.setOnClickListener(v -> { activeFilter[0] = 3; updateChipStyles.run(); applyFilter.run(); });

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery[0] = (s != null) ? s.toString() : "";
                    if (ivClearSearch != null) {
                        ivClearSearch.setVisibility(currentQuery[0].isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    applyFilter.run();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                if (etSearch != null) etSearch.setText("");
            });
        }

        // Async load all installed applications to eliminate the 12-app limitation and prevent any UI freeze
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);

        new Thread(() -> {
            final List<NotificationFixHelper.TargetApp> loaded = NotificationFixHelper.getAllInstalledApps(MainActivity.this);
            runOnUiThread(() -> {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                apps.clear();
                apps.addAll(loaded);

                if (apps.isEmpty()) {
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    if (tvSelectedCount != null) tvSelectedCount.setText("(0 ứng dụng)");
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                for (NotificationFixHelper.TargetApp app : apps) {
                    View itemView = inflater.inflate(R.layout.item_notif_app, layoutApps, false);
                    itemView.setTag(app);
                    MaterialCheckBox cb = itemView.findViewById(R.id.cb_app_select);
                    ImageView ivIcon = itemView.findViewById(R.id.iv_app_icon);
                    TextView tvName = itemView.findViewById(R.id.tv_app_name);
                    TextView tvBadge = itemView.findViewById(R.id.tv_app_status_badge);
                    TextView tvCategory = itemView.findViewById(R.id.tv_app_category);
                    MaterialButton btnAction = itemView.findViewById(R.id.btn_app_action);

                    if (app.icon != null) ivIcon.setImageDrawable(app.icon);
                    tvName.setText(app.label != null ? app.label : app.defaultName);
                    tvCategory.setText(app.category + " • " + app.packageName);

                    Runnable updateAppBadge = () -> {
                        if (app.isOptimized) {
                            tvBadge.setText("✓ Không giới hạn pin");
                            tvBadge.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.neon_green));
                            tvBadge.setBackgroundResource(R.drawable.bg_badge_green);
                        } else {
                            tvBadge.setText("⚠️ Chưa tối ưu pin");
                            tvBadge.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.mi_orange));
                            tvBadge.setBackgroundResource(R.drawable.bg_badge_orange);
                        }
                    };
                    updateAppBadge.run();

                    cb.setChecked(app.isSelected);
                    cb.setOnCheckedChangeListener((b, isChecked) -> {
                        app.isSelected = isChecked;
                        updateSelectedCounter.run();
                        if (activeFilter[0] == 2) applyFilter.run();
                    });

                    // Tap anywhere on card to toggle selection
                    itemView.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));

                    btnAction.setOnClickListener(v -> {
                        String[] options = hasShizuku
                                ? new String[]{
                                        "⚡ Tối ưu ngay bằng Shizuku (1-Chạm)",
                                        "🔋 Tiết kiệm pin MIUI (Không giới hạn)",
                                        "🚀 Tự khởi chạy MIUI (Autostart)",
                                        "⚙️ Thông tin ứng dụng"
                                }
                                : new String[]{
                                        "🔋 Tiết kiệm pin MIUI (Không giới hạn)",
                                        "🚀 Tự khởi chạy MIUI (Autostart)",
                                        "⚙️ Thông tin ứng dụng"
                                };

                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle(app.label != null ? app.label : app.defaultName)
                                .setItems(options, (dialog, which) -> {
                                    if (hasShizuku && which == 0) {
                                        NotificationFixHelper.fixSingleApp(MainActivity.this, app.packageName, (success, msg) -> {
                                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                                            app.isOptimized = NotificationFixHelper.isIgnoringBatteryOptimizations(MainActivity.this, app.packageName);
                                            updateAppBadge.run();
                                            updateToolsCardSummaries();
                                        });
                                    } else {
                                        int actionIndex = hasShizuku ? which - 1 : which;
                                        if (actionIndex == 0) {
                                            NotificationFixHelper.openAppBatterySaver(MainActivity.this, app.packageName);
                                        } else if (actionIndex == 1) {
                                            NotificationFixHelper.openMiuiAutostart(MainActivity.this);
                                        } else {
                                            NotificationFixHelper.openAppDetails(MainActivity.this, app.packageName);
                                        }
                                    }
                                })
                                .setNegativeButton("Đóng", null)
                                .show();
                    });

                    layoutApps.addView(itemView);
                }

                updateSelectedCounter.run();
                applyFilter.run();
            });
        }).start();

        if (tvSelectAll != null) {
            tvSelectAll.setOnClickListener(v -> {
                boolean anyVisibleUnchecked = false;
                for (int i = 0; i < layoutApps.getChildCount(); i++) {
                    View child = layoutApps.getChildAt(i);
                    if (child.getVisibility() == View.VISIBLE) {
                        NotificationFixHelper.TargetApp a = (NotificationFixHelper.TargetApp) child.getTag();
                        if (a != null && !a.isSelected) {
                            anyVisibleUnchecked = true;
                            break;
                        }
                    }
                }
                boolean targetState = anyVisibleUnchecked;
                for (int i = 0; i < layoutApps.getChildCount(); i++) {
                    View child = layoutApps.getChildAt(i);
                    if (child.getVisibility() == View.VISIBLE) {
                        MaterialCheckBox cb = child.findViewById(R.id.cb_app_select);
                        if (cb != null) cb.setChecked(targetState);
                    }
                }
            });
        }

        btnBatchFix.setOnClickListener(v -> {
            List<String> selectedPkgs = new ArrayList<>();
            for (NotificationFixHelper.TargetApp app : apps) {
                if (app.isSelected) selectedPkgs.add(app.packageName);
            }

            if (selectedPkgs.isEmpty()) {
                Toast.makeText(MainActivity.this, "Vui lòng chọn ít nhất 1 ứng dụng!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ShizukuHelper.hasShizukuPermission()) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Cần Quyền Shizuku Để Tự Động Hóa")
                        .setMessage("Để tự động cấp quyền Tự khởi chạy và Gỡ bỏ giới hạn pin cho hàng loạt ứng dụng trong 1 click, bạn cần cấp quyền Shizuku cho Mi Tool.\n\nNếu chưa có Shizuku, bạn có thể bấm nút 'Tự khởi chạy MIUI' hoặc 'Tiết kiệm pin MIUI' để cấu hình thủ công.")
                        .setPositiveButton("Cấp quyền Shizuku", (dialog, which) -> {
                            ShizukuHelper.requestShizukuPermission(MainActivity.this);
                        })
                        .setNeutralButton("Mở Tự khởi chạy", (dialog, which) -> {
                            NotificationFixHelper.openMiuiAutostart(MainActivity.this);
                        })
                        .setNegativeButton("Đóng", null)
                        .show();
                return;
            }

            btnBatchFix.setEnabled(false);
            btnBatchFix.setText("⏳ Đang tối ưu hóa...");

            NotificationFixHelper.applyBatchFix(MainActivity.this, selectedPkgs, new NotificationFixHelper.FixCallback() {
                @Override
                public void onProgress(int current, int total, String appName) {
                    btnBatchFix.setText("⏳ Đang xử lý (" + current + "/" + total + ")...");
                }

                @Override
                public void onComplete(boolean success, int count, String message) {
                    btnBatchFix.setEnabled(true);
                    btnBatchFix.setText("⚡ 1-CHẠM FIX TẤT CẢ ỨNG DỤNG ĐÃ CHỌN");
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();

                    // Refresh all item badges on the screen in-place
                    for (int i = 0; i < layoutApps.getChildCount(); i++) {
                        View item = layoutApps.getChildAt(i);
                        NotificationFixHelper.TargetApp target = (NotificationFixHelper.TargetApp) item.getTag();
                        if (target != null) {
                            target.isOptimized = NotificationFixHelper.isIgnoringBatteryOptimizations(MainActivity.this, target.packageName);
                            TextView badge = item.findViewById(R.id.tv_app_status_badge);
                            if (badge != null) {
                                if (target.isOptimized) {
                                    badge.setText("✓ Không giới hạn pin");
                                    badge.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.neon_green));
                                    badge.setBackgroundResource(R.drawable.bg_badge_green);
                                } else {
                                    badge.setText("⚠️ Chưa tối ưu pin");
                                    badge.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.mi_orange));
                                    badge.setBackgroundResource(R.drawable.bg_badge_orange);
                                }
                            }
                        }
                    }

                    updateToolsCardSummaries();
                }
            });
        });

        btnOpenAutostart.setOnClickListener(v -> NotificationFixHelper.openMiuiAutostart(MainActivity.this));
        btnOpenPowerKeeper.setOnClickListener(v -> NotificationFixHelper.openMiuiBatterySaverList(MainActivity.this));
        if (btnOpenFcmDiag != null) {
            btnOpenFcmDiag.setOnClickListener(v -> NotificationFixHelper.openFcmDiagnostics(MainActivity.this));
        }

        bs.show();
    }

    // =========================================================================
    // REFRESH RATE & PRIVATE DNS & DEBLOATER CORE LOGIC
    // =========================================================================

    private void applyRefreshRate(final int targetRate) {
        if (targetRate > 0 && (targetRate < 30 || targetRate > 240)) {
            Toast.makeText(this, "Tần số quét phải từ 30Hz đến 240Hz!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.preferredRefreshRate = (float) (targetRate > 0 ? targetRate : 0);
                getWindow().setAttributes(lp);
            } catch (Exception ignored) {
            }
        }

        prefs.setTargetRefreshRate(targetRate);

        boolean success;
        if (ShizukuHelper.hasShizukuPermission()) {
            ShizukuHelper.setRefreshRateViaShizuku(targetRate, null);
            success = true;
        } else {
            success = RefreshRateHelper.setRefreshRate(this, targetRate);
        }

        if (success) {
            String msg = (targetRate == 0) ? "Đã đặt tần số quét Mặc định (Auto - Hệ thống tự điều tiết)." : ("Đã cố định tần số quét " + targetRate + "Hz!");
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

            final Handler h = new Handler(Looper.getMainLooper());
            h.postDelayed(() -> { updateToolsCardSummaries(); updateDeviceSpecsDisplay(); }, 400);
            h.postDelayed(() -> { updateToolsCardSummaries(); updateDeviceSpecsDisplay(); }, 1000);
        } else {
            Toast.makeText(this, "Cần cấp quyền WRITE_SECURE_SETTINGS qua Shizuku hoặc Root!", Toast.LENGTH_LONG).show();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("ADB Refresh Rate", RefreshRateHelper.getAdbCommand(targetRate)));
                Toast.makeText(this, "Đã sao chép lệnh ADB để ép tần số quét!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestAddHzTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                android.app.StatusBarManager sbm = (android.app.StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
                if (sbm != null) {
                    sbm.requestAddTileService(
                            new ComponentName(this, RefreshRateTileService.class),
                            getString(R.string.tile_hz_label),
                            android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_tile_hz),
                            getMainExecutor(),
                            result -> {
                                if (result == android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                    Toast.makeText(MainActivity.this, "Đã thêm phím Tần số quét vào thanh điều khiển!", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm phím Tần số quét vào Trung tâm điều khiển")
                .setMessage("Để thêm nút chuyển 60Hz / 120Hz vào thanh trạng thái kéo xuống của HyperOS / MIUI:\n\n"
                        + "1. Kéo thanh trạng thái (Trung tâm điều khiển) xuống.\n"
                        + "2. Bấm vào biểu tượng Sửa / Cây bút (chỉnh sửa ô chức năng).\n"
                        + "3. Tìm ô 'Tần số quét Hz' của Mi Tool ở bên dưới và kéo lên thanh chính.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void applyPrivateDns(String mode, String hostname, final String label) {
        boolean success = PrivateDnsHelper.setPrivateDns(this, mode, hostname);
        if (success) {
            Toast.makeText(this, "Đã áp dụng: " + label, Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateToolsCardSummaries();
                }
            }, 600);
        }
    }

    private void handleDebloatFreezeRecommended() {
        List<String> toFreeze = new ArrayList<>();
        for (DebloatHelper.BloatwareApp app : DebloatHelper.SAFE_BLOATWARE_LIST) {
            if (app.recommended && DebloatHelper.isInstalled(this, app.pkg) && !DebloatHelper.isFrozen(this, app.pkg)) {
                toFreeze.add(app.pkg);
            }
        }
        if (toFreeze.isEmpty()) {
            Toast.makeText(this, "Tất cả ứng dụng rác khuyên dùng đã được đóng băng!", Toast.LENGTH_SHORT).show();
            return;
        }
        DebloatHelper.batchFreeze(this, toFreeze, new ShizukuHelper.CommandCallback() {
            @Override
            public void onComplete(boolean success, final String output) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, output, Toast.LENGTH_LONG).show();
                        updateToolsCardSummaries();
                    }
                });
            }
        });
    }

    private void handleDebloatRestoreAll() {
        List<String> toRestore = new ArrayList<>();
        for (DebloatHelper.BloatwareApp app : DebloatHelper.SAFE_BLOATWARE_LIST) {
            if (DebloatHelper.isInstalled(this, app.pkg) && DebloatHelper.isFrozen(this, app.pkg)) {
                toRestore.add(app.pkg);
            }
        }
        if (toRestore.isEmpty()) {
            Toast.makeText(this, "Không có ứng dụng nào đang bị đóng băng.", Toast.LENGTH_SHORT).show();
            return;
        }
        DebloatHelper.batchRestore(this, toRestore, new ShizukuHelper.CommandCallback() {
            @Override
            public void onComplete(boolean success, final String output) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, output, Toast.LENGTH_LONG).show();
                        updateToolsCardSummaries();
                    }
                });
            }
        });
    }

    private void showDebloaterDialog() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_debloater, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        final LinearLayout itemsContainer = view.findViewById(R.id.bs_layout_debloat_items);
        final TextView tvSummary = view.findViewById(R.id.bs_tv_debloat_summary);
        MaterialButton btnFreezeRec = view.findViewById(R.id.bs_btn_debloat_freeze_rec);
        MaterialButton btnRestoreAll = view.findViewById(R.id.bs_btn_debloat_restore_all);
        MaterialButton btnClose = view.findViewById(R.id.bs_btn_debloat_close);

        btnFreezeRec.setOnClickListener(v -> {
            handleDebloatFreezeRecommended();
            new Handler(Looper.getMainLooper()).postDelayed(() -> populateDebloatList(itemsContainer, tvSummary), 600);
        });

        btnRestoreAll.setOnClickListener(v -> {
            handleDebloatRestoreAll();
            new Handler(Looper.getMainLooper()).postDelayed(() -> populateDebloatList(itemsContainer, tvSummary), 600);
        });

        btnClose.setOnClickListener(v -> bs.dismiss());

        populateDebloatList(itemsContainer, tvSummary);
        bs.show();
    }

    private void populateDebloatList(final LinearLayout itemsContainer, final TextView tvSummary) {
        if (itemsContainer == null) return;
        itemsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        int totalFound = 0;
        int activeCount = 0;
        int frozenCount = 0;

        for (final DebloatHelper.BloatwareApp app : DebloatHelper.SAFE_BLOATWARE_LIST) {
            boolean installed = DebloatHelper.isInstalled(this, app.pkg);
            if (!installed) continue;

            totalFound++;
            final boolean frozen = DebloatHelper.isFrozen(this, app.pkg);
            if (frozen) {
                frozenCount++;
            } else {
                activeCount++;
            }

            View itemView = inflater.inflate(R.layout.dialog_debloater_item, itemsContainer, false);

            TextView tvName = itemView.findViewById(R.id.tv_debloat_app_name);
            TextView tvPkg = itemView.findViewById(R.id.tv_debloat_pkg);
            TextView tvBadge = itemView.findViewById(R.id.tv_debloat_status_badge);
            TextView tvDesc = itemView.findViewById(R.id.tv_debloat_desc);
            final MaterialButton btnToggle = itemView.findViewById(R.id.btn_toggle_freeze);

            tvName.setText(app.name + (app.recommended ? " • Khuyên tắt" : " • Tùy chọn"));
            tvPkg.setText(app.pkg);
            tvDesc.setText(app.desc);

            if (frozen) {
                tvBadge.setText("❄️ ĐÃ ĐÓNG BĂNG");
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
                btnToggle.setText("🔄 Kích hoạt lại ứng dụng");
                btnToggle.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
                btnToggle.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.neon_green)));
            } else {
                tvBadge.setText("🟢 ĐANG CHẠY");
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.mi_orange));
                btnToggle.setText("❄️ Đóng băng ứng dụng");
                btnToggle.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
                btnToggle.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.neon_cyan)));
            }

            btnToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    btnToggle.setEnabled(false);
                    boolean newFreeze = !frozen;
                    DebloatHelper.setPackageState(MainActivity.this, app.pkg, newFreeze, new ShizukuHelper.CommandCallback() {
                        @Override
                        public void onComplete(boolean success, final String output) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, output, Toast.LENGTH_SHORT).show();
                                    updateToolsCardSummaries();
                                    populateDebloatList(itemsContainer, tvSummary);
                                }
                            });
                        }
                    });
                }
            });

            itemsContainer.addView(itemView);
        }

        if (tvSummary != null) {
            tvSummary.setText(String.format(Locale.getDefault(), "Phát hiện: %d ứng dụng • Đang chạy: %d • Đã đóng băng: %d", totalFound, activeCount, frozenCount));
        }

        if (itemsContainer.getChildCount() == 0) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("Tuyệt vời! Không tìm thấy ứng dụng rác nào trên thiết bị này.");
            emptyTv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            emptyTv.setPadding(0, 30, 0, 30);
            itemsContainer.addView(emptyTv);
        }
    }

    private void applyFloatingPreset(int index) {
        prefs.setFloatingClockPreset(index);
        int statusBarHeight = getStatusBarHeight();
        switch (index) {
            case 0:
                prefs.setFloatingClockX(16);
                prefs.setFloatingClockY(statusBarHeight + 4);
                break;
            case 1:
                prefs.setFloatingClockX(140);
                prefs.setFloatingClockY(statusBarHeight + 4);
                break;
            case 2:
                DisplayMetrics dm = getResources().getDisplayMetrics();
                prefs.setFloatingClockX((dm.widthPixels / 2) - 100);
                prefs.setFloatingClockY(statusBarHeight + 6);
                break;
            case 3:
                prefs.setFloatingClockLocked(false);
                break;
        }
        if (prefs.isFloatingClockEnabled()) {
            startFloatingClockService();
        }
    }

    private void updatePresetButtonsHighlight(MaterialButton b0, MaterialButton b1, MaterialButton b2, MaterialButton b3, int active) {
        MaterialButton[] btns = {b0, b1, b2, b3};
        int activeColor = ContextCompat.getColor(this, R.color.neon_green);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);
        int activeBg = ContextCompat.getColor(this, R.color.card_highlight);
        int normalBg = ContextCompat.getColor(this, R.color.card_dark);
        int strokeWidth = (int) (1.5f * getResources().getDisplayMetrics().density);

        for (int i = 0; i < btns.length; i++) {
            if (btns[i] == null) continue;
            boolean isCur = (i == active);
            btns[i].setTextColor(isCur ? activeColor : inactiveColor);
            btns[i].setBackgroundTintList(ColorStateList.valueOf(isCur ? activeBg : normalBg));
            btns[i].setStrokeColor(ColorStateList.valueOf(isCur ? activeColor : normalBg));
            btns[i].setStrokeWidth(isCur ? strokeWidth : 0);
        }
    }

    private void requestAddClockTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            StatusBarManager sbm = getSystemService(StatusBarManager.class);
            if (sbm != null) {
                sbm.requestAddTileService(
                        new ComponentName(this, FloatingClockTileService.class),
                        getString(R.string.tile_clock_label),
                        Icon.createWithResource(this, R.drawable.ic_tile_clock),
                        getMainExecutor(),
                        resultCode -> {
                            if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                Toast.makeText(this, "Đã thêm phím Đồng hồ vào Trung tâm điều khiển thành công!", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
                return;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm phím vào Trung tâm điều khiển")
                .setMessage("Để thêm phím tắt Đồng hồ giây:\n\n1. Vuốt mở Trung tâm điều khiển (Bảng điều khiển).\n2. Chạm vào biểu tượng Chỉnh sửa (hình bút chì).\n3. Kéo ô 'Đồng hồ giây' lên danh sách phím kích hoạt.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    // =========================================================================
    // TAB 3: BATTERY MONITORING & SCREEN ON TIME
    // =========================================================================

    private void setupBatteryControls() {
        if (btnOpenBatterySecretInfo != null) {
            btnOpenBatterySecretInfo.setOnClickListener(v -> {
                String[] options = {
                    "🛠️ Menu CIT Phần Cứng (*#*#6484#*#*)",
                    "🔋 Thông Tin Kiểm Tra Pin (*#*#4636#*#*)"
                };
                new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("Thông Tin Bí Mật Pin & CIT")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            XiaomiHelper.openCITHardwareTest(MainActivity.this);
                        } else {
                            XiaomiHelper.openHiddenBatteryInfo(MainActivity.this);
                        }
                    })
                    .setNegativeButton("Đóng", null)
                    .show();
            });
        }

        if (btnResetSot != null) {
            btnResetSot.setOnClickListener(v -> {
                int level = BatteryHelper.getBatteryPercentage(this);
                SotHelper.resetSot(this, level);
                updateSotView();
                loadBatteryDrainApps();
                app.cd.mitool.widget.SotAppWidgetProvider.updateAllWidgets(this);
                Toast.makeText(this, "Đã đặt lại mốc tính thời gian sáng màn hình!", Toast.LENGTH_SHORT).show();
            });
        }

        MaterialButton btnCustomizeSotWidget = findViewById(R.id.btn_customize_sot_widget);
        if (btnCustomizeSotWidget != null) {
            btnCustomizeSotWidget.setOnClickListener(v -> showWidgetCustomizeDialog());
        }

        if (btnRefreshDrainApps != null) {
            btnRefreshDrainApps.setOnClickListener(v -> {
                updateSotView();
                loadBatteryDrainApps();
                Toast.makeText(this, "Đã làm mới danh sách tiêu thụ pin", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnDrainFilterUnplug != null && btnDrainFilterToday != null) {
            btnDrainFilterUnplug.setStrokeWidth(0);
            btnDrainFilterToday.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_dark_stroke)));
            btnDrainFilterToday.setStrokeWidth(2);
            btnDrainFilterToday.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            btnDrainFilterUnplug.setOnClickListener(v -> {
                currentDrainFilter = BatteryUsageHelper.FILTER_SINCE_UNPLUG;
                btnDrainFilterUnplug.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.mi_orange)));
                btnDrainFilterUnplug.setTextColor(ContextCompat.getColor(this, R.color.white));
                btnDrainFilterUnplug.setStrokeWidth(0);
                btnDrainFilterToday.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_highlight)));
                btnDrainFilterToday.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                btnDrainFilterToday.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_dark_stroke)));
                btnDrainFilterToday.setStrokeWidth(2);
                loadBatteryDrainApps();
            });

            btnDrainFilterToday.setOnClickListener(v -> {
                currentDrainFilter = BatteryUsageHelper.FILTER_TODAY;
                btnDrainFilterToday.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.mi_orange)));
                btnDrainFilterToday.setTextColor(ContextCompat.getColor(this, R.color.white));
                btnDrainFilterToday.setStrokeWidth(0);
                btnDrainFilterUnplug.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_highlight)));
                btnDrainFilterUnplug.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                btnDrainFilterUnplug.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_dark_stroke)));
                btnDrainFilterUnplug.setStrokeWidth(2);
                loadBatteryDrainApps();
            });
        }

        if (btnDrainGrantShizuku != null) {
            btnDrainGrantShizuku.setOnClickListener(v -> {
                if (!ShizukuHelper.hasShizukuPermission()) {
                    Toast.makeText(this, "Vui lòng mở Shizuku và cấp quyền cho Mi Tool trước!", Toast.LENGTH_SHORT).show();
                    ShizukuHelper.requestShizukuPermission(this);
                    return;
                }
                ShizukuHelper.grantUsageStats(this, (success, msg) -> {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    if (success) {
                        updateSotView();
                        loadBatteryDrainApps();
                    }
                });
            });
        }

        if (btnDrainOpenSettings != null) {
            btnDrainOpenSettings.setOnClickListener(v -> BatteryUsageHelper.openUsageAccessSettings(this));
        }

        if (btnTestCable != null) {
            btnTestCable.setOnClickListener(v -> {
                if (ChargingTesterHelper.isTesting()) {
                    ChargingTesterHelper.stopCableTest();
                    btnTestCable.setText("🔍 Đo chất lượng (10s)");
                    if (pbCableTest != null) pbCableTest.setVisibility(View.GONE);
                    Toast.makeText(this, "Đã hủy kiểm tra củ cáp", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnTestCable.setEnabled(false);
                btnTestCable.setText("⏳ Đang đo (0/10s)...");
                if (pbCableTest != null) {
                    pbCableTest.setVisibility(View.VISIBLE);
                    pbCableTest.setProgress(0);
                }
                if (layoutCableTestResult != null) {
                    layoutCableTestResult.setVisibility(View.GONE);
                }

                ChargingTesterHelper.startCableTest(this, new ChargingTesterHelper.CableTestListener() {
                    @Override
                    public void onProgress(int currentSample, int totalSamples, ChargingTesterHelper.RealtimeChargingState liveData) {
                        runOnUiThread(() -> {
                            btnTestCable.setEnabled(true);
                            btnTestCable.setText(String.format(Locale.getDefault(), "⏳ Đang đo (%d/%ds)...", currentSample, totalSamples));
                            if (pbCableTest != null) {
                                pbCableTest.setProgress(currentSample);
                            }
                            updateChargingTesterUI(liveData);
                        });
                    }

                    @Override
                    public void onComplete(ChargingTesterHelper.CableTestResult result) {
                        runOnUiThread(() -> {
                            btnTestCable.setText("🔄 Đo lại củ cáp (10s)");
                            btnTestCable.setEnabled(true);
                            if (pbCableTest != null) {
                                pbCableTest.setVisibility(View.GONE);
                            }
                            if (layoutCableTestResult != null) {
                                layoutCableTestResult.setVisibility(View.VISIBLE);
                            }
                            if (tvCableRatingStars != null) {
                                tvCableRatingStars.setText(result.stars + " " + result.title);
                                tvCableRatingStars.setTextColor(ContextCompat.getColor(MainActivity.this, result.color));
                            }
                            if (tvCableRatingDesc != null) {
                                tvCableRatingDesc.setText(result.description);
                            }
                            if (tvCableRatingMetrics != null) {
                                tvCableRatingMetrics.setText(String.format(Locale.getDefault(),
                                        "Dòng đỉnh: %d mA • Dòng TB: %d mA • Ổn định: %d%% • Công suất: %.1f W",
                                        result.maxCurrentMa, result.avgCurrentMa, result.stabilityPercent, result.avgPowerW));
                            }
                            Toast.makeText(MainActivity.this, "Đã hoàn thành đánh giá chất lượng củ cáp!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            btnTestCable.setText("🔍 Đo chất lượng (10s)");
                            btnTestCable.setEnabled(true);
                            if (pbCableTest != null) {
                                pbCableTest.setVisibility(View.GONE);
                            }
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            });
        }

        updateSotView();
        loadBatteryDrainApps();
    }

    private void updateSotView() {
        if (tvSotDuration == null) return;
        long sotMillis = SotHelper.getScreenOnTimeMillis(this);
        tvSotDuration.setText(SotHelper.formatDuration(sotMillis));

        if (tvSotTotalUsed != null) {
            long lastUnplug = prefs.getSotLastUnplugTime();
            long now = System.currentTimeMillis();
            if (lastUnplug <= 0 || lastUnplug > now) {
                tvSotTotalUsed.setText("Vừa mới đây");
            } else {
                long diff = now - lastUnplug;
                if (diff < 60000L) {
                    tvSotTotalUsed.setText("Vừa mới rút");
                } else if (diff > 7 * 86400000L) {
                    tvSotTotalUsed.setText("> 7 ngày");
                } else {
                    tvSotTotalUsed.setText(SotHelper.formatDuration(diff));
                }
            }
        }

        if (tvSotUnplugInfo != null) {
            int level = BatteryHelper.getBatteryPercentage(this);
            tvSotUnplugInfo.setText(SotHelper.getUnplugSummary(this, level));
        }

        // Update Battery Estimator (On-screen & Mixed duration)
        SotHelper.BatteryEstimate estimate = SotHelper.getBatteryEstimate(this);
        if (tvSotEstimateScreenOn != null) {
            tvSotEstimateScreenOn.setText("~" + estimate.formattedSot);
        }
        if (tvSotEstimateMixed != null) {
            tvSotEstimateMixed.setText("~" + estimate.formattedMixed);
        }
        if (tvSotDrainRate != null) {
            tvSotDrainRate.setText(estimate.formattedRate);
        }

        updateDrainChart();
        app.cd.mitool.widget.SotAppWidgetProvider.updateAllWidgets(this);
    }

    private void updateDrainChart() {
        if (chartBatteryDrain == null) return;
        List<BatteryHistoryHelper.BatteryPoint> history = BatteryHistoryHelper.getHistory(this);
        chartBatteryDrain.setData(history);

        if (tvDrainChartSummary != null) {
            BatteryHistoryHelper.DrainAnalysis analysis = BatteryHistoryHelper.analyzeDrain(history);
            tvDrainChartSummary.setText(analysis.summary);
            if (analysis.hasAbnormalDrain) {
                tvDrainChartSummary.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
            } else {
                tvDrainChartSummary.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        }
    }

    private void loadBatteryDrainApps() {
        if (layoutDrainAppsContainer == null) return;

        if (tvDrainAppsSubtitle != null) {
            if (currentDrainFilter == BatteryUsageHelper.FILTER_SINCE_UNPLUG) {
                tvDrainAppsSubtitle.setText("Tính từ lần rút sạc gần nhất (hoặc từ khi đặt lại mốc)");
            } else {
                tvDrainAppsSubtitle.setText("Tổng hợp toàn bộ hoạt động từ 00:00 sáng hôm nay");
            }
        }

        boolean hasPerm = BatteryUsageHelper.hasUsageAccessPermission(this);
        if (!hasPerm) {
            if (layoutDrainPermNeeded != null) layoutDrainPermNeeded.setVisibility(View.VISIBLE);
            layoutDrainAppsContainer.removeAllViews();
            if (tvDrainAppsEmpty != null) tvDrainAppsEmpty.setVisibility(View.GONE);
            return;
        }

        if (layoutDrainPermNeeded != null) layoutDrainPermNeeded.setVisibility(View.GONE);
        if (tvDrainAppsEmpty != null) {
            tvDrainAppsEmpty.setVisibility(View.VISIBLE);
            tvDrainAppsEmpty.setText("Đang phân tích dữ liệu ứng dụng & tiêu thụ pin...");
        }

        new Thread(() -> {
            List<BatteryUsageHelper.AppDrainItem> items = BatteryUsageHelper.getDrainApps(this, currentDrainFilter, 12);
            runOnUiThread(() -> renderDrainAppsList(items));
        }).start();
    }

    private void renderDrainAppsList(List<BatteryUsageHelper.AppDrainItem> items) {
        if (layoutDrainAppsContainer == null) return;
        layoutDrainAppsContainer.removeAllViews();

        if (items == null || items.isEmpty()) {
            if (tvDrainAppsEmpty != null) {
                tvDrainAppsEmpty.setVisibility(View.VISIBLE);
                tvDrainAppsEmpty.setText("Chưa ghi nhận ứng dụng tiêu hao pin đáng kể.");
            }
            return;
        }

        if (tvDrainAppsEmpty != null) {
            tvDrainAppsEmpty.setVisibility(View.GONE);
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < items.size(); i++) {
            final BatteryUsageHelper.AppDrainItem item = items.get(i);
            View itemView = inflater.inflate(R.layout.item_battery_drain_app, layoutDrainAppsContainer, false);

            TextView tvRank = itemView.findViewById(R.id.item_tv_rank);
            ImageView ivIcon = itemView.findViewById(R.id.item_iv_icon);
            TextView tvName = itemView.findViewById(R.id.item_tv_name);
            TextView tvDuration = itemView.findViewById(R.id.item_tv_duration);
            TextView tvPercentage = itemView.findViewById(R.id.item_tv_percentage);
            TextView tvEstBattery = itemView.findViewById(R.id.item_tv_est_battery);
            ProgressBar pbProgress = itemView.findViewById(R.id.item_pb_progress);
            MaterialButton btnStop = itemView.findViewById(R.id.item_btn_stop);

            // Rank format (medal or number)
            if (tvRank != null) {
                if (item.rank == 1) {
                    tvRank.setText("🥇");
                } else if (item.rank == 2) {
                    tvRank.setText("🥈");
                } else if (item.rank == 3) {
                    tvRank.setText("🥉");
                } else {
                    tvRank.setText(String.valueOf(item.rank));
                }
            }

            // App Icon
            if (ivIcon != null) {
                if (item.icon != null) {
                    ivIcon.setImageDrawable(item.icon);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_app_icon);
                }
            }

            // App info
            if (tvName != null) tvName.setText(item.appName);
            if (tvDuration != null) tvDuration.setText(item.formattedTime);
            if (tvPercentage != null) tvPercentage.setText("(" + item.percentage + "%)");
            if (tvEstBattery != null) {
                tvEstBattery.setText(String.format(Locale.getDefault(), "~%.1f%%", item.estimatedBatteryDrain));
            }

            // Progress bar
            if (pbProgress != null) {
                pbProgress.setMax(100);
                pbProgress.setProgress(item.percentage);
                int tintRes = item.percentage > 25 ? R.color.mi_orange : R.color.neon_cyan;
                pbProgress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, tintRes)));
            }

            // Stop button (Compact close icon button)
            if (btnStop != null) {
                btnStop.setOnClickListener(v -> {
                    BatteryUsageHelper.forceStopApp(MainActivity.this, item.packageName, (success, msg) -> {
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        if (success) {
                            btnStop.setIconResource(R.drawable.ic_check_circle);
                            btnStop.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.neon_green)));
                            btnStop.setEnabled(false);
                        }
                    });
                });
            }

            layoutDrainAppsContainer.addView(itemView);
        }
    }

    private void updateBatteryDisplay() {
        if (tvBatteryPercent == null) return;
        int level = BatteryHelper.getBatteryPercentage(this);
        boolean isCharging = BatteryHelper.isCharging(this);
        int plugType = BatteryHelper.getPlugType(this);
        float temp = BatteryHelper.getBatteryTemperature(this);
        float volt = BatteryHelper.getBatteryVoltage(this);

        tvBatteryPercent.setText(level + "%");
        tvBatteryTemp.setText(String.format(Locale.getDefault(), "%.1f °C", temp));
        tvBatteryVoltage.setText(String.format(Locale.getDefault(), "%.2f V", volt));

        if (isCharging) {
            String plugStr = (plugType == BatteryManager.BATTERY_PLUGGED_AC) ? "Củ sạc tường (AC)"
                    : (plugType == BatteryManager.BATTERY_PLUGGED_USB) ? "Cổng USB"
                    : (plugType == BatteryManager.BATTERY_PLUGGED_WIRELESS) ? "Sạc không dây"
                    : "Đang sạc";
            tvBatteryPlugType.setText("Nguồn: " + plugStr);

            String eta = BatteryHelper.getEstimatedChargeTime(this);
            tvChargeEta.setText(eta != null ? eta : "Đang ước tính...");
        } else {
            tvBatteryPlugType.setText("Nguồn: Pin thiết bị");
            tvChargeEta.setText("Chưa cắm sạc");
        }

        updateBatteryPowerRealtime();
        updateChargingTesterUI(null);
        updateSotView();
    }

    private void updateBatteryPowerRealtime() {
        if (tvBatteryPower == null) return;
        float powerW = BatteryHelper.getBatteryPowerWatts(this);
        int currentMa = BatteryHelper.getBatteryCurrentNow(this);
        boolean isCharging = BatteryHelper.isCharging(this);

        if (powerW > 0.0f) {
            String prefix = isCharging ? "+" : "-";
            tvBatteryPower.setText(String.format(Locale.getDefault(), "%s%.1f W", prefix, powerW));
            tvBatteryPower.setTextColor(ContextCompat.getColor(this, isCharging ? R.color.neon_green : R.color.neon_cyan));
        } else {
            tvBatteryPower.setText("0.0 W");
            tvBatteryPower.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        }

        if (tvChargingCurrentLabel != null) {
            tvChargingCurrentLabel.setText(isCharging ? "🔌 Dòng sạc" : "🔋 Dòng xả");
        }

        if (tvChargingCurrentVal != null) {
            if (currentMa > 0) {
                String prefix = isCharging ? "+" : "";
                tvChargingCurrentVal.setText(prefix + Math.abs(currentMa) + " mA");
                tvChargingCurrentVal.setTextColor(ContextCompat.getColor(this, isCharging ? R.color.neon_green : R.color.neon_cyan));
            } else {
                tvChargingCurrentVal.setText("-- mA");
                tvChargingCurrentVal.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            }
        }

        // Battery Capacity & Health Specs
        BatteryHelper.BatteryCapacityData capData = BatteryHelper.getBatteryCapacityData(this);
        if (tvBatteryDesignCapacity != null) {
            tvBatteryDesignCapacity.setText(capData.designMah + " mAh");
        }
        if (tvBatteryActualCapacity != null) {
            tvBatteryActualCapacity.setText(String.format(Locale.getDefault(), "~%d mAh (%d%%)",
                    capData.actualMah, Math.round(capData.healthPercent)));
        }
        if (tvBatteryCycleCount != null) {
            if (capData.cycleCount > 0) {
                tvBatteryCycleCount.setText(capData.cycleCount + " chu kỳ");
            } else {
                tvBatteryCycleCount.setText("Xem tại CIT");
            }
        }
    }

    private void updateChargingTesterUI(ChargingTesterHelper.RealtimeChargingState overrideState) {
        ChargingTesterHelper.RealtimeChargingState state = overrideState != null
                ? overrideState
                : ChargingTesterHelper.getRealtimeChargingState(this);

        if (tvChargingProtocolBadge != null) {
            tvChargingProtocolBadge.setText(state.protocolBadge);
            tvChargingProtocolBadge.setTextColor(ContextCompat.getColor(this, state.protocolColor));
        }

        if (tvChargingCurrentLabel != null) {
            tvChargingCurrentLabel.setText(state.isCharging ? "🔌 Dòng sạc" : "🔋 Dòng xả");
        }

        if (tvChargingCurrentVal != null && state.currentMa > 0) {
            String prefix = state.isCharging ? "+" : "";
            tvChargingCurrentVal.setText(prefix + state.currentMa + " mA");
            tvChargingCurrentVal.setTextColor(ContextCompat.getColor(this, state.isCharging ? R.color.neon_green : R.color.neon_cyan));
        }
    }

    private void registerBatteryMonitorReceiver() {
        if (batteryReceiver == null) {
            batteryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateBatteryDisplay();
                    updateDeviceSpecsDisplay();
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
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

    private int batteryRealtimeTickCounter = 0;

    private void startBatteryRealtimeTicker() {
        if (batteryTickerRunnable == null) {
            batteryRealtimeTickCounter = 0;
            batteryTickerRunnable = new Runnable() {
                @Override
                public void run() {
                    updateBatteryPowerRealtime();
                    updateChargingTesterUI(null);
                    batteryRealtimeTickCounter++;
                    if (batteryRealtimeTickCounter % 15 == 0) {
                        updateSotView();
                    }
                    batteryTickerHandler.postDelayed(this, 1000);
                }
            };
            batteryTickerHandler.postDelayed(batteryTickerRunnable, 1000);
        }
    }

    private void stopBatteryRealtimeTicker() {
        if (batteryTickerRunnable != null) {
            batteryTickerHandler.removeCallbacks(batteryTickerRunnable);
            batteryTickerRunnable = null;
        }
    }

    // =========================================================================
    // TAB 4: SETTINGS & SHIZUKU
    // =========================================================================

    private void setupSettingsControls() {
        setupThemeSelection();
        updateSettingsVersionDisplay();

        if (btnCheckUpdateNow != null) {
            btnCheckUpdateNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    btnCheckUpdateNow.setEnabled(false);
                    btnCheckUpdateNow.setText("⏳ Đang kết nối GitHub...");
                    UpdateHelper.checkForUpdate(MainActivity.this, true, new UpdateHelper.UpdateCheckListener() {
                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onFinished(final boolean hasUpdate, final boolean success) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnCheckUpdateNow.setEnabled(true);
                                    btnCheckUpdateNow.setText("⚡ Kiểm tra bản cập nhật mới ngay");
                                    if (success) {
                                        prefs.setLastUpdateCheckTime(System.currentTimeMillis());
                                        updateSettingsVersionDisplay();
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }

        if (btnPermAutostart != null) {
            btnPermAutostart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    XiaomiHelper.openAutostartSettings(MainActivity.this);
                }
            });
        }

        if (btnPermBatterySaver != null) {
            btnPermBatterySaver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    XiaomiHelper.openBatterySaverSettings(MainActivity.this);
                }
            });
        }
    }

    private void setupThemeSelection() {
        updateThemeButtonsHighlight(prefs.getAppThemeMode());

        if (btnThemeSystem != null) {
            btnThemeSystem.setOnClickListener(v -> selectTheme(PreferencesHelper.THEME_MODE_SYSTEM));
        }
        if (btnThemeLight != null) {
            btnThemeLight.setOnClickListener(v -> selectTheme(PreferencesHelper.THEME_MODE_LIGHT));
        }
        if (btnThemeDark != null) {
            btnThemeDark.setOnClickListener(v -> selectTheme(PreferencesHelper.THEME_MODE_DARK));
        }

        MaterialButton btnSettingsCustomizeWidget = findViewById(R.id.btn_settings_customize_widget);
        if (btnSettingsCustomizeWidget != null) {
            btnSettingsCustomizeWidget.setOnClickListener(v -> showWidgetCustomizeDialog());
        }
    }

    private void selectTheme(int mode) {
        prefs.setAppThemeMode(mode);
        updateThemeButtonsHighlight(mode);
        int nightMode;
        switch (mode) {
            case PreferencesHelper.THEME_MODE_LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case PreferencesHelper.THEME_MODE_DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case PreferencesHelper.THEME_MODE_SYSTEM:
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        } else {
            recreate();
        }
    }

    private void updateThemeButtonsHighlight(int mode) {
        int activeBg = ContextCompat.getColor(this, R.color.mi_orange);
        int inactiveBg = ContextCompat.getColor(this, R.color.card_highlight);
        int activeText = ContextCompat.getColor(this, R.color.white);
        int inactiveText = ContextCompat.getColor(this, R.color.text_primary);
        int strokeColor = ContextCompat.getColor(this, R.color.card_dark_stroke);

        if (btnThemeSystem != null) {
            boolean isSel = mode == PreferencesHelper.THEME_MODE_SYSTEM;
            btnThemeSystem.setBackgroundTintList(ColorStateList.valueOf(isSel ? activeBg : inactiveBg));
            btnThemeSystem.setTextColor(isSel ? activeText : inactiveText);
            btnThemeSystem.setStrokeColor(ColorStateList.valueOf(strokeColor));
            btnThemeSystem.setStrokeWidth(isSel ? 0 : 2);
        }
        if (btnThemeLight != null) {
            boolean isSel = mode == PreferencesHelper.THEME_MODE_LIGHT;
            btnThemeLight.setBackgroundTintList(ColorStateList.valueOf(isSel ? activeBg : inactiveBg));
            btnThemeLight.setTextColor(isSel ? activeText : inactiveText);
            btnThemeLight.setStrokeColor(ColorStateList.valueOf(strokeColor));
            btnThemeLight.setStrokeWidth(isSel ? 0 : 2);
        }
        if (btnThemeDark != null) {
            boolean isSel = mode == PreferencesHelper.THEME_MODE_DARK;
            btnThemeDark.setBackgroundTintList(ColorStateList.valueOf(isSel ? activeBg : inactiveBg));
            btnThemeDark.setTextColor(isSel ? activeText : inactiveText);
            btnThemeDark.setStrokeColor(ColorStateList.valueOf(strokeColor));
            btnThemeDark.setStrokeWidth(isSel ? 0 : 2);
        }

        if (tvThemeCurrentBadge != null) {
            switch (mode) {
                case PreferencesHelper.THEME_MODE_LIGHT:
                    tvThemeCurrentBadge.setText("Sáng");
                    break;
                case PreferencesHelper.THEME_MODE_DARK:
                    tvThemeCurrentBadge.setText("Tối");
                    break;
                case PreferencesHelper.THEME_MODE_SYSTEM:
                default:
                    tvThemeCurrentBadge.setText("Hệ thống");
                    break;
            }
        }
    }

    private void updateSettingsVersionDisplay() {
        if (tvSettingsCurrentVersion != null) {
            tvSettingsCurrentVersion.setText("v" + UpdateHelper.getCurrentVersion(this));
        }
        if (tvSettingsUpdateBadge != null) {
            tvSettingsUpdateBadge.setText("CHÍNH THỨC");
            tvSettingsUpdateBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
        }
        if (tvSettingsLastCheck != null) {
            long last = prefs.getLastUpdateCheckTime();
            if (last > 0) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm - dd/MM", java.util.Locale.getDefault());
                tvSettingsLastCheck.setText(sdf.format(new java.util.Date(last)));
            } else {
                tvSettingsLastCheck.setText("Vừa xong");
            }
        }
    }

    private void applyAppTheme(int themeMode) {
        int nightMode;
        switch (themeMode) {
            case PreferencesHelper.THEME_MODE_LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case PreferencesHelper.THEME_MODE_DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case PreferencesHelper.THEME_MODE_SYSTEM:
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        }
    }

    private void setupShizukuControls() {
        if (btnShizukuGrantAuto != null) {
            btnShizukuGrantAuto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!ShizukuHelper.isShizukuInstalled(MainActivity.this)) {
                        Toast.makeText(MainActivity.this, "Đang mở trang tải Shizuku...", Toast.LENGTH_SHORT).show();
                        ShizukuHelper.openShizukuDownload(MainActivity.this);
                        return;
                    }
                    if (!ShizukuHelper.isShizukuRunning()) {
                        Toast.makeText(MainActivity.this, "Shizuku chưa chạy. Vui lòng mở Shizuku và bấm Bắt đầu (Start) trước!", Toast.LENGTH_LONG).show();
                        ShizukuHelper.openShizukuApp(MainActivity.this);
                        return;
                    }
                    if (!ShizukuHelper.hasShizukuPermission()) {
                        ShizukuHelper.requestShizukuPermission(MainActivity.this);
                        return;
                    }

                    btnShizukuGrantAuto.setEnabled(false);
                    btnShizukuGrantAuto.setText("Đang cấp quyền qua Shizuku...");
                    ShizukuHelper.grantSecureSettings(MainActivity.this, new ShizukuHelper.CommandCallback() {
                        @Override
                        public void onComplete(final boolean success, final String message) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnShizukuGrantAuto.setEnabled(true);
                                    btnShizukuGrantAuto.setText(R.string.btn_shizuku_grant_auto);
                                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                                    updateShizukuDisplay();
                                    updateDeviceSpecsDisplay();
                                    updateToolsCardSummaries();
                                }
                            });
                        }
                    });
                }
            });
        }

        if (btnShizukuOpen != null) {
            btnShizukuOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ShizukuHelper.isShizukuInstalled(MainActivity.this)) {
                        ShizukuHelper.openShizukuApp(MainActivity.this);
                    } else {
                        ShizukuHelper.openShizukuDownload(MainActivity.this);
                    }
                }
            });
        }

        updateShizukuDisplay();
    }

    private void updateShizukuDisplay() {
        if (tvShizukuStatus == null) return;
        boolean installed = ShizukuHelper.isShizukuInstalled(this);
        boolean running = ShizukuHelper.isShizukuRunning();
        boolean hasPerm = ShizukuHelper.hasShizukuPermission();

        if (hasPerm) {
            tvShizukuStatus.setText(R.string.shizuku_status_granted);
            tvShizukuStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            if (tvShizukuBadge != null) {
                tvShizukuBadge.setText("Đã kích hoạt");
                tvShizukuBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            }
            if (btnShizukuGrantAuto != null) {
                btnShizukuGrantAuto.setText("🟢 Đã Cấp Quyền Secure Settings");
                btnShizukuGrantAuto.setEnabled(false);
                btnShizukuGrantAuto.setAlpha(0.6f);
            }
        } else if (running) {
            tvShizukuStatus.setText(R.string.shizuku_status_running_no_perm);
            tvShizukuStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_yellow));
            if (tvShizukuBadge != null) {
                tvShizukuBadge.setText("Chờ cấp quyền");
                tvShizukuBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_yellow));
            }
            if (btnShizukuGrantAuto != null) {
                btnShizukuGrantAuto.setText("⚡ Cấp Quyền Shizuku Cho Mi Tool");
                btnShizukuGrantAuto.setEnabled(true);
                btnShizukuGrantAuto.setAlpha(1.0f);
            }
        } else {
            tvShizukuStatus.setText(installed ? "🟡 Shizuku đã cài nhưng chưa Khởi chạy (Start)" : getString(R.string.shizuku_status_not_running));
            tvShizukuStatus.setTextColor(ContextCompat.getColor(this, installed ? R.color.neon_yellow : R.color.neon_red));
            if (tvShizukuBadge != null) {
                tvShizukuBadge.setText(installed ? "Chưa Start" : "Chưa cài đặt");
                tvShizukuBadge.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            }
            if (btnShizukuGrantAuto != null) {
                btnShizukuGrantAuto.setText(R.string.btn_shizuku_grant_auto);
                btnShizukuGrantAuto.setEnabled(true);
                btnShizukuGrantAuto.setAlpha(1.0f);
            }
        }
    }

    private void startNotificationClockService() {
        Intent intent = new Intent(this, NotificationClockService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopNotificationClockService() {
        Intent intent = new Intent(this, NotificationClockService.class);
        stopService(intent);
    }

    private void startFloatingClockService() {
        Intent intent = new Intent(this, FloatingClockService.class);
        intent.setAction(FloatingClockService.ACTION_RELOAD_SETTINGS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopFloatingClockService() {
        Intent intent = new Intent(this, FloatingClockService.class);
        intent.setAction(FloatingClockService.ACTION_STOP);
        startService(intent);
    }

    private void startFloatingGameHudService() {
        Intent intent = new Intent(this, FloatingGameHudService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopFloatingGameHudService() {
        Intent intent = new Intent(this, FloatingGameHudService.class);
        intent.setAction(FloatingGameHudService.ACTION_STOP);
        startService(intent);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIF_PERM
                    );
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_RINGTONE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                String title = RingtoneManager.getRingtone(this, uri).getTitle(this);
                prefs.setCustomAlertToneUri(uri.toString());
                prefs.setCustomAlertToneTitle(title);
                Toast.makeText(this, "Đã chọn âm báo: " + title, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDeviceSpecsDisplay();
        updateToolsCardSummaries();
        updateBatteryDisplay();
        loadBatteryDrainApps();
        updateShizukuDisplay();
        registerBatteryMonitorReceiver();
        startBatteryRealtimeTicker();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterBatteryMonitorReceiver();
        stopBatteryRealtimeTicker();
        BatteryHelper.stopAlertTone();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBatteryRealtimeTicker();
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener);
            Shizuku.removeBinderReceivedListener(shizukuBinderReceivedListener);
            Shizuku.removeBinderDeadListener(shizukuBinderDeadListener);
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNav != null) {
            outState.putInt("active_tab_id", bottomNav.getSelectedItemId());
        }
    }
}
