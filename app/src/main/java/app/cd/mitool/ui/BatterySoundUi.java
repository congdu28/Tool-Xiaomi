package app.cd.mitool.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import app.cd.mitool.R;
import app.cd.mitool.util.AlertAudioSettings;
import app.cd.mitool.util.BatteryHelper;
import app.cd.mitool.util.PreferencesHelper;
import app.cd.mitool.util.VoiceAlertHelper;

public final class BatterySoundUi {
    private BatterySoundUi() {}
    public static Runnable bind(Activity activity, View view, Runnable changed, Runnable pickTone) {
        PreferencesHelper prefs = new PreferencesHelper(activity);
        AlertAudioSettings audio = new AlertAudioSettings(activity);
        String[] modes = activity.getResources().getStringArray(R.array.audio_modes);
        int[] ids = {R.id.bs_audio_mode_connect, R.id.bs_audio_mode_disconnect, R.id.bs_audio_mode_target,
                R.id.bs_audio_mode_full, R.id.bs_audio_mode_hot};
        TextView health = view.findViewById(R.id.bs_tv_audio_health);
        TextView tone = view.findViewById(R.id.bs_tv_selected_audio_tone);
        java.util.function.IntConsumer displayHealth = message -> {
            if (activity.isDestroyed() || !health.isAttachedToWindow()) return;
            AudioManager manager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            health.setText(manager != null && manager.getStreamVolume(AudioManager.STREAM_ALARM) == 0
                    ? R.string.audio_volume_zero : message);
        };
        Runnable checkHealth = () -> {
            AudioManager manager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (manager != null && manager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
                health.setText(R.string.audio_volume_zero); return;
            }
            if (!audio.hasVoiceEnabled()) { health.setText(R.string.audio_no_voice_needed); return; }
            VoiceAlertHelper.prepare(activity, message -> {
                if (audio.hasVoiceEnabled()) displayHealth.accept(message);
            });
        };
        Runnable refresh = () -> {
            for (int i = 0; i < ids.length; i++) ((MaterialButton) view.findViewById(ids[i])).setText(modes[audio.mode(i)]);
            tone.setText(activity.getString(R.string.audio_tone, prefs.getCustomAlertToneTitle()));
            ((TextView) view.findViewById(R.id.bs_audio_label_target)).setText(activity.getString(R.string.audio_target, prefs.getBatteryAlertLevel()));
            ((TextView) view.findViewById(R.id.bs_tv_battery_audio_level)).setText(activity.getString(R.string.audio_threshold, prefs.getBatteryAlertLevel()));
            view.findViewById(R.id.bs_layout_alert_level_slider).setVisibility(audio.mode(2) != 0 ? View.VISIBLE : View.GONE);
        };
        for (int i = 0; i < ids.length; i++) {
            final int event = i;
            view.findViewById(ids[i]).setOnClickListener(v -> new MaterialAlertDialogBuilder(activity)
                    .setTitle(((TextView) ((View) v.getParent()).findViewById(new int[]{R.id.bs_audio_label_connect,
                            R.id.bs_audio_label_disconnect,R.id.bs_audio_label_target,R.id.bs_audio_label_full,R.id.bs_audio_label_hot}[event])).getText())
                    .setSingleChoiceItems(modes, audio.mode(event), (dialog, selected) -> {
                        audio.setMode(event, selected); VoiceAlertHelper.cancelSpeech();
                        if (selected == 0) BatteryHelper.stopAlertTone(); refresh.run(); changed.run(); checkHealth.run(); dialog.dismiss();
                    }).setNegativeButton(R.string.action_cancel, null).show());
        }
        SeekBar level = view.findViewById(R.id.bs_sb_battery_alert_level);
        level.setProgress(prefs.getBatteryAlertLevel() - 50);
        level.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar,int value,boolean user) {
                if (user) { prefs.setBatteryAlertLevel(50 + value); refresh.run(); changed.run(); }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        view.findViewById(R.id.bs_btn_pick_audio).setOnClickListener(v -> pickTone.run());
        view.findViewById(R.id.bs_btn_reset_audio).setOnClickListener(v -> {
            prefs.setCustomAlertToneUri(null); prefs.setCustomAlertToneTitle(""); refresh.run();
        });
        view.findViewById(R.id.bs_btn_test_audio).setOnClickListener(v -> displayHealth.accept(
                BatteryHelper.previewTone(activity) ? R.string.audio_ring_started : R.string.audio_ring_failed));
        view.findViewById(R.id.bs_btn_test_voice).setOnClickListener(v -> VoiceAlertHelper.speak(activity,
                activity.getString(R.string.audio_test_sentence), () -> BatteryHelper.previewTone(activity),
                message -> { displayHealth.accept(message); }));
        view.findViewById(R.id.bs_btn_audio_settings).setOnClickListener(v -> new MaterialAlertDialogBuilder(activity)
                .setItems(new String[]{"Âm lượng báo thức", "Bộ đọc văn bản tiếng Việt"}, (dialog, choice) -> {
                    try { activity.startActivity(new Intent(choice == 0 ? Settings.ACTION_SOUND_SETTINGS : "com.android.settings.TTS_SETTINGS")); }
                    catch (RuntimeException e) { activity.startActivity(new Intent(Settings.ACTION_SETTINGS)); }
                }).show());
        MaterialSwitch ambient = view.findViewById(R.id.bs_switch_lockscreen_ambient);
        ambient.setChecked(prefs.isLockscreenAmbientEnabled());
        ambient.setOnCheckedChangeListener((button, checked) -> prefs.setLockscreenAmbientEnabled(checked));
        refresh.run();
        return () -> { refresh.run(); checkHealth.run(); };
    }
}
