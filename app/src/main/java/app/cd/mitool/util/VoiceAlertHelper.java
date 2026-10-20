package app.cd.mitool.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class VoiceAlertHelper {

    private static final String TAG = "VoiceAlertHelper";

    private static TextToSpeech tts;
    private static boolean isTtsInitialized = false;
    private static boolean hasAnnounced80 = false;
    private static boolean hasAnnounced100 = false;
    private static long lastTempAlertTime = 0;

    public static synchronized void init(Context context) {
        if (tts != null) return;
        final Context appCtx = context.getApplicationContext();
        try {
            tts = new TextToSpeech(appCtx, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        try {
                            int result = tts.setLanguage(new Locale("vi", "VN"));
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                tts.setLanguage(Locale.getDefault());
                            }
                            tts.setPitch(1.0f);
                            tts.setSpeechRate(1.0f);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                AudioAttributes attrs = new AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                        .build();
                                tts.setAudioAttributes(attrs);
                            }
                            isTtsInitialized = true;
                        } catch (Exception e) {
                            Log.w(TAG, "Error configuring TTS: " + e.getMessage());
                        }
                    } else {
                        Log.w(TAG, "TTS initialization failed with code: " + status);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e);
        }
    }

    public static synchronized void speak(Context context, final String text) {
        if (context == null || text == null || text.trim().isEmpty()) return;

        if (tts == null) {
            init(context);
        }

        if (isTtsInitialized && tts != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MiToolVoice_" + System.currentTimeMillis());
                } else {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                }
                return;
            } catch (Exception e) {
                Log.w(TAG, "TTS speak failed: " + e.getMessage());
            }
        }

        // Fallback tone if TTS is unavailable
        try {
            BatteryHelper.playAlertTone(context);
        } catch (Exception ignored) {
        }
    }

    public static void resetSessionAlerts() {
        hasAnnounced80 = false;
        hasAnnounced100 = false;
    }

    public static void onPowerConnected(Context context, int plugType, float powerWatt, int level) {
        PreferencesHelper prefs = new PreferencesHelper(context);
        if (!prefs.isVoiceAlertMasterEnabled() || !prefs.isVoiceAlertConnectEnabled()) {
            return;
        }

        String typeStr = (plugType == 2) ? "bằng cổng sạc USB." : "bằng bộ sạc.";
        String fastStr = (powerWatt >= 15.0f) ? String.format(Locale.getDefault(), " Sạc nhanh %.0f oát.", powerWatt) : "";
        String msg = "Đã kết nối " + typeStr + fastStr + " Pin hiện tại " + level + " phần trăm.";
        speak(context, msg);
    }

    public static void onPowerDisconnected(Context context, int level) {
        resetSessionAlerts();
        PreferencesHelper prefs = new PreferencesHelper(context);
        if (!prefs.isVoiceAlertMasterEnabled() || !prefs.isVoiceAlertDisconnectEnabled()) {
            return;
        }

        String msg = "Đã ngắt sạc. Mức pin hiện tại " + level + " phần trăm.";
        speak(context, msg);
    }

    public static void checkLevelAlerts(Context context, int level, boolean isCharging) {
        PreferencesHelper prefs = new PreferencesHelper(context);
        if (!prefs.isVoiceAlertMasterEnabled() || !isCharging) {
            return;
        }

        if (level >= 80 && level < 85 && prefs.isVoiceAlert80Enabled() && !hasAnnounced80) {
            hasAnnounced80 = true;
            speak(context, "Pin đã đạt 80 phần trăm. Bạn có thể rút sạc để bảo vệ tuổi thọ pin tối ưu.");
        }

        if (level >= 100 && prefs.isVoiceAlert100Enabled() && !hasAnnounced100) {
            hasAnnounced100 = true;
            speak(context, "Pin đã được sạc đầy 100 phần trăm. Vui lòng rút bộ sạc.");
        }
    }

    public static void checkTempAlert(Context context, float tempC) {
        PreferencesHelper prefs = new PreferencesHelper(context);
        if (!prefs.isVoiceAlertMasterEnabled() || !prefs.isVoiceAlertTempEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        // Cooldown: 10 minutes between temperature voice alerts
        if (tempC >= 42.0f && (now - lastTempAlertTime > 10 * 60 * 1000)) {
            lastTempAlertTime = now;
            speak(context, "Cảnh báo, nhiệt độ pin đang cao " + Math.round(tempC) + " độ C. Vui lòng tạm ngừng sạc để bảo vệ máy.");
        }
    }

    public static void testVoice(Context context) {
        speak(context, "Hệ thống âm báo sạc thông minh Tiếng Việt hoạt động rất tốt trên máy Xiaomi của bạn!");
    }

    public static synchronized void destroy() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }
            tts = null;
            isTtsInitialized = false;
        }
    }
}
