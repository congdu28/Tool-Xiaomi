"""Uses a synthetic PCM TTS fixture on an isolated emulator. Not a Vietnamese voice quality test."""
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


def choose(id, label):
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
def logs(): return adb("logcat","-d","-s","MiToolAudio:I").decode("utf-8",errors="replace")
def expect_log(value):
    for _ in range(20):
        text=logs()
        if value in text:return text
        time.sleep(0.5)
    raise AssertionError("Missing audio evidence: "+value+"\n"+text)
def set_battery(key,value): adb("shell","dumpsys","battery","set",key,str(value))

pkg="app.cd.mitool.ttsprobe"
old=adb("shell","settings","get","secure","tts_default_synth").decode().strip()
assert b"package:" in adb("shell","pm","path",pkg), "Install the synthetic TTS fixture first."
try:
    adb("shell","settings","put","secure","tts_default_synth",pkg)
    adb("shell","am","force-stop","app.cd.mitool")
    adb("shell","dumpsys","battery","unplug");set_battery("status",3);set_battery("level",50)
    adb("shell","am","start","-n","app.cd.mitool/.MainActivity")
    open_feature("canh bao","card_tool_battery_alert")
    for id in ["bs_audio_mode_connect","bs_audio_mode_disconnect"]:
        if find(id,scroll=True).get("text")!="Giọng nói":choose(id,"Giọng nói")
    for _ in range(12):
        value=find("bs_tv_audio_health",scroll=True).get("text","")
        if "đã sẵn sàng" in value:break
        time.sleep(0.5)
    assert "đã sẵn sàng" in value,value
    adb("logcat","-c")
    set_battery("ac",1);set_battery("status",2)
    expect_log("voice_started")
    set_battery("ac",0);set_battery("status",3)
    for _ in range(16):
        text=logs()
        if text.count("voice_started")>=2:break
        time.sleep(0.5)
    assert text.count("voice_started")==2,text
    assert "Voice unavailable" not in text,text
    capture("audio-tts-engine-ready")
    print("Controlled TTS engine delivered both connect/disconnect through Android speech callbacks.",flush=True)
    adb("shell","am","broadcast","-a","app.cd.mitool.ttsprobe.CONFIGURE","-p",pkg,"--ez","fail","true")
    adb("logcat","-c")
    set_battery("ac",1);set_battery("status",2)
    text=expect_log("ring_started=true")
    assert text.count("event=0 mode=2")==1,text
    assert text.count("Voice unavailable")==1,text
    assert text.count("ring_started=true")==1,text
    tap("bs_btn_test_voice",scroll=True)
    for _ in range(12):
        value=find("bs_tv_audio_health",scroll=True).get("text","")
        if "Không phát được giọng nói" in value:break
        time.sleep(0.5)
    assert "Không phát được giọng nói" in value,value
    capture("audio-tts-error-visible")
    print("Asynchronous synthesis error produced one fallback and a visible error.",flush=True)
finally:
    adb("shell","am","broadcast","-a","app.cd.mitool.ttsprobe.CONFIGURE","-p",pkg,"--ez","fail","false")
    if old in ("null",""):adb("shell","settings","delete","secure","tts_default_synth")
    else:adb("shell","settings","put","secure","tts_default_synth",old)
    adb("shell","dumpsys","battery","reset")
