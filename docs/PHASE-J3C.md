# Phase J3C — Essential Settings, Screensaver Provider and Launcher Recovery

Baseline 499ed8b / TV 1.0.0-rc1.11; feature/phase-b-cinematic-glass. Phone unchanged. User's follow-up takes precedence over the old 5% stopping rule: continue A → B → C while usage permits; never initiate the available reset. Preserve tested APKs, verify physically and commit each checkpoint. No system package may be disabled, removed, suspended or modified.

## Checkpoint A — TV 1.0.0-rc1.12 (41)

Apps & Media now opens Android's actual all-app management for permissions/storage/cache and internal/USB Storage settings. A real StatFs snapshot displays available/total shared internal data space and refreshes on resume without polling. System & Recovery adds About, combined Display & sound, Date & time, Storage, Updates and Accessibility, alongside full Settings and the existing Home/recovery actions. No Home design change or new permissions.

Public intents are resolved against exported system applications; package visibility queries explicitly cover these destinations. Framework stubs/resolvers are rejected. Failed/missing destinations fall back to general Android Settings with the actual navigation route, instead of entering an empty activity. Updates require a system handler with positive priority. No OEM private component is hardcoded. Permissions/cache are managed in Android app details, not directly by RC TV.

Physical MiTV-AFKR0 evidence (D-pad OK activation, Back return and category focus restoration; all favourite preferences/order preserved):
- App management: com.android.tv.settings/.device.apps.AllAppsActivity
- Internal/USB storage: com.android.tv.settings/.device.StorageResetActivity
- Full Android Settings: com.android.tv.settings/.MainSettings
- About: com.android.tv.settings/.about.AboutActivity
- Display action absent; public Sound action correctly opens combined .device.displaysound.DisplaySoundActivity
- Date/time: .system.DateTimeActivity
- System updates: com.google.android.gms/.update.SystemUpdatePanoActivity (opened only; no update initiated)
- Accessibility resolves to framework stub and safely falls back to MainSettings, with System → Accessibility guidance.

Measured internal data volume: 463273984 available / 4627488768 total bytes at test time. Values change with normal device use. This is usable app data space, not a claim about physical flash capacity. Storage lists attached USB media when Android exposes it; no disk formatting/ejection or file operation was performed.

Build, 47 unit tests and lint pass (0 errors, 27 existing warnings). The automated physical test saved its completion evidence after every assertion passed. ADB intermittently disconnected before returning its final console result; internal completion evidence and all destination screenshots were recovered. No AndroidRuntime crash was reported.

APK: releases/phase-j3c/RC-TV-Launcher-1.0.0-rc1.12-debug.apk. Previous rollback: releases/rollback/RC-TV-Launcher-499ed8b-debug.apk. Physical screenshots/evidence: releases/review/phase-j3c/.

