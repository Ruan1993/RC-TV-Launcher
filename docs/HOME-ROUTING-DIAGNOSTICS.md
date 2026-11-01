# Xiaomi primary Home routing investigation

Investigated 1 November 2026, from `161fd77`, TV `1.0.0-rc1.15` (44). **J3C primary-Home acceptance remains failed. No takeover was attempted.** This investigation used read-only ADB and local analysis of the installed launcher manifest. No package, preference, role, data, or firmware modification was performed; no automated key press, launch, standby or reboot was performed in this investigation.

## Device and actual routing

- Xiaomi `MiTV-AFKR0`, device `jaws`, Android 11 / API 30.
- Firmware fingerprint: `Xiaomi/jaws/jaws:11/RTT0.211222.001/774:user/release-keys`; security patch `2025-04-05`.
- Active user: Owner, **0**. A separate dormant user 10 exists; its state is not the active Home configuration.
- Installed RC TV: `za.co.rc.tv`, activity `za.co.rc.tv/.MainActivity`, version `1.0.0-rc1.15` (44), installed and enabled for user 0.
- **Saved HOME role holder: `za.co.rc.tv`. Actual resolved HOME: `com.google.android.apps.tv.launcherx/.home.HomeActivity`.**

This firmware rejects `cmd role get-role-holders android.app.role.HOME` as an unknown command. `dumpsys role` supplies the role evidence instead. `cmd package resolve-activity` and `query-activities` with MAIN/HOME and explicit user 0 supply the operative resolution and candidates.

### Every enabled Home handler for user 0

| Component | Priority | Purpose |
| --- | ---: | --- |
| `com.google.android.apps.tv.launcherx/.home.HomeActivity` | 2 | Google TV; actual resolved Home |
| `com.google.android.tungsten.setupwraith/.RecoveryActivity` | 1 | Setup/recovery Home; explicitly enabled |
| `za.co.rc.tv/.MainActivity` | 0 | RC TV; saved role and preferred Home |
| `com.android.tv.settings/.system.FallbackHome` | -1000 | Android fallback Home |

The disabled-inclusive query (`--query-flags 512`) also exposes setupwraith `/.MainActivity` (priority 4, explicitly disabled), Google TV `/.home.VanillaModeHomeActivity` (2, explicitly disabled), and `android/com.android.internal.app.SystemUserHomeActivity` (-100, not a default candidate). These are not additional enabled choices in the ordinary query.

PatchWall `com.mitv.tvhome.atv` is installed but is **not** a Home handler in either query. Its priority-1000 filter belongs to a `pw:` VIEW link, not HOME. Xiaomi quick settings, Xiaomi/Droidlogic settings, partner customizer and other installed Xiaomi services likewise do not appear in these HOME results.

## Evidence and likely cause

The preferred-activity XML records RC TV as an `always=true` MAIN/HOME/DEFAULT choice, with the same four enabled candidates. No conflicting persistent-preferred HOME entry was exposed by the readable package dumps. The active user has `user_setup_complete=1`; global `device_provisioned=1`. No overriding Home policy was exposed by the inspected device-policy dump. This does not exclude undisclosed firmware logic.

During the requested **physical remote** test, the user reported opening an app, pressing Home and reaching Google TV. At device log time **12:04:26**, WindowManager recorded HOME down/up; ActivityTaskManager then recorded:

```text
12:04:26.454 START u0 {act=android.intent.action.MAIN cat=[android.intent.category.HOME]
flg=0x10000100 cmp=com.google.android.apps.tv.launcherx/.home.HomeActivity (has extras)} from uid 0
```

YouCine paused and Google TV resumed. There was no RC TV launch attempt in that sequence. A resolved component in this log is not itself proof that a Xiaomi service hardcoded the destination. The UID attribution is the observed system log, not an ADB root operation.

The strongest explanation is **intent priority taking precedence over the stored preference**. Android 11's `PackageManagerService.chooseBestActivity` returns the leading candidate when its priority differs from the runner-up, before consulting the saved preferred activity. That matches this device's 2/1/0 hierarchy. The OEM framework implementation was not extracted, so this is a source-supported explanation consistent with device results, not proof of every Xiaomi internal branch. [AOSP Android 11 resolver source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android11-release/services/core/java/com/android/server/pm/PackageManagerService.java).

Android also caps positive activity priorities requested by non-privileged apps to zero. Increasing RC TV's manifest priority therefore cannot provide a supported fix. [Android priority rules](https://developer.android.com/guide/topics/manifest/intent-filter-element#priority).

Previous physical reboot and wake results are preserved in [primary Home acceptance](PRIMARY-HOME-ACCEPTANCE.md): RC TV's role survived reboot, Google TV appeared first, and standby could resume an already-open RC TV activity. Those tests were not repeated here.

## Supported fixes and model-specific research

The supported Home chooser already stores RC TV correctly. Repeating it has not fixed operative routing. Its handler resolves to `com.google.android.permissioncontroller/com.android.permissioncontroller.role.ui.HomeSettingsActivity`. AOSP's `set-home-activity` shell implementation sets the Home role; it is not evidence of an independent priority override and was not executed. [Android 11 shell implementation](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android11-release/services/core/java/com/android/server/pm/PackageManagerShellCommand.java).

