package app.cd.mitool.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import app.cd.mitool.R;
import app.cd.mitool.util.GameThermalPolicy;
import app.cd.mitool.util.GamingFpsHelper;
import app.cd.mitool.util.PreferencesHelper;

public final class GameThermalUi {

    private GameThermalUi() {}

    public static Runnable bind(Activity activity, View view, Runnable onStateChanged) {
        PreferencesHelper prefs = new PreferencesHelper(activity);

        TextView tvThermalStatus = view.findViewById(R.id.bs_tv_thermal_status);
        TextView tvTargetGame = view.findViewById(R.id.bs_tv_thermal_target_game);
        MaterialButton btnPickGame = view.findViewById(R.id.bs_btn_pick_thermal_game);

        MaterialButton btnPresetCool60 = view.findViewById(R.id.bs_btn_preset_cool_60);
        MaterialButton btnPresetBalanced90 = view.findViewById(R.id.bs_btn_preset_balanced_90);
        MaterialButton btnPresetCustom = view.findViewById(R.id.bs_btn_preset_custom);

        View layoutCustom = view.findViewById(R.id.bs_layout_thermal_custom);
        MaterialButton btnFps = view.findViewById(R.id.bs_btn_thermal_fps);
        MaterialButton btnScale = view.findViewById(R.id.bs_btn_thermal_scale);
        MaterialButton btnMode = view.findViewById(R.id.bs_btn_thermal_mode);
        MaterialSwitch swAlignHz = view.findViewById(R.id.bs_switch_thermal_align_hz);
        MaterialSwitch swPrecompile = view.findViewById(R.id.bs_switch_thermal_precompile);
        MaterialSwitch swTrimBg = view.findViewById(R.id.bs_switch_thermal_trim_bg);

        MaterialButton btnApply = view.findViewById(R.id.bs_btn_apply_thermal_opt);
        MaterialButton btnApplyLaunch = view.findViewById(R.id.bs_btn_apply_and_launch_game);
        MaterialButton btnRestore = view.findViewById(R.id.bs_btn_restore_thermal_opt);
        MaterialButton btnRamCleaner = view.findViewById(R.id.bs_btn_open_ram_from_game);

        TextView tvResultTitle = view.findViewById(R.id.bs_tv_thermal_result_title);
        TextView tvResultDesc = view.findViewById(R.id.bs_tv_thermal_result_desc);

        final String[] targetPkg = {GamingFpsHelper.resolveTargetGamePackage(activity)};
        final GameThermalPolicy.Preset[] activePreset = {parsePreset(prefs.getGameOptPreset())};
        final boolean[] syncingUi = {false};
        final boolean[] busy = {false};

        Runnable renderConfigControls = () -> {
            syncingUi[0] = true;
            GamingFpsHelper.GameEntry entry = GamingFpsHelper.getGameEntry(activity, targetPkg[0]);
            String label = entry != null ? entry.title : targetPkg[0];
            boolean installed = GamingFpsHelper.isPackageInstalled(activity, targetPkg[0]);
            if (tvTargetGame != null) {
                tvTargetGame.setText(installed
                        ? "Mục tiêu: " + label + " (" + targetPkg[0] + ")"
                        : "Chưa cài đặt game mục tiêu (" + targetPkg[0] + ")");
            }

            stylePresetButton(activity, btnPresetCool60, activePreset[0] == GameThermalPolicy.Preset.COOL_60);
            stylePresetButton(activity, btnPresetBalanced90, activePreset[0] == GameThermalPolicy.Preset.BALANCED_90);
            stylePresetButton(activity, btnPresetCustom, activePreset[0] == GameThermalPolicy.Preset.CUSTOM);

            if (layoutCustom != null) {
                layoutCustom.setVisibility(activePreset[0] == GameThermalPolicy.Preset.CUSTOM ? View.VISIBLE : View.GONE);
            }

            GameThermalPolicy.Config cfg = currentConfig(prefs, activePreset[0]);
            if (btnFps != null) {
                btnFps.setText("Trần FPS: " + (cfg.targetFps > 0 ? cfg.targetFps + " FPS" : "Mặc định"));
            }
            if (btnScale != null) {
                btnScale.setText("Scale GPU: " + formatDownscaleLabel(cfg.downscale));
            }
            if (btnMode != null) {
                btnMode.setText("Game Mode: " + formatModeLabel(cfg.gameMode));
            }
            if (swAlignHz != null) swAlignHz.setChecked(cfg.alignDisplayHz);
            if (swPrecompile != null) swPrecompile.setChecked(cfg.precompileProfile);
            if (swTrimBg != null) swTrimBg.setChecked(cfg.trimCachedProcesses);
            syncingUi[0] = false;
        };

        Runnable refreshInspection = () -> {
            renderConfigControls.run();
            GamingFpsHelper.inspectThermalState(activity, targetPkg[0], info -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (tvThermalStatus != null) {
                    String thermalLabel = info.thermalStatus >= 0
                            ? GameThermalPolicy.thermalStatusLabel(info.thermalStatus)
                            : "Không báo cáo";
                    tvThermalStatus.setText(String.format(Locale.getDefault(),
                            "Nhiệt độ pin: %.1f°C · Nhiệt hệ thống: %s",
                            info.batteryTempC, thermalLabel));
                    int colorRes = (info.batteryTempC >= 43.0f || info.thermalStatus >= 3)
                            ? R.color.neon_red
                            : (info.batteryTempC >= 40.0f || GameThermalPolicy.isThrottlingThermalStatus(info.thermalStatus))
                                    ? R.color.mi_orange
                                    : R.color.neon_cyan;
                    tvThermalStatus.setTextColor(ContextCompat.getColor(activity, colorRes));
                }
                if (tvResultDesc != null && tvResultTitle != null) {
                    String savedSummary = prefs.getGameOptLastSummary();
                    if (info.hasShizuku && info.gameConfigSummary != null && !info.gameConfigSummary.isEmpty()) {
                        tvResultTitle.setText(info.interventionActive
                                ? "GameManager đang áp dụng cấu hình cho " + targetPkg[0]
                                : "Trạng thái GameManager hiện tại");
                        tvResultDesc.setText(info.gameConfigSummary + (savedSummary.isEmpty() ? "" : "\n\n" + savedSummary));
                    } else if (!savedSummary.isEmpty()) {
                        tvResultTitle.setText("Lần tối ưu gần nhất");
                        tvResultDesc.setText(savedSummary);
                    }
                }
            });
        };

