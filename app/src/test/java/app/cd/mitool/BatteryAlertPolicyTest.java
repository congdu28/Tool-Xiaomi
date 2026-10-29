package app.cd.mitool;

import app.cd.mitool.util.BatteryAlertPolicy;
import org.junit.Test;
import static org.junit.Assert.*;

public class BatteryAlertPolicyTest {
    @Test public void levelJumpPastTheThresholdStillAlertsOnce() {
        BatteryAlertPolicy p = new BatteryAlertPolicy();
        assertEquals(0, p.evaluate(true, 79, 30, 80, true, false, false));
        assertEquals(BatteryAlertPolicy.TARGET, p.evaluate(true, 86, 30, 80, true, false, false));
        assertEquals(0, p.evaluate(true, 87, 30, 80, true, false, false));
    }
    @Test public void fullOnExternalPowerAlertsWithoutRequiringActiveCharging() {
        BatteryAlertPolicy p = new BatteryAlertPolicy();
        assertEquals(BatteryAlertPolicy.FULL, p.evaluate(true, 100, 30, 80, true, true, false));
        assertEquals(0, p.evaluate(true, 100, 30, 80, true, true, false));
    }
    @Test public void unpluggedFullBatteryDoesNotTriggerChargingAlarms() {
        assertEquals(0, new BatteryAlertPolicy().evaluate(false, 100, 30, 80, true, true, false));
    }
    @Test public void unplugResetsTheNextChargingSession() {
        BatteryAlertPolicy p = new BatteryAlertPolicy();
        p.evaluate(true, 90, 30, 80, true, false, false);
        p.evaluate(false, 90, 30, 80, true, false, false);
        assertEquals(BatteryAlertPolicy.TARGET, p.evaluate(true, 90, 30, 80, true, false, false));
    }
    @Test public void temperatureUsesHysteresisAndDisabledAlarmsStaySilent() {
        BatteryAlertPolicy p = new BatteryAlertPolicy();
        assertEquals(0, p.evaluate(false, 50, 43, 80, false, false, false));
        assertEquals(BatteryAlertPolicy.HOT, p.evaluate(false, 50, 43, 80, false, false, true));
        assertEquals(0, p.evaluate(false, 50, 41, 80, false, false, true));
        p.evaluate(false, 50, 39, 80, false, false, true);
        assertEquals(BatteryAlertPolicy.HOT, p.evaluate(false, 50, 42, 80, false, false, true));
    }
    @Test public void changingTheTargetCanArmTheNewThreshold() {
        BatteryAlertPolicy p = new BatteryAlertPolicy();
        p.evaluate(true, 85, 30, 80, true, false, false);
        assertEquals(0, p.evaluate(true, 85, 30, 90, true, false, false));
        assertEquals(BatteryAlertPolicy.TARGET, p.evaluate(true, 90, 30, 90, true, false, false));
    }
}
