# J3D — Xiaomi Home takeover readiness

Prepared 1 November 2026 from `ff175de`, TV **1.0.0-rc1.15 (44)**. Preparation only: **no Home-role, preferred-activity, component-state or system-package changes were executed.** Normal explicit activity launches, one wake key and one normal reboot were used for the requested readiness checks. No install, data clearing, reset or instrumentation was performed.

**Decision: recovery access in the intact configuration is verified; a safe takeover is not yet ready. Wait for the user's confirmation after their usage reset before any next stage.** A reset or confirmation does not by itself resolve the blockers below.

## 1. Recovery-readiness results

| Check | Result |
| --- | --- |
| Exact device | Xiaomi MiTV-AFKR0 / `jaws`, Android 11 (API 30), fingerprint `Xiaomi/jaws/jaws:11/RTT0.211222.001/774:user/release-keys` |
| Active user | Owner, user **0**; dormant user 10 is not the test target |
| ADB before reboot | Authorised and accessible at `192.168.1.39:5555` |
| Full reboot | Confirmed by changed kernel boot ID and `sys.boot_completed=1` |
| ADB after reboot | Existing host connection became stale/offline. Targeted host disconnect/connect restored `device` status without a new authorisation prompt; shell commands worked |
| Independent RC TV launch | Explicit `za.co.rc.tv/.MainActivity` launch and resumed-activity confirmation passed before and after reboot |
| Independent Android Settings | Explicit `com.android.tv.settings/.MainSettings` launch and resumed-activity confirmation passed before and after reboot |
| Home after reboot | Google TV resolved and was resumed before the explicit readiness launches |
| Stored preference | RC TV HOME role retained; preferred-activity XML identical before/after reboot |
| Package state | RC TV, Google TV, setupwraith and Android Settings all remain installed, unsuspended, package state DEFAULT (`enabled=0`) for user 0 |
| Rollback APK | Saved rc1.15 APK matches the freshly pulled installed APK byte for byte by SHA-256; saved APK signature verification passed |
| Data preservation | No configuration or data-writing commands issued. Favourites and pairing were not independently revalidated logically in this session; prior preservation tests are in J3C acceptance |

The first pre-reboot RC launch waited behind the system dream/screensaver. `KEYCODE_WAKEUP` released it; RC TV then resumed. Its roughly 45-second wait is not a startup benchmark. Post-reboot launch observations: RC TV WARM 1896 ms; Settings WARM 828 ms. These are single explicit-launch samples, not cold-boot or comparative performance results. Settings was left open after verification.

### Exact Home handlers and states

| Package / activity | Priority | User-0 state |
| --- | ---: | --- |
| `com.google.android.apps.tv.launcherx/.home.HomeActivity` | 2 | Explicitly enabled; actual resolved HOME |
| `com.google.android.tungsten.setupwraith/.RecoveryActivity` | 1 | Explicitly enabled; competing recovery HOME |
| `za.co.rc.tv/.MainActivity` | 0 | Available; saved HOME role holder |
| `com.android.tv.settings/.system.FallbackHome` | -1000 | Available; fallback, not a full replacement launcher |
| Google TV `/.home.VanillaModeHomeActivity` | 2 | Explicitly disabled; not an enabled competitor |
| setupwraith `/.MainActivity` | 4 | Explicitly disabled; not an enabled competitor |

Disabled-inclusive inspection also exposes `android/com.android.internal.app.SystemUserHomeActivity` (-100, not a default candidate). PatchWall `com.mitv.tvhome.atv` is installed but does not claim HOME in either query. Current versions: Google TV `1.0.1002449598`, setupwraith `1.0.993403918`, Android Settings `1.0`, RC TV `1.0.0-rc1.15`.

## 2. Confirmed findings and remaining uncertainty

Confirmed: the saved role/preference survives reboot, generic HOME resolution chooses Google TV, and the previous physical Home trace launches Google TV with no RC TV attempt. This is not simply a lost selection. See [full routing diagnosis](HOME-ROUTING-DIAGNOSTICS.md) and [physical acceptance](PRIMARY-HOME-ACCEPTANCE.md).

Strong explanation: AOSP Android 11's `chooseBestActivity` selects a higher-priority leading candidate before consulting saved preference. Ordinary apps cannot gain positive priority. This matches the measured hierarchy but does not prove all Xiaomi boot, input and policy behaviour. No new inference is made that a particular Xiaomi service hardcodes Home. The exact OEM framework implementation and RecoveryActivity runtime behaviour remain unverified. [Android priority rules](https://developer.android.com/guide/topics/manifest/intent-filter-element#priority), [Android 11 PackageManagerService](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android11-release/services/core/java/com/android/server/pm/PackageManagerService.java).

