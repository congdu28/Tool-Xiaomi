package app.cd.mitool.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java policy and shell command builder for thermal-safe Shizuku game optimization.
 * Adapts to CPU-bound games (e.g. Arena of Valor / MOBA) vs GPU-bound 3D games instead of
 * blindly downscaling resolution or disabling vendor thermal daemons.
 */
public final class GameThermalPolicy {

    private GameThermalPolicy() {}

    public static final String MODE_STANDARD = "standard";
    public static final String MODE_PERFORMANCE = "performance";
    public static final String MODE_BATTERY = "battery";

    public static final String DOWNSCALE_DISABLE = "disable";
    public static final Set<String> VALID_DOWNSCALES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            DOWNSCALE_DISABLE, "0.9", "0.85", "0.8", "0.75", "0.7"
    )));
    public static final Set<Integer> VALID_FPS_CAPS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            0, 30, 45, 60, 90, 120
    )));
    public static final Set<String> VALID_MODES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            MODE_STANDARD, MODE_PERFORMANCE, MODE_BATTERY
    )));

    private static final Set<String> CPU_BOUND_PACKAGES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "com.garena.game.kgvn",            // Liên Quân Mobile (VN)
            "com.garena.game.kgtw",            // Truyền Thuyết Đối Quyết (TW)
            "com.garena.game.kgth",            // RoV (TH)
            "com.ngame.allstar.eu",            // Arena of Valor (EU/NA)
            "com.levelinfinite.sgameGlobal",   // Honor of Kings (Global)
            "com.tencent.tmgp.sgame",          // Vương Giả Vinh Diệu (CN)
            "com.riotgames.league.wildriftvn", // LMHT: Tốc Chiến (VN)
            "com.riotgames.league.wildrift",   // Wild Rift (Global)
            "com.mobile.legends",              // Mobile Legends: Bang Bang
            "com.roblox.client",               // Roblox
            "com.haegin.playtogether",         // Play Together
            "com.mojang.minecraftpe"           // Minecraft
    )));

    private static final Set<String> GPU_BOUND_PACKAGES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "com.miHoYo.GenshinImpact",        // Genshin Impact (Global)
            "com.miHoYo.Yuanshen",             // Genshin Impact (CN)
            "com.miHoYo.hkrpgoversea",         // Honkai: Star Rail (Global)
            "com.miHoYo.hkrpg",                // Honkai: Star Rail (CN)
            "com.HoYoverse.Nap",               // Zenless Zone Zero
            "com.kurogame.wutheringwaves.global", // Wuthering Waves
            "com.carxtech.sr",                 // CarX Street
            "com.vng.pubgmobile",              // PUBG Mobile (VN)
            "com.tencent.ig",                  // PUBG Mobile (Global)
            "com.vng.codmvn",                  // Call of Duty Mobile (VN)
            "com.activision.callofduty.shooter", // COD Mobile (Global)
            "com.dts.freefiremax"              // Free Fire MAX
    )));

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+");
    private static final Pattern DEXOPT_STATUS_PATTERN = Pattern.compile("\\[status=([a-zA-Z0-9_\\-]+)\\]");
    private static final Pattern ACTIVE_FPS_PATTERN = Pattern.compile("fps\\s*[:=]\\s*([1-9][0-9]*)");
    private static final Pattern ACTIVE_SCALE_PATTERN = Pattern.compile("downscale(?:factor)?\\s*[:=]\\s*(0\\.[0-9]+)");

    public enum WorkloadProfile {
        CPU_BOUND,
        GPU_BOUND,
        BALANCED
    }

    public enum Preset {
        COOL_60,
        BALANCED_90,
        CUSTOM,
        RESTORE_DEFAULT
    }

    public static WorkloadProfile classifyWorkload(String pkg) {
        if (pkg == null) return WorkloadProfile.BALANCED;
        String clean = pkg.trim();
        if (CPU_BOUND_PACKAGES.contains(clean)) return WorkloadProfile.CPU_BOUND;
        if (GPU_BOUND_PACKAGES.contains(clean)) return WorkloadProfile.GPU_BOUND;
        return WorkloadProfile.BALANCED;
    }

    public static String workloadLabel(WorkloadProfile profile) {
        if (profile == WorkloadProfile.CPU_BOUND) return "Thiên CPU";
        if (profile == WorkloadProfile.GPU_BOUND) return "Thiên GPU";
        return "Cân bằng CPU/GPU";
    }

    public static final class Config {
        public final Preset preset;
        public final String gameMode;
        public final int targetFps;
        public final String downscale;
        public final boolean alignDisplayHz;
        public final boolean precompileProfile;
        public final boolean trimCachedProcesses;
        public final boolean keepJoyoseEnabled;

        public Config(Preset preset, String gameMode, int targetFps, String downscale,
                      boolean alignDisplayHz, boolean precompileProfile,
                      boolean trimCachedProcesses, boolean keepJoyoseEnabled) {
            this.preset = preset != null ? preset : Preset.CUSTOM;
            this.gameMode = normalizeMode(gameMode);
            this.targetFps = normalizeFps(targetFps);
            this.downscale = normalizeDownscale(downscale);
            this.alignDisplayHz = alignDisplayHz;
            this.precompileProfile = precompileProfile;
            this.trimCachedProcesses = trimCachedProcesses;
            this.keepJoyoseEnabled = keepJoyoseEnabled;
        }
    }

    public static Config presetConfig(Preset preset) {
        return presetConfig(preset, null);
    }

    /**
     * Build preset config adapted to the game's workload profile:
     * - CPU_BOUND (e.g. Liên Quân Mobile, Honor of Kings, Wild Rift): keeps 100% native resolution
     *   (`disable` downscale) so UI/minimap stay crisp, focusing thermal savings on FPS cap (cuts CPU
     *   simulation/render-thread work), Display Hz alignment, ART pre-compilation, and CPU background relief.
     * - GPU_BOUND (e.g. Genshin Impact, Honkai: Star Rail, PUBG): adds GPU downscale (0.8 / 0.85).
     * - BALANCED (other games): gentle GPU downscale (0.85 / 0.9).
     */
    public static Config presetConfig(Preset preset, String pkg) {
        WorkloadProfile profile = classifyWorkload(pkg);
        if (preset == Preset.COOL_60) {
            String scale = profile == WorkloadProfile.CPU_BOUND ? DOWNSCALE_DISABLE
                    : (profile == WorkloadProfile.GPU_BOUND ? "0.8" : "0.85");
            return new Config(Preset.COOL_60, MODE_STANDARD, 60, scale, true, true, true, true);
        }
        if (preset == Preset.BALANCED_90) {
            String scale = profile == WorkloadProfile.CPU_BOUND ? DOWNSCALE_DISABLE
                    : (profile == WorkloadProfile.GPU_BOUND ? "0.85" : "0.9");
            return new Config(Preset.BALANCED_90, MODE_STANDARD, 90, scale, true, true, true, true);
        }
        if (preset == Preset.RESTORE_DEFAULT) {
            return new Config(Preset.RESTORE_DEFAULT, MODE_STANDARD, 0, DOWNSCALE_DISABLE, false, false, false, true);
        }
        String customScale = profile == WorkloadProfile.CPU_BOUND ? DOWNSCALE_DISABLE : "0.85";
        return new Config(Preset.CUSTOM, MODE_STANDARD, 60, customScale, false, true, true, true);
    }

    public static boolean validPackage(String pkg) {
        return pkg != null && pkg.length() <= 200 && PACKAGE_PATTERN.matcher(pkg).matches();
    }

    public static String normalizeMode(String mode) {
        if (mode == null) return MODE_STANDARD;
        String clean = mode.trim().toLowerCase(Locale.US);
        return VALID_MODES.contains(clean) ? clean : MODE_STANDARD;
    }

    public static int normalizeFps(int fps) {
        return VALID_FPS_CAPS.contains(fps) ? fps : 0;
    }

    public static String normalizeDownscale(String downscale) {
        if (downscale == null) return DOWNSCALE_DISABLE;
        String clean = downscale.trim().toLowerCase(Locale.US);
        if ("1".equals(clean) || "1.0".equals(clean) || "off".equals(clean)) {
            return DOWNSCALE_DISABLE;
        }
        return VALID_DOWNSCALES.contains(clean) ? clean : DOWNSCALE_DISABLE;
    }

    /**
     * Build `cmd game mode` command for Android 12+ (API 31+). Returns null when unsupported.
     */
    public static String buildGameModeCommand(int sdkInt, String pkg, String mode) {
        if (sdkInt < 31 || !validPackage(pkg)) return null;
        return "cmd game mode " + normalizeMode(mode) + " " + pkg;
    }

    /**
     * Build `cmd game set` intervention command for Android 13+ (API 33+). Returns null when unsupported.
     */
    public static String buildGameInterventionCommand(int sdkInt, String pkg, int targetFps, String downscale) {
        if (sdkInt < 33 || !validPackage(pkg)) return null;
        int fps = normalizeFps(targetFps);
        String scale = normalizeDownscale(downscale);
        return "cmd game set --fps " + (fps > 0 ? Integer.toString(fps) : DOWNSCALE_DISABLE)
                + " --downscale " + scale + " " + pkg;
    }

    /**
     * Build `cmd game reset` (API 33+) or `cmd game mode standard` (API 31-32) restore command.
     */
    public static String buildGameResetCommand(int sdkInt, String pkg) {
        if (sdkInt < 31 || !validPackage(pkg)) return null;
        if (sdkInt >= 33) {
            return "cmd game reset " + pkg;
        }
        return "cmd game mode " + MODE_STANDARD + " " + pkg;
    }

    public static String buildListConfigsCommand(int sdkInt, String pkg) {
        if (sdkInt < 33 || !validPackage(pkg)) return null;
        return "cmd game list-configs " + pkg;
    }

    public static String buildListModesCommand(int sdkInt, String pkg) {
        if (sdkInt < 31 || !validPackage(pkg)) return null;
        return "cmd game list-modes " + pkg;
    }

    /**
     * Pre-compile hot code paths without `-f` so already-optimized apps finish immediately without compile heat.
     */
    public static String buildCompileSpeedProfileCommand(String pkg) {
        if (!validPackage(pkg)) return null;
        return "cmd package compile -m speed-profile " + pkg;
    }

    public static String buildDexoptInspectCommand(String pkg) {
        if (!validPackage(pkg)) return null;
        return "dumpsys package " + pkg;
    }

    public static String buildTrimCachedCommand() {
        return "am kill-all";
    }

    /**
     * Cancel any in-progress background ART/dexopt job so it does not peg CPU cores and heat the SoC mid-match.
     * Safe and reversible: Android automatically reschedules dexopt when idle and charging.
     */
    public static String buildCancelBgDexoptCommand() {
        return "cmd package bg-dexopt-job --cancel";
    }

    /**
     * Choose display Hz aligned to target FPS only if the display natively exposes that exact step.
     * Never forces 120Hz when playing at 90 FPS on a 60/120Hz panel, and never forces higher Hz to "boost" FPS.
     */
    public static int recommendedDisplayHz(int targetFps, List<Integer> supportedRates) {
        if (targetFps <= 0 || supportedRates == null || supportedRates.isEmpty()) return -1;
        if (targetFps == 60 && supportedRates.contains(60)) return 60;
        if (targetFps == 90 && supportedRates.contains(90)) return 90;
        if (targetFps == 120 && supportedRates.contains(120)) return 120;
        return -1;
    }

    public static boolean isAlreadySpeedProfileCompiled(String dumpsysPackageOutput) {
        if (dumpsysPackageOutput == null || dumpsysPackageOutput.isEmpty()) return false;
        Matcher m = DEXOPT_STATUS_PATTERN.matcher(dumpsysPackageOutput);
        while (m.find()) {
            String status = m.group(1);
            if ("speed-profile".equals(status) || "speed".equals(status) || "everything".equals(status)) {
                return true;
            }
        }
        return false;
    }

    public static final class GameConfigVerification {
        public final boolean supported;
        public final boolean activeOverride;
        public final String summary;

        public GameConfigVerification(boolean supported, boolean activeOverride, String summary) {
            this.supported = supported;
            this.activeOverride = activeOverride;
            this.summary = summary;
        }
    }

    public static GameConfigVerification parseGameConfigsOutput(String stdout, String stderr, int targetFps, String downscale) {
        String out = stdout != null ? stdout.trim() : "";
        String err = stderr != null ? stderr.trim() : "";
        if (!err.isEmpty() && out.isEmpty()) {
            return new GameConfigVerification(false, false, err);
        }
        String lower = out.toLowerCase(Locale.US);
        if (lower.isEmpty() || lower.contains("no configurations") || lower.contains("unknown package") || lower.contains("not a game")) {
            return new GameConfigVerification(false, false, out.isEmpty() ? "ROM/game không báo cấu hình GameManager" : out);
        }
        boolean expectFps = normalizeFps(targetFps) > 0;
        boolean expectScale = !DOWNSCALE_DISABLE.equals(normalizeDownscale(downscale));
        boolean matchesFps = !expectFps || lower.contains("fps=" + targetFps) || lower.contains("fps: " + targetFps);
        boolean matchesScale = !expectScale || lower.contains("downscalefactor=" + downscale)
                || lower.contains("downscale=" + downscale)
                || lower.contains(downscale);
        boolean active = (expectFps || expectScale) && matchesFps && matchesScale;
        return new GameConfigVerification(true, active, out);
    }

    /**
     * Detect whether `cmd game list-configs` reports any active non-zero FPS cap or non-1.0 downscale.
     */
    public static boolean hasActiveIntervention(String listConfigsOutput) {
        if (listConfigsOutput == null || listConfigsOutput.isEmpty()) return false;
        String lower = listConfigsOutput.toLowerCase(Locale.US);
        return ACTIVE_FPS_PATTERN.matcher(lower).find() || ACTIVE_SCALE_PATTERN.matcher(lower).find();
    }

    public static String thermalStatusLabel(int status) {
        switch (status) {
            case 0: return "Bình thường";
            case 1: return "Ấm nhẹ";
            case 2: return "Nóng vừa (bắt đầu giảm xung)";
            case 3: return "Nóng cao (đang bóp FPS)";
            case 4: return "Quá nhiệt nghiêm trọng";
            case 5: return "Khẩn cấp quá nhiệt";
            case 6: return "Nguy cơ tắt máy vì nhiệt";
            default: return "Không rõ";
        }
    }

    public static boolean isThrottlingThermalStatus(int status) {
        return status >= 2;
    }
}
