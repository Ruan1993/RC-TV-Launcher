# J3D — Xiaomi Home takeover readiness

Current follow-up: [J3E recovery recheck and Home-registration audit](PHASE-J3E.md). The usage reset and J3E continuation are authorised. Wired recovery and a permitted routing method remain unverified; no takeover changes have been made.

Prepared 1 November 2026 from `ff175de`, TV **1.0.0-rc1.15 (44)**. Preparation only: **no Home-role, preferred-activity, component-state or system-package changes were executed.** Normal explicit activity launches, one wake key and one normal reboot were used for the requested readiness checks. No install, data clearing, reset or instrumentation was performed.

**Decision: recovery access in the intact configuration is verified; a safe takeover is not yet ready. Wait for the user's confirmation after their usage reset before any next stage.** A reset or confirmation does not by itself resolve the blockers below.

Latest independent-recovery follow-up (from `2170bdc`): **NO-GO for takeover.** USB device-mode capability is evidenced in the running firmware, but no wired ADB connection or launcher-independent physical Settings route is verified. See section 7. This follow-up used read-only device checks only; no cables, transport changes, key injection, reboot, recovery entry or package/configuration writes.

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

## 7. Independent recovery investigation — follow-up from 2170bdc

### Physical identity and USB feasibility

ADB again identified `MiTV-AFKR0`, `jaws`, Amlogic, build 774 / Android 11. The exact chassis label, hardware revision and physical connector layout have **not** been inspected. The user requested proceeding with ADB/documented information and no cable connection. Original packaging is not required.