No supported configuration fix was established for **jaws build 774**. An OEM firmware correction remains a possible resolution requiring Xiaomi confirmation and subsequent testing; no update, downgrade or reset is recommended as a proven fix.

Public first-hand Mi Box S notes concern `MIBOX4` / `oneday`, Android 9 build 3933, with a different stock package and no setupwraith Home claimant. They do not validate a procedure for this Android 11 `jaws` device. No authoritative exact-build remedy was found. [Contributor's device report](https://github.com/Gathin23/cleanyourtv/blob/main/docs/devices/xiaomi-mi-box-s.md).

## Takeover risk and recovery prerequisites

**A Google-TV-only takeover is currently a no-go:** removing its Home candidate would still leave enabled setupwraith RecoveryActivity at priority 1 above RC TV at 0. This is a prediction from the measured candidate hierarchy, not a disable test. Setup/recovery must remain unchanged under the user's constraints.

The exact stock package is `com.google.android.apps.tv.launcherx`, version `1.0.1002449598` (835645), an updated privileged system app. Its installed manifest also declares the protected dashboard handler, boot-mode screen/dream events and last-used-input receivers, plus content providers. Whole-package disabling would remove more than Home and could disrupt dashboard/input/boot-mode integration. Hardware input selection and all recovery paths have not been independently proven to survive. Separate Android Settings remains installed and resolves to `com.android.tv.settings/.MainSettings`; that alone does not guarantee remote access to it after a failure.

Component-only changes cannot be assumed to offer a safer escape: AOSP Android 11 restricts shell changes to components of non-test apps, while allowing certain whole-package state transitions. This firmware's exact enforcement was not probed with a mutation. The source is the `setEnabledSetting` implementation in the resolver source linked above.

Recovery status:

- Verified read-only: current user 0, stock package installed, package state `enabled=0` (DEFAULT), HomeActivity enabled, independent Settings and Home-settings handlers resolvable, authenticated wireless ADB usable now, and shell help supports `enable`, `default-state`, and `--user`.
- Previously observed: wireless ADB can disconnect during standby. Current connectivity is not an independent recovery guarantee.
- **Not verified:** recovery after disabling the launcher, wired/out-of-band access if wireless fails, Settings/dashboard/input usability under a takeover, or safe reboot under altered routing. No factory reset is an acceptable data-preserving recovery plan.

### Conditional reversible test proposal — not approved or executed

1. Preserve rc1.15 rollback APK and current configuration; record package/component states and recheck the candidate hierarchy. Do not reset app data.
2. Establish and physically test an independent recovery connection and access to Android Settings before any change. Prepare the commands below on the host. Wireless access alone does not meet this prerequisite.
3. Apply the go/no-go gate: RC TV must have a viable routing path without touching setup/recovery or other Xiaomi packages. **The current hierarchy fails this gate. Stop here.**
4. Only if that gate is resolved and separate user approval is obtained, a bounded test could target the Google TV package alone for user 0, with the independent Settings activity already open. No setup, recovery, Xiaomi service, package removal or data clearing is in scope. No forward disable command is supplied for this failed gate.
5. Immediately inspect resolution; if RecoveryActivity wins, restore the stock state and stop. Do not press Home merely to experiment with a known recovery-handler destination. A package-state change itself can trigger activity transitions, so avoiding reboot does not eliminate risk.
6. Restore and verify Google TV, its original package state and physical Home before ending any trial. No reboot/standby trial under altered routing until a separate safety review and approval; loss of network or failed RC launch could otherwise strand recovery.

The following are **prepared recovery commands only**, not commands run in this investigation. Syntax and target state are checked; recovery after a disable is not tested:

```powershell
$adb = 'C:\Users\ruanc\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb connect 192.168.1.39:5555
# Emergency package re-enable; then restore the exact original DEFAULT state.
& $adb -s 192.168.1.39:5555 shell pm enable --user 0 com.google.android.apps.tv.launcherx
& $adb -s 192.168.1.39:5555 shell pm default-state --user 0 com.google.android.apps.tv.launcherx
& $adb -s 192.168.1.39:5555 shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
& $adb -s 192.168.1.39:5555 shell am start --user 0 -n com.google.android.apps.tv.launcherx/.home.HomeActivity
& $adb -s 192.168.1.39:5555 shell am start --user 0 -n com.android.tv.settings/.MainSettings
# Supported visible Home selection, if needed after recovery:
& $adb -s 192.168.1.39:5555 shell am start --user 0 -a android.settings.HOME_SETTINGS
```

## Recommendation and evidence retention

Keep Google TV unchanged and rc1.15 installed. Pursue an OEM-supported routing correction for this exact firmware. Do not attempt launcher takeover under the current constraints: Google-only disabling is insufficient and recovery is not independently proven. J3C's direct startup/Home criterion remains unmet.

Raw diagnostic dumps, the pulled stock APK, manifest inspection and AOSP source snapshots are retained locally under ignored `releases/review/home-routing-diagnostics/`; broad logs may contain device/user details and are intentionally excluded from Git. This report contains the relevant sanitized findings. Documentation-only validation requires diff checks, not a new APK build or device mutation.
