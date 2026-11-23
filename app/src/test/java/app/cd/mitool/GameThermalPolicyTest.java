package app.cd.mitool;

import app.cd.mitool.util.GameThermalPolicy;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameThermalPolicyTest {

    @Test public void packageValidationRejectsShellSyntax() {
        assertTrue(GameThermalPolicy.validPackage("com.garena.game.kgvn"));
        assertTrue(GameThermalPolicy.validPackage("com.levelinfinite.sgameGlobal"));
        for (String bad : new String[]{
                null, "", "kgvn", "com.garena;id", "com.garena && su", "com.garena\nwhoami", "--user", "com.garena:remote"
        }) {
            assertFalse("Expected invalid package: " + bad, GameThermalPolicy.validPackage(bad));
        }
    }

    @Test public void presetsPrioritizeCoolThermalSafetyOverDisablingJoyose() {
        GameThermalPolicy.Config cool60 = GameThermalPolicy.presetConfig(GameThermalPolicy.Preset.COOL_60);
        assertEquals(60, cool60.targetFps);
        assertEquals("0.8", cool60.downscale);
        assertEquals(GameThermalPolicy.MODE_STANDARD, cool60.gameMode);
        assertTrue(cool60.keepJoyoseEnabled);
        assertTrue(cool60.precompileProfile);
        assertTrue(cool60.trimCachedProcesses);

        GameThermalPolicy.Config balanced90 = GameThermalPolicy.presetConfig(GameThermalPolicy.Preset.BALANCED_90);
        assertEquals(90, balanced90.targetFps);
        assertEquals("0.85", balanced90.downscale);
        assertTrue(balanced90.keepJoyoseEnabled);
    }

    @Test public void gameManagerCommandsRespectApiLevelsAndWhitelists() {
        assertNull(GameThermalPolicy.buildGameModeCommand(30, "com.garena.game.kgvn", "standard"));
        assertEquals("cmd game mode standard com.garena.game.kgvn",
                GameThermalPolicy.buildGameModeCommand(31, "com.garena.game.kgvn", "standard"));
        assertEquals("cmd game mode standard com.garena.game.kgvn",
                GameThermalPolicy.buildGameModeCommand(33, "com.garena.game.kgvn", "performance;reboot"));

        assertNull(GameThermalPolicy.buildGameInterventionCommand(32, "com.garena.game.kgvn", 60, "0.8"));
        assertEquals("cmd game set --fps 60 --downscale 0.8 com.garena.game.kgvn",
                GameThermalPolicy.buildGameInterventionCommand(33, "com.garena.game.kgvn", 60, "0.8"));
        assertEquals("cmd game set --fps disable --downscale disable com.garena.game.kgvn",
                GameThermalPolicy.buildGameInterventionCommand(35, "com.garena.game.kgvn", 999, "0.1"));

        assertEquals("cmd game reset com.garena.game.kgvn",
                GameThermalPolicy.buildGameResetCommand(34, "com.garena.game.kgvn"));
        assertEquals("cmd game mode standard com.garena.game.kgvn",
                GameThermalPolicy.buildGameResetCommand(31, "com.garena.game.kgvn"));
    }

    @Test public void compileCommandAvoidsForcedRecompileHeat() {
        String cmd = GameThermalPolicy.buildCompileSpeedProfileCommand("com.garena.game.kgvn");
        assertEquals("cmd package compile -m speed-profile com.garena.game.kgvn", cmd);
        assertFalse(cmd.contains(" -f "));
    }

    @Test public void dexoptParserDetectsAlreadyCompiledPackages() {
        assertTrue(GameThermalPolicy.isAlreadySpeedProfileCompiled(
                "Dexopt state:\n  [com.garena.game.kgvn]\n    path: /data/app/base.apk\n      arm64: [status=speed-profile] [reason=bg-dexopt]\n"));
        assertFalse(GameThermalPolicy.isAlreadySpeedProfileCompiled(
                "Dexopt state:\n  [com.garena.game.kgvn]\n      arm64: [status=verify] [reason=install]\n"));
        assertFalse(GameThermalPolicy.isAlreadySpeedProfileCompiled(""));
    }

    @Test public void displayHzAlignmentOnlySelectsExactSupportedStep() {
        assertEquals(60, GameThermalPolicy.recommendedDisplayHz(60, Arrays.asList(60, 120)));
        assertEquals(-1, GameThermalPolicy.recommendedDisplayHz(90, Arrays.asList(60, 120)));
        assertEquals(90, GameThermalPolicy.recommendedDisplayHz(90, Arrays.asList(60, 90, 120)));
        assertEquals(-1, GameThermalPolicy.recommendedDisplayHz(0, Arrays.asList(60, 120)));
        assertEquals(-1, GameThermalPolicy.recommendedDisplayHz(60, Collections.emptyList()));
    }

    @Test public void gameConfigsVerificationDoesNotFakeSuccessWhenEmptyOrUnsupported() {
        GameThermalPolicy.GameConfigVerification empty =
                GameThermalPolicy.parseGameConfigsOutput("No configurations found", "", 60, "0.8");
        assertFalse(empty.supported);
        assertFalse(empty.activeOverride);

        GameThermalPolicy.GameConfigVerification ok =
                GameThermalPolicy.parseGameConfigsOutput(
                        "com.garena.game.kgvn:\n  mode=1, downscaleFactor=0.8, fps=60", "", 60, "0.8");
        assertTrue(ok.supported);
        assertTrue(ok.activeOverride);

        GameThermalPolicy.GameConfigVerification mismatch =
                GameThermalPolicy.parseGameConfigsOutput(
                        "com.garena.game.kgvn:\n  mode=1, downscaleFactor=disable, fps=0", "", 60, "0.8");
        assertTrue(mismatch.supported);
        assertFalse(mismatch.activeOverride);
    }
}