Sources checked before implementation: [Android Settings actions](https://developer.android.com/reference/android/provider/Settings), [StatFs](https://developer.android.com/reference/android/os/StatFs), [Home role and user consent](https://developer.android.com/reference/android/app/role/RoleManager).

## Checkpoint B — TV 1.0.0-rc1.13 (42)

Checkpoint A committed as 7ff2f25 before provider work. Added Android / Xiaomi Screensaver and RC TV Screensaver selection. A persisted one-time DataStore migration detects the pre-existing launcher store before first read: existing installations retain RC TV; new installations default to Android/Xiaomi. Saved choices override this migration on every later startup. Invalid provider data falls back to Android. No original style, interval or favourites are reset.

Android provider cancels/skips the RC idle coroutine; RC preview remains an explicitly labelled preview and does not change providers. The system ambient shortcut resolves genuine system handlers, rejects stubs and falls back to Android Settings → System → Ambient mode. RC provider reuses the verified timer, foreground/audio guards and visuals. Android's own ambient system is never disabled or reconfigured: it retains priority and RC's lifecycle guard removes its screen when another activity takes over. RC TV does not claim to force Android's screensaver to start.

Physical provider-switch test passed: upgrade selected RC TV as expected; Android selection suppressed an accelerated idle deadline; explicit RC preview and Back dismissal worked while Android was selected; RC selection enabled the same deadline; style/interval and all eight favourites/order stayed intact. Original provider and interval were restored after testing. A separate instrumentation process after explicitly stopping only RC TV confirmed provider/interval/style/favourites/order persisted. Screenshots: provider-android.png and provider-rc.png. Evidence: checkpoint-b.txt under releases/review/phase-j3c.

48 unit tests, APK/test APK build and lint passed (0 errors, 27 existing warnings). Fresh-install default/migration choices are unit-tested; the physical user data was not cleared to simulate a fresh install. APK: releases/phase-j3c/RC-TV-Launcher-1.0.0-rc1.13-debug.apk. A rollback: releases/rollback/RC-TV-Launcher-7ff2f25-debug.apk.

## Checkpoint C — TV 1.0.0-rc1.14 (43)

Checkpoint B committed as faba59d before this work. Added System & Recovery → Launcher Management, with live-on-resume actual Home status, explicit supported RC Home selection, original-launcher chooser, full Android Settings fallback and confirmed visual reset. Chooser/stub resolutions are reported as unknown instead of being mistaken for a default app. A Home role grant is explicitly distinguished from actual Home routing. Returning from the nested screen restores focus to its Launcher Management entry.

The reset atomically removes a visual-only preference allowlist (wallpaper and rotation, theme, colours, font/size, visibility and focus/interface animation). It does not remove imported wallpaper files. Favourites/order, phone pairing, keyboard, Reduced Motion, screensaver provider/style/interval and unrelated future preferences are preserved. Cancel is initially focused. The physical confirmed-reset test exercised the production action, checked actual logical encrypted pairing records and selected keyboard without printing credentials, and restored only the original visual keys afterward. The test uses reflection only on RC TV's own private DataStore field; no Android hidden API or permission bypass is used.

### Physical recovery result — BLOCKED by firmware

1. The public Android Home chooser opened successfully and showed Google TV, RC TV Launcher and Setup Wizard.
2. Before this test, RC TV was already checked in that chooser and Android reported the RC TV Home role as held; actual Home resolution was nevertheless Google TV. The test explicitly selected the visible RC TV Launcher row again.
3. Three actual Android Home-key events still opened com.google.android.apps.tv.launcherx/.home.HomeActivity. PackageManager's default Home resolution also remained that Google TV activity. RC TV cannot honestly claim it became the operative default Home.
4. The switch/restart recovery sequence was stopped at this failure, as requested. Reboot and switch-back/reboot steps 4–8 were not attempted or claimed. No launcher disabling, forceful preferred-activity/role modification, root, device-owner or accessibility workaround was used. Google TV remains installed and was observed launching correctly. RC TV remains usable by opening it as an app; the management screen shows the role/routing mismatch.

This is an unresolved device/platform default-Home acceptance blocker, not a quota stop. No unsupported default-launcher workaround is included.

### Verification

- 50 unit tests passed, including safe Home-status classification and visual-reset preservation.
- APK and separate test APK builds passed. Final lint: 0 errors, 27 existing warnings.
- Physical Launcher Management, live Home description, confirmation/Cancel, Android Settings, nested Back focus and supported Home chooser entry passed without a silent default change.
- Physical confirmed reset passed default-visual assertions and preserved all eight favourites/order, logical pairing grants, keyboard, Reduced Motion and screensaver choices. Original visual preferences were restored after testing.
- Extra Large physical review passed Apps & Media, Launcher Management, reset confirmation and provider dialog; text size was restored afterward. Corresponding screenshots were retrieved and visually reviewed.
- Approved HomeAppCard SHA256 remains 57340A0268E445499609688E9150ADB031125C8C596BB90BC6DA0FD3A68C1508; no Home geometry/artwork/navbar/weather/animation redesign. Phone sources unchanged; PR #2 unmerged; no public release.
- The user's supplied J3B acceptance (real 10-minute timer, Reduced Motion and 15–20 minutes of uninterrupted streaming, 15/17 tested checks passed) is retained; those long checks were not repeated unnecessarily.

### Lightweight baseline (debug, not a stock comparison)

APK: 27873174 bytes. Android own-package StorageStats: appBytes=27893760, dataBytes=24621056, cacheBytes=249856. Data includes retained test screenshots/evidence and normal app data. No usage-access permission was requested. [Own-package storage API](https://developer.android.com/reference/android/app/usage/StorageStatsManager#queryStatsForPackage(java.util.UUID,%20java.lang.String,%20android.os.UserHandle)).

One post-open memory sample: total PSS 77135 KiB, RSS 161284 KiB, graphics 7020 KiB. Explicit cold app opening took 3459 ms (ADB total time); this is not working Home-button routing or a statistically controlled benchmark. The Google TV process was present while RC TV was foreground; no stock background service was stopped. No faster-than-stock claim is made.

Final APK: releases/phase-j3c/RC-TV-Launcher-1.0.0-rc1.14-debug.apk, SHA256 81A8ED5742CB3A32D1AE54973E721804BCE1041B118C43FE2C8260F255EDD9F0.
Rollbacks: releases/rollback/RC-TV-Launcher-faba59d-debug.apk (B / rc1.13), RC-TV-Launcher-7ff2f25-debug.apk (A / rc1.12), RC-TV-Launcher-499ed8b-debug.apk (J3B / rc1.11). All earlier rollback APKs remain preserved.

Final evidence under releases/review/phase-j3c: apps.png, system.png, provider-android.png, provider-rc.png, launcher-management.png, reset-confirmation.png, home-selection.png, home-routing-result.png; Extra Large apps-xl.png, launcher-xl.png, reset-confirmation-xl.png and provider-choice-xl.png. Text evidence includes checkpoint-a.txt, checkpoint-b.txt, checkpoint-c-ui.txt, reset-evidence.txt, size-evidence.txt and memory-baseline.txt.

Next product work: J4 USB/HDD file management with VLC, with explicit device-granted storage access and existing Android storage controls as recovery. Default-Home firmware acceptance remains separately blocked; do not silently relax the no-system-package-workaround rule.

Settled foreground-idle sample (after wallpaper/icon caches had loaded): total PSS 136617 KiB, RSS 219488 KiB, graphics 48064 KiB; saved as memory-idle.txt. This is a more representative idle sample than the earlier immediate post-open number, still a single debug-build observation rather than a controlled comparison.

## Mandatory primary-Home follow-up — rc1.15 (44), NOT ACCEPTED

The critical startup/Home requirement supersedes any impression that the Settings checkpoints complete J3C. Further physical tests now include a full reboot, the user's real remote Home check, a supported original/RC chooser round trip, standby and physical wake, and Home from Android Settings/YouTube. Google TV still handles boot and Home despite RC TV's persisted role. Wake correctly resumes an already-open RC TV activity, but Home after wake still goes to Google TV. [Full mandatory acceptance report](PRIMARY-HOME-ACCEPTANCE.md) supersedes the earlier point at which reboot testing had stopped.

Optional startup work is now released after Home content draws, without delaying the UI. Power/restart guidance was added after verifying the remote's Restart menu; ordinary-app reboot/shutdown remains unsupported. No workarounds, system-package changes, extra services or boot receiver were added. 50 unit tests, builds and lint pass; startup-order/navigation/data-preservation instrumentation passed. Explicit-open debug timing samples did not show a speed improvement and are reported without a faster-than-stock claim.
