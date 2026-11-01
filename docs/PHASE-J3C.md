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
