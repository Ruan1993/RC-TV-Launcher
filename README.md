# RC TV Launcher

A native, offline-first Android TV Home launcher, written in Kotlin and Jetpack Compose. No ads, analytics, network services or paid APIs.

## Status

**Phase B wallpaper personalisation; not version 1.0.** Approved Home cards, icons, mountain default, navigation and gallery styling are preserved. Wallpaper supports private JPEG/PNG/WebP imports, confirmed removal, persistent rotation participants/intervals and Instant/Crossfade/Gentle Slide. Minimal/reduced motion switch instantly; rotation only runs on active Home, without a service. This Xiaomi firmware has only a document-picker stub: a compatible picker/provider is needed for actual local/USB selection. The import/decoder pipeline is separately device-tested. See [sprint verification and limitations](docs/SPRINT-WALLPAPERS.md).

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