Removing Google's candidate is predicted to expose setupwraith RecoveryActivity, not RC TV. Changing the candidate set can also affect saved preferences; their persistence after a future mutation must be measured, not assumed. RecoveryActivity has not been deliberately launched or altered. Its name is not proof that it offers a usable recovery UI.

## 3. Options compared

### A — Supported Home selection: lowest risk, already ineffective here

Target: `za.co.rc.tv/.MainActivity`; visible chooser: `com.google.android.permissioncontroller/com.android.permissioncontroller.role.ui.HomeSettingsActivity`.

Proposed command to open the supported chooser, only in a separately confirmed next session:

```text
adb -s 192.168.1.39:5555 shell am start --user 0 -a android.settings.HOME_SETTINGS
```

Select RC TV manually, inspect role/preference/resolution, then physically test Home. Expected stored selection: RC TV; current evidence predicts operative routing will remain Google TV. Reversal: open the same chooser and select Google TV; prior J3C physical tests verified switching both ways. Explicit RC/Settings launches and ADB remain the fallback in the intact configuration. Repeated selection is not recommended as a cure. `cmd package set-home-activity --user 0 za.co.rc.tv/.MainActivity` is another role-setting mechanism in AOSP, not a demonstrated priority bypass; do not execute it as a supposed fix. [Android 11 shell implementation](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android11-release/services/core/java/com/android/server/pm/PackageManagerShellCommand.java).

### B — Component-only configuration: conceptually narrower, not viable yet

Candidate competing components are the exact Google HomeActivity and setupwraith RecoveryActivity listed above. A shell component operation would have the form `pm disable-user --user 0 PACKAGE/ACTIVITY`; reversal must restore the captured component override (both candidates are currently explicitly ENABLED, so `pm enable --user 0 PACKAGE/ACTIVITY` would restore that override).

**These are planning syntax, not permission or a runnable test sequence.** AOSP Android 11 rejects shell changes to individual components of non-test apps. The device's enforcement has not been tested by a write, and Google-only component exclusion would still leave RecoveryActivity above RC TV. Altering setup/recovery is prohibited. An OEM-supported way to exclude only a Home filter while preserving all recovery functions has not been established. No root, privilege-grant or bypass is proposed.

Risk: loss of normal Home, fallback/recovery loops, inaccessible launcher if RC fails, or component-state rejection. Recovery would require a surviving ADB connection and restoration of the recorded component state. Independent wireless access works after a normal reboot but has not been proven under these changes. Therefore no component takeover is approved or recommended.

### C — Per-user package configuration: fails the nonessential-package gate

Exact potential stock target: `com.google.android.apps.tv.launcherx`, user 0. A package operation would have the form `pm disable-user --user 0 PACKAGE`; reversal for this package is shown below. It remains an installed system app in that hypothetical operation, but package reversibility alone does not prove safe functional recovery.

Google TV is not confirmed nonessential: its installed manifest also declares dashboard, last-used-input and boot-mode screen/dream receivers and content providers. Whole-package exclusion could remove those functions. It would also leave setupwraith above RC TV. Setupwraith supplies setup, account-linking, device-reset, restricted-profile and remote-pairing activities; its whole package must not be disabled to remove one HOME filter. This option is **not viable under the current constraints**. No forward package-disable command is proposed for execution.

## 4. Recommended sequence for the next session

1. Wait for the user to reset usage and explicitly confirm continuation. Do not start takeover automatically.
2. Recheck device identity, firmware, active user, ADB authorisation, package/component overrides, role and candidates. Recheck APK hash and preserve app configuration securely; an APK is not a backup of favourites or pairing credentials.
3. Establish a physically tested independent recovery route if wireless ADB fails. Current wireless ADB is independent of the launcher UI in the intact configuration, but wired/out-of-band recovery is untested and standby previously disconnected wireless access.
4. Resolve the method gate: obtain an OEM-supported routing fix or establish an independently reviewed component-only capability that preserves setup/recovery. **No tested method currently passes. Stop if this remains unresolved.** Do not disable setupwraith or broaden the experiment to bypass this gate.
5. Only after a viable method, exact state changes, restoration and scope have been reviewed and separately approved, capture baselines and apply one bounded change. The exact executable change sequence cannot honestly be specified until that method exists; A/B/C syntax above is not an approval-ready takeover script.
6. Before any Home press or reboot, inspect the candidate list and resolution. If RC TV does not resolve, any unexpected recovery component wins, or Settings/ADB fails, roll back immediately. Candidate changes themselves may trigger Home; staying in Settings does not eliminate risk.
7. Proceed through the physical checklist only with recovery available. Restore original state and verify stock Home if any acceptance test fails.

