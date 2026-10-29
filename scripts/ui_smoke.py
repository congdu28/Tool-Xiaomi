"""Exercise the real Android UI on an isolated emulator; writes screenshots under app/build.

Usage: python scripts/ui_smoke.py --adb PATH_TO_ADB [--serial emulator-5554]
Clears Mi Tool data on the isolated emulator. Never confirms destructive system actions.
"""
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
adb("logcat", "-c")
adb("shell", "pm", "grant", "app.cd.mitool", "android.permission.POST_NOTIFICATIONS")
adb("shell", "settings", "put", "system", "font_scale", "1.0")
adb("shell", "cmd", "uimode", "night", "no")
adb("shell", "am", "force-stop", "app.cd.mitool")
adb("shell", "am", "start", "-n", "app.cd.mitool/.MainActivity")
time.sleep(1)
capture("overview-light")
for tab in ("nav_tools", "nav_battery", "nav_settings"):
    tap(tab)
    capture(tab + "-light")

open_feature("dns", "card_tool_private_dns")
capture("dns-light")
tap("bs_btn_dns_custom")
tap("dialog_value_input")
adb("shell", "input", "text", "1.1.1.1")
tap("button1")
assert find("button1") is not None, "Validation must retain the input dialog."
assert any(n.get("text") == "1.1.1.1" for n in tree().iter("node")), "Invalid input was lost."
capture("dns-validation")
tap("button2")
close_sheet()

for query, identifier, name in (
    ("dong ho", "card_tool_clock", "clock"),
    ("tan so", "card_tool_hz", "refresh-rate"),
    ("RAM", "card_tool_ram", "memory"),
    ("thong bao", "card_tool_notif_fix", "notifications"),
    ("canh bao", "card_tool_battery_alert", "battery-alerts"),
    ("Joyose", "card_tool_game_fps", "gaming"),
    ("dong bang", "card_tool_debloater", "packages"),
    ("VoLTE", "card_tool_volte", "calls"),
    ("loi tat", "card_tool_miui_utils", "system-shortcuts"),
):
    open_feature(query, identifier)
    capture(name + "-light")
    if name == "notifications":
        assert find("bs_btn_batch_fix").get("enabled") == "false", "No application should be selected implicitly."
    if name == "gaming":
        tap("bs_btn_auto_unlock_120", scroll=True)
        capture("destructive-confirmation")
        assert find("button2") is not None, "The destructive action must have a cancel path."
        tap("button2")
    close_sheet()

# Monitoring state must follow the switches, with alerts independent of the clock.
open_feature("canh bao", "card_tool_battery_alert")
assert find("bs_audio_mode_full", scroll=True).get("text") == "Tắt"
toggle_full_audio()
close_sheet()
open_feature("dong ho", "card_tool_clock")
assert find("bs_switch_notif_clock").get("checked") == "false"
tap("bs_switch_notif_clock")
tap("bs_switch_notif_clock")
assert any(b"* ServiceRecord{" in line and b"app.cd.mitool/.service.NotificationClockService" in line for line in adb("shell", "dumpsys", "activity", "services", "app.cd.mitool").splitlines()), "Turning off clock stopped an enabled battery alert."
close_sheet()
open_feature("canh bao", "card_tool_battery_alert")
toggle_full_audio()
close_sheet()

adb("shell", "settings", "put", "system", "font_scale", "1.3")
adb("shell", "cmd", "uimode", "night", "yes")
time.sleep(1)
tap("nav_home")
capture("overview-dark-large-font")
tap("nav_tools")
# Clear the catalog search before capturing its default grouping.
search = find("et_tool_search")
if any(n.get("resource-id", "").endswith("/btn_tool_search_clear") for n in tree().iter("node")):
    tap("btn_tool_search_clear")
capture("tools-dark-large-font")
open_feature("dns", "card_tool_private_dns")
capture("dns-dark-large-font")
close_sheet()
logs = adb("logcat", "-d", "-s", "AndroidRuntime:E").decode("utf-8", errors="replace")
assert "FATAL EXCEPTION" not in logs, logs
print("UI smoke checks passed. Screenshots:", out, flush=True)
