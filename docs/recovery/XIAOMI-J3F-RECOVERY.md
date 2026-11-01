# RC TV recovery checklist — Xiaomi MiTV-AFKR0

Target: `jaws`, Android 11, active Owner user **0**, firmware `Xiaomi/jaws/jaws:11/RTT0.211222.001/774:user/release-keys`. This folder documents launcher recovery only: it is **not** an Android firmware image, complete app/Keystore backup, or guaranteed rescue from an unbootable device.

The local recovery bundle is `releases/recovery/j3f/`. It contains the preserved rc1.15 APK, relevant rc1.14 rollback, the new verified candidate, SHA-256 file manifest, this checklist, original Home/component states, Home diagnostics/readiness/J3E records and the backup-format document. Its `private-settings/` folder contains a verified real settings backup, including weather location: keep the bundle private. APKs and private backups are intentionally not committed to Git. Keep the bundle on the PC and a separate trusted drive. Do not delete existing rollback APKs.

## First preserve data

1. Keep the device powered and on its current network; record the current debugging address. Existing authorised host is `192.168.1.39:5555`, which can change with network allocation.
2. Keep a trusted schema-1 RC TV backup off the TV. RC TV → Settings → System & Recovery → Backup & Restore. On this firmware the system picker is a stub; use a TV Downloads copy and transfer it using a trusted file app or PC. Local Downloads alone is not independent protection.
3. Schema-1 settings files contain the weather location, not passwords or pairing credentials. Do not publish them. The private pre-upgrade safety tar is local-only diagnostic protection, not a portable credential or full data restore mechanism.
4. Pairing/Keystore material cannot be ported with this backup. Existing pairing on the same installed app is preserved. On another TV, enable keyboard/remote permissions through Android and securely pair again.

## Known access routes

Wireless ADB reconnected after normal reboot in J3D/J3E; a host reconnect was sometimes needed. This is not proof it survives every launcher failure. Normal RC TV and independent Settings launches were verified. Physical Apps/long-Home routes are provided by Google TV and cannot be relied on if that package is unavailable. Wired USB ADB remains unverified; the USB-A host powers HDDs and plays media, which does not establish device-mode ADB. No host-to-host USB-A connection or adapter purchase is recommended without exact hardware evidence.

```powershell
$adb = 'C:\Users\ruanc\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb disconnect 192.168.1.39:5555
& $adb connect 192.168.1.39:5555
& $adb devices -l
& $adb -s 192.168.1.39:5555 shell am start --user 0 -n com.android.tv.settings/.MainSettings
& $adb -s 192.168.1.39:5555 shell am start --user 0 -n za.co.rc.tv/.MainActivity
```

## Repair the RC TV app only

Reinstall the same/newer known-good signed APK using `adb install -r <APK>`. Do not uninstall or clear data. Older APKs can be rejected by version checks; do not turn a rejected downgrade into an uninstall. A debug-only downgrade using `-r -d` requires a separate review, and older versions do not implement schema-1 restore. Keep rc1.16 for restoring schema-1 settings. Verify APK hash against the bundle manifest first. Never run a command against the wrong user/device.

## Stock Home restoration — prepared, not executed during J3F

J3F does not change Home roles or system states; **no Home rollback is required now**. If a separate future authorised experiment changes the whole Google TV package state, emergency package restoration syntax is:

```powershell
& $adb -s 192.168.1.39:5555 shell pm enable --user 0 com.google.android.apps.tv.launcherx
& $adb -s 192.168.1.39:5555 shell pm default-state --user 0 com.google.android.apps.tv.launcherx
& $adb -s 192.168.1.39:5555 shell am start --user 0 -n com.google.android.apps.tv.launcherx/.home.HomeActivity
& $adb -s 192.168.1.39:5555 shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
```

The original package override is DEFAULT (0), not explicit ENABLED (1). A package-level restore does not undo a separately changed component override. Stock HomeActivity is explicitly enabled; VanillaModeHomeActivity explicitly disabled. The original saved Home role/preference is RC TV even though Google TV actually resolves. Use Android's supported Home chooser for a separately authorised role selection, not an assumed shell bypass.

These restoration commands are verified as syntax/targets, **not** as recovery after disabling packages. Do not run them as part of backup testing. Never disable setupwraith RecoveryActivity, setup services, Settings/FallbackHome, SystemUI, input/network/ADB services or other protected components to make RC TV win.

## Stop conditions

If wireless access is lost and neither launcher/Settings route works, no independent data-preserving rescue is proven. Stop; do not enter recovery, factory reset, flash, unlock, root or modify system partitions. Xiaomi's generic hardware recovery instructions lead to a data-erasing reset and are not an acceptable way to test restore. Seek device-specific manufacturer support without promising data retention.

A successful settings backup is **not approval for launcher takeover**. The priority-1 setup recovery candidate still outranks RC TV, and a permitted safe routing change remains unestablished.
