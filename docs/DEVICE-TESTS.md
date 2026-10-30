# Xiaomi device acceptance checklist

No physical-device results have been recorded. Fill this independently for **Box 1** and **Box 2**, including model, Android build, firmware, date and tester. Never infer one box's outcome from the other.

| Check | Box 1 | Box 2 |
| --- | --- | --- |
| Install APK / visible TV banner | Pending | Pending |
| Launch as ordinary app | Pending | Pending |
| Home role chooser available / current default accurate | Pending | Pending |
| Home button returns to RC TV | Pending | Pending |
| Activity restart / full reboot | Pending | Pending |
| Restore stock Home / Home button verified | Pending | Pending |
| Remote arrows, select, back, held arrows | Pending | Pending |
| Keyboard typing, navigation / mouse click | Pending | Pending |
| Offline discovery and launch preferred installed apps | Pending | Pending |
| Pin / unpin survives force stop and restart | Pending | Pending |
| App install / uninstall refresh | Pending | Pending |
| Open external app and return / focus location | Pending | Pending |
| 1080p / 4K, overscan and 3-metre readability | Pending | Pending |
| Low memory: no blank Home or crash | Pending | Pending |
| Cold/warm start, memory, input latency | Not measured | Not measured |
| Wallpaper rotation / ambient | Not implemented | Not implemented |
| USB and file operations | Not implemented | Not implemented |
| Speed test and history | Not implemented | Not implemented |
| Profiles / backup restore | Not implemented | Not implemented |

Record time-to-interactive from repeated cold and warm starts, memory via Android Studio profiler or authorised `adb shell dumpsys meminfo za.co.rc.tv`, and frame timing via profiler/system trace. Measure on both 2 GB devices; do not promise 60 Hz or device boot time from desktop compilation. First hardware gate passes only after Home selection, reboot and safe recovery succeed, or an explicit firmware limitation is recorded and ordinary app use confirmed.