The corresponding Xiaomi TV Box S (2nd Gen) product specifications list **one USB 2.0 port**, one HDMI, one power interface and one audio output. Subsequent user physical clarification confirms a regular USB-A port, HDMI, audio jack and dedicated power input, with no visible USB-C/micro-USB data port. The user has successfully powered external HDDs and played their movies, series and images: **USB host functionality is physically verified**. Exact hardware revision and wired ADB device-mode connectivity remain unverified. No inference of wired ADB support follows from host media operation. [Xiaomi specifications](https://www.mi.com/global/product/xiaomi-tv-box-s-2nd-gen/specs/).

Read-only observations on this exact running firmware:

```text
ro.adb.secure=1
init.svc.adbd=running
service.adb.tcp.port=5555
sys.usb.config=adb
persist.sys.usb.config=adb
sys.usb.controller=fdd00000.dwc2_a
sys.usb.ffs.ready=1
sys.usb.configfs=2
settings global adb_enabled=1
settings global adb_wifi_enabled=0
/sys/class/udc contains fdd00000.dwc2_a
```

`dumpsys usb` reports `connected=false`, `configured=false`, and kernel state `DISCONNECTED`. The readable vendor file `/vendor/etc/init/hw/init.amlogic.usb.rc` declares a configfs gadget, an `ffs.adb` function, and the ADB FunctionFS mount. This is concrete evidence of firmware gadget/ADB infrastructure, stronger than assuming host-only capability from a USB-A socket. It does **not** prove the controller reaches the accessible connector, that it enumerates on a PC, or that USB and TCP ADB operate concurrently. Direct reads of gadget binding and controller state were permission-denied; no privilege escalation on Android or attempt to change permissions was made. Missing `/sys/class/typec` and `/sys/class/dual_role_usb` do not establish that device mode is impossible.

USB host support is advertised; the current USB dump shows the internal MediaTek wireless device, not an attached PC. A manufacturer feature flag also advertises Ethernet support, but no Ethernet adapter or wired network route was tested. `adb_wifi_enabled=0` does not contradict current TCP ADB at port 5555: it does not mean the existing legacy TCP connection is unavailable.

An original contributor's `jaws` tutorial describes USB-A-to-A use for recovery sideload/fastboot. That demonstrates a reported path on related hardware, **not normal Android wired ADB on this unit/build**. Its rooting, unlocking, flashing and DIY cable instructions are not adopted. [Original model-specific report](https://gist.github.com/supechicken/3c8378be3469bc2f82b7b319f202ed82).

**USB conclusion: plausible, unverified; not an available rescue method yet.** Do not connect two host-mode USB-A ports with an arbitrary cable. Before any physical test, confirm the exact exposed port's device-mode behaviour and an appropriate electrical/data cable specification with Xiaomi or reliable hardware-specific documentation. A generic connector shape, kernel gadget, or bootloader USB report is insufficient. No cable purchase or DIY wiring is recommended from the current evidence.

### Recovery methods, including when both Homes fail

| Method | Evidence class | If both Home activities fail and wireless ADB is lost |
| --- | --- | --- |
| Existing TCP ADB → explicit Settings/stock Home | Physically tested in prior J3D, including intact reboot | Unavailable if the network/transport itself is lost; independent of Home UI only while Android/ADB/network function |
| USB ADB in normal Android | Firmware capability evidenced; physical transport untested | Potentially useful if Android boots and authorisation works; currently cannot be relied on |
| Explicit Settings activity via ADB | Physically tested before/after intact reboot | Needs working ADB; activity independence alone is not an independent input route |
| Remote Settings shortcut, long-Home/dashboard, Assistant “open settings” | Documented general voice capability / other routes untested | Might reach Settings if relevant services survive; no verified path with both Homes inaccessible; Assistant may require network, and dashboard may depend on Google TV |
| USB keyboard or HDMI-CEC → Settings | Unsupported/unknown for the required sequence on this unit | Input alone supplies no proven Settings entry or package-restoration route; USB host availability may conflict with device mode |
| USB Ethernet | Ethernet feature advertised, untested | Could bypass Wi-Fi failure but still requires networking, working ADB listener and compatible adapter/USB host mode; not out-of-band rescue |
| Hardware recovery menu | Manufacturer-documented generically for Mi Box S; untested on exact revision/build | May bypass normal Homes, but no documented data-preserving package-state repair established |
| Recovery ADB sideload | Platform-documented restricted mode; model-family report, untested here | Not an ordinary shell for `pm enable`; cannot be treated as launcher rollback |
| Fastboot, Amlogic burning, UART, bootloader unlock | No tested supported data-preserving repair for this task | Excluded: could require firmware writes, teardown, unlock/data wipe or privileges; not a prepared rescue path |
| Manufacturer service | Official support channel available; repair outcome untested | Escalation option, not a guarantee of retaining data |

The read-only GLOBAL_BUTTON query exposes handlers in `com.droidlogic`, Google TV and Xiaomi partner customizer. This does not prove which physical key opens Settings or that the route survives a failed Home. On-device Settings still resolves independently to `com.android.tv.settings/.MainSettings`. Xiaomi documents voice app launch/settings capabilities generally, not a guaranteed offline rescue command. [Manufacturer Assistant description](https://www.mi.com/ph/support/article/KA-123922/).

Xiaomi's documented generic hardware procedure is to disconnect power, hold remote **Back + Confirm**, then reconnect power to reach recovery. Its next steps are **Wipe data / Factory reset**. **Do not perform this procedure in the current session.** Exact-build entry/navigation/exit is untested, and reset would violate data preservation. No verified official non-wiping launcher-state repair was found. [Manufacturer recovery instructions](https://www.mi.com/global/support/article/KA-12918/).

Recovery sideload is not equivalent to normal ADB: Android documents that shell and other ordinary ADB commands do not work in sideload mode. An OTA should not be assumed to undo user-specific component overrides. [AOSP recovery/sideload documentation](https://source.android.com/docs/core/ota/nonab/device_code).

### Follow-up checks, prerequisites and next experiment

The existing wireless connection answered USB and Settings reads. A later read batch briefly returned `closed`/`offline`; a subsequent Home-resolution read succeeded without any transport-setting change. This is further evidence that current wireless access is not an assured last-resort connection. No new reboot was needed; prior J3D already tested it. The rc1.15 rollback hash remains the value in section 6. Local ignored evidence: `releases/review/independent-recovery/usb.txt` and `usb-init.txt`.

Before a controlled takeover:

1. Confirm the physical hardware/connector revision when feasible, without needing the packaging. Obtain an appropriate supported data-connection specification before considering wired USB.
2. With **separate approval for physical interaction**, first test a normal remote/Assistant path into Android Settings from an unrelated app, without using either Home or ADB to open it. Observe the actual resumed component via read-only ADB. Do not change any setting or simulate broken launchers. This is the safest next experiment, but success would only prove that intact route; assess its Google TV/network dependencies before relying on it.
3. If hardware documentation supports normal USB ADB, separately approve a connection-only test while both launchers remain unchanged. Keep current TCP settings intact. Use `adb devices -l` to identify a separate USB serial; then explicitly select that serial for `shell getprop ro.product.model` and an explicit Settings launch. Verify serial-specific shell access and existing TCP access. Do not run `adb usb`, `adb tcpip`, `setprop`, role switching, recovery/bootloader commands, or change Developer Options. Stop if a transport change is necessary and request approval first. Neither physical approval nor cable suitability is currently established.
4. Test retention of the proven rescue route across an ordinary reboot only under separate approval; record required authorisation and host drivers. A USB listing alone is not enough. Do not deliberately disconnect working networking or break RC TV to manufacture a failure.
5. Preserve secure, restorable app configuration separately from APKs. Pairing may depend on Android Keystore material; copying encrypted files alone is not proven to restore logical pairing. Do not uninstall/reset as a backup strategy. Keep the authorised host's ADB key secure and unchanged; do not revoke debugging authorisations.
6. Resolve the independent Home-routing blocker as well: setupwraith RecoveryActivity still competes above RC TV. No recovery check authorises disabling setup/recovery or makes Google-only exclusion sufficient.

**GO:** further read-only preparation and, after explicit approval, the benign physical Settings-route test above. **NO-GO:** any launcher takeover, USB mode switch, recovery entry, factory reset or privileged workaround. No data-preserving recovery is verified for the combined failure of both Homes and wireless ADB. Wait for user confirmation after the usage reset; no takeover starts automatically.

## 8. Physical Apps and long-Home routes — follow-up from 905fd41

The user physically observed Apps opening an installed-app list containing RC TV, and long-Home opening a quick-access panel containing Settings. The remote has no dedicated Settings button. Those observations establish working shortcuts in the intact configuration, not independence from Google TV. This investigation issued read-only ADB commands only; no injected keys, launch commands or configuration changes.

### Observed component chain

| Physical shortcut | Device evidence on 1 November 2026 | Owner |
| --- | --- | --- |
| Apps | At 12:38:39.954, system UID 1000 starts `android.intent.action.ALL_APPS` explicitly in `com.google.android.apps.tv.launcherx/.gateway.LauncherDeepLink`; at 12:38:41.658 Google TV starts `/.guide.home.moreapps.ui.FullscreenAppsActivity`. The same chain recurs at 12:38:57.737/.833. The latter activity was foreground when inspected. | Google TV, not a separate Xiaomi/SystemUI app drawer |
| Long-Home | HOME key repeats coincide with 12:41:17.203 launch of `android.app.action.TOGGLE_NOTIFICATION_HANDLER_PANEL` to `com.google.android.apps.tv.launcherx/.dashboard.DashboardHandler`, from UID 10074. The device package list maps 10074 to `com.android.systemui`; foreground reporting identifies Google TV `/.dashboard.DashboardActivity`. | SystemUI initiates; Google TV provides the panel |
| Settings destination | User confirmed physical long-Home → Settings. At 12:51:08.108 Google TV UID 10057 starts `android.settings.SETTINGS` targeting `com.android.tv.settings/.MainSettings`; the same activity is confirmed resumed afterward. | Android Settings destination, reached through the Google TV dashboard |

The installed Google TV manifest corroborates the chain: `LauncherDeepLink` is an exported alias of `com.google.apps.tiktok.nav.gateway.GatewayActivity` with an ALL_APPS filter. `FullscreenAppsActivity` is a distinct non-exported activity. `DashboardHandler` is an exported, STATUS_BAR_SERVICE-protected alias targeting the distinct non-exported `DashboardActivity`. Neither alias targets `/.home.HomeActivity`. Google TV's UID is 10057; the observed internal app-drawer launch comes from that UID. SystemUI being the dashboard caller does not make the dashboard independent of Google TV.

### Survival assessment and smallest potential change

**Whole Google TV package exclusion would remove both recovery shortcuts' providers.** They are not independent fallbacks for option C. This is a manifest/component consequence; no package was disabled to test it.

If *only* Google TV `/.home.HomeActivity` were disabled, the distinct app drawer, gateway and dashboard components would remain enabled in principle. This makes survival plausible at the component-declaration level, not proven at runtime. Shared process initialization, internal navigation through Home, boot behaviour and framework routing can still defeat either route. No disabled-Home test was performed. Even a working Settings screen would need an actual, supported means to restore the changed component; merely reaching Settings is not proof of that ability.

The unchanged enabled HOME query still returns Google TV priority 2, setupwraith `/.RecoveryActivity` priority 1, RC TV priority 0, and Settings FallbackHome -1000. Under the source-supported priority explanation, Google-Home-only exclusion is insufficient: RecoveryActivity would become the leading candidate. Its actual runtime response remains untested. The OEM resolver/boot implementation has not been proven exhaustively, so this is a strong prediction rather than a measured post-change outcome.

The smallest *conceptual candidate exclusion* capable of leaving RC TV above fallback would affect **both** Google's HomeActivity and setupwraith RecoveryActivity for user 0, leaving all other package functions intact. This is **not an available or approved method**: modifying setup/recovery remains prohibited, and the inspected Android 11 shell guard rejects component changes for non-test packages. No public per-user switch for only their HOME intent filters was established. Increasing RC TV priority is capped for ordinary apps; changing the saved role already failed. There is therefore **no currently demonstrated permissible reversible configuration change that makes RC TV the operative Home**. Do not turn this conceptual minimum into a two-component disable recipe, root workaround or wider package disable.

### Updated recommendation

**NO-GO for takeover remains.** The shortcuts improve intact-device accessibility, but both originate in Google TV and neither is verified under the proposed failure conditions. USB ADB and data-preserving recovery after loss of wireless access remain unverified. No system-package, component, Home role, preference or ADB transport state was changed.

Safest next experiment after the user's reset and separate approval: with all components unchanged, open an unrelated app and physically use Apps to launch RC TV; separately use long-Home → Settings, correlate each transition, then inspect whether Settings exposes any supported restoration control for individual activities **without operating it**. A supported control must be demonstrated before calling that route recovery-capable. This does not require disabling either Home or setup/recovery. If no supported component restoration exists, prioritise verification of normal USB ADB under the hardware/cable prerequisites in section 7 or OEM guidance. Do not begin a takeover merely because the shortcuts work normally.

Evidence is retained locally in ignored `releases/review/remote-recovery-routes/remote-log.txt`, with the installed-manifest evidence in `releases/review/home-routing-diagnostics/google-tv-manifest.txt`. Raw logs/APKs are excluded from Git. The current task's scope ends with documentation and reporting.

Late physical confirmation: the user selected Settings from long-Home and reported “settings open.” Read-only activity/log capture confirms the destination above; evidence is `releases/review/remote-recovery-routes/settings-selection-log.txt`. This completes verification of the intact physical Settings entry route without ADB launching it. It does not verify survival after a Home/component/package change or a supported means to reverse such a change. The NO-GO recommendation is unchanged. The Settings-entry portion of the proposed next experiment is now completed; do not repeat it solely for evidence.
