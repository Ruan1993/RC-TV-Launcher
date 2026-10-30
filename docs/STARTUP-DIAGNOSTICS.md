# Phase A startup investigation — 30 October 2026

## Status and source review

### Confirmed diagnosis and repair (supersedes the initial review below)

Network ADB subsequently advertised legacy `_adb._tcp` on port 5555. After the user approved the TV's RSA prompt, the PC connected without root, a USB cable or wireless pairing. No assumption that USB debugging always enables TCP/IP is needed: the advertised service and authorised connection establish it for this box in its current state. The earlier refusal was real, but its transition to a listening service was not observed and is not explained here.

Preserved crash logs and exit history before any reproduction or installation. An explicit Leanback launch reproduced `RuntimeException: Unable to start activity`, caused by `NullPointerException` in `PhoneWindow.getInsetsController`, at the original `MainActivity.kt:53`. Exit history recorded reason 4, APP CRASH(EXCEPTION). The Android 11 getter dereferenced its not-yet-created DecorView; Kotlin's `?.` only protects the getter's result, not an exception thrown inside it. Compose, discovery and DataStore had not yet started and were not the cause of this captured failure.

Small repair: retain immersive system-bar behaviour, but post its setup to the decor view after `setContent` has created the content, and obtain the controller from that view. No dependency, permission, Home registration or data-storage changes. Build, 2 unit tests and lint passed (53 executed tasks, 0 lint errors / 7 advisory warnings).

Installed the APK over the existing app with `adb install -r`, preserving preferences. The initial repaired cold start reached MainActivity in about 4 seconds and its UI hierarchy contained the Compose Home interface and installed-app tiles. D-pad Down/Select opened Search; Back returned Home. Selecting the YouTube tile resumed the real YouTube activity; explicitly returning to RC TV succeeded. Settings displayed the existing Home status and recovery controls without changing them. Repeated cold-start checks stop only RC TV, so their user-requested exit records should not be mistaken for crashes. Raw device properties, logs and UI dumps are stored under Git-excluded `releases/diagnostics/`; no full device logs are committed.

Diagnostic alternatives: Android's built-in Take bug report can export logs/stack traces when exposed by firmware. Modern wireless pairing is not guaranteed on Android 11 TV; Android documents it for TV from Android 13. Xiaomi's generic ADB guide does not establish USB peripheral-mode debugging support for this exact firmware. USB-host support for drives is not proof that a PC can debug the box through that port. Neither USB cabling nor a third-party log reader was needed here. An ordinary log-reader app cannot be assumed to read another app's crash logs without privileged access; the app itself can query its own Android 11 exit history as a possible future diagnostic fallback, but that is unnecessary for this confirmed crash.

Three additional cold starts succeeded in approximately 2.9–3.0 seconds each; the final process remained alive for over a minute through follow-up checks. No new RC TV fatal exception was observed. Screenshot inspection confirmed the Home interface. The stock Google TV/Xiaomi launcher remained the resolved default Home. Fixed APK SHA-256: `498F1597437A82E171E41C0AEF991A92B34F60D71F8BBE8C5D81691C95FC5536`.

Phase B remains on hold. Physical remote usability, full visual layout, stock Home/recovery and reboot acceptance still need the user's broader checklist.

### Initial review before device authorisation

The user reports successful installation on Xiaomi MiTV-AFKR0 / Android 11, followed by a black screen and return to Xiaomi Home when opening RC TV. Startup acceptance has failed. A crash, native failure or firmware-driven activity exit cannot be distinguished from this symptom alone. No device logs were available; an elevated `adb devices -l` found no connected devices. No app-code fix is claimed or speculative dependency change made. Phase B is on hold until real-device startup succeeds.

- Activity/manifest: packaged application ID is `za.co.rc.tv`; both launcher categories resolve to `za.co.rc.tv.MainActivity`, which is exported, singleTask and also registered for Home. There is no automatic finish, stock-launcher change, boot receiver or incoming-intent payload processing. Home-role selection only happens on a button press.
- Compose: ComponentActivity calls super.onCreate and setContent with Material3. The platform NoActionBar theme does not require AppCompat. InsetsController is an API 30 API and accessed with a null-safe call. No definite initialisation error was identified by source review.
- Discovery: launcher/Home visibility queries are declared, and discovery runs on Dispatchers.IO, removes duplicate packages and reports discovery exceptions. No QUERY_ALL_PACKAGES or runtime permission is needed for these scoped intent queries.
- Lifecycle: discovery refreshes on resume and clock collection is lifecycle-bound. The subsequent Home resolveActivity/loadLabel calls are not guarded; an exception there would escape the launched coroutine. This is an investigation point, not evidence that Xiaomi throws there.
- DataStore: one top-level application-context delegate owns the store; corruption resets to defaults and IOExceptions emit defaults. Other exceptions propagate to collection, so the real stack trace is needed before attributing startup failure to storage or native code. No data is cleared during this investigation.
- Dependencies: resolved Compose UI/runtime 1.8.0, Material3 1.3.2, Lifecycle 2.9.0, Activity 1.10.1, DataStore 1.1.7 and Kotlin 2.1.0 compile and package. APK minimum API is 30, target is 36, and native ABIs include armeabi-v7a, arm64-v8a, x86 and x86_64. Build success and ABI presence do not establish device runtime compatibility.
- Verification rerun: `gradlew.bat assembleDebug testDebugUnitTest lintDebug --rerun-tasks` succeeded, all 53 tasks executed; 2 unit tests passed; lint 0 errors / 7 advisory warnings. Gradle also reports that two dependency native libraries could not be stripped and are packaged as-is; this is not a demonstrated loading failure. Unit tests cover ordering only, not startup. No Android 11 runtime test was possible here.

