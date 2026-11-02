# RC TV Launcher

A native, offline-first Android TV Home launcher, written in Kotlin and Jetpack Compose. No ads, analytics or paid APIs. The explicit Speed Test contacts Cloudflare; optional configured weather uses Open-Meteo. Home works offline. Optional encrypted phone control uses the local network.

## Status

**Development candidate: TV 1.0.0-rc1.26-sprint / Remote 0.5.1-rc1 unchanged.** Persistent two-panel Settings with adaptive large-text pages, a shared favourites editor, durable ten-step Easy setup and a guided original-launcher recovery framework are implemented. This candidate requires owner acceptance before installation on the preserved Xiaomi. See the [implementation results and testing instructions](docs/IMPLEMENTATION-SPRINT.md).

The owner's Xiaomi firmware-774 takeover is separately [documented as surviving two restarts](docs/XIAOMI-774-WORKING-TAKEOVER.md); earlier J3C/primary-Home no-go reports are historical for that exact installation. This sprint does not alter the Xiaomi, execute package restoration/takeover or establish independent emergency rescue. Secure TV-hosted browser/download distribution and phone ADB assistance remain incomplete; native TLS phone control is retained.

Physical app context menu, direct-shelf favourites rearranging, Search, saved theme/motion and eight favourites remain preserved. RC Assist remains available in Git history at `e8e20c4`. [Release blockers](docs/RELEASE-READINESS.md) remain explicit; the old Minimal regression is reserved for a dedicated optimisation checkpoint.

## Build

Open this directory in Android Studio. Install Android SDK 36, accept its licences and use JDK 17 or 21. Set `sdk.dir` in untracked `local.properties`, or configure `ANDROID_HOME`.

On Windows: ` .\gradlew.bat assembleDebug testDebugUnitTest lintDebug`

On macOS/Linux: `sh gradlew assembleDebug testDebugUnitTest lintDebug`

The debug APK is `app/build/outputs/apk/debug/app-debug.apk` (1.0.0-rc1.26-sprint). `assembleProfile` produces a same-signed, non-debuggable, shell-profileable development APK. Both support Android 11 (API 30) and later and target API 36. Do not use the debug key for public releases. Prior APKs remain under local `releases/`; do not uninstall/clear data or assume downgrades are supported.

## Install and recover

The Android 11 startup-crash investigation and earlier stock-Home tests are historical; current Xiaomi working-state evidence is linked above. New setup/recovery code never changes package states automatically. A disabled original launcher cannot be recovered merely by opening Home selection. Do not uninstall RC TV while it is the only working full launcher.

See [setup and recovery](docs/SETUP-AND-RECOVERY.md). Do not replace the default Home until you have confirmed that Android Settings and the original launcher remain accessible. Test both Xiaomi boxes with [the checklist](docs/DEVICE-TESTS.md).

## Architecture and rights

See [architecture, dependency inventory and privacy](docs/ARCHITECTURE.md), and [remaining work](docs/RELEASE-READINESS.md). Project code and original vector artwork are MIT licensed. Dependencies retain their own licences. No third-party streaming logos or fonts are bundled. Original bundled wallpaper provenance and prompts are recorded in [the asset inventory](docs/WALLPAPER-ASSETS.md).
