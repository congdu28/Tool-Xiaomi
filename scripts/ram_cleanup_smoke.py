"""Tests only synthetic RAM probe apps on an isolated emulator; never use on a phone."""
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


# Requires official Shizuku running and authorized for Mi Tool, plus the two
# no-code fixture APKs described in docs/ram-cleanup-v1.0.61.md.
target = "app.cd.mitool.ramprobe.target"
protected = "app.cd.mitool.ramprobe.protected"
def running(pkg):
    process = subprocess.run([args.adb, "-s", args.serial, "shell", "pidof", pkg], capture_output=True)
    return process.returncode == 0 and bool(process.stdout.strip())

def scroll_list():
    area = next((n for n in tree().iter("node") if n.get("scrollable") == "true"), None)
    if area is None:
        time.sleep(0.5)
        return
    x1, y1, x2, y2 = bounds(area)
    adb("shell", "input", "swipe", str((x1+x2)//2), str(y2-100), str((x1+x2)//2), str(y1+100), "300")

def node_for_label(label, keep=False):
    for _ in range(8):
        root = tree(); parents = {c: p for p in root.iter("node") for c in p}
        for node in root.iter("node"):
            if node.get("text") != label: continue
            if not keep: return node
            for child in parents[node].iter("node"):
                if child.get("text") == "Giữ ứng dụng này khi dọn RAM": return child
        scroll_list()
    raise AssertionError("Missing fixture control: " + label)

def tap_node(node):
    x1,y1,x2,y2 = bounds(node)
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2)); time.sleep(0.7)

adb("logcat", "-c")
adb("shell", "settings", "put", "system", "font_scale", "1.0")
adb("shell", "cmd", "uimode", "night", "no")
for pkg in (target, protected):
    assert adb("shell", "pm", "path", pkg).strip().startswith(b"package:"), "Install the isolated fixture first: " + pkg
    adb("shell", "am", "start", "-n", pkg + "/android.app.Activity")
adb("shell", "dumpsys", "deviceidle", "whitelist", "+" + protected)
adb("shell", "am", "force-stop", "app.cd.mitool")
adb("shell", "am", "start", "-n", "app.cd.mitool/.MainActivity")
open_feature("RAM", "card_tool_ram")
tap("bs_btn_boost_ram", scroll=True)
tap("ram_reload")
assert find("ram_stop_selected").get("enabled") == "false", "Nothing may be preselected."
assert node_for_label("RAM QA protected").get("enabled") == "false", "Doze-whitelisted app must be protected."
keep = node_for_label("RAM QA target", keep=True)
if keep.get("checked") == "false": tap_node(keep)
assert node_for_label("RAM QA target").get("enabled") == "false"
tap("ram_reload")
assert node_for_label("RAM QA target", keep=True).get("checked") == "true", "Protection was not saved."
tap_node(node_for_label("RAM QA target", keep=True))
assert node_for_label("RAM QA target").get("enabled") == "true"
tap_node(node_for_label("RAM QA target"))
capture("ram-cleaner-selected")
tap("ram_stop_selected")
capture("ram-cleaner-confirmation")
tap("button2")
assert running(target), "Cancel stopped the test application."
tap("ram_stop_selected")
tap("button1")
for _ in range(20):
    time.sleep(0.5)
    if not running(target): break
assert not running(target), "Target process is still running."
assert running(protected), "A protected process was stopped."
for _ in range(8):
    text = find("ram_result", scroll=True).get("text", "")
    if "Đã xác nhận dừng: 1" in text: break
    time.sleep(0.5)
assert "Đã xác nhận dừng: 1" in text, text
capture("ram-cleaner-result")
logs = adb("logcat", "-d", "-s", "AndroidRuntime:E").decode("utf-8", errors="replace")
assert "FATAL EXCEPTION" not in logs, logs
print("RAM cleanup integration passed: keep, reload, cancel, actual stop, protected process retained.", flush=True)
