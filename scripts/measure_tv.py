"""Repeatable, local-only RC TV profiling. Does not install, clear data or change Home.

Requires the same-signed profile APK already installed and an authorised ADB device.
Only the app's motion preference is changed; restore the user's value after testing.
"""
import argparse
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--adb", required=True)
parser.add_argument("--serial", required=True)
parser.add_argument("--label", required=True)
parser.add_argument("--motion", choices=["Minimal", "Balanced", "Cinematic"], required=True)
parser.add_argument("--out", default="releases/diagnostics/back-performance")
args = parser.parse_args()
if not re.fullmatch(r"[a-zA-Z0-9_-]+", args.label):
    parser.error("label must contain only letters, numbers, underscore or hyphen")
out = Path(args.out)
out.mkdir(parents=True, exist_ok=True)
prefix = out / args.label


def adb(*command, stdin=None):
    result = subprocess.run([args.adb, "-s", args.serial, *command],
                            capture_output=True, input=stdin, text=True, encoding="utf-8", errors="replace", timeout=60)
    if result.returncode:
        raise RuntimeError(f"ADB failed: {command}: {result.stderr}")
    return result.stdout


def save(suffix, text):
    Path(str(prefix) + suffix).write_text(text, encoding="utf-8")


def ui():
    adb("shell", "uiautomator", "dump", "/sdcard/rc-tv-profile-ui.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/rc-tv-profile-ui.xml"))


def tap(node):
    coords = list(map(int, re.findall(r"\d+", node.attrib["bounds"])))
    adb("shell", "input", "tap", str((coords[0] + coords[2]) // 2), str((coords[1] + coords[3]) // 2))


def launch():
    adb("shell", "am", "force-stop", "za.co.rc.tv")
    result = adb("shell", "am", "start", "-W", "-n", "za.co.rc.tv/.MainActivity")
    if "Activity: za.co.rc.tv/.MainActivity" not in result:
        raise RuntimeError("RC TV did not become the launch target: " + result)
    time.sleep(2)
    return result


print(f"{args.label}: configure motion and cold start", flush=True)
launch()
settings = next(n for n in ui().iter("node") if n.get("content-desc") == "Settings")
tap(settings)
mode = [n for n in ui().iter("node") if n.get("text") == args.motion][-1]  # Cinematic also names the theme.
tap(mode)
adb("shell", "input", "keyevent", "KEYCODE_BACK")
save("-cold-start.txt", launch())
pid = adb("shell", "pidof", "za.co.rc.tv").strip()

workload = "#!/system/bin/sh\n" + "\n".join(
    f"input keyevent {key}\nsleep 0.3" for _ in range(3)
    for key in [22, 22, 22, 22, 21, 21, 21, 21]) + "\n"
workfile = Path(str(prefix) + "-workload.sh")
workfile.write_text(workload, encoding="utf-8", newline="\n")
adb("push", str(workfile), "/data/local/tmp/rc-tv-focus-workload.sh")
print(f"{args.label}: unmeasured warm-up", flush=True)
adb("shell", "sh", "/data/local/tmp/rc-tv-focus-workload.sh")
time.sleep(1)
traceconfig = '''buffers { size_kb: 16384 fill_policy: RING_BUFFER }
duration_ms: 30000
data_sources { config { name: "linux.ftrace" ftrace_config {
  ftrace_events: "sched/sched_switch"
  ftrace_events: "sched/sched_waking"
  atrace_categories: "gfx"
  atrace_categories: "view"
  atrace_categories: "dalvik"
  atrace_categories: "input"
  atrace_apps: "za.co.rc.tv"
} } }
'''
config = Path(str(prefix) + ".cfg")
config.write_text(traceconfig, encoding="utf-8", newline="\n")
device_trace = f"/data/misc/perfetto-traces/rc-tv-{args.label}.trace"
trace_start = time.monotonic()
save("-trace-start.txt", adb("shell", "perfetto", "--background", "--txt", "-c",
     "-", "-o", device_trace, stdin=traceconfig))
results = []
for run in range(1, 3):
    print(f"{args.label}: measured pass {run}", flush=True)
    adb("shell", "dumpsys", "gfxinfo", "za.co.rc.tv", "reset")
    adb("shell", "sh", "/data/local/tmp/rc-tv-focus-workload.sh")
    time.sleep(1)
    raw = adb("shell", "dumpsys", "gfxinfo", "za.co.rc.tv", "framestats")
    save(f"-run{run}-frames.txt", raw)
    metrics = {"run": run}
    for label, pattern in {
        "frames": r"Total frames rendered: (\d+)",
        "jank_percent": r"Janky frames: \d+ \(([\d.]+)%\)",
        "p50_ms": r"50th percentile: (\d+)ms",
        "p90_ms": r"90th percentile: (\d+)ms",
        "p95_ms": r"95th percentile: (\d+)ms",
    }.items():
        match = re.search(pattern, raw)
        metrics[label] = float(match[1]) if match else None
    results.append(metrics)
save("-memory.txt", adb("shell", "dumpsys", "meminfo", "za.co.rc.tv"))
adb("shell", "input", "keyevent", "KEYCODE_HOME")  # triggers profile-only counter snapshot
save("-counters.txt", adb("logcat", "-d", "--pid=" + pid, "-v", "brief", "-s", "RcProfile"))
remaining = 31 - (time.monotonic() - trace_start)
if remaining > 0:
    time.sleep(remaining)
adb("pull", device_trace, str(prefix) + ".perfetto-trace")
adb("shell", "am", "start", "-W", "-n", "za.co.rc.tv/.MainActivity")
result = {"label": args.label, "motion": args.motion, "pid": pid, "runs": results}
save("-summary.json", json.dumps(result, indent=2))
print(json.dumps(result, indent=2), flush=True)
