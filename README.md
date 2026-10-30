# RC TV Launcher

A native, offline-first Android TV Home launcher, written in Kotlin and Jetpack Compose. No ads, analytics, network services or paid APIs.

## Status

**Phase B first design preview; not version 1.0.** Phase A passed the user's functional Xiaomi Android 11 device tests and is merged into main. This branch begins Cinematic Glass from that verified checkpoint. It preserves installed app discovery/launching, preferred app ordering, DataStore favourites, search, five sections and safe Home recovery. See [the first increment and rollback guide](docs/PHASE-B-CINEMATIC-GLASS.md) and [the master specification](docs/MASTER-SPECIFICATION.md).

The native Compose preview adds original cinematic landscape artwork, translucent glass surfaces, a compact expanding rail, polished installed-app tiles and RC TV / “Your world. In focus.” branding. Cinematic and Minimalist appearances and three motion intensities persist across restarts. Home Back stays within RC TV; other sections return Home. The [Back and performance fix report](docs/PHASE-B-BACK-AND-PERFORMANCE.md) records matched release-like improvements to about 0.5–2% jank in the measured workload. Minimal remains the safe default; broader acceptance is outstanding. Wallpaper selection/rotation, full transitions, focus restoration, custom ordering, profiles, media/file management, weather and companion features remain future increments. The future long-press menu and RC Assist are recorded in the [interaction roadmap](docs/INTERACTION-ROADMAP.md).

## Build

Open this directory in Android Studio. Install Android SDK 36, accept its licences and use JDK 17 or 21. Set `sdk.dir` in untracked `local.properties`, or configure `ANDROID_HOME`.

On Windows: ` .\gradlew.bat assembleDebug testDebugUnitTest lintDebug`

On macOS/Linux: `sh gradlew assembleDebug testDebugUnitTest lintDebug`

The debug APK is `app/build/outputs/apk/debug/app-debug.apk` (0.2.1-cinematic-fixes). `assembleProfile` produces a same-signed, non-debuggable, shell-profileable development APK for practical TV performance review. Both support Android 11 (API 30) and later and target API 36. Do not use the debug key for public releases. Phase A and the preceding Phase B APKs remain under local `releases/rollback/`; see the current fix report before downgrading from the profile build.

## Install and recover

**Device-test update:** the Android 11 startup crash is fixed and Phase A functional acceptance passed. Phase B is now authorised and underway. The default Home remains Xiaomi's stock launcher. See [startup investigation and Windows ADB log capture](docs/STARTUP-DIAGNOSTICS.md) for the historical diagnosis.

See [setup and recovery](docs/SETUP-AND-RECOVERY.md). Do not replace the default Home until you have confirmed that Android Settings and the original launcher remain accessible. Test both Xiaomi boxes with [the checklist](docs/DEVICE-TESTS.md).

## Architecture and rights

See [architecture, dependency inventory and privacy](docs/ARCHITECTURE.md), and [remaining work](docs/RELEASE-READINESS.md). Project code and original vector artwork are MIT licensed. Dependencies retain their own licences. No streaming logos, wallpapers or third-party fonts are bundled.
