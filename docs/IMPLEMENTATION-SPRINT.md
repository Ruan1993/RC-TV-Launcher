# RC TV implementation sprint — 2 November 2026

Candidate **TV 1.0.0-rc1.26-sprint (58)**; native Remote **0.5.1-rc1 (6)** unchanged. Local branch: `codex/settings-setup-recovery`. This is a development review build, not a physically accepted consumer release.

**The owner's Xiaomi was not modified.** It was not connected during implementation; the authorised ADB inventory initially contained no devices. At the owner's subsequent explicit request, the previously authorised TCP endpoint was reconnected for a brief read-only identity check (see diagnostic addendum below). No physical installation, package command, Home selection, Accessibility/keyboard change, debugging change, reboot, uninstall or data clear was performed. All install/test commands named isolated emulator serials explicitly.

## Implemented features and validation limits

| Area | Implemented | Validation / remaining work |
| --- | --- | --- |
| Professional Settings | Persistent category rail plus content panel; category browsing without preference writes; OK/Right entry; Back returns through existing navigation; full-width fallback below 800 dp or with large font scale; saveable page scroll; focus restoration; 64 dp Settings targets and two-line labels; Cinematic Glass tokens retained | Compiles and navigation policies pass JVM tests. Emulator UI run was interrupted by device disconnection; full focus traversal, 720p/1080p screenshots, Extra Large, three-metre readability and screen-reader acceptance remain **unverified** |
| Settings preservation | Approved S01–S39 inventory committed before feature changes; existing categories, control callbacks, preference keys and functional subpages retained; direct Manage favourites and Home-button controls added; restore is reachable from System & Recovery and Home & Apps | Static callback/control review, existing unit suites and successful builds. This is not a claim that every on-device control was exercised |
| Shared controls | Settings shell, readable button sizing, status feedback, Cancel-focused confirmation, guided progress/navigation and the same favourites editor used by Settings/setup | No new runtime UI dependency. Scoped appearance reset now explains exactly what it resets before confirmation |
| Initial setup | Ten stages: capabilities, appearance, favourites/order, optional features, permission/IME guidance, native phone pairing, Home selection, neutral physical-Home trial, recovery readiness and summary; durable step/feature/skip checkpoints; Finish later/manual resume; active-session automatic resume; current preferences retained | 91 JVM tests pass across project; emulator checkpoint recreation/feature/pause/native-intent test passed. Actual process-kill/reboot UI acceptance remains pending. Appearance supports existing colour/font/size/layout/motion and bundled wallpaper choices; import/gallery/advanced wallpaper controls remain in Settings rather than embedded in setup |
| Verification | Genuine MAIN/HOME evidence in a two-minute neutral trial; intercepted Home and explicit app launches do not qualify; RC resolver, no enabled Home interception and owner confirmation all required; no restart claim | Intent-policy tests pass. Owner-operated Home test remains unverified. Successful confirmation is session-local and intentionally does not imply reboot reliability |
| Recovery | Six-stage matching guide; system-Home discovery excluding fallback/recovery/chooser screens; explicit original selection; first-observed device/user/package/component override record; durable stage/target; separate interception inspection; direct app details/Home/Accessibility/settings screens; exact component resolution plus owner test confirmation | Read-only framework only. Activation method is unknown for externally configured installs; snapshot is never mislabelled as pre-takeover baseline. No shell operations, automatic service changes, emergency-recovery guarantee or self-uninstall shortcut implemented |
| Companion improvements | Stop remote without forgetting approved phones; separate Cancel-focused Forget all confirmation; revocation while stopped; old sockets/leases suspended on address loss/change; retained TLS/native protocol and existing scope gates | Existing PairingGate/remote policy tests pass; phone APK unchanged. New network-change/stop UI still needs physical phone/router acceptance |

The architecture's simple-default/advanced disclosure is only partially delivered: familiar groups remain, but not every existing long option list has been converted into a compact value row/choice sheet. The current shell is a functional navigation foundation, not a claim that all professional-polish acceptance criteria have passed.

## Deferred or deliberately disabled

