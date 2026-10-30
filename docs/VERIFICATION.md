# Foundation verification — 30 October 2026

**Real-device update:** ADB confirmed the Xiaomi MiTV-AFKR0 / Android 11 startup crash in PhoneWindow.getInsetsController, called before the decor view existed. Insets setup now runs after content creation. The repaired APK was installed over the existing app and opens on the box. D-pad Search/Back, discovered app tiles, YouTube launching, return to RC TV and the Settings page were checked. See [startup diagnostics](STARTUP-DIAGNOSTICS.md). The fix passed a fresh 53-task build/test/lint run, 2 unit tests, and 0 lint errors / 7 warnings. Broader hardware acceptance is still outstanding; Phase B remains on hold.

Executed on the Windows development machine with JDK 21, Android SDK 36 and Gradle 8.13.

- `gradlew.bat assembleDebug testDebugUnitTest lintDebug --rerun-tasks`: **BUILD SUCCESSFUL**, with all 53 tasks executed in the pre-commit rerun. Android 11 backup is explicitly disabled with `fullBackupContent="false"`; Android 12+ retains explicit backup/transfer exclusions.
- Kotlin compilation and debug APK packaging passed.
- JUnit: **2 tests, 0 failures, 0 errors**. Coverage is limited to preferred-app ordering, duplicate removal and absent-app handling. Settings, profiles, backup and UI-flow tests are not yet present.
- Android lint: **0 errors, 7 warnings** (reduced from 8 by fixing the Android 11 backup compatibility warning). Five are upgrade notices: Gradle, Compose BOM, Activity Compose, DataStore and Lifecycle. The other two concern the 320dp TV vector banner and fixed landscape orientation on Android 16. The banner is intentionally TV-sized and minSdk 30 supports vectors natively; verify its rendering on hardware. Landscape matches the TV target, but newer-device layout behaviour remains a device-test item. No warnings are suppressed and dependency versions remain pinned for Phase A testing.
- Debug APK copied to `releases/RC-TV-Launcher-0.1.0-debug.apk`.
- Initial verification had no devices attached. Subsequently, authorised network ADB enabled the startup repair and limited real-device smoke tests above. No default-Home change, reboot/recovery test, performance benchmark or complete accessibility review was performed.

The early milestone has a compiled APK but has **not passed physical-device acceptance**. This build is a foundation for testing, not completion of the full version 1.0 specification. See DEVICE-TESTS.md and RELEASE-READINESS.md.
