# J3E — primary Xiaomi Home investigation

1 November 2026; continued from `dc07d48` on `feature/phase-b-cinematic-glass`. TV remains **1.0.0-rc1.15 (44)**. **Primary-Home acceptance is not achieved. No takeover mutation was attempted.** The usage reset was confirmed by the account usage tool; it does not change the recovery or routing requirements.

## Decision

**NO-GO at the recovery gate, with a separate unresolved routing gate.** Normal wireless ADB recovery and independent app launches pass. Wired USB ADB remains plausible but physically unverified. No data-preserving recovery is verified for loss of both Home access and wireless ADB. The user's J3E instructions require stopping before system-component changes in this situation.

Even successful wired recovery would not establish a permitted takeover method: Google's HomeActivity is not the only competing higher-priority Home, and disabling setup/recovery is expressly prohibited. No package-disable, component-disable, suspension, role write, preference write, root, partition modification, reset, new service or boot workaround was executed.

## Stage 1 — current recovery evidence

| Check | Result |
| --- | --- |
| Device | ADB reports Xiaomi MiTV-AFKR0, `jaws`, Android 11, active user 0 |
| Firmware | `Xiaomi/jaws/jaws:11/RTT0.211222.001/774:user/release-keys` |
| Board / revision | `ro.board.platform=sc2`, `ro.product.board=jaws`, `ro.revision=0`; no reliable mapping to an exact chassis/PCB revision established |
| Authorisation before reboot | Existing `192.168.1.39:5555` transport answered shell reads |
| Normal full reboot | Kernel boot ID changed and `sys.boot_completed=1` |
| Wireless ADB afterward | Temporarily offline; targeted PC disconnect/connect restored `device` status and shell access with the existing authorisation |
| RC TV independent launch | Explicit `za.co.rc.tv/.MainActivity` launch and resumed-activity confirmation passed before and after reboot |
| Settings independent launch | Explicit `com.android.tv.settings/.MainSettings` launch and resumed-activity confirmation passed before and after reboot |
| First post-boot inspection | Google TV `/.home.HomeActivity` resumed; this is an ADB observation, not a new user-observed first-frame acceptance result |
| Role / preference | RC TV still holds HOME; full preferred-activity XML identical before and after reboot |
| Physical Settings route | Previous user-operated long-Home → Google dashboard → Android Settings trace remains valid; it has not been tested under a disabled Home |
| Wired USB | No cable attached, separate USB transport enumerated or serial-specific USB shell verified |

Post-reboot explicit-launch samples were RC TV WARM 1515 ms and Settings WARM 808 ms. These single observations do not measure cold boot or prove a responsiveness improvement. Settings was left open. No standby or injected Home-key test was needed because no routing change was made.

### USB connection: what is and is not established