- TV-hosted HTTPS page, QR, browser pairing/remote/keyboard and APK streaming are **not implemented**. Local distribution remains the chosen design; there is no replacement public host, fake QR, unauthenticated HTTP control, weakened pinning or browser command endpoint. Download UI states this limitation.
- Native phone ADB/read-only diagnostic adapter and allowlisted privileged runner are not implemented. Earlier Settings/setup/recovery acceptance takes priority.
- Xiaomi package restoration/takeover, automatic Home interception changes and reboot tests remain disabled/unperformed. Independent rescue is still a hard gate for future stock-package disabling.
- Recovery transaction rollback/external journal export, separate crash-isolated recovery activity, complete settings-row descriptor catalogue, automatic per-step completion transitions and broad wizard wallpaper integration remain follow-ups.
- Self-uninstallation stays absent until stock Home and external preservation of settings/imported images have passed acceptance. A settings JSON does not contain imported image bytes.

## Files and components changed

TV navigation/visuals: `ui/SettingsHub.kt`, `CinematicLauncher.kt`, `Appearance.kt`, new `GuidedUi.kt` and `FavouriteSettings.kt`. Existing utility, wallpaper, media, weather, backup and native keyboard implementations remain in place.

Setup/recovery: rewritten `ui/SetupWizard.kt`, new `core/SetupSession.kt`, `core/RecoverySnapshot.kt`, `ui/RestoreLauncherWizard.kt`, `HomeVerificationActivity.kt`; wiring in `MainActivity.kt`, `LauncherManagement.kt`, manifest and version metadata. `core/HomeIntegration.kt` now records the resolved component and excludes fallback/recovery activities from successful Home evidence. `core/SystemSettings.kt` resolves scoped app-details intents to exported system handlers; manifest queries cover the new keyboard/app-details entry points.

Companion: `companion/CompanionServer.kt`, `CompanionSettings.kt`, and accurate local-distribution wording in `ui/RemoteDownload.kt`. No phone source or runtime dependencies changed.

Tests: updated Settings category/navigation fixture labels, new `SetupVerificationTest.kt`, `SetupSessionCheck.kt`, `SprintUiCheck.kt`, runner modes in `WallpaperImportCheck.kt`, and `scripts/review-sprint-emulator.ps1`. Documentation: README, setup/recovery and Settings/companion current-state notes. The approved report remains the preimplementation preservation map.

## Build, test and lint results

- Baseline offline build/test/lint succeeded before editing. GitHub repository read access, Git, JDK 21, SDK 36/36.1, Gradle 8.13 and cached dependencies were verified.
- Final TV debug APK, TV test APK, native phone APK and phone test APK built successfully. `:app:testDebugUnitTest` reports **91 tests, 0 failures, 0 errors**.
- TV lint: **0 errors, 47 warnings**. Phone lint: **0 errors, 11 warnings**. Advisory warnings remain, including fixed orientation/platform adaptation, existing localisation/manifest issues, KTX suggestions, and the existing native certificate-pinning trust-manager warning. No lint failures were suppressed to obtain this result.
- Emulator `setupsession` instrumentation **passed** using an isolated target-owned test store: object recreation retained progress/feature choices; pause retained the step; intercepted intent was rejected; native Home intent was accepted only during a test. This is persistence/intent evidence, not a physical Home test or an actual reboot test.
- Settings/full UI and short UI smoke attempts were interrupted by emulator disconnection/offline state. No completed screenshot review, physical-D-pad acceptance, recovery execution, phone session, startup benchmark or leak/performance pass is claimed. The first persistence-fixture attempt also exposed an incorrect test-package storage context; the fixture was corrected to a target-owned isolated store and then passed.
- Logs/test outputs remain in ignored `releases/sprint-build.log` and `releases/sprint-review/`. The harness targets only an emulator it starts, with bounded startup/test windows. Failed/incomplete runs are not passes merely because a shell command returned.

