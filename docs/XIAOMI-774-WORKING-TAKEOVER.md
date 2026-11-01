# Xiaomi firmware 774: verified RC TV launcher takeover

Confirmed physically by the owner after **two normal restarts**, 1 November 2026.
Sequence: original Xiaomi/Google boot animation, black interval, RC TV Cinematic Glass Home
automatically, without pressing Home. This supersedes earlier NO-GO assessments for this
specific approved two-package procedure; it does not retroactively validate earlier tests.

## Preserved working installation

- Xiaomi TV Box S 2nd Gen / MDZ-28-AA; reported model MiTV-AFKR0, codename jaws.
- Android 11 / API 30; fingerprint `Xiaomi/jaws/jaws:11/RTT0.211222.001/774:user/release-keys`.
- RC TV `1.0.0-rc1.19-dream-test` (51), source commit `02639eb`.
- Working APK: `releases/review/native-dream/RC-TV-rc1.19-dream-test.apk`.
- APK SHA-256: `cd2052769b2dccf22d24503166d6c35ce696f0327fddd0d1cb026fc83d20b5d6`.
- User 0: `za.co.rc.tv` package state DEFAULT (`enabled=0`).
- User 0: `com.google.android.tungsten.setupwraith` and
  `com.google.android.apps.tv.launcherx` both DISABLED_USER (`enabled=3`).
  The installed updated-package records are authoritative; package dumps also contain
  inactive factory-version records showing different states.
- Home role/preference and operative Home: `za.co.rc.tv/.MainActivity`.
- Remaining enabled Home fallback: `com.android.tv.settings/.system.FallbackHome`.
- No uninstallation, component override changes, firmware/root changes, or RC TV data clearing.
- Existing short Home -> Home, long Home -> RC TV Settings, D-pad and independent Android
  Settings passed physical checks before reboot. Earlier build-49 Home gestures also
  survived reboot. The successful takeover's post-reboot full remote/app checklist was
  not separately reported. Google TV Apps/dashboard/branded shortcuts may be unavailable.
- Google Ambient Mode remains selected; the native RC dream is installed but activation
  failed the earlier controlled test and original dream settings were restored.

## Preconditions and actual approved sequence

Do not apply automatically to another device. Obtain explicit approval for both packages.
Keep a verified installed RC TV, data backup, remote and independent Android Settings access.
Know the Wi-Fi password; do not rely on uninterrupted wireless ADB. On this unit the active
network ID 5 was owned by `android.uid.system:1000`, not setupwraith. Another saved network
was setupwraith-owned. Check the **active** network on any future installation. If it belongs
to setupwraith, stop and arrange owner-approved forgetting/rejoining through Android Settings
before the takeover; this can disconnect ADB. Wired ADB was not verified on this hardware.

The owner physically verified RC TV Settings -> Android Settings -> Network & Internet and
Apps -> Show system apps, and confirmed the Wi-Fi password was available. Save package
overrides, role/preferred records and private RC TV configuration before mutation.

Using the authorised PC's ADB, the successful sequence was:

```text
am start --user 0 -W -n za.co.rc.tv/.MainActivity
pm disable-user --user 0 com.google.android.tungsten.setupwraith
```

**Stop and check** Wi-Fi, ADB, Android Settings and stable navigation. These passed on 774.
Only then:

