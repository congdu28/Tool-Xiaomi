package app.cd.mitool.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {

    private static final String PREF_NAME = "mi_tool_prefs";

    public static final String KEY_NOTIF_CLOCK_ENABLED = "notif_clock_enabled";
    public static final String KEY_CLOCK_ENABLED = "clock_enabled";
    public static final String KEY_CLOCK_SHOW_MS = "clock_show_ms";
    public static final String KEY_CLOCK_SHOW_BATTERY = "clock_show_battery";
    public static final String KEY_CLOCK_LOCK_POSITION = "clock_lock_position";
    public static final String KEY_CLOCK_SIZE = "clock_size"; // 0: small, 1: medium, 2: large
    public static final String KEY_CLOCK_COLOR = "clock_color"; // 0: white, 1: green, 2: orange
    public static final String KEY_CLOCK_X = "clock_pos_x";
    public static final String KEY_CLOCK_Y = "clock_pos_y";

    public static final String KEY_CHARGING_HUD_ENABLED = "charging_hud_enabled";
    public static final String KEY_ALERT_80 = "alert_battery_80";
    public static final String KEY_ALERT_100 = "alert_battery_100";
    public static final String KEY_ALERT_TEMP = "alert_temp_high";
    public static final String KEY_LOCKSCREEN_NOTIF = "lockscreen_notif_enabled";
    public static final String KEY_LOCKSCREEN_AMBIENT = "lockscreen_ambient_enabled";

    // Battery Alert Customization
    public static final String KEY_BATTERY_ALERT_LEVEL = "battery_alert_level"; // default 80
    public static final String KEY_CUSTOM_ALERT_URI = "custom_alert_tone_uri";
    public static final String KEY_CUSTOM_ALERT_NAME = "custom_alert_tone_name";

    // Voice Alert Keys
    public static final String KEY_VOICE_ALERT_MASTER = "voice_alert_master";
    public static final String KEY_VOICE_ALERT_CONNECT = "voice_alert_connect";
    public static final String KEY_VOICE_ALERT_DISCONNECT = "voice_alert_disconnect";
    public static final String KEY_VOICE_ALERT_80 = "voice_alert_80";
    public static final String KEY_VOICE_ALERT_100 = "voice_alert_100";
    public static final String KEY_VOICE_ALERT_TEMP = "voice_alert_temp";

    // Refresh Rate Key
    public static final String KEY_TARGET_REFRESH_RATE = "target_refresh_rate";

    // Status Bar Pin & Clock Enhancements
    public static final String KEY_CLOCK_STATUS_BAR_MODE = "clock_status_bar_mode";
    public static final String KEY_CLOCK_TRANSPARENT_BG = "clock_transparent_bg";
    public static final String KEY_CLOCK_HIDE_SYSTEM_CLOCK = "clock_hide_system_clock";

    private final SharedPreferences prefs;

    public PreferencesHelper(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isNotifClockEnabled() {
        return prefs.getBoolean(KEY_NOTIF_CLOCK_ENABLED, false);
    }

    public void setNotifClockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_CLOCK_ENABLED, enabled).apply();
    }

    public boolean isClockEnabled() {
        return prefs.getBoolean(KEY_CLOCK_ENABLED, false);
    }

    public void setClockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CLOCK_ENABLED, enabled).apply();
    }

    public boolean isClockShowMs() {
        return prefs.getBoolean(KEY_CLOCK_SHOW_MS, false);
    }

    public void setClockShowMs(boolean show) {
        prefs.edit().putBoolean(KEY_CLOCK_SHOW_MS, show).apply();
    }

    public boolean isClockShowBattery() {
        return prefs.getBoolean(KEY_CLOCK_SHOW_BATTERY, false);
    }

    public void setClockShowBattery(boolean show) {
        prefs.edit().putBoolean(KEY_CLOCK_SHOW_BATTERY, show).apply();
    }

    public boolean isClockLockPosition() {
        return prefs.getBoolean(KEY_CLOCK_LOCK_POSITION, false);
    }

    public void setClockLockPosition(boolean lock) {
        prefs.edit().putBoolean(KEY_CLOCK_LOCK_POSITION, lock).apply();
    }

    public int getClockSize() {
        return prefs.getInt(KEY_CLOCK_SIZE, 1);
    }

    public void setClockSize(int size) {
        prefs.edit().putInt(KEY_CLOCK_SIZE, size).apply();
    }

    public int getClockColor() {
        return prefs.getInt(KEY_CLOCK_COLOR, 0);
    }

    public void setClockColor(int color) {
        prefs.edit().putInt(KEY_CLOCK_COLOR, color).apply();
    }

    public int getClockX() {
        return prefs.getInt(KEY_CLOCK_X, -1);
    }

    public void setClockX(int x) {
        prefs.edit().putInt(KEY_CLOCK_X, x).apply();
    }

    public int getClockY() {
        return prefs.getInt(KEY_CLOCK_Y, -1);
    }

    public void setClockY(int y) {
        prefs.edit().putInt(KEY_CLOCK_Y, y).apply();
    }

    public boolean isClockStatusBarMode() {
        return prefs.getBoolean(KEY_CLOCK_STATUS_BAR_MODE, false);
    }

    public void setClockStatusBarMode(boolean mode) {
        prefs.edit().putBoolean(KEY_CLOCK_STATUS_BAR_MODE, mode).apply();
    }

    public boolean isClockTransparentBg() {
        return prefs.getBoolean(KEY_CLOCK_TRANSPARENT_BG, false);
    }

    public void setClockTransparentBg(boolean transparent) {
        prefs.edit().putBoolean(KEY_CLOCK_TRANSPARENT_BG, transparent).apply();
    }

    public boolean isClockHideSystemClock() {
        return prefs.getBoolean(KEY_CLOCK_HIDE_SYSTEM_CLOCK, false);
    }

    public void setClockHideSystemClock(boolean hide) {
        prefs.edit().putBoolean(KEY_CLOCK_HIDE_SYSTEM_CLOCK, hide).apply();
    }

    public boolean isChargingHudEnabled() {
        return prefs.getBoolean(KEY_CHARGING_HUD_ENABLED, false);
    }

    public void setChargingHudEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CHARGING_HUD_ENABLED, enabled).apply();
    }

    public boolean isAlert80Enabled() {
        return prefs.getBoolean(KEY_ALERT_80, false);
    }

    public void setAlert80Enabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALERT_80, enabled).apply();
    }

    public int getBatteryAlertLevel() {
        return prefs.getInt(KEY_BATTERY_ALERT_LEVEL, 80);
    }

    public void setBatteryAlertLevel(int level) {
        int bounded = Math.max(50, Math.min(95, level));
        prefs.edit().putInt(KEY_BATTERY_ALERT_LEVEL, bounded).apply();
    }

    public String getCustomAlertToneUri() {
        return prefs.getString(KEY_CUSTOM_ALERT_URI, "");
    }

    public void setCustomAlertToneUri(String uri) {
        prefs.edit().putString(KEY_CUSTOM_ALERT_URI, uri != null ? uri : "").apply();
    }

    public String getCustomAlertToneName() {
        return prefs.getString(KEY_CUSTOM_ALERT_NAME, "");
    }

    public void setCustomAlertToneName(String name) {
        prefs.edit().putString(KEY_CUSTOM_ALERT_NAME, name != null ? name : "").apply();
    }

    public boolean isAlert100Enabled() {
        return prefs.getBoolean(KEY_ALERT_100, false);
    }

    public void setAlert100Enabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALERT_100, enabled).apply();
    }

    public boolean isAlertTempEnabled() {
        return prefs.getBoolean(KEY_ALERT_TEMP, false);
    }

    public void setAlertTempEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALERT_TEMP, enabled).apply();
    }

    public boolean isLockscreenNotifEnabled() {
        return prefs.getBoolean(KEY_LOCKSCREEN_NOTIF, false);
    }

    public void setLockscreenNotifEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOCKSCREEN_NOTIF, enabled).apply();
    }

    public boolean isLockscreenAmbientEnabled() {
        return prefs.getBoolean(KEY_LOCKSCREEN_AMBIENT, false);
    }

    public void setLockscreenAmbientEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOCKSCREEN_AMBIENT, enabled).apply();
    }

    public boolean isVoiceAlertMasterEnabled() {
        return prefs.getBoolean(KEY_VOICE_ALERT_MASTER, false);
    }

    public void setVoiceAlertMasterEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOICE_ALERT_MASTER, enabled).apply();
    }

    public boolean isVoiceAlertConnectEnabled() {
        return prefs.getBoolean(KEY_VOICE_ALERT_CONNECT, true);
    }

    public void setVoiceAlertConnectEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOICE_ALERT_CONNECT, enabled).apply();
    }

    public boolean isVoiceAlertDisconnectEnabled() {
        return prefs.getBoolean(KEY_VOICE_ALERT_DISCONNECT, true);
    }

    public void setVoiceAlertDisconnectEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOICE_ALERT_DISCONNECT, enabled).apply();
    }

    public boolean isVoiceAlert80Enabled() {
        return prefs.getBoolean(KEY_VOICE_ALERT_80, true);
    }

    public void setVoiceAlert80Enabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOICE_ALERT_80, enabled).apply();
    }

    public boolean isVoiceAlert100Enabled() {
        return prefs.getBoolean(KEY_VOICE_ALERT_100, true);
    }

    public void setVoiceAlert100Enabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOICE_ALERT_100, enabled).apply();
    }

    public boolean isVoiceAlertTempEnabled() {
        return prefs.getBoolean(KEY_VOICE_ALERT_TEMP, true);
    }

    public void setVoiceAlertTempEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOICE_ALERT_TEMP, enabled).apply();
    }

    public int getTargetRefreshRate() {
        return prefs.getInt(KEY_TARGET_REFRESH_RATE, 0); // 0 = Auto/Default
    }

    public void setTargetRefreshRate(int rate) {
        prefs.edit().putInt(KEY_TARGET_REFRESH_RATE, rate).apply();
    }

    // Aliases and convenience methods for Floating Clock and Battery
    public boolean isFloatingClockEnabled() {
        return isClockEnabled();
    }

    public void setFloatingClockEnabled(boolean enabled) {
        setClockEnabled(enabled);
    }

    public boolean isFloatingClockTransparent() {
        return isClockTransparentBg();
    }

    public void setFloatingClockTransparent(boolean transparent) {
        setClockTransparentBg(transparent);
    }

    public boolean isFloatingClockLocked() {
        return isClockLockPosition();
    }

    public void setFloatingClockLocked(boolean lock) {
        setClockLockPosition(lock);
    }

    public boolean isFloatingClockMs() {
        return isClockShowMs();
    }

    public void setFloatingClockMs(boolean show) {
        setClockShowMs(show);
    }

    public boolean isBatteryAlertLevelEnabled() {
        return isAlert80Enabled();
    }

    public void setBatteryAlertLevelEnabled(boolean enabled) {
        setAlert80Enabled(enabled);
    }

    public String getCustomAlertToneTitle() {
        String name = getCustomAlertToneName();
        return (name != null && !name.isEmpty()) ? name : "Mặc định hệ thống";
    }

    public void setCustomAlertToneTitle(String title) {
        setCustomAlertToneName(title);
    }

    public void setFloatingClockX(int x) {
        setClockX(x);
    }

    public void setFloatingClockY(int y) {
        setClockY(y);
    }

    public void setFloatingClockPreset(int preset) {
        prefs.edit().putInt("floating_clock_preset", preset).apply();
    }

    public int getFloatingClockPreset() {
        return prefs.getInt("floating_clock_preset", 0);
    }

    // Screen On Time (SOT) Tracking
    public static final String KEY_SOT_LAST_UNPLUG_TIME = "sot_last_unplug_time";
    public static final String KEY_SOT_LAST_UNPLUG_LEVEL = "sot_last_unplug_level";
    public static final String KEY_SOT_ACCUMULATED_MILLIS = "sot_accumulated_millis";
    public static final String KEY_SOT_SCREEN_ON_START = "sot_screen_on_start";
    public static final String KEY_SOT_TRACKER_ACTIVE = "sot_tracker_active";

    public boolean isSotTrackerActive() {
        return prefs.getBoolean(KEY_SOT_TRACKER_ACTIVE, false);
    }

    public void setSotTrackerActive(boolean active) {
        prefs.edit().putBoolean(KEY_SOT_TRACKER_ACTIVE, active).apply();
    }

    public long getSotLastUnplugTime() {
        return prefs.getLong(KEY_SOT_LAST_UNPLUG_TIME, 0);
    }

    public void setSotLastUnplugTime(long time) {
        prefs.edit().putLong(KEY_SOT_LAST_UNPLUG_TIME, time).apply();
    }

    public int getSotLastUnplugLevel() {
        return prefs.getInt(KEY_SOT_LAST_UNPLUG_LEVEL, 100);
    }

    public void setSotLastUnplugLevel(int level) {
        prefs.edit().putInt(KEY_SOT_LAST_UNPLUG_LEVEL, level).apply();
    }

    public long getSotAccumulatedMillis() {
        return prefs.getLong(KEY_SOT_ACCUMULATED_MILLIS, 0);
    }

    public void setSotAccumulatedMillis(long millis) {
        prefs.edit().putLong(KEY_SOT_ACCUMULATED_MILLIS, millis).apply();
    }

    public long getSotScreenOnStart() {
        return prefs.getLong(KEY_SOT_SCREEN_ON_START, 0);
    }

    public void setSotScreenOnStart(long time) {
        prefs.edit().putLong(KEY_SOT_SCREEN_ON_START, time).apply();
    }

    public String getCustomString(String key, String defValue) {
        return prefs.getString(key, defValue);
    }

    public void setCustomString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    // App Theme Configuration (0: System default, 1: Light, 2: Dark)
    public static final String KEY_APP_THEME_MODE = "app_theme_mode";
    public static final int THEME_MODE_SYSTEM = 0;
    public static final int THEME_MODE_LIGHT = 1;
    public static final int THEME_MODE_DARK = 2;

    public int getAppThemeMode() {
        return prefs.getInt(KEY_APP_THEME_MODE, THEME_MODE_SYSTEM);
    }

    public void setAppThemeMode(int mode) {
        prefs.edit().putInt(KEY_APP_THEME_MODE, mode).apply();
    }

    // Widget SOT Customization
    public static final String KEY_WIDGET_STYLE = "widget_sot_style"; // 0: HyperOS Glass, 1: Minimalist, 2: Dashboard
    public static final int WIDGET_STYLE_HYPEROS = 0;
    public static final int WIDGET_STYLE_MINIMAL = 1;
    public static final int WIDGET_STYLE_DASHBOARD = 2;

    public static final String KEY_WIDGET_THEME_MODE = "widget_sot_theme_mode"; // 0: System, 1: Dark, 2: Light
    public static final int WIDGET_THEME_SYSTEM = 0;
    public static final int WIDGET_THEME_DARK = 1;
    public static final int WIDGET_THEME_LIGHT = 2;

    public static final String KEY_WIDGET_REFRESH_INTERVAL = "widget_sot_refresh_interval"; // minutes: 15, 30, 60, 0
    public static final String KEY_WIDGET_SHOW_TEMP = "widget_sot_show_temp";
    public static final String KEY_WIDGET_SHOW_ESTIMATE = "widget_sot_show_estimate";

    public int getWidgetStyle() {
        return prefs.getInt(KEY_WIDGET_STYLE, WIDGET_STYLE_DASHBOARD);
    }

    public void setWidgetStyle(int style) {
        prefs.edit().putInt(KEY_WIDGET_STYLE, style).apply();
    }

    public int getWidgetThemeMode() {
        return prefs.getInt(KEY_WIDGET_THEME_MODE, WIDGET_THEME_SYSTEM);
    }

    public void setWidgetThemeMode(int mode) {
        prefs.edit().putInt(KEY_WIDGET_THEME_MODE, mode).apply();
    }

    public int getWidgetRefreshInterval() {
        return prefs.getInt(KEY_WIDGET_REFRESH_INTERVAL, 30);
    }

    public void setWidgetRefreshInterval(int minutes) {
        prefs.edit().putInt(KEY_WIDGET_REFRESH_INTERVAL, minutes).apply();
    }

    public boolean isWidgetShowTemp() {
        return prefs.getBoolean(KEY_WIDGET_SHOW_TEMP, true);
    }

    public void setWidgetShowTemp(boolean show) {
        prefs.edit().putBoolean(KEY_WIDGET_SHOW_TEMP, show).apply();
    }

    public boolean isWidgetShowEstimate() {
        return prefs.getBoolean(KEY_WIDGET_SHOW_ESTIMATE, true);
    }

    public void setWidgetShowEstimate(boolean show) {
        prefs.edit().putBoolean(KEY_WIDGET_SHOW_ESTIMATE, show).apply();
    }

    // App Updater state
    public static final String KEY_LAST_UPDATE_CHECK_TIME = "last_update_check_time";

    public long getLastUpdateCheckTime() {
        return prefs.getLong(KEY_LAST_UPDATE_CHECK_TIME, 0);
    }

    public void setLastUpdateCheckTime(long time) {
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK_TIME, time).apply();
    }

    // Mini Game HUD (Floating FPS & Performance Monitor)
    public static final String KEY_GAME_HUD_ENABLED = "game_hud_enabled";
    public static final String KEY_GAME_HUD_EXPANDED = "game_hud_expanded";
    public static final String KEY_GAME_HUD_X = "game_hud_pos_x";
    public static final String KEY_GAME_HUD_Y = "game_hud_pos_y";
    public static final String KEY_CUSTOM_GAME_PACKAGES = "custom_game_packages";

    public java.util.Set<String> getCustomGamePackages() {
        java.util.Set<String> set = prefs.getStringSet(KEY_CUSTOM_GAME_PACKAGES, null);
        if (set == null) {
            return new java.util.LinkedHashSet<>();
        }
        return new java.util.LinkedHashSet<>(set);
    }

    public void setCustomGamePackages(java.util.Set<String> pkgs) {
        prefs.edit().putStringSet(KEY_CUSTOM_GAME_PACKAGES, pkgs).apply();
    }

    public void addCustomGamePackage(String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) return;
        java.util.Set<String> set = getCustomGamePackages();
        set.add(pkg.trim());
        setCustomGamePackages(set);
    }

    public void removeCustomGamePackage(String pkg) {
        if (pkg == null) return;
        java.util.Set<String> set = getCustomGamePackages();
        set.remove(pkg.trim());
        setCustomGamePackages(set);
    }

    public boolean isGameHudEnabled() {
        return prefs.getBoolean(KEY_GAME_HUD_ENABLED, false);
    }

    public void setGameHudEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GAME_HUD_ENABLED, enabled).apply();
    }

    public boolean isGameHudExpanded() {
        return prefs.getBoolean(KEY_GAME_HUD_EXPANDED, false);
    }

    public void setGameHudExpanded(boolean expanded) {
        prefs.edit().putBoolean(KEY_GAME_HUD_EXPANDED, expanded).apply();
    }

    public int getGameHudX() {
        return prefs.getInt(KEY_GAME_HUD_X, -1);
    }

    public void setGameHudX(int x) {
        prefs.edit().putInt(KEY_GAME_HUD_X, x).apply();
    }

    public int getGameHudY() {
        return prefs.getInt(KEY_GAME_HUD_Y, -1);
    }

    public void setGameHudY(int y) {
        prefs.edit().putInt(KEY_GAME_HUD_Y, y).apply();
    }

    public static final String KEY_GAME_HUD_SCALE = "game_hud_scale";
    public static final String KEY_GAME_HUD_OPACITY = "game_hud_opacity";
    public static final String KEY_GAME_HUD_BG_OPACITY = "game_hud_bg_opacity";
    public static final String KEY_GAME_HUD_TEXT_OPACITY = "game_hud_text_opacity";
    public static final String KEY_GAME_HUD_SHOW_FPS = "game_hud_show_fps";
    public static final String KEY_GAME_HUD_SHOW_TEMP = "game_hud_show_temp";
    public static final String KEY_GAME_HUD_SHOW_CPU = "game_hud_show_cpu";
    public static final String KEY_GAME_HUD_SHOW_RAM = "game_hud_show_ram";
    public static final String KEY_GAME_HUD_SHOW_POWER = "game_hud_show_power";

    public int getGameHudScale() {
        return prefs.getInt(KEY_GAME_HUD_SCALE, 100);
    }

    public void setGameHudScale(int scale) {
        prefs.edit().putInt(KEY_GAME_HUD_SCALE, scale).apply();
    }

    public int getGameHudOpacity() {
        return prefs.getInt(KEY_GAME_HUD_OPACITY, 90);
    }

    public void setGameHudOpacity(int opacity) {
        prefs.edit().putInt(KEY_GAME_HUD_OPACITY, opacity).apply();
    }

    public int getGameHudBgOpacity() {
        return prefs.getInt(KEY_GAME_HUD_BG_OPACITY, 85);
    }

    public void setGameHudBgOpacity(int opacity) {
        prefs.edit().putInt(KEY_GAME_HUD_BG_OPACITY, opacity).apply();
    }

    public int getGameHudTextOpacity() {
        return prefs.getInt(KEY_GAME_HUD_TEXT_OPACITY, 100);
    }

    public void setGameHudTextOpacity(int opacity) {
        prefs.edit().putInt(KEY_GAME_HUD_TEXT_OPACITY, opacity).apply();
    }

    public static final String KEY_LAST_NOTIFIED_UPDATE_VERSION = "last_notified_update_version";
    public static final String KEY_LAST_NOTIFIED_UPDATE_TIME = "last_notified_update_time";

    public String getLastNotifiedUpdateVersion() {
        return prefs.getString(KEY_LAST_NOTIFIED_UPDATE_VERSION, "");
    }

    public void setLastNotifiedUpdateVersion(String ver) {
        prefs.edit().putString(KEY_LAST_NOTIFIED_UPDATE_VERSION, ver).apply();
    }

    public long getLastNotifiedUpdateTime() {
        return prefs.getLong(KEY_LAST_NOTIFIED_UPDATE_TIME, 0);
    }

    public void setLastNotifiedUpdateTime(long time) {
        prefs.edit().putLong(KEY_LAST_NOTIFIED_UPDATE_TIME, time).apply();
    }

    public boolean isGameHudShowFps() {
        return prefs.getBoolean(KEY_GAME_HUD_SHOW_FPS, true);
    }

    public void setGameHudShowFps(boolean show) {
        prefs.edit().putBoolean(KEY_GAME_HUD_SHOW_FPS, show).apply();
    }

    public boolean isGameHudShowTemp() {
        return prefs.getBoolean(KEY_GAME_HUD_SHOW_TEMP, true);
    }

    public void setGameHudShowTemp(boolean show) {
        prefs.edit().putBoolean(KEY_GAME_HUD_SHOW_TEMP, show).apply();
    }

    public boolean isGameHudShowCpu() {
        return prefs.getBoolean(KEY_GAME_HUD_SHOW_CPU, true);
    }

    public void setGameHudShowCpu(boolean show) {
        prefs.edit().putBoolean(KEY_GAME_HUD_SHOW_CPU, show).apply();
    }

    public boolean isGameHudShowRam() {
        return prefs.getBoolean(KEY_GAME_HUD_SHOW_RAM, true);
    }

    public void setGameHudShowRam(boolean show) {
        prefs.edit().putBoolean(KEY_GAME_HUD_SHOW_RAM, show).apply();
    }

    public boolean isGameHudShowPower() {
        return prefs.getBoolean(KEY_GAME_HUD_SHOW_POWER, true);
    }

    public void setGameHudShowPower(boolean show) {
        prefs.edit().putBoolean(KEY_GAME_HUD_SHOW_POWER, show).apply();
    }

    // Notification Fixer state
    public static final String KEY_NOTIF_FIX_LAST_APPLIED = "notif_fix_last_applied";

    public long getNotifFixLastApplied() {
        return prefs.getLong(KEY_NOTIF_FIX_LAST_APPLIED, 0);
    }

    public void setNotifFixLastApplied(long time) {
        prefs.edit().putLong(KEY_NOTIF_FIX_LAST_APPLIED, time).apply();
    }
}
