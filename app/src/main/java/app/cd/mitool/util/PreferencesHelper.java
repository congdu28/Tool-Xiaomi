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

    // Voice Alert Keys
    public static final String KEY_VOICE_ALERT_MASTER = "voice_alert_master";
    public static final String KEY_VOICE_ALERT_CONNECT = "voice_alert_connect";
    public static final String KEY_VOICE_ALERT_DISCONNECT = "voice_alert_disconnect";
    public static final String KEY_VOICE_ALERT_80 = "voice_alert_80";
    public static final String KEY_VOICE_ALERT_100 = "voice_alert_100";
    public static final String KEY_VOICE_ALERT_TEMP = "voice_alert_temp";

    // Refresh Rate Key
    public static final String KEY_TARGET_REFRESH_RATE = "target_refresh_rate";

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
        return prefs.getInt(KEY_CLOCK_X, 100);
    }

    public void setClockX(int x) {
        prefs.edit().putInt(KEY_CLOCK_X, x).apply();
    }

    public int getClockY() {
        return prefs.getInt(KEY_CLOCK_Y, 200);
    }

    public void setClockY(int y) {
        prefs.edit().putInt(KEY_CLOCK_Y, y).apply();
    }

    public boolean isChargingHudEnabled() {
        return prefs.getBoolean(KEY_CHARGING_HUD_ENABLED, true);
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

    public boolean isAlert100Enabled() {
        return prefs.getBoolean(KEY_ALERT_100, true);
    }

    public void setAlert100Enabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALERT_100, enabled).apply();
    }

    public boolean isAlertTempEnabled() {
        return prefs.getBoolean(KEY_ALERT_TEMP, true);
    }

    public void setAlertTempEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALERT_TEMP, enabled).apply();
    }

    public boolean isLockscreenNotifEnabled() {
        return prefs.getBoolean(KEY_LOCKSCREEN_NOTIF, true);
    }

    public void setLockscreenNotifEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOCKSCREEN_NOTIF, enabled).apply();
    }

    public boolean isLockscreenAmbientEnabled() {
        return prefs.getBoolean(KEY_LOCKSCREEN_AMBIENT, true);
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
}
