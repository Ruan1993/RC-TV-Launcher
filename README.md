# RC TV Launcher

A native, offline-first Android TV Home launcher, written in Kotlin and Jetpack Compose. No ads, analytics, network services or paid APIs.

## Status

**Phase B first design preview; not version 1.0.** Phase A passed the user's functional Xiaomi Android 11 device tests and is merged into main. This branch begins Cinematic Glass from that verified checkpoint. It preserves installed app discovery/launching, preferred app ordering, DataStore favourites, search, five sections and safe Home recovery. See [the first increment and rollback guide](docs/PHASE-B-CINEMATIC-GLASS.md) and [the master specification](docs/MASTER-SPECIFICATION.md).

The native Compose preview adds original cinematic landscape artwork, translucent glass surfaces, a compact expanding rail, polished installed-app tiles and RC TV / “Your world. In focus.” branding. Cinematic and Minimalist appearances and three motion intensities persist across restarts. Minimal motion is the safe default; animated modes still need performance work on the Xiaomi. Wallpaper selection/rotation, full transitions, focus restoration, custom ordering, profiles, media/file management, weather and companion features remain future increments.

## Build

Open this directory in Android Studio. Install Android SDK 36, accept its licences and use JDK 17 or 21. Set `sdk.dir` in untracked `local.properties`, or configure `ANDROID_HOME`.

On Windows: ` .\gradlew.bat assembleDebug testDebugUnitTest lintDebug`

On macOS/Linux: `sh gradlew assembleDebug testDebugUnitTest lintDebug`

The APK is `app/build/outputs/apk/debug/app-debug.apk` (0.2.0-cinematic-preview). It supports Android 11 (API 30) and later, targets API 36, and is a development build signed with the local debug key. Do not use the debug key for public releases. The device-tested Phase A APK is preserved locally under `releases/rollback/`; see the rollback guide before downgrading.

## Install and recover

**Device-test update:** the Android 11 startup crash is fixed and Phase A functional acceptance passed. Phase B is now authorised and underway. The default Home remains Xiaomi's stock launcher. See [startup investigation and Windows ADB log capture](docs/STARTUP-DIAGNOSTICS.md) for the historical diagnosis.

See [setup and recovery](docs/SETUP-AND-RECOVERY.md). Do not replace the default Home until you have confirmed that Android Settings and the original launcher remain accessible. Test both Xiaomi boxes with [the checklist](docs/DEVICE-TESTS.md).

## Architecture and rights

See [architecture, dependency inventory and privacy](docs/ARCHITECTURE.md), and [remaining work](docs/RELEASE-READINESS.md). Project code and original vector artwork are MIT licensed. Dependencies retain their own licences. No streaming logos, wallpapers or third-party fonts are bundled.
