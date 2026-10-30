# RC TV Launcher

A native, offline-first Android TV Home launcher, written in Kotlin and Jetpack Compose. No ads, analytics, network services or paid APIs.

## Status

**Phase B wallpaper-selection feedback checkpoint; not version 1.0.** The approved Cinematic Glass Home direction is preserved. Typography and grouped Settings, layered glass navigation and restrained focus springs are implemented. Long-press Select opens a floating application menu with favourites, draft reordering, Android App Information and Android-confirmed Uninstall. Settings now includes three bundled scenes with thumbnail selection, Home preview, Apply/Cancel and restart persistence. The mountain remains the default. Development pauses here for wallpaper feedback before imports/rotation/appearance controls. See [the wallpaper checkpoint](docs/WALLPAPER-SELECTION-CHECKPOINT.md), [the previous interaction verification](docs/PHASE-B-POLISH.md) and [the original Home review](docs/GLASS-2-HOME-REVIEW.md).

Android system sans-serif typography is shared across sections. Minimal skips focus scaling and panel entrance effects; reduced motion takes priority. Temporary Apps/Search Pin controls have been removed after the user confirmed physical remote menu operation. Rearranging now happens directly on the Home shelf with arrows, visible card movement, Select/Save to commit and Back/Cancel to discard. Saved favourites migrate without resetting membership or preferences. No full personalisation system, account integration or RC Assist is included.

## Build

Open this directory in Android Studio. Install Android SDK 36, accept its licences and use JDK 17 or 21. Set `sdk.dir` in untracked `local.properties`, or configure `ANDROID_HOME`.

On Windows: ` .\gradlew.bat assembleDebug testDebugUnitTest lintDebug`

On macOS/Linux: `sh gradlew assembleDebug testDebugUnitTest lintDebug`

The debug APK is `app/build/outputs/apk/debug/app-debug.apk` (0.2.9-wallpaper-selection). `assembleProfile` produces a same-signed, non-debuggable, shell-profileable development APK for practical TV review. Both support Android 11 (API 30) and later and target API 36. Do not use the debug key for public releases. Prior working APKs remain under local `releases/rollback/`; see the Home review report before downgrading from the profile build.

## Install and recover

**Device-test update:** the Android 11 startup crash is fixed and Phase A functional acceptance passed. Phase B is now authorised and underway. The default Home remains Xiaomi's stock launcher. See [startup investigation and Windows ADB log capture](docs/STARTUP-DIAGNOSTICS.md) for the historical diagnosis.

See [setup and recovery](docs/SETUP-AND-RECOVERY.md). Do not replace the default Home until you have confirmed that Android Settings and the original launcher remain accessible. Test both Xiaomi boxes with [the checklist](docs/DEVICE-TESTS.md).

## Architecture and rights

See [architecture, dependency inventory and privacy](docs/ARCHITECTURE.md), and [remaining work](docs/RELEASE-READINESS.md). Project code and original vector artwork are MIT licensed. Dependencies retain their own licences. No third-party streaming logos or fonts are bundled. Original bundled wallpaper provenance and prompts are recorded in [the asset inventory](docs/WALLPAPER-ASSETS.md).
