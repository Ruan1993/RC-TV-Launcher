# RC TV Launcher

A native, offline-first Android TV Home launcher, written in Kotlin and Jetpack Compose. No ads, analytics, network services or paid APIs.

## Status

**Phase A foundation; not version 1.0.** This repository begins implementation of [the master specification](docs/MASTER-SPECIFICATION.md). It discovers installed TV and conventional launcher apps, opens them through Android intents, prioritises YouCine / YouTube Music / YouTube / F1 TV when present, stores pinned favourites in DataStore, searches apps, and provides five navigation sections and safe Home setup. Library explicitly explains its unavailable features.

The dark glass-inspired interface is native Compose. It currently uses a procedural gradient, text app tiles and static focus borders. Landscape artwork, app icons, collapsible sidebar, focus restoration, profiles, advanced customisation, media/file management, diagnostics, backup, weather, ambient mode and companion support remain future work. No unavailable integration is presented as working.

## Build

Open this directory in Android Studio. Install Android SDK 36, accept its licences and use JDK 17 or 21. Set `sdk.dir` in untracked `local.properties`, or configure `ANDROID_HOME`.

On Windows: ` .\gradlew.bat assembleDebug testDebugUnitTest lintDebug`

On macOS/Linux: `sh gradlew assembleDebug testDebugUnitTest lintDebug`

The APK is `app/build/outputs/apk/debug/app-debug.apk`. It supports Android 11 (API 30) and later, targets API 36, and is a development build signed with the local debug key. Do not use the debug key for public releases.

## Install and recover

See [setup and recovery](docs/SETUP-AND-RECOVERY.md). Do not replace the default Home until you have confirmed that Android Settings and the original launcher remain accessible. Test both Xiaomi boxes with [the checklist](docs/DEVICE-TESTS.md).

## Architecture and rights

See [architecture, dependency inventory and privacy](docs/ARCHITECTURE.md), and [remaining work](docs/RELEASE-READINESS.md). Project code and original vector artwork are MIT licensed. Dependencies retain their own licences. No streaming logos, wallpapers or third-party fonts are bundled.
