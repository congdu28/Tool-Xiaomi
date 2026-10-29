package app.cd.mitool.util;

import android.content.Context;
import android.content.SharedPreferences;

/** One mode per event, preserving the effective legacy ring/voice selections. */
public final class AlertAudioSettings {
    public static final int CONNECT = 0, DISCONNECT = 1, TARGET = 2, FULL = 3, HOT = 4;
    public static final int OFF = 0, RING = 1, VOICE = 2, BOTH = 3;
    private static final String[] RING_KEYS = {"sound_connect", "sound_disconnect", PreferencesHelper.KEY_ALERT_80,
            PreferencesHelper.KEY_ALERT_100, PreferencesHelper.KEY_ALERT_TEMP};
    private static final String[] VOICE_KEYS = {PreferencesHelper.KEY_VOICE_ALERT_CONNECT, PreferencesHelper.KEY_VOICE_ALERT_DISCONNECT,
            PreferencesHelper.KEY_VOICE_ALERT_80, PreferencesHelper.KEY_VOICE_ALERT_100, PreferencesHelper.KEY_VOICE_ALERT_TEMP};
    private final SharedPreferences prefs;
    public AlertAudioSettings(Context context) { prefs = context.getSharedPreferences("mi_tool_prefs", Context.MODE_PRIVATE); }
    public int mode(int event) {
        return AudioModePolicy.combine(prefs.getBoolean(RING_KEYS[event], false),
                prefs.getBoolean(PreferencesHelper.KEY_VOICE_ALERT_MASTER, false), prefs.getBoolean(VOICE_KEYS[event], true));
    }
    public boolean anyEnabled() { for (int i = 0; i < 5; i++) if (mode(i) != OFF) return true; return false; }
    public boolean hasVoiceEnabled() { for (int i = 0; i < 5; i++) if ((mode(i) & VOICE) != 0) return true; return false; }
    public void setMode(int event, int mode) {
        if (event < 0 || event >= 5 || mode < OFF || mode > BOTH) throw new IllegalArgumentException();
        int[] current = new int[5];
        for (int i = 0; i < 5; i++) current[i] = mode(i);
        current[event] = mode;
        SharedPreferences.Editor edit = prefs.edit();
        boolean voice = false;
        for (int i = 0; i < 5; i++) {
            edit.putBoolean(RING_KEYS[i], (current[i] & RING) != 0);
            edit.putBoolean(VOICE_KEYS[i], (current[i] & VOICE) != 0);
            voice |= (current[i] & VOICE) != 0;
        }
        edit.putBoolean(PreferencesHelper.KEY_VOICE_ALERT_MASTER, voice).apply();
    }
}