```text
pm disable-user --user 0 com.google.android.apps.tv.launcherx
cmd package set-home-activity --user 0 za.co.rc.tv/.MainActivity
cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Require RC TV as resolved Home. Verify actual foreground with `dumpsys activity activities`,
including native Home routing from another activity. Settings launches, native injected Home
-> RC TV, connected Wi-Fi, authorised ADB and both existing RC accessibility services passed.
Owner then physically verified navigation/Settings and performed two successful restarts.
Never reboot on RecoveryActivity, an unstable Home, or after loss of recovery access.
Restore immediately if the new Home is wrong or critical functionality fails.

## Exact package restoration

Prepared commands use the current authorised TCP connection; IP can change. **Not executed**
after success. Enable Google TV **before** setupwraith to avoid handing Home to RecoveryActivity.
Original package overrides were DEFAULT (0), so `enable` alone is not exact restoration.

```powershell
$adb = 'C:\Users\ruanc\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb -s 192.168.1.39:5555 shell pm enable --user 0 com.google.android.apps.tv.launcherx
& $adb -s 192.168.1.39:5555 shell pm default-state --user 0 com.google.android.apps.tv.launcherx
& $adb -s 192.168.1.39:5555 shell pm enable --user 0 com.google.android.tungsten.setupwraith
& $adb -s 192.168.1.39:5555 shell pm default-state --user 0 com.google.android.tungsten.setupwraith
# Original saved Home role already named RC TV; restore that preference if needed:
& $adb -s 192.168.1.39:5555 shell cmd package set-home-activity --user 0 za.co.rc.tv/.MainActivity
& $adb -s 192.168.1.39:5555 shell am start --user 0 -n com.google.android.apps.tv.launcherx/.home.HomeActivity
& $adb -s 192.168.1.39:5555 shell am start --user 0 -n com.android.tv.settings/.MainSettings
```

This restores the package/Home baseline; it does not remove existing Accessibility Home
interception. Turning that off requires separate approval through Android Accessibility.
If offline, use the physically verified remote -> RC TV Settings -> Android Settings route
to reconnect Wi-Fi or enable disabled system apps. The exact Enable controls under the
disabled state were not physically exercised. Recovery if RC TV and wireless ADB both fail
is not guaranteed. Do not assume a factory reset is data-preserving or universally effective.

Private backups, original/current package dumps, role/preferred records, prepared commands,
and successful boot logs are local ignored artifacts in `releases/review/user0-takeover/`.
Keep private archives out of Git: they contain user configuration/pairing material.

## Read-only black-screen timing investigation

Existing latest-boot event buffer retained:

| Event | Local log time | Interpretation |
| --- | --- | --- |
| `sf_stop_bootanim` / `wm_boot_animation_done` | 19:22:51.783 | End-of-animation marker |
| `wm_create_activity` RC TV MainActivity | 19:22:54.120 | Home activity created, 2.337 s later |
| `wm_on_create_called` | 19:22:54.609 | App activity creation callback |
| `wm_on_resume_called` | 19:22:54.657 | App resumed |
| `wm_activity_launch_time` RC TV | 19:23:00.906 | First-window launch/display marker |

Animation-end to RC TV first-window marker is **9.123 seconds**; activity creation to that
marker is **6.786 seconds**. These are approximate proxies for the owner's visible black
interval, not a video measurement or proof all Home content was ready. The framework's
launch-time payload separately reports 2453 ms; that counter has a different start point
and must not be equated to the whole interval. Retained main logs do not establish which
UI work accounts for the delay. No additional reboot, tracing, build or code change ran.

Likely contributors are Android/user-unlock/Home handoff before activity creation and cold
UI/first-frame startup during system boot load. No precise CPU/I/O cause is proven. Current
launch theme already sets `android:windowBackground` to `#09151F` (dark navy).

Android's normal starting-window theme, or Jetpack SplashScreen compatibility support on
Android 11, could improve the visual transition **after** the system starts the activity.
Neither can cover time before launch or inherently shorten boot/rendering. A static launch
background would be a smaller future visual change than a separate splash activity, but no
change is authorised or implemented. Preserve the successful takeover. Any optimisation
needs separate approval and timing verification; avoid boot receivers, delays or forced starts.

Sources: [Android splash-screen documentation](https://developer.android.com/develop/ui/views/launch/splash-screen),
[CleanYourTV Xiaomi reports](https://github.com/Gathin23/cleanyourtv/blob/main/examples/xiaomi-mi-box-s-2nd-gen/debloat.txt),
[takeover implementation](https://github.com/Gathin23/cleanyourtv/blob/main/launcher/build-and-install.sh).
External reports covered 737/772; the two physical successful restarts here establish this unit's 774 result.