## Windows PowerShell: identify the TV

Use Android SDK Platform Tools. On this PC:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
```

Enable the box's developer/ADB debugging option and approve this computer's authorisation prompt on the TV. Use a supported USB data connection, or, if the box explicitly offers network debugging, connect to its displayed address with `& $adb connect 'TV_IP:PORT'`. Do not assume a port; if it offers wireless pairing, first use `& $adb pair 'TV_IP:PAIRING_PORT'` and enter the displayed code. Then use its connection port. Firmware options vary.

In `devices -l`, the first column is the serial (sometimes IP:port). `device` means connected; `unauthorized` requires TV approval; `offline` requires reconnecting. An empty list means no device is available. Select the TV explicitly, especially if other Android devices are attached:

```powershell
$serial = 'REPLACE_WITH_FIRST_COLUMN_FOR_THE_TV'
$out = Join-Path $env:USERPROFILE 'Desktop\RC-TV-startup-logs'
New-Item -ItemType Directory -Force -Path $out | Out-Null
& $adb -s $serial shell getprop ro.product.model
& $adb -s $serial shell getprop ro.build.version.release
& $adb -s $serial shell getprop ro.build.version.sdk
& $adb -s $serial shell getprop ro.product.cpu.abilist
& $adb -s $serial shell getprop ro.build.fingerprint
& $adb -s $serial shell dumpsys package za.co.rc.tv > "$out\package.txt"
```

## Preserve existing evidence and record a launch

Do this before reinstalling, clearing app data, force-stopping or rebooting. Do not clear Logcat: the previous crash may still be there.

```powershell
& $adb -s $serial logcat -b crash -d -v threadtime > "$out\crash-before.txt"
& $adb -s $serial shell dumpsys activity exit-info za.co.rc.tv > "$out\exit-before.txt"
& $adb -s $serial logcat -b main -b system -b crash -b events -v threadtime -T 1 > "$out\startup-full.txt"
```

The last command continues recording. While it runs, select the RC TV icon on the TV once. Wait for the return to Xiaomi Home, then press Ctrl+C in PowerShell. This records system activity decisions as well as app crashes. Capture post-launch evidence:

```powershell
& $adb -s $serial logcat -b crash -d -v threadtime > "$out\crash-after.txt"
& $adb -s $serial shell dumpsys activity exit-info za.co.rc.tv > "$out\exit-after.txt"
& $adb -s $serial shell dumpsys activity activities > "$out\activities-after.txt"
Select-String -Path "$out\startup-full.txt" -Pattern 'za\.co\.rc\.tv|AndroidRuntime|FATAL EXCEPTION|Fatal signal|am_crash|am_anr|am_kill|Force finishing|ActivityTaskManager' -Context 5,35 | Out-File "$out\startup-filtered.txt"
```

Filtering is for navigation; retain the full log because exception continuation lines and system/native messages may not contain the package name. Do not rely solely on a PID filter: an instantly crashing process may be gone before pidof runs. If exit-info is unavailable on this firmware, save its error and the other logs anyway.

For a second attempt that bypasses the Xiaomi icon, use another PowerShell window with the same `$adb`, `$serial` and `$out` assignments while the full capture is running:

```powershell
& $adb -s $serial shell am start -W -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER -n za.co.rc.tv/.MainActivity > "$out\explicit-launch.txt"
```

`Status: ok` only establishes that Android accepted the launch; it does not establish that the interface stayed open. Compare explicit launch with the TV-icon attempt. Neither command changes the default Home.

## Evidence needed next

Share crash-before/after, exit-before/after, startup-full (or the complete exception and every Caused by block with surrounding activity messages), package.txt, explicit-launch.txt if used, and the model/Android/API/ABI/fingerprint output. Include the approximate launch time and whether it was the TV icon or explicit launch. Full logs may contain unrelated device/app information; review before sharing, retaining the RC TV crash and relevant system context.

Keep the original launcher installed and enabled and retain the existing Home recovery procedure. Do not change default Home until RC TV opens reliably. A rebuilt APK without an evidence-backed code change is not a startup fix.

References: [ADB connections and device selection](https://developer.android.com/tools/adb), [Logcat buffers and filtering](https://developer.android.com/tools/logcat), [Android 11 process exit information](https://developer.android.com/reference/android/app/ApplicationExitInfo).
