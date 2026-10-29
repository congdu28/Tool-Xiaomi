package app.cd.mitool;
import app.cd.mitool.util.PowerConnectionPolicy;
import app.cd.mitool.util.AudioModePolicy;
import org.junit.Test;
import static org.junit.Assert.*;
public class PowerAudioPolicyTest {
    @Test public void initialStickyBatteryDoesNotAnnounceAConnection() {
        assertFalse(new PowerConnectionPolicy(true).observe(true, false));
        assertFalse(new PowerConnectionPolicy(false).observe(false, false));
    }
    @Test public void batteryAndPowerBroadcastsProduceOnlyOneSound() {
        PowerConnectionPolicy p = new PowerConnectionPolicy(false);
        assertTrue(p.observe(true, false));
        assertFalse(p.observe(true, true));
        assertFalse(p.observe(true, false));
        assertTrue(p.observe(false, true));
        assertFalse(p.observe(false, false));
        assertFalse(p.observe(false, true));
    }
    @Test public void coldServiceCanAcceptAnActualPowerEventOnce() {
        PowerConnectionPolicy p = new PowerConnectionPolicy(true);
        assertTrue(p.observe(true, true));
        assertFalse(p.observe(true, true));
    }
    @Test public void realRapidTransitionsAreNotSuppressedByATimeWindow() {
        PowerConnectionPolicy p = new PowerConnectionPolicy(false);
        assertTrue(p.observe(true, true)); assertTrue(p.observe(false, true)); assertTrue(p.observe(true, true));
    }
    @Test public void anInactiveLegacyVoiceMasterDoesNotEnableStoredVoiceFlags() {
        assertEquals(0, AudioModePolicy.combine(false, false, true));
        assertEquals(1, AudioModePolicy.combine(true, false, true));
    }
    @Test public void allFourModesPreserveIndependentRingAndVoiceSelections() {
        assertEquals(0, AudioModePolicy.combine(false, true, false));
        assertEquals(1, AudioModePolicy.combine(true, true, false));
        assertEquals(2, AudioModePolicy.combine(false, true, true));
        assertEquals(3, AudioModePolicy.combine(true, true, true));
    }
}