Validation command used:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :phone:assembleDebug :phone:assembleDebugAndroidTest :phone:lintDebug --offline
```

The unchanged phone tasks passed in the complete check; subsequent TV-only rechecks covered final TV refinements. No physical-device instrumentation was run.

## Measured size, storage and performance

Identical debug signing/toolchain, saved rc1.25 baseline and new candidate:

| Artifact | Compressed APK bytes | Sum of uncompressed ZIP entries | DEX bytes |
| --- | ---: | ---: | ---: |
| TV rc1.25 baseline | 30,205,663 | 28,605,509 | 27,559,532 |
| TV rc1.26 sprint | 30,572,167 | 28,800,501 | 27,753,744 |
| Native phone, unchanged | 857,991 | 2,460,321 | 2,421,708 |

TV delta: **366,504 APK bytes (+1.21%)**, **194,992 uncompressed-entry bytes**, **194,212 DEX bytes**. Both TV APKs contain 173 ZIP entries: no duplicate images, bundled phone APK or new runtime library was added. ZIP entry totals are packaging measurements, **not installed-storage estimates**. Alignment/signature/container overhead explains why these totals do not simply equal APK size.

TV and phone APKs are delivered separately; native phone packaging adds zero bytes to the TV artifact today. Future local APK bundling must be measured separately, not hidden in the Settings delta. APK signature verification passed; the new TV candidate and saved baseline have the same signing-certificate SHA-256 fingerprint, so signer continuity is established against that saved APK, not against an uninspected live installation.

**Not measured:** live Xiaomi free space, installed app code/data/cache size, wallpaper/cache growth, KD5 PSS, Settings latency, frame timing or cold startup. No physical device was connected, and emulator disconnections prevented a reliable UI/performance run. The owner's reported approximately 600 MB free is planning context, not a fresh measurement. No arbitrary APK-size threshold is substituted for these missing results.

## Git and preserved artifacts

Coherent local commits:

- `54bcafb` — approved architecture and S01–S39 preservation map.
- `10a90b4` — Settings/shared controls, durable setup/recovery framework, verification and tests.
- `5cbc27f` — companion stop/approval preservation and network-address handling.
- `b4aee0a` — scoped Android system-screen resolution and manifest visibility.
- Final documentation/test-harness handoff commit is the subsequent branch tip.

No push, merge or deployment performed. Final working-tree status is checked after the handoff commit. APKs, logs and device-test artifacts are ignored, not committed. Review artifacts:

- `releases/sprint-settings-setup/RC-TV-Launcher-1.0.0-rc1.26-sprint-debug.apk` — SHA-256 `e690369059bdce02f171ef01cfed3795722bd00d995d815fc475bf3123eceb8e`.
- `releases/sprint-settings-setup/RC-TV-Remote-0.5.1-rc1-debug.apk` — SHA-256 `79bf78d8aa2c339140d52134339b15c7f9de6184f77299a333f016c3dbd375b1`.
- Saved TV baseline: `releases/phase-online-wallpapers/RC-TV-Launcher-1.0.0-rc1.25-debug.apk`.

## Exact owner acceptance steps

1. **Keep the current working Xiaomi installation until ready for a supervised review.** Preserve an off-device settings backup, original imported images and the actual installed working APK. The saved rc1.25 artifact is not assumed to equal the current installed build. Do not uninstall, clear data or assume a downgrade is safe.
2. After inspecting this report, choose whether to authorise an **in-place update** with the new TV APK. There is no need to update the unchanged phone APK. No installation was performed for you. If using a PC, identify the authorised TV serial and verify the currently installed signer/version first; use an in-place installer/update workflow, never uninstall as a workaround.
3. On launch, confirm the usual Home, favourites, order, wallpaper and physical remote still work. Press Home once to confirm existing routing remains as expected; do not open a Home chooser, change services or reboot as part of the Settings-only review.
4. Open **Settings**. Up/Down should browse the seven categories while keeping the rail visible; Right/OK enters content. Back should return to the rail, then exit Settings. Test Left/Right within existing option groups, repeated keys, scroll, and Back from wallpaper, apps, Cast, Speed Test and Backup; verify focus returns to the originating entry.
5. Change font/text size/accent, inspect immediate feedback, then restore your original choices. Check Standard and Extra Large from three metres. Large type may use full-width category/page navigation. Verify labels, current values and confirmation buttons remain readable. Test Cancel on scoped/full reset; do not confirm a reset unless desired.
6. **Home & Apps → Manage favourites:** add/remove a disposable favourite, move it up/down, reopen and confirm persistence; restore the original membership/order. Check existing long-OK menus still work. Open wallpaper/gallery/media/weather/screensaver/backup pages and verify their original controls and saved values remain present.
7. **System & Recovery → Easy setup:** review Welcome, appearance, favourites and optional features. Use Continue/Skip, physical Back and Finish later; reopen and check the saved step. To test process-loss persistence, use a separately agreed app-only restart procedure; do not reboot the Xiaomi merely for this review. Skip Home choice and any permission-changing action to preserve the current configuration.
8. In Home verification, the existing enabled RC Home-button service should be reported separately and must prevent a false default-Home success claim. Do not disable that service or change Home just to obtain a green status. Actual neutral Home trials belong to a separately agreed routing test.
9. **System & Recovery → Restore Original Launcher:** inspect candidates/disabled state, observation provenance, interception status and instructions. Test Back/Finish later/resume. **Do not enable stock packages, select another Home, change Accessibility or execute the detailed package instructions during this interface review.** No privileged restore button is active and no self-uninstall shortcut is offered.
10. Optional native phone review: reconnect an already approved phone, use **Stop remote · keep approved phones**, start again and verify approval survives. Only revoke/forget a disposable test phone. Test DHCP/network changes later with the physical remote available. Download UI should truthfully state that secure local hosting is not implemented.
11. Record screenshots and any failed focus/Back/readability cases. Before wider use, measure installed storage/cache/PSS/frame timing and repeat the Settings review on the 1 GB KD5. Reboot/default-Home/restoration acceptance requires its own explicit approval and recovery plan.

## Approvals and next implementation tasks

No coding/build permission remains blocked. The owner authorised the read-only ADB diagnosis below; that does not authorise installation or system changes. Fresh owner approval is required before physical TV installation or any routing/permission/debugging/reboot/recovery operation. The preserved Xiaomi remains unchanged.

Next work, in order: obtain stable isolated UI execution and complete screenshots/focus/large-text acceptance; refine common option rows and advanced disclosure from that evidence; test actual process-loss/resume and direct system-screen return; measure performance/storage on authorised hardware; implement and test local HTTPS trust/download/browser pairing as a separate security-reviewed increment; then native phone read-only diagnostics. Keep privileged package operations disabled until independent rescue and restoration acceptance pass.

## Requested read-only Xiaomi ADB diagnosis

Performed after the implementation handoff was prepared, at the owner's explicit request, on 2 November 2026. This physical connection is separate from the emulator failures.

- Host ADB: Android Debug Bridge 1.0.41, Platform Tools `36.0.0-13206524`. Local server responded successfully and a listener was visible on `127.0.0.1:5037`. Initial device inventory was empty.
- Host Ethernet address: `192.168.1.59/24`. Repository evidence in `XIAOMI-774-WORKING-TAKEOVER.md` and `HOME-TAKEOVER-READINESS.md` identifies the previously authorised **legacy TCP endpoint `192.168.1.39:5555`**, not Android-11-phone-style wireless pairing.
- One explicit `adb connect 192.168.1.39:5555` returned **connected** immediately. The subsequent inventory showed `device`, product/codename `jaws`, model `MiTV_AFKR0`. Read-only `getprop ro.product.model` and `getprop ro.build.version.release` confirmed **MiTV-AFKR0 / Android 11**.
- Classification: **connection restored and authorised**. The endpoint was reachable; the connection was not refused; it was not `unauthorized` or `offline`; no fresh approval prompt or TV setting change was needed. The earlier restricted-process failures were development-environment access failures and were already resolved by using approved host access. They were not evidence that the physical TV was unreachable.
- Best-supported explanation: the working local ADB server had no registered TCP connection to the TV. Re-registering the documented endpoint was sufficient. The precise earlier cause of losing that host-side connection is not proven; a host-server restart is plausible, especially because preflight had started a nonrunning server, but no TV/network/debugging change is established.
- No server reset, key replacement, network scan, `tcpip` transport change, Settings launch, package operation, APK installation, Home/Accessibility/keyboard change, reboot or data modification was performed on the TV. No further physical-device tests or performance measurements were undertaken under this diagnosis request. The authorised TCP connection was left available.
