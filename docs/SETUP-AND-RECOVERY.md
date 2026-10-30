# Installation and recovery

1. Keep the original launcher installed and enabled. Confirm you can open Android Settings before changing Home.
2. Transfer the debug APK to your TV using a trusted USB installer or install over an explicitly authorised ADB connection: `adb install -r app-debug.apk`.
3. Open RC TV Launcher from the installed apps list. Test remote focus, app opening and the Settings section.
4. In RC TV Settings, choose **Choose default Home**. The app requests Android's Home role when available, otherwise opens Home settings. Select RC TV Launcher if the firmware offers it.
5. Press Home, restart, and reboot. Record results separately for both Xiaomi MiTV-AFKR0 boxes. Firmware may force the manufacturer launcher; this application does not bypass it.

## Return to the original launcher

Use **Settings → Open Home settings / restore** inside RC TV. In Android Settings → Apps → Default apps → Home app, select Xiaomi / Google TV's original launcher. Firmware menu labels vary. Confirm Home opens it before uninstalling RC TV.

If RC TV cannot open, use the device's settings button or its Settings shortcut. If you previously enabled and authorised ADB, `adb shell am start -a android.settings.SETTINGS` opens ordinary settings. Choose the original Home there. As a last application-level recovery option, `adb uninstall za.co.rc.tv` removes RC TV and its launcher preferences, allowing Android to resolve Home again while the original launcher remains installed. These commands are instructions only; none are executed automatically.

Never disable, uninstall or clear the stock launcher's data. No package-disabling, remapping, accessibility interception or boot receiver is used. If ordinary Home selection is unavailable, keep RC TV as a launchable app and report the firmware limitation.