The manufacturer documents one USB 2.0 socket for the corresponding TV Box S (2nd Gen) product; imagery shows a full-size USB-A-style socket, with separate power and HDMI connections. Exact unit hardware revision and connector inspection remain unverified. [Xiaomi specifications](https://www.mi.com/global/product/xiaomi-tv-box-s-2nd-gen/specs/).

Prior device reads exposed `fdd00000.dwc2_a` in the USB device-controller class, ADB gadget configuration, and a disconnected USB manager. Fresh local reads of the actual firmware's `/vendor/etc/init/hw/init.amlogic.board.rc` also show that the `adbd=running` / `persist.vendor.usb.debug=1` condition sets GPIO474 direction to input; the debug-off condition sets it to output. This corroborates board-specific USB-debug infrastructure. The GPIO's electrical purpose is not documented by the readable file, and reading its live direction was permission-denied. No attempt was made to write it, alter USB mode or bypass permissions.

The potential topology would be **PC USB host → appropriate data cable → the box's externally exposed USB connector acting as an ADB peripheral**, with normal box power and HDMI retained. It must not be implemented as two assumed host ports connected together. No authoritative specification for the required cable wiring, VBUS handling and normal-Android peripheral behaviour on this exact unit/build was established; therefore **no exact cable connection is approved or requested**. A PC USB-C connector by itself would not resolve those electrical/role uncertainties.

The user has external HDD data cables but has not identified their connectors; the box may be movable near the PC. HDD cables are not assumed suitable. No cable purchase, DIY cable, direct host-to-host A-to-A connection or USB mode change is recommended. If verified hardware guidance becomes available, inspect the available connector types first, explain the precise connection and obtain the user's physical confirmation before any test.

Original model-family tutorials describe USB-A-to-A use for recovery/fastboot and, after modifications, ADB. They do not establish a safe stock-build-774 normal-Android recovery connection on this unit. Their unlock/flash/root/reset steps are excluded. [Original contributor report](https://github.com/x011/Root-Xiaomi-TV-Box-S-2nd-Gen-MDZ-28-AA).

## Stage 2 — permitted approaches audited without mutation

### A. Supported Home configuration

Android Home settings still resolves to `com.google.android.permissioncontroller/com.android.permissioncontroller.role.ui.HomeSettingsActivity`. The saved Home role and preferred record already name RC TV. Prior physical selection of Google TV and RC TV was tested; RC TV selection did not change operative routing. No newly documented Xiaomi-supported override was found for build 774. A role-setting shell command is not evidence of a separate priority bypass, so repeating it would not constitute a demonstrated fix. [Android Home role API](https://developer.android.com/reference/android/app/role/RoleManager#ROLE_HOME).

### B. RC TV registration audit

Inspected repository manifest, MainActivity's Home-selection code, HomeIntegration, profile manifest, and binary manifest from the exact saved APK previously hash-matched to the installed APK:

- Package `za.co.rc.tv`; exported `.MainActivity`, `singleTask`, landscape.
- MAIN / HOME / DEFAULT activity filter present; separate MAIN / LEANBACK_LAUNCHER / LAUNCHER filter present.
- No competing activity alias or profile-manifest override removes that registration.
- Supported role-request API is used when available and not held; otherwise the app opens supported Home settings.
- UI distinguishes a held role from actual resolved Home; package-manager queries see RC TV as an enabled HOME candidate.

**No genuine application-side Home-registration defect was found.** No manifest change, speculative priority increase, new APK or unnecessary build was made. Ordinary-app positive priority is capped by Android. [Manifest priority rules](https://developer.android.com/guide/topics/manifest/intent-filter-element#priority).

### C. Component-only configuration

The enabled HOME hierarchy remains Google TV `com.google.android.apps.tv.launcherx/.home.HomeActivity` (2), setup `com.google.android.tungsten.setupwraith/.RecoveryActivity` (1), RC TV `.MainActivity` (0), Android Settings `.system.FallbackHome` (-1000).

Actual device resolution and earlier physical logs establish that Google TV handles HOME despite the saved RC role. AOSP Android 11's resolver checks differing candidate priorities before saved preference, strongly explaining the mismatch. Exact Xiaomi boot/resolver internals were not independently extracted; intent priority is not asserted as proof of every firmware behaviour. [Android 11 resolver source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android11-release/services/core/java/com/android/server/pm/PackageManagerService.java).

Google-Home-only exclusion is predicted to leave RecoveryActivity ahead of RC TV. This prediction has not been validated through a disable test. Setup/recovery cannot be changed under J3E. AOSP also restricts shell component-state changes for non-test packages; no mutation was used merely to test enforcement. Separate Google app-drawer/dashboard activities do not prove that their runtime paths survive a Home-only change. No candidate passes both the recovery and permissible-method gates.

### D. Whole-package configuration

Rejected: `com.google.android.apps.tv.launcherx` owns the physical Apps drawer and long-Home dashboard. Excluding the whole package would remove those providers, violate J3E's explicit constraint and still not remove the higher-priority setup recovery candidate. Wi-Fi/ADB/Settings are separate services, but their independence is not a guarantee against system activity loops or loss of practical control. No wider package change is proposed.

## Before / after and preservation

| Item | Before J3E | After checks |
| --- | --- | --- |
| RC TV package | Installed 44 / rc1.15, DEFAULT enabled state | Same |
| Google TV package | Installed, DEFAULT enabled state; HomeActivity explicitly enabled | Same |
| Setup package | Installed, DEFAULT enabled state; RecoveryActivity explicitly enabled; MainActivity disabled | Same |
| Android Settings | Installed, DEFAULT enabled state | Same |
| Saved Home / preferred XML | RC TV | Same; XML comparison equal |
| Resolved Home | Google TV HomeActivity, priority 2 | Same |
| Phone companion | Untouched | Untouched |

No uninstall, reinstall, clear-data, preferences edit or instrumentation occurred. Favourites, weather, settings and pairing were not deliberately written; the logical eight-favourite/pairing preservation test was not rerun because there was no implementation or package change. Normal app execution may update runtime caches. Do not mistake the APK for a complete app-data/Keystore backup.

Rollback APK remains `releases/phase-j3c/RC-TV-Launcher-1.0.0-rc1.15-debug.apk`, SHA-256 `395538656B044832B5ABC88CDFB3F1AE23174BA6B5C9AAC4F6F72DC9E6BDD947`. Its hash was rechecked. Earlier checkpoint APKs and installed-APK copy are preserved.

## Restoration and next action

No J3E configuration rollback is required because no takeover change was made. Prepared package restoration commands and limitations remain in [readiness section 6](HOME-TAKEOVER-READINESS.md#6-rollback-and-protected-components). They have **not** been verified after disabling anything, and must not be described as a tested takeover rollback. Confirmed intact-state access commands are:

```powershell
$adb = 'C:\Users\ruanc\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb disconnect 192.168.1.39:5555
& $adb connect 192.168.1.39:5555
& $adb devices -l
& $adb -s 192.168.1.39:5555 shell am start --user 0 -n com.android.tv.settings/.MainSettings
& $adb -s 192.168.1.39:5555 shell am start --user 0 -n za.co.rc.tv/.MainActivity
```

These require working wireless connectivity and the authorised PC; they do not solve its loss. Physical long-Home → Settings was previously tested but passes through Google TV. No hardware recovery menu, sideload or fastboot test was entered, and no factory reset is an acceptable data-preserving rollback.

Next prerequisite: obtain reliable model/build-specific normal-Android USB-debug connection guidance (connector, role and cable electrical specification), then inspect available cables and seek physical confirmation for a connection-only test. Do not purchase an arbitrary cable or begin takeover to test the rescue route. Independently, an OEM-supported Home-routing correction that preserves setup recovery remains necessary under the current permitted approaches. Further allowance does not justify bypassing either blocker.

Local ignored evidence: `releases/review/phase-j3e/` contains pre/post package/role/preference captures, USB/init files, boot IDs and binary manifest analysis. Raw identifiers, dumps and APKs are not committed. Validation: documentation diff checks; no code change requiring compilation or tests. Stage 3 and primary-Home physical acceptance remain **not run**; J3E is **blocked, not complete**.