        if (btnPickGame != null) {
            btnPickGame.setOnClickListener(v -> showGameSelector(activity, prefs, targetPkg[0], picked -> {
                targetPkg[0] = picked;
                prefs.setGameOptTargetPackage(picked);
                refreshInspection.run();
            }));
        }

        if (btnPresetCool60 != null) {
            btnPresetCool60.setOnClickListener(v -> {
                activePreset[0] = GameThermalPolicy.Preset.COOL_60;
                prefs.setGameOptPreset(activePreset[0].name());
                renderConfigControls.run();
            });
        }
        if (btnPresetBalanced90 != null) {
            btnPresetBalanced90.setOnClickListener(v -> {
                activePreset[0] = GameThermalPolicy.Preset.BALANCED_90;
                prefs.setGameOptPreset(activePreset[0].name());
                renderConfigControls.run();
            });
        }
        if (btnPresetCustom != null) {
            btnPresetCustom.setOnClickListener(v -> {
                activePreset[0] = GameThermalPolicy.Preset.CUSTOM;
                prefs.setGameOptPreset(activePreset[0].name());
                renderConfigControls.run();
            });
        }

        if (btnFps != null) {
            final int[] fpsChoices = {0, 60, 90, 120};
            final String[] fpsLabels = {"Mặc định game", "60 FPS (Mát máy nhất)", "90 FPS (Cân bằng)", "120 FPS (Dễ nóng)"};
            btnFps.setOnClickListener(v -> new MaterialAlertDialogBuilder(activity)
                    .setTitle("Chọn trần FPS (GameManager)")
                    .setItems(fpsLabels, (d, which) -> {
                        activePreset[0] = GameThermalPolicy.Preset.CUSTOM;
                        prefs.setGameOptPreset(GameThermalPolicy.Preset.CUSTOM.name());
                        prefs.setGameOptFps(fpsChoices[which]);
                        renderConfigControls.run();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show());
        }

        if (btnScale != null) {
            final String[] scaleValues = {GameThermalPolicy.DOWNSCALE_DISABLE, "0.9", "0.85", "0.8", "0.75"};
            final String[] scaleLabels = {"100% (Không giảm)", "90% (Sắc nét, nhẹ hơn)", "85% (Cân bằng)", "80% (Giảm nhiệt rõ rệt)", "75% (Mát nhất)"};
            btnScale.setOnClickListener(v -> new MaterialAlertDialogBuilder(activity)
                    .setTitle("Tỉ lệ dựng hình GPU (Downscale)")
                    .setItems(scaleLabels, (d, which) -> {
                        activePreset[0] = GameThermalPolicy.Preset.CUSTOM;
                        prefs.setGameOptPreset(GameThermalPolicy.Preset.CUSTOM.name());
                        prefs.setGameOptDownscale(scaleValues[which]);
                        renderConfigControls.run();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show());
        }

        if (btnMode != null) {
            final String[] modeValues = {GameThermalPolicy.MODE_STANDARD, GameThermalPolicy.MODE_BATTERY, GameThermalPolicy.MODE_PERFORMANCE};
            final String[] modeLabels = {"Standard (Ổn định nhiệt)", "Battery (Tiết kiệm nhiệt tối đa)", "Performance (Hiệu năng cao - dễ nóng)"};
            btnMode.setOnClickListener(v -> new MaterialAlertDialogBuilder(activity)
                    .setTitle("Chế độ Game Mode")
                    .setItems(modeLabels, (d, which) -> {
                        activePreset[0] = GameThermalPolicy.Preset.CUSTOM;
                        prefs.setGameOptPreset(GameThermalPolicy.Preset.CUSTOM.name());
                        prefs.setGameOptMode(modeValues[which]);
                        renderConfigControls.run();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show());
        }

        if (swAlignHz != null) {
            swAlignHz.setOnCheckedChangeListener((b, checked) -> {
                if (syncingUi[0]) return;
                activePreset[0] = GameThermalPolicy.Preset.CUSTOM;
                prefs.setGameOptPreset(GameThermalPolicy.Preset.CUSTOM.name());
                prefs.setGameOptAlignHz(checked);
                renderConfigControls.run();
            });
        }
        if (swPrecompile != null) {
            swPrecompile.setOnCheckedChangeListener((b, checked) -> {
                if (syncingUi[0]) return;
                activePreset[0] = GameThermalPolicy.Preset.CUSTOM;
                prefs.setGameOptPreset(GameThermalPolicy.Preset.CUSTOM.name());
                prefs.setGameOptPrecompile(checked);
                renderConfigControls.run();
            });
        }
        if (swTrimBg != null) {
            swTrimBg.setOnCheckedChangeListener((b, checked) -> {
                if (syncingUi[0]) return;
                activePreset[0] = GameThermalPolicy.Preset.CUSTOM;
                prefs.setGameOptPreset(GameThermalPolicy.Preset.CUSTOM.name());
                prefs.setGameOptTrimBg(checked);
                renderConfigControls.run();
            });
        }

        java.util.function.Consumer<Boolean> runApply = launchAfter -> {
            if (busy[0]) return;
            busy[0] = true;
            if (btnApply != null) btnApply.setEnabled(false);
            if (btnApplyLaunch != null) btnApplyLaunch.setEnabled(false);
            if (tvResultTitle != null) {
                tvResultTitle.setText(launchAfter ? "Đang tối ưu & chuẩn bị mở game..." : "Đang áp dụng qua Shizuku...");
            }
            GameThermalPolicy.Config cfg = currentConfig(prefs, activePreset[0]);
            GamingFpsHelper.applyThermalOptimization(activity, targetPkg[0], cfg, res -> {
                busy[0] = false;
                if (btnApply != null) btnApply.setEnabled(true);
                if (btnApplyLaunch != null) btnApplyLaunch.setEnabled(true);
                if (tvResultTitle != null) {
                    tvResultTitle.setText(res.success ? "Đã áp dụng tối ưu chống quá nhiệt" : "Chưa áp dụng thành công");
                    tvResultTitle.setTextColor(ContextCompat.getColor(activity, res.success ? R.color.neon_green : R.color.mi_orange));
                }
                if (tvResultDesc != null && res.details != null && !res.details.isEmpty()) {
                    tvResultDesc.setText(res.details);
                }
                Toast.makeText(activity, res.headline, Toast.LENGTH_LONG).show();
                refreshInspection.run();
                if (onStateChanged != null) onStateChanged.run();
                if (launchAfter && res.success && !GamingFpsHelper.launchGame(activity, targetPkg[0])) {
                    Toast.makeText(activity, "Không thể mở " + targetPkg[0], Toast.LENGTH_SHORT).show();
                }
            });
        };

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> runApply.accept(false));
        }

        if (btnApplyLaunch != null) {
            btnApplyLaunch.setOnClickListener(v -> runApply.accept(true));
        }

        if (btnRestore != null) {
            btnRestore.setOnClickListener(v -> {
                if (busy[0]) return;
                busy[0] = true;
                btnRestore.setEnabled(false);
                GamingFpsHelper.restoreThermalOptimization(activity, targetPkg[0], res -> {
                    busy[0] = false;
                    btnRestore.setEnabled(true);
                    if (tvResultTitle != null) {
                        tvResultTitle.setText(res.success ? "Đã khôi phục mặc định" : "Chưa khôi phục được");
                        tvResultTitle.setTextColor(ContextCompat.getColor(activity, res.success ? R.color.neon_cyan : R.color.mi_orange));
                    }
                    if (tvResultDesc != null && res.details != null && !res.details.isEmpty()) {
                        tvResultDesc.setText(res.details);
                    }
                    Toast.makeText(activity, res.headline, Toast.LENGTH_LONG).show();
                    refreshInspection.run();
                    if (onStateChanged != null) onStateChanged.run();
                });
            });
        }

        if (btnRamCleaner != null) {
            btnRamCleaner.setOnClickListener(v -> RamCleanerUi.show(activity, onStateChanged));
        }

        refreshInspection.run();
        return refreshInspection;
    }

    private static GameThermalPolicy.Preset parsePreset(String name) {
        try {
            return GameThermalPolicy.Preset.valueOf(name);
        } catch (Exception ignored) {
            return GameThermalPolicy.Preset.COOL_60;
        }
    }

    private static GameThermalPolicy.Config currentConfig(PreferencesHelper prefs, GameThermalPolicy.Preset preset) {
        if (preset == GameThermalPolicy.Preset.CUSTOM) {
            return new GameThermalPolicy.Config(
                    GameThermalPolicy.Preset.CUSTOM,
                    prefs.getGameOptMode(),
                    prefs.getGameOptFps(),
                    prefs.getGameOptDownscale(),
                    prefs.isGameOptAlignHz(),
                    prefs.isGameOptPrecompile(),
                    prefs.isGameOptTrimBg(),
                    true
            );
        }
        return GameThermalPolicy.presetConfig(preset);
    }

    private static String formatDownscaleLabel(String downscale) {
        if (GameThermalPolicy.DOWNSCALE_DISABLE.equals(downscale)) return "100% (Gốc)";
        try {
            return Math.round(Float.parseFloat(downscale) * 100) + "%";
        } catch (NumberFormatException ignored) {
            return downscale;
        }
    }

    private static String formatModeLabel(String mode) {
        if (GameThermalPolicy.MODE_BATTERY.equals(mode)) return "Battery (Mát)";
        if (GameThermalPolicy.MODE_PERFORMANCE.equals(mode)) return "Performance";
        return "Standard (Cân bằng)";
    }

    private static void stylePresetButton(Activity activity, MaterialButton button, boolean selected) {
        if (button == null) return;
        int bgColor = ContextCompat.getColor(activity, selected ? R.color.mi_orange : R.color.card_highlight);
        int textColor = ContextCompat.getColor(activity, selected ? R.color.white : R.color.text_primary);
        button.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        button.setTextColor(textColor);
    }

    private static void showGameSelector(Activity activity, PreferencesHelper prefs, String currentPkg,
                                         java.util.function.Consumer<String> onPicked) {
        Set<String> candidates = new LinkedHashSet<>();
        for (String lq : GamingFpsHelper.LIEN_QUAN_PACKAGES) {
            if (GamingFpsHelper.isPackageInstalled(activity, lq)) candidates.add(lq);
        }
        for (String saved : prefs.getCustomGamePackages()) {
            if (GamingFpsHelper.isPackageInstalled(activity, saved)) candidates.add(saved);
        }
        for (String detected : GamingFpsHelper.detectInstalledGames(activity)) {
            if (GamingFpsHelper.isPackageInstalled(activity, detected)) candidates.add(detected);
        }
        if (candidates.isEmpty()) {
            Toast.makeText(activity, "Chưa phát hiện game nào được cài đặt.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> pkgs = new ArrayList<>(candidates);
        String[] labels = new String[pkgs.size()];
        int checked = 0;
        for (int i = 0; i < pkgs.size(); i++) {
            String pkg = pkgs.get(i);
            GamingFpsHelper.GameEntry entry = GamingFpsHelper.getGameEntry(activity, pkg);
            labels[i] = (entry != null ? entry.title : pkg) + "\n" + pkg;
            if (pkg.equals(currentPkg)) checked = i;
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Chọn game để tối ưu chống nóng")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    onPicked.accept(pkgs.get(which));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
