# RC TV Launcher

A native, offline-first Android TV Home launcher, written in Kotlin and Jetpack Compose. No ads, analytics, network services or paid APIs.

## Status

**Phase D development preview (TV 0.5.1 / Android Remote 0.1.0), not version 1.0.** Approved Home and saved preferences are preserved. Keyboard adds cursor-left/right, repeat and safe middle/selection editing. Optional Android companion provides encrypted local pairing with TV approval and RC TV-only navigation/search; physical-phone acceptance remains pending. See [connection instructions](docs/COMPANION.md), [Phase D checks](docs/PHASE-D.md), [casting findings](docs/CASTING-COMPATIBILITY.md) and [RC Assist architecture](docs/RC-ASSIST-ARCHITECTURE.md).

Physical app context menu, direct-shelf favourites rearranging, Search, saved theme/motion and eight favourites remain preserved. No RC Assist or optional profile implementation. [Release blockers](docs/RELEASE-READINESS.md) remain explicit; the old Minimal regression is reserved for a dedicated optimisation checkpoint.

## Build

Open this directory in Android Studio. Install Android SDK 36, accept its licences and use JDK 17 or 21. Set `sdk.dir` in untracked `local.properties`, or configure `ANDROID_HOME`.

On Windows: ` .\gradlew.bat assembleDebug testDebugUnitTest lintDebug`

On macOS/Linux: `sh gradlew assembleDebug testDebugUnitTest lintDebug`

The debug APK is `app/build/outputs/apk/debug/app-debug.apk` (0.3.0-wallpaper-personalisation). `assembleProfile` produces a same-signed, non-debuggable, shell-profileable development APK for practical TV review. Both support Android 11 (API 30) and later and target API 36. Do not use the debug key for public releases. Prior working APKs remain under local `releases/rollback/`; see the Home review report before downgrading from the profile build.

## Install and recover

**Device-test update:** the Android 11 startup crash is fixed and Phase A functional acceptance passed. Phase B is now authorised and underway. The default Home remains Xiaomi's stock launcher. See [startup investigation and Windows ADB log capture](docs/STARTUP-DIAGNOSTICS.md) for the historical diagnosis.

See [setup and recovery](docs/SETUP-AND-RECOVERY.md). Do not replace the default Home until you have confirmed that Android Settings and the original launcher remain accessible. Test both Xiaomi boxes with [the checklist](docs/DEVICE-TESTS.md).

## Architecture and rights

See [architecture, dependency inventory and privacy](docs/ARCHITECTURE.md), and [remaining work](docs/RELEASE-READINESS.md). Project code and original vector artwork are MIT licensed. Dependencies retain their own licences. No third-party streaming logos or fonts are bundled. Original bundled wallpaper provenance and prompts are recorded in [the asset inventory](docs/WALLPAPER-ASSETS.md).