Recommended method today: retain supported Home selection and seek an OEM-supported correction; **no takeover mutation is recommended yet**. Fresh usage enables further investigation, not an automatic change in the safety assessment.

## 5. Physical acceptance checklist for an approved future method

- [ ] RC TV resolves as Home and opens directly from other apps via repeated physical Home presses.
- [ ] Full reboot: observe the first interface after Android finishes booting; RC TV appears without Google TV or a repeating chooser.
- [ ] ADB reconnects using the existing authorisation after reboot; record whether host reconnect is required.
- [ ] Standby/wake preserves usable RC TV Home; Home still returns to RC TV afterward.
- [ ] Settings, network, input selection, quick settings and remote functions remain usable.
- [ ] Supported switch to Google TV and back works, or an explicitly approved alternative recovery path is physically proven.
- [ ] Favourites/order, screensaver selections, other settings and phone pairing remain intact.
- [ ] No forced-start receiver, service, artificial splash or delay is introduced; compare repeatable launch timings separately from boot observation.
- [ ] Test RC launch failure recovery only with an independently verified rescue path; do not intentionally break RC TV merely to complete a checklist.

## 6. Rollback and protected components

Never disable/remove/reset setupwraith, Android Settings/FallbackHome, PermissionController, SystemUI, SettingsProvider, network/ADB services or Xiaomi/Droidlogic system services. Specifically preserve `com.xiaomi.tvqs.overseas`, `com.xiaomi.gtv.settings`, `com.droidlogic.googletv.settings`, `com.xiaomi.android.tvsetup.partnercustomizer`, `mitv.service` and `com.xiaomi.mitv.updateservice`. This is a conservative protected list, not a complete dependency map or a claim that every listed service is essential. Google TV remains protected in this session too.

Prepared recovery only, **not executed**. Targeted host disconnect/connect was tested; package restoration under altered state was not:

```powershell
$adb = 'C:\Users\ruanc\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb disconnect 192.168.1.39:5555
& $adb connect 192.168.1.39:5555
& $adb devices -l
# Only if a later approved test changed the whole Google TV package state:
& $adb -s 192.168.1.39:5555 shell pm enable --user 0 com.google.android.apps.tv.launcherx
& $adb -s 192.168.1.39:5555 shell pm default-state --user 0 com.google.android.apps.tv.launcherx
& $adb -s 192.168.1.39:5555 shell am start --user 0 -n com.google.android.apps.tv.launcherx/.home.HomeActivity
& $adb -s 192.168.1.39:5555 shell am start --user 0 -n com.android.tv.settings/.MainSettings
& $adb -s 192.168.1.39:5555 shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
```

Package DEFAULT restores the captured original package override; emergency ENABLED alone would not exactly match baseline. Package restoration would not repair a separately changed component override. No component changes are approved. If RC TV cannot launch, explicitly start the stock Home/Settings through authorised ADB after restoring the relevant state; if ADB is unavailable, no independently tested rescue is presently available. Do not assume a factory reset, unplug/reboot or reinstall restores user data safely.

Saved rollback: `releases/phase-j3c/RC-TV-Launcher-1.0.0-rc1.15-debug.apk` (28,955,840 bytes), SHA-256 `395538656B044832B5ABC88CDFB3F1AE23174BA6B5C9AAC4F6F72DC9E6BDD947`. Package/version verified with aapt2; apksigner verification passed. Earlier rc1.12–rc1.14 APKs are also present; do not assume downgrades preserve schema or data. Reinstalling rc1.15 with `adb install -r` would be a separate repair action, not Home restoration, and was not executed.

Local ignored evidence: `releases/review/home-takeover-readiness/` contains pre/post package and role dumps, matching preferred XML, handler lists and the pulled RC APK. Raw logs/APKs are not committed. Documentation checks only; no new build or tests needed. No claim is made about current exact Codex allowance: approximately 10% was user-reported at session start; the user controls the reset.
