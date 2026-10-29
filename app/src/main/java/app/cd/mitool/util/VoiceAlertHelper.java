package app.cd.mitool.util;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import app.cd.mitool.R;

/** Bounded TTS initialization, checked Vietnamese support, queued speech and one-shot fallback. */
public final class VoiceAlertHelper {
    public interface StatusCallback { void onStatus(int message); }
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static TextToSpeech engine;
    private static boolean ready, initializing;
    private static int generation, serial;
    private static long failedAt;
    private static int status = R.string.audio_wait;
    private static final List<Request> pending = new ArrayList<>();
    private static final List<StatusCallback> checks = new ArrayList<>();
    private static final Map<String, Request> active = new HashMap<>();
    private static final class Request {
        String text; Runnable fallback; StatusCallback callback; boolean started, failed;
        Request(String text, Runnable fallback, StatusCallback callback) {
            this.text = text; this.fallback = fallback; this.callback = callback;
        }
    }
    private VoiceAlertHelper() {}
    public static void prepare(Context context, StatusCallback callback) {
        Context app = context.getApplicationContext();
        MAIN.post(() -> {
            if (ready) { if (callback != null) callback.onStatus(status); return; }
            if (callback != null) checks.add(callback);
            failedAt = 0; initialize(app);
        });
    }
    public static void init(Context context) { prepare(context, null); }
    public static void speak(Context context, String text) {
        speak(context, text, () -> BatteryHelper.playAlertTone(context), null);
    }
    public static void speak(Context context, String text, Runnable fallback, StatusCallback callback) {
        Context app = context.getApplicationContext();
        MAIN.post(() -> {
            Request request = new Request(text, fallback, callback);
            if (ready) { dispatch(request); return; }
            if (failedAt > 0 && SystemClock.elapsedRealtime() - failedAt < 30000) { fail(request, R.string.audio_missing); return; }
            if (callback != null) callback.onStatus(R.string.audio_wait);
            if (pending.size() >= 8) fail(pending.remove(0), R.string.audio_failed);
            pending.add(request); initialize(app);
        });
    }
    private static void initialize(Context app) {
        if (initializing || ready) return;
        initializing = true; status = R.string.audio_wait;
        int token = ++generation;
        if (app.getPackageManager().queryIntentServices(new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0).isEmpty()) {
            initializationFailed(); return;
        }
        try {
            engine = new TextToSpeech(app, result -> MAIN.post(() -> {
                if (token != generation || !initializing) return;
                if (result != TextToSpeech.SUCCESS || engine == null) { initializationFailed(); return; }
                try {
                    if (engine.setLanguage(new Locale("vi", "VN")) < TextToSpeech.LANG_AVAILABLE) {
                        initializationFailed(); return;
                    }
                    engine.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
                    engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override public void onStart(String id) { MAIN.post(() -> {
                            Request request = active.get(id);
                            if (request != null) { android.util.Log.i("MiToolAudio", "voice_started"); request.started = true; status = R.string.audio_ready; if (request.callback != null) request.callback.onStatus(R.string.audio_ready); }
                        }); }
                        @Override public void onDone(String id) { MAIN.post(() -> active.remove(id)); }
                        @Override public void onError(String id) { MAIN.post(() -> {
                            Request request = active.remove(id); if (request != null) fail(request, R.string.audio_failed);
                        }); }
                    });
                    ready = true; initializing = false; status = R.string.audio_ready;
                    for (StatusCallback check : new ArrayList<>(checks)) check.onStatus(status);
                    checks.clear();
                    for (Request request : new ArrayList<>(pending)) dispatch(request);
                    pending.clear();
                } catch (RuntimeException e) { initializationFailed(); }
            }));
        } catch (RuntimeException e) { initializationFailed(); }
        MAIN.postDelayed(() -> { if (token == generation && initializing) initializationFailed(); }, 8000);
    }
    private static void initializationFailed() {
        generation++; initializing = false; ready = false; failedAt = SystemClock.elapsedRealtime(); status = R.string.audio_missing;
        if (engine != null) { try { engine.shutdown(); } catch (RuntimeException ignored) {} engine = null; }
        for (StatusCallback check : new ArrayList<>(checks)) check.onStatus(status);
        checks.clear();
        for (Request request : new ArrayList<>(pending)) fail(request, status);
        pending.clear();
    }
    private static void dispatch(Request request) {
        String id = "MiTool_" + (++serial);
        active.put(id, request);
        try {
            android.os.Bundle parameters = new android.os.Bundle();
            parameters.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_ALARM);
            if (engine == null || engine.speak(request.text, TextToSpeech.QUEUE_ADD, parameters, id) != TextToSpeech.SUCCESS) {
                active.remove(id); fail(request, R.string.audio_failed); return;
            }
        } catch (RuntimeException e) { active.remove(id); fail(request, R.string.audio_failed); return; }
        MAIN.postDelayed(() -> {
            if (active.get(id) == request && !request.started) {
                List<Request> stalled = new ArrayList<>(active.values()); active.clear();
                try { if (engine != null) engine.stop(); } catch (RuntimeException ignored) { }
                for (Request waiting : stalled) if (!waiting.started) fail(waiting, R.string.audio_failed);
            }
        }, 8000);
        MAIN.postDelayed(() -> active.remove(id), 60000);
    }
    private static void fail(Request request, int message) {
        if (request.failed) return;
        request.failed = true; status = message;
        if (request.fallback != null) request.fallback.run();
        if (request.callback != null) request.callback.onStatus(message);
        android.util.Log.w("MiToolAudio", "Voice unavailable; fallback evaluated");
    }
    public static void testVoice(Context context) { speak(context, context.getString(R.string.audio_test_sentence)); }
    public static void cancelSpeech() { MAIN.post(() -> {
        pending.clear(); active.clear(); if (engine != null) try { engine.stop(); } catch (RuntimeException ignored) {}
    }); }
    public static void destroy() { MAIN.post(() -> {
        generation++; if (engine != null) try { engine.shutdown(); } catch (RuntimeException ignored) {} engine = null;
        ready = false; initializing = false; pending.clear(); checks.clear(); active.clear();
    }); }
}
