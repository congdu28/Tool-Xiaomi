"""Resets Mi Tool data and simulates battery broadcasts ONLY on an isolated emulator."""
import argparse
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--adb", required=True)
parser.add_argument("--serial", default="emulator-5554")
args = parser.parse_args()
assert args.serial.startswith("emulator-"), "Use an isolated emulator for these UI checks."
out = Path(__file__).resolve().parents[1] / "app/build/ui-qa"
out.mkdir(parents=True, exist_ok=True)


def adb(*command):
    return subprocess.check_output([args.adb, "-s", args.serial, *command])


def tree():
    for attempt in range(4):
        result = adb("shell", "uiautomator", "dump", "/sdcard/mitool-ui.xml")
        if b"dumped to" in result:
            return ET.fromstring(adb("shell", "cat", "/sdcard/mitool-ui.xml"))
        time.sleep(0.5)
    raise AssertionError("Android accessibility tree was not ready.")


def bounds(node):
    return list(map(int, re.findall(r"\d+", node.get("bounds"))))


def find(identifier, scroll=False):
    for attempt in range(7 if scroll else 2):
        root = tree()
        matches = [n for n in root.iter("node") if n.get("resource-id", "").endswith("/" + identifier)]
        if matches:
            return matches[0]
        if scroll:
            area = next((n for n in root.iter("node") if n.get("scrollable") == "true"), None)
            if area is None:
                break
            x1, y1, x2, y2 = bounds(area)
            adb("shell", "input", "swipe", str((x1 + x2) // 2), str(y2 - 120), str((x1 + x2) // 2), str(y1 + 150), "350")
        else:
            time.sleep(0.4)
    raise AssertionError("Missing UI element: " + identifier)


def tap(identifier, scroll=False):
    x1, y1, x2, y2 = bounds(find(identifier, scroll))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(0.7)


def close_sheet():
    for _ in range(3):
        nodes = tree()
        if not any(n.get("resource-id", "").endswith("/sheet_close") for n in nodes.iter("node")):
            return
        tap("sheet_close")
        time.sleep(0.5)
    raise AssertionError("The feature sheet did not close.")


def capture(name):
    current = tree()
    time.sleep(0.3)
    (out / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p"))
    (out / (name + ".xml")).write_text(ET.tostring(current, encoding="unicode"), encoding="utf-8")
    print("Captured", name, flush=True)


def open_feature(query, identifier):
    tap("nav_tools")
    search = find("et_tool_search")
    if any(n.get("resource-id", "").endswith("/btn_tool_search_clear") for n in tree().iter("node")):
        tap("btn_tool_search_clear")
    tap("et_tool_search")
    if query:
        adb("shell", "input", "text", query.replace(" ", "%s"))
    tap(identifier)
    find("sheet_close")


def notification():
    output = adb("shell", "dumpsys", "notification", "--noredact").decode("utf-8", errors="replace")
    for record in output.split("NotificationRecord(")[1:]:
        heading = record.split("\n", 1)[0]
        if "pkg=app.cd.mitool " in heading and "id=4004 " in heading: return record
    return ""

def expect_notification(text):
    for _ in range(16):
        record = notification()
        if text in record: return record
        time.sleep(0.5)
    raise AssertionError("Missing notification state: " + text + "\n" + record)

def service_alive():
    output = adb("shell", "dumpsys", "activity", "services", "app.cd.mitool")
    return any(b"* ServiceRecord{" in line and b"app.cd.mitool/.service.NotificationClockService" in line
               for line in output.splitlines())


def battery(key, value): adb("shell", "dumpsys", "battery", "set", key, str(value))

def choose_audio_mode(id, label):
    tap(id, scroll=True)
    for attempt in range(3):
        time.sleep(0.5)
        for n in tree().iter("node"):
            if n.get("text") == label and n.get("resource-id") == "android:id/text1":
                x1,y1,x2,y2=bounds(n); adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2));time.sleep(0.7);break
        current=tree()
        if not any(n.get("resource-id", "").endswith("/alertTitle") for n in current.iter("node")):
            assert find(id, scroll=True).get("text")==label
            return
    raise AssertionError("Audio mode dialog did not select: " + label)
def toggle_full_audio():
    current = find("bs_audio_mode_full", scroll=True).get("text")
    choose_audio_mode("bs_audio_mode_full", "Chuông" if current == "Tắt" else "Tắt")


adb("shell", "pm", "clear", "app.cd.mitool")
adb("shell", "pm", "grant", "app.cd.mitool", "android.permission.POST_NOTIFICATIONS")
adb("logcat", "-c")
adb("shell", "settings", "put", "system", "font_scale", "1.0")
adb("shell", "cmd", "uimode", "night", "no")
try:
    adb("shell", "dumpsys", "battery", "unplug")
    battery("level", 50); battery("status", 3)
    adb("shell", "am", "start", "-n", "app.cd.mitool/.MainActivity")
    assert not service_alive(), "Monitoring started with every feature off."
    open_feature("canh bao", "card_tool_battery_alert")
    assert find("bs_switch_charging_notification").get("checked") == "false"
    tap("bs_switch_charging_notification")
    assert service_alive(), "Charging notification did not start independently of the clock."
    expect_notification("Đang dùng pin")
    capture("charging-notification-switch")
    close_sheet()
    battery("ac", 1); battery("status", 2)
    expect_notification("Đang sạc")
    capture("charging-connected-app")
    open_feature("canh bao", "card_tool_battery_alert")
    toggle_full_audio()
    close_sheet()
    # The launcher shortcut uses the same on/off path as the notification tile.
    for _ in range(2):
        adb("shell", "am", "start", "-n", "app.cd.mitool/.ToggleNotificationActivity")
        time.sleep(0.5)
    assert service_alive(), "Clock toggle stopped charging/alert monitoring."
    expect_notification("Mi Tool • Theo dõi pin")
    battery("level", 100); battery("status", 5)
    record = expect_notification("Pin đã đầy")
    assert "Đang dùng pin" not in record, "Connected full battery was rendered as unplugged."
    battery("ac", 0); battery("status", 3)
    expect_notification("Đang dùng pin")
    assert service_alive(), "Unplugging disabled an enabled alert."
    open_feature("canh bao", "card_tool_battery_alert")
    assert find("bs_switch_charging_notification").get("checked") == "true"
    assert find("bs_audio_mode_full", scroll=True).get("text") == "Chuông"
    # Return to the top to turn off charging status; the independent alert must keep running.
    close_sheet(); open_feature("canh bao", "card_tool_battery_alert")
    tap("bs_switch_charging_notification")
    assert service_alive(), "Turning off charging status stopped the full-battery alert."
    toggle_full_audio()
    for _ in range(10):
        if not service_alive(): break
        time.sleep(0.5)
    assert not service_alive(), "Monitoring survived after every feature was disabled."
    assert not notification(), "A stale foreground notification survived disabling every feature."
    capture("charging-all-disabled")
    close_sheet()
    adb("shell", "pm", "revoke", "app.cd.mitool", "android.permission.POST_NOTIFICATIONS")
    adb("shell", "am", "start", "-n", "app.cd.mitool/.MainActivity")
    open_feature("canh bao", "card_tool_battery_alert")
    assert "Android chặn" in find("bs_tv_notification_permission").get("text", "")
    capture("charging-notification-permission-blocked")
    close_sheet()
    adb("shell", "pm", "grant", "app.cd.mitool", "android.permission.POST_NOTIFICATIONS")
    adb("shell", "am", "start", "-n", "app.cd.mitool/.MainActivity")
    open_feature("canh bao", "card_tool_battery_alert")
    assert "đã được cho phép" in find("bs_tv_notification_permission").get("text", "")
    logs = adb("logcat", "-d", "-s", "AndroidRuntime:E").decode("utf-8", errors="replace")
    assert "FATAL EXCEPTION" not in logs, logs
    print("Charging notification integration passed: independent switch, plug/full/unplug, clock toggle, alerts and stop.", flush=True)
finally:
    adb("shell", "dumpsys", "battery", "reset")
