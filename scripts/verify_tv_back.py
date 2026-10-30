"""Device Back regression checks. Requires authorised ADB and a visible YouTube Home tile.

Launches YouTube/RC TV, visits all RC sections and the system Home. Does not pin,
uninstall, clear data, set the default Home or modify launcher preferences.
"""
import argparse
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument("--adb", required=True)
p.add_argument("--serial", required=True)
p.add_argument("--out", default="releases/diagnostics/back-performance/navigation")
args = p.parse_args()
out = Path(args.out)
out.mkdir(parents=True, exist_ok=True)
checks = []


def adb(*command):
    r = subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True,
                       text=True, encoding="utf-8", errors="replace", timeout=30)
    if r.returncode:
        raise RuntimeError(r.stderr)
    return r.stdout


def assert_foreground(package, name):
    time.sleep(.3)
    stack = adb("shell", "dumpsys", "activity", "activities")
    (out / f"{name}-stack.txt").write_text(stack, encoding="utf-8")
    resumed = next(line for line in stack.splitlines() if "mResumedActivity:" in line)
    assert package + "/" in resumed, resumed
    checks.append(name)


def ui():
    adb("shell", "uiautomator", "dump", "/sdcard/rc-tv-back-test.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/rc-tv-back-test.xml"))


def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def home(name):
    assert_foreground("za.co.rc.tv", name)
    tree = ui()
    assert any(n.get("text") in ["Quiet horizons.", "Your favourites"] for n in tree.iter("node"))


adb("shell", "am", "start", "-W", "-a", "android.intent.action.MAIN", "-c",
    "android.intent.category.LEANBACK_LAUNCHER", "-n", "za.co.rc.tv/.MainActivity")
adb("shell", "input", "keyevent", "KEYCODE_BACK")
home("normal-launch-back")
original_pid = adb("shell", "pidof", "za.co.rc.tv").strip()
# Use the actual app tile, not an implicit app-launch request.
tap(next(n for n in ui().iter("node") if n.get("text") == "YouTube"))
assert_foreground("com.google.android.youtube.tv", "youtube-tile-launch")
adb("shell", "input", "keyevent", "KEYCODE_BACK")
assert_foreground("za.co.rc.tv", "youtube-back-return")
adb("shell", "am", "start", "-W", "-n", "za.co.rc.tv/.MainActivity")
for i in range(5):
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    assert_foreground("za.co.rc.tv", f"back-after-youtube-{i+1}")
assert adb("shell", "pidof", "za.co.rc.tv").strip() == original_pid
for section in ["Search", "Apps", "Library", "Settings"]:
    tap(next(n for n in ui().iter("node") if n.get("content-desc") == section))
    assert_foreground("za.co.rc.tv", "section-" + section)
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    home("back-from-" + section)
default_home = adb("shell", "cmd", "package", "resolve-activity", "--brief", "-a",
                   "android.intent.action.MAIN", "-c", "android.intent.category.HOME")
assert "com.google.android.apps.tv.launcherx/" in default_home
adb("shell", "input", "keyevent", "KEYCODE_HOME")
assert_foreground("com.google.android.apps.tv.launcherx", "stock-home-route")
adb("shell", "am", "start", "-W", "-a", "android.intent.action.MAIN", "-c",
    "android.intent.category.LEANBACK_LAUNCHER", "-n", "za.co.rc.tv/.MainActivity")
for i in range(3):
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    assert_foreground("za.co.rc.tv", f"reopen-back-{i+1}")
adb("shell", "screencap", "-p", "/sdcard/rc-tv-back-test.png")
adb("pull", "/sdcard/rc-tv-back-test.png", str(out / "home.png"))
(out / "checks.json").write_text(json.dumps(checks, indent=2), encoding="utf-8")
print(json.dumps({"passed": len(checks), "checks": checks}, indent=2), flush=True)
