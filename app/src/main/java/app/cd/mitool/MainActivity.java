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
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.cd.mitool.service.FloatingClockService;
import app.cd.mitool.service.FloatingClockTileService;
import app.cd.mitool.service.NotificationClockService;
import app.cd.mitool.service.RefreshRateTileService;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.BatteryUsageHelper;
import app.cd.mitool.util.DebloatHelper;
import app.cd.mitool.util.DeviceInfoHelper;
import app.cd.mitool.util.GamingFpsHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.PrivateDnsHelper;
import app.cd.mitool.util.RamBoosterHelper;
import app.cd.mitool.util.RefreshRateHelper;
import app.cd.mitool.util.ShizukuHelper;
import app.cd.mitool.util.ShortcutHelper;
import app.cd.mitool.util.SotHelper;
import app.cd.mitool.util.SystemClockHelper;
import app.cd.mitool.util.UpdateHelper;
import app.cd.mitool.util.VoiceAlertHelper;
import app.cd.mitool.util.VoLteHelper;
import app.cd.mitool.util.XiaomiHelper;
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
    private TextView tvSpecOs;
    private TextView tvSpecAndroid;
    private TextView tvSpecSecurity;
    private TextView tvSpecCpuModel;
    private TextView tvSpecCpuCores;
    private TextView tvSpecCpuArch;
    private TextView tvSpecDisplayRes;
    private TextView tvSpecDisplayHz;
    private TextView tvSpecDisplayDpi;
    private TextView tvSpecRamText;
    private ProgressBar pbSpecRam;
    private TextView tvSpecStorageText;
    private ProgressBar pbSpecStorage;
    private TextView tvSpecBatteryPercent;
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

    private TextView tvToolHzSub;
    private TextView tvToolGameFpsSub;
    private TextView tvToolDebloaterSub;
    private TextView tvToolPrivateDnsSub;
    private TextView tvToolVolteSub;
    private TextView tvToolRamSub;
    private TextView tvToolClockSub;
    private TextView tvToolBatteryAlertSub;
    private TextView tvToolMiuiUtilsSub;

    // Tab 3: Battery Monitoring Views
    private TextView tvBatteryPercent;
    private TextView tvBatteryStatus;
    private TextView tvBatteryPlugType;
    private TextView tvChargeEta;
    private TextView tvBatteryPower;
    private TextView tvBatteryTemp;
    private TextView tvBatteryVoltage;
    private TextView tvBatteryCycleCount;
    private MaterialButton btnOpenBatterySecretInfo;

    // SOT & Battery Drain Monitor Views
    private TextView tvSotDuration;
    private TextView tvSotTotalUsed;
    private TextView tvSotUnplugInfo;
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
    private TextView tvSettingsCurrentVersion;
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
        if (intent != null && intent.getIntExtra("extra_tab_index", -1) == 1) {
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_battery);
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
        tvSpecOs = findViewById(R.id.tv_spec_os);
        tvSpecAndroid = findViewById(R.id.tv_spec_android);
        tvSpecSecurity = findViewById(R.id.tv_spec_security);
        tvSpecCpuModel = findViewById(R.id.tv_spec_cpu_model);
        tvSpecCpuCores = findViewById(R.id.tv_spec_cpu_cores);
        tvSpecCpuArch = findViewById(R.id.tv_spec_cpu_arch);
        tvSpecDisplayRes = findViewById(R.id.tv_spec_display_res);
        tvSpecDisplayHz = findViewById(R.id.tv_spec_display_hz);
        tvSpecDisplayDpi = findViewById(R.id.tv_spec_display_dpi);
        tvSpecRamText = findViewById(R.id.tv_spec_ram_text);
        pbSpecRam = findViewById(R.id.pb_spec_ram);
        tvSpecStorageText = findViewById(R.id.tv_spec_storage_text);
        pbSpecStorage = findViewById(R.id.pb_spec_storage);
        tvSpecBatteryPercent = findViewById(R.id.tv_spec_battery_percent);
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

        tvToolHzSub = findViewById(R.id.tv_tool_hz_sub);
        tvToolGameFpsSub = findViewById(R.id.tv_tool_game_fps_sub);
        tvToolDebloaterSub = findViewById(R.id.tv_tool_debloater_sub);
        tvToolPrivateDnsSub = findViewById(R.id.tv_tool_private_dns_sub);
        tvToolVolteSub = findViewById(R.id.tv_tool_volte_sub);
        tvToolRamSub = findViewById(R.id.tv_tool_ram_sub);
        tvToolClockSub = findViewById(R.id.tv_tool_clock_sub);
        tvToolBatteryAlertSub = findViewById(R.id.tv_tool_battery_alert_sub);
        tvToolMiuiUtilsSub = findViewById(R.id.tv_tool_miui_utils_sub);

        // Battery Views
        tvBatteryPercent = findViewById(R.id.tv_battery_percent);
        tvBatteryStatus = findViewById(R.id.tv_battery_status);
        tvBatteryPlugType = findViewById(R.id.tv_battery_plug_type);
        tvChargeEta = findViewById(R.id.tv_charge_eta);
        tvBatteryPower = findViewById(R.id.tv_battery_power);
        tvBatteryTemp = findViewById(R.id.tv_battery_temp);
        tvBatteryVoltage = findViewById(R.id.tv_battery_voltage);
        tvBatteryCycleCount = findViewById(R.id.tv_battery_cycle_count);
        btnOpenBatterySecretInfo = findViewById(R.id.btn_open_battery_secret_info);

        // SOT & Battery Drain Views
        tvSotDuration = findViewById(R.id.tv_sot_duration);
        tvSotTotalUsed = findViewById(R.id.tv_sot_total_used);
        tvSotUnplugInfo = findViewById(R.id.tv_sot_unplug_info);
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
        tvSettingsCurrentVersion = findViewById(R.id.tv_settings_current_version);
        btnCheckUpdateNow = findViewById(R.id.btn_check_update_now);
        btnPermAutostart = findViewById(R.id.btn_perm_autostart);
        btnPermBatterySaver = findViewById(R.id.btn_perm_battery_saver);

        // Shizuku Views
        tvShizukuStatus = findViewById(R.id.tv_shizuku_status);
        tvShizukuBadge = findViewById(R.id.tv_shizuku_badge);
        btnShizukuGrantAuto = findViewById(R.id.btn_shizuku_grant_auto);
        btnShizukuOpen = findViewById(R.id.btn_shizuku_open);

        if (tvSettingsCurrentVersion != null) {
            tvSettingsCurrentVersion.setText("Phiên bản đang cài: v" + UpdateHelper.getCurrentVersion(this));
        }
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
            btnQuickDns.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyPrivateDns(PrivateDnsHelper.MODE_HOSTNAME, PrivateDnsHelper.DNS_ADGUARD, "AdGuard DNS");
                    updateToolsCardSummaries();
                }
            });
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
        tvSpecOs.setText(os);
        tvSpecAndroid.setText(androidVer);
        tvSpecSecurity.setText(security);

        // Shizuku badge
        if (ShizukuHelper.hasShizukuPermission()) {
            tvDeviceShizukuBadge.setText("🟢 Shizuku đã cấp");
            tvDeviceShizukuBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
        } else if (ShizukuHelper.isShizukuRunning()) {
            tvDeviceShizukuBadge.setText("🟡 Shizuku chờ cấp");
            tvDeviceShizukuBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_yellow));
        } else {
            tvDeviceShizukuBadge.setText("🔴 Shizuku chưa chạy");
            tvDeviceShizukuBadge.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        }

        // CPU / SoC
        tvSpecCpuModel.setText(DeviceInfoHelper.getCpuModel());
        tvSpecCpuCores.setText(DeviceInfoHelper.getCpuCoreCount() + " Nhân (Octa-core)");
        tvSpecCpuArch.setText(DeviceInfoHelper.getCpuArchitecture());

        // Display
        DeviceInfoHelper.DisplayStats ds = DeviceInfoHelper.getDisplayStats(this);
        tvSpecDisplayRes.setText(ds.width + " x " + ds.height + " px");
        tvSpecDisplayHz.setText(ds.refreshRate + " Hz (" + (ds.refreshRate > 60 ? "Mượt mà" : "Tiết kiệm pin") + ")");
        tvSpecDisplayDpi.setText(ds.dpi + " DPI");

        // RAM & Storage
        DeviceInfoHelper.MemoryStats ram = DeviceInfoHelper.getRamStats(this);
        tvSpecRamText.setText(String.format(Locale.getDefault(), "%.1f GB / %.1f GB (%d%%)", ram.usedGb, ram.totalGb, ram.percentUsed));
        pbSpecRam.setProgress(ram.percentUsed);

        DeviceInfoHelper.MemoryStats rom = DeviceInfoHelper.getInternalStorageStats();
        tvSpecStorageText.setText(String.format(Locale.getDefault(), "%.1f GB / %.1f GB (%d%%)", rom.usedGb, rom.totalGb, rom.percentUsed));
        pbSpecStorage.setProgress(rom.percentUsed);

        // Battery Overview
        int level = BatteryHelper.getBatteryPercentage(this);
        boolean isCharging = BatteryHelper.isCharging(this);
        float temp = BatteryHelper.getBatteryTemperature(this);
        float volt = BatteryHelper.getBatteryVoltage(this);

        tvSpecBatteryPercent.setText(level + "% (" + (isCharging ? "Đang sạc" : "Đang dùng pin") + ")");
        tvSpecBatteryTempVoltage.setText(String.format(Locale.getDefault(), "%.1f °C • %.2f V", temp, volt));
        tvSpecBatteryHealth.setText(temp > 40.0f ? "Khá nóng" : "Tốt (Bình thường)");
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
                if (PrivateDnsHelper.DNS_ADGUARD.equalsIgnoreCase(spec)) label = "AdGuard DNS (Chặn QC)";
                else if (PrivateDnsHelper.DNS_CLOUDFLARE_FAMILY.equalsIgnoreCase(spec)) label = "Cloudflare Family";
                else if (PrivateDnsHelper.DNS_CLOUDFLARE.equalsIgnoreCase(spec)) label = "Cloudflare 1.1.1.1";
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
    }

    // --- BottomSheet 1: Refresh Rate ---
    private void showHzBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_hz, null);
        bs.setContentView(view);

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

        bs.show();
    }

    // --- BottomSheet 3: Private DNS ---
    private void showPrivateDnsBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_private_dns, null);
        bs.setContentView(view);

        TextView tvStatus = view.findViewById(R.id.bs_tv_private_dns_status);
        MaterialButton btnAdguard = view.findViewById(R.id.bs_btn_dns_adguard);
        MaterialButton btnFamily = view.findViewById(R.id.bs_btn_dns_cloudflare_family);
        MaterialButton btnCf = view.findViewById(R.id.bs_btn_dns_cloudflare);
        MaterialButton btnOff = view.findViewById(R.id.bs_btn_dns_off);
        MaterialButton btnOpenSettings = view.findViewById(R.id.bs_btn_open_dns_settings);

        String mode = PrivateDnsHelper.getCurrentDnsMode(this);
        String spec = PrivateDnsHelper.getCurrentDnsSpecifier(this);
        if (PrivateDnsHelper.MODE_HOSTNAME.equalsIgnoreCase(mode) && !spec.isEmpty()) {
            tvStatus.setText("🛡️ Đang bật: " + spec);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
        } else if (PrivateDnsHelper.MODE_OFF.equalsIgnoreCase(mode)) {
            tvStatus.setText("⭕ Đang tắt DNS riêng tư");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
        } else {
            tvStatus.setText("⚡ Chế độ Tự động (Mặc định nhà mạng)");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
        }

        btnAdguard.setOnClickListener(v -> {
            applyPrivateDns(PrivateDnsHelper.MODE_HOSTNAME, PrivateDnsHelper.DNS_ADGUARD, "AdGuard DNS");
            bs.dismiss();
        });

        btnFamily.setOnClickListener(v -> {
            applyPrivateDns(PrivateDnsHelper.MODE_HOSTNAME, PrivateDnsHelper.DNS_CLOUDFLARE_FAMILY, "Cloudflare Family");
            bs.dismiss();
        });

        btnCf.setOnClickListener(v -> {
            applyPrivateDns(PrivateDnsHelper.MODE_HOSTNAME, PrivateDnsHelper.DNS_CLOUDFLARE, "Cloudflare 1.1.1.1");
            bs.dismiss();
        });

        btnOff.setOnClickListener(v -> {
            applyPrivateDns(PrivateDnsHelper.MODE_AUTO, "", "DNS Tự động");
            bs.dismiss();
        });

        btnOpenSettings.setOnClickListener(v -> {
            PrivateDnsHelper.openPrivateDnsSettings(MainActivity.this);
            bs.dismiss();
        });

        bs.show();
    }

    // --- BottomSheet 4: VoLTE & VoWiFi ---
    private void showVoLteBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_volte, null);
        bs.setContentView(view);

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

    // --- BottomSheet 8: Hidden MIUI / HyperOS Utils ---
    private void showMiuiUtilsBottomSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_miui_utils, null);
        bs.setContentView(view);

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
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_debloater_list, null);
        final LinearLayout itemsContainer = dialogView.findViewById(R.id.layout_debloat_items);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Đóng", null)
                .setNeutralButton("Khôi phục toàn bộ", (d, which) -> handleDebloatRestoreAll())
                .setNegativeButton("Đóng băng khuyên dùng", (d, which) -> handleDebloatFreezeRecommended())
                .create();

        populateDebloatList(itemsContainer, dialog);
        dialog.show();
    }

    private void populateDebloatList(final LinearLayout itemsContainer, final AlertDialog dialog) {
        itemsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (final DebloatHelper.BloatwareApp app : DebloatHelper.SAFE_BLOATWARE_LIST) {
            boolean installed = DebloatHelper.isInstalled(this, app.pkg);
            if (!installed) continue;

            final boolean frozen = DebloatHelper.isFrozen(this, app.pkg);
            View itemView = inflater.inflate(R.layout.dialog_debloater_item, itemsContainer, false);

            TextView tvName = itemView.findViewById(R.id.tv_debloat_app_name);
            TextView tvPkg = itemView.findViewById(R.id.tv_debloat_pkg);
            TextView tvBadge = itemView.findViewById(R.id.tv_debloat_status_badge);
            TextView tvDesc = itemView.findViewById(R.id.tv_debloat_desc);
            final MaterialButton btnToggle = itemView.findViewById(R.id.btn_toggle_freeze);

            tvName.setText(app.name + (app.recommended ? " (Khuyên tắt)" : ""));
            tvPkg.setText(app.pkg);
            tvDesc.setText(app.desc);

            if (frozen) {
                tvBadge.setText("ĐÃ ĐÓNG BĂNG");
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
                btnToggle.setText("🔄 Khôi phục ứng dụng");
                btnToggle.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
                btnToggle.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.neon_green)));
            } else {
                tvBadge.setText("ĐANG HOẠT ĐỘNG");
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
                                    populateDebloatList(itemsContainer, dialog);
                                }
                            });
                        }
                    });
                }
            });

            itemsContainer.addView(itemView);
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
            btnOpenBatterySecretInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    XiaomiHelper.openHiddenBatteryInfo(MainActivity.this);
                }
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

        if (btnRefreshDrainApps != null) {
            btnRefreshDrainApps.setOnClickListener(v -> {
                updateSotView();
                loadBatteryDrainApps();
                Toast.makeText(this, "Đã làm mới danh sách tiêu thụ pin", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnDrainFilterUnplug != null && btnDrainFilterToday != null) {
            btnDrainFilterUnplug.setOnClickListener(v -> {
                currentDrainFilter = BatteryUsageHelper.FILTER_SINCE_UNPLUG;
                btnDrainFilterUnplug.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.mi_orange)));
                btnDrainFilterUnplug.setTextColor(ContextCompat.getColor(this, R.color.white));
                btnDrainFilterToday.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_highlight)));
                btnDrainFilterToday.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                loadBatteryDrainApps();
            });

            btnDrainFilterToday.setOnClickListener(v -> {
                currentDrainFilter = BatteryUsageHelper.FILTER_TODAY;
                btnDrainFilterToday.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.mi_orange)));
                btnDrainFilterToday.setTextColor(ContextCompat.getColor(this, R.color.white));
                btnDrainFilterUnplug.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_highlight)));
                btnDrainFilterUnplug.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
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
                    tvSotTotalUsed.setText("> 7 ngày trước");
                } else {
                    tvSotTotalUsed.setText(SotHelper.formatDuration(diff) + " trước");
                }
            }
        }

        if (tvSotUnplugInfo != null) {
            int level = BatteryHelper.getBatteryPercentage(this);
            tvSotUnplugInfo.setText(SotHelper.getUnplugSummary(this, level));
        }

        app.cd.mitool.widget.SotAppWidgetProvider.updateAllWidgets(this);
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
            if (tvPercentage != null) tvPercentage.setText(item.percentage + "% thời lượng");
            if (tvEstBattery != null) {
                tvEstBattery.setText(String.format(Locale.getDefault(), "~%.1f%% pin", item.estimatedBatteryDrain));
            }

            // Progress bar
            if (pbProgress != null) {
                pbProgress.setMax(100);
                pbProgress.setProgress(item.percentage);
                int tintRes = item.percentage > 25 ? R.color.mi_orange : R.color.neon_cyan;
                pbProgress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, tintRes)));
            }

            // Stop button
            if (btnStop != null) {
                btnStop.setOnClickListener(v -> {
                    BatteryUsageHelper.forceStopApp(MainActivity.this, item.packageName, (success, msg) -> {
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        if (success) {
                            btnStop.setText("✓ Đã dừng");
                            btnStop.setEnabled(false);
                            btnStop.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_muted));
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
            tvBatteryStatus.setText(R.string.battery_status_charging);
            tvBatteryStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            tvBatteryPlugType.setText("Nguồn: " + plugStr);

            String eta = BatteryHelper.getEstimatedChargeTime(this);
            tvChargeEta.setText(eta != null ? eta : "Đang ước tính...");
        } else {
            tvBatteryStatus.setText(R.string.battery_status_discharging);
            tvBatteryStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvBatteryPlugType.setText("Nguồn: Pin thiết bị");
            tvChargeEta.setText("Chưa cắm sạc");
        }

        updateBatteryPowerRealtime();
        updateSotView();
    }

    private void updateBatteryPowerRealtime() {
        if (tvBatteryPower == null) return;
        float powerW = BatteryHelper.getBatteryPowerWatts(this);
        int currentMa = BatteryHelper.getBatteryCurrentNow(this);
        boolean isCharging = BatteryHelper.isCharging(this);

        if (powerW > 0.0f) {
            String prefix = isCharging ? "+" : "-";
            tvBatteryPower.setText(String.format(Locale.getDefault(), "%s%.2f W (%s%d mA)", prefix, powerW, prefix, Math.abs(currentMa)));
            tvBatteryPower.setTextColor(ContextCompat.getColor(this, isCharging ? R.color.neon_green : R.color.neon_cyan));
        } else {
            tvBatteryPower.setText("0.0 W (Cảm biến đang chờ...)");
            tvBatteryPower.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        }

        if (tvBatteryCycleCount != null) {
            int cycle = BatteryHelper.getBatteryCycleCount();
            if (cycle > 0) {
                tvBatteryCycleCount.setText(cycle + " chu kỳ");
            } else {
                tvBatteryCycleCount.setText("Bấm xem thông tin ẩn bên dưới");
            }
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

    private void startBatteryRealtimeTicker() {
        if (batteryTickerRunnable == null) {
            batteryTickerRunnable = new Runnable() {
                @Override
                public void run() {
                    updateBatteryPowerRealtime();
                    batteryTickerHandler.postDelayed(this, 2000);
                }
            };
            batteryTickerHandler.postDelayed(batteryTickerRunnable, 2000);
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
        if (btnCheckUpdateNow != null) {
            btnCheckUpdateNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Đang kiểm tra bản cập nhật mới từ GitHub...", Toast.LENGTH_SHORT).show();
                    UpdateHelper.checkForUpdate(MainActivity.this, true);
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
                btnShizukuGrantAuto.setText("🟢 Đã Cấp Quyền WRITE_SECURE_SETTINGS");
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
                btnShizukuGrantAuto.setText("⚡ Bấm Cấp Quyền Shizuku Cho Mi Tool");
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
}
