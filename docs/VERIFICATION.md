# Foundation verification — 30 October 2026

**Real-device update:** installation succeeded on Xiaomi MiTV-AFKR0 / Android 11, but opening the icon briefly shows black then returns to Xiaomi Home. Startup acceptance failed; cause awaits device logs. See [startup diagnostics](STARTUP-DIAGNOSTICS.md). A fresh 53-task build/test/lint rerun passed with 2 tests and 0 lint errors / 7 warnings; it does not resolve or verify this runtime failure. Phase B remains on hold.

Executed on the Windows development machine with JDK 21, Android SDK 36 and Gradle 8.13.

- `gradlew.bat assembleDebug testDebugUnitTest lintDebug --rerun-tasks`: **BUILD SUCCESSFUL**, with all 53 tasks executed in the pre-commit rerun. Android 11 backup is explicitly disabled with `fullBackupContent="false"`; Android 12+ retains explicit backup/transfer exclusions.
- Kotlin compilation and debug APK packaging passed.
- JUnit: **2 tests, 0 failures, 0 errors**. Coverage is limited to preferred-app ordering, duplicate removal and absent-app handling. Settings, profiles, backup and UI-flow tests are not yet present.
- Android lint: **0 errors, 7 warnings** (reduced from 8 by fixing the Android 11 backup compatibility warning). Five are upgrade notices: Gradle, Compose BOM, Activity Compose, DataStore and Lifecycle. The other two concern the 320dp TV vector banner and fixed landscape orientation on Android 16. The banner is intentionally TV-sized and minSdk 30 supports vectors natively; verify its rendering on hardware. Landscape matches the TV target, but newer-device layout behaviour remains a device-test item. No warnings are suppressed and dependency versions remain pinned for Phase A testing.
- Debug APK copied to `releases/RC-TV-Launcher-0.1.0-debug.apk`.
- ADB started successfully with normal Windows access. **No devices attached.** No install, runtime visual test, emulator test, hardware Home/reboot/recovery test, performance measurement or accessibility review was performed.

The early milestone has a compiled APK but has **not passed physical-device acceptance**. This build is a foundation for testing, not completion of the full version 1.0 specification. See DEVICE-TESTS.md and RELEASE-READINESS.md.
