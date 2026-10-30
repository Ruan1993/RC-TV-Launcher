# RC TV Launcher

A native, offline-first Android TV Home launcher, written in Kotlin and Jetpack Compose. No ads, analytics, network services or paid APIs.

## Status

**Cinematic Glass 2.0 Home review checkpoint; not version 1.0.** Development is paused for the user's requested visual feedback before context-menu, Settings and personalisation work. Phase A is merged; this branch preserves discovery/launching, favourites, search and safe Back/Home recovery. See [the Home review, measurements and continuation plan](docs/GLASS-2-HOME-REVIEW.md), [the previous Back/performance fix](docs/PHASE-B-BACK-AND-PERFORMANCE.md) and [the master specification](docs/MASTER-SPECIFICATION.md).

Home now uses larger glass cards and 80dp installed icons, layered reflections, luminous focus edges and the original mountain artwork. Decorative wallpaper headings and Home Pin/Unpin controls are removed. Temporary Apps/Search favourite controls remain until the tested long-press menu is implemented. Existing themes and motion choices persist. The larger shelf has a measured Minimal-mode cost; see the review report rather than treating prior smaller-card results as current guarantees. No additional expensive effects, AI integration or online services have been added.

## Build

Open this directory in Android Studio. Install Android SDK 36, accept its licences and use JDK 17 or 21. Set `sdk.dir` in untracked `local.properties`, or configure `ANDROID_HOME`.

On Windows: ` .\gradlew.bat assembleDebug testDebugUnitTest lintDebug`

On macOS/Linux: `sh gradlew assembleDebug testDebugUnitTest lintDebug`

The debug APK is `app/build/outputs/apk/debug/app-debug.apk` (0.2.2-glass-home-review). `assembleProfile` produces a same-signed, non-debuggable, shell-profileable development APK for practical TV review. Both support Android 11 (API 30) and later and target API 36. Do not use the debug key for public releases. Prior working APKs remain under local `releases/rollback/`; see the Home review report before downgrading from the profile build.

## Install and recover

**Device-test update:** the Android 11 startup crash is fixed and Phase A functional acceptance passed. Phase B is now authorised and underway. The default Home remains Xiaomi's stock launcher. See [startup investigation and Windows ADB log capture](docs/STARTUP-DIAGNOSTICS.md) for the historical diagnosis.

See [setup and recovery](docs/SETUP-AND-RECOVERY.md). Do not replace the default Home until you have confirmed that Android Settings and the original launcher remain accessible. Test both Xiaomi boxes with [the checklist](docs/DEVICE-TESTS.md).

## Architecture and rights

See [architecture, dependency inventory and privacy](docs/ARCHITECTURE.md), and [remaining work](docs/RELEASE-READINESS.md). Project code and original vector artwork are MIT licensed. Dependencies retain their own licences. No streaming logos, wallpapers or third-party fonts are bundled.
