# Phase J2 — Connections & Weather polish

TV **1.0.0-rc1.5 (34)**, phone **0.5.1-rc1 unchanged**. Baseline `72eab6c`, branch `feature/phase-b-cinematic-glass`. Checkpoint A `45a8e89`; checkpoint B `c3fa92e`. Release candidate. PR #2 remains draft and unmerged; no push, public publication or default Home change.

## A. Weather footer

The existing timestamp/details footer is 10 sp with 12 sp line height, muted grey, right aligned, with 2 dp top/end inset plus the existing container padding. Its prior 14 sp line occupies the same normal-size height after the inset; no outer card/shelf geometry was changed. Main temperature/location/condition, real observation timestamp formatting and the whole-card clickable/D-pad details action remain unchanged. One line with ellipsis prevents awkward wrapping. Extremely long translated status text can truncate; the details page remains available.

Real-weather emulator checks passed Standard and Extra Large/high-readability fonts; full-size favourite cards remained visible. Screenshots were visually inspected. The user physically reviewed Xiaomi and confirmed the footer looks right.

[Physical Xiaomi weather/Home](../releases/review/phase-j2/current-xiaomi.png), [Standard](../releases/review/phase-j2/weather-standard.png), [Extra Large](../releases/review/phase-j2/weather-xl.png).

## B. Connections & Devices

Added Wi-Fi & Internet and Bluetooth Devices buttons, with vector Wi-Fi/Bluetooth marks and the existing FocusButton animation. Existing network status is displayed directly below Wi-Fi; Speed Test remains below the system-management guidance. Extra Large content scrolls normally. Home network indicator still opens this category. No new categories, permissions, startup requests, observers, services or pairing implementation were added.

Uses documented [Android Settings public actions](https://developer.android.com/reference/android/provider/Settings): ACTION_WIFI_SETTINGS and ACTION_BLUETOOTH_SETTINGS. The actual launch is guarded; failure tries ACTION_SETTINGS once and explains the fallback in a long toast. Permanent page guidance also identifies Network & Internet and Remotes & Accessories. If general Settings also fails, the user is directed to the original launcher. No device/component name is hardcoded in product code. No new package-query entries are needed because the code attempts the implicit launch rather than relying on package-visibility resolution.

### Actual Xiaomi Android 11 results

- ACTION_WIFI_SETTINGS resolves to `com.android.tv.settings/.connectivity.NetworkActivity`. The Android Network and Internet page was visually verified; the system add-network flow and existing RC keyboard were also observed. Back returned to RC TV. No network was added or modified.
- ACTION_BLUETOOTH_SETTINGS and ACTION_WIRELESS_SETTINGS have no resolver. Selecting the actual RC TV Bluetooth button opened `.MainSettings` and displayed the precise fallback explanation. The live Bluetooth Back check was interrupted by the test-connection loss; both emulator return paths passed. This OEM requires the user to choose Remotes & Accessories from general Settings. No Bluetooth device pairing/audio/control session was attempted; universal direct pairing support is not claimed.
- A temporary ADB connection loss interrupted further live checks; reconnect restored access and the user confirmed the TV stayed on Wi-Fi. No permission grants or Wi-Fi toggles were performed.
- Private before/after audit: all launcher preference keys identical; exact eight favourites and order unchanged; encrypted approvals remain present at 181 bytes; saved weather location/effective enabled state unchanged. Stock Google Home, RC keyboard and user-enabled accessibility service remain selected/enabled.

[Physical Connections page](../releases/review/phase-j2/connections-xiaomi.png), [Standard emulator](../releases/review/phase-j2/connections-standard.png), [Extra Large emulator](../releases/review/phase-j2/connections-xl.png). Local system screenshots also document network/fallback destinations; they may contain system network names and are not public artifacts.

## Checks and performance

- Targeted unit tests: SystemConnections fallback sequencing (2) and weather policy (2) passed. Existing SpeedMeasurements tests also passed separately. No repeated full suite.
- Emulator Settings review passed Standard and Extra Large: both system destinations opened, Back returned to RC TV; category focus restoration, phone pairing/typing/keyboard control access, wallpaper chooser Back, Weather/System/Apps categories and Speed Test Start Test access passed. These automated checks ran only on emulator with accessibility preservation enabled, never on Xiaomi.
- Speed Test measurement code is unchanged. Entry/Start Test navigation and calculation tests were verified; no fresh full bandwidth measurement is claimed for J2.
- Final product build and lint passed: 0 errors, 27 advisory warnings. Subsequent test-only review extension compiled/passed. HomeAppCard remains byte-identical (SHA256 `57340A0268E445499609688E9150ADB031125C8C596BB90BC6DA0FD3A68C1508`); navbar, clock, wallpaper and focus implementation unchanged.
- No new startup work. One emulator cold launch was 2894 ms; first Xiaomi start after package replacement was 4735 ms with normal initial app discovery. These small, differently conditioned samples do not establish a speed regression or improvement. No continuous network work was added.

## Delivery

[TV APK](../releases/phase-j2/RC-TV-Launcher-1.0.0-rc1.5-debug.apk). The installed APK retains the same development signing certificate. Baseline rollback preserved at `releases/rollback/RC-TV-Launcher-72eab6c-debug.apk`; earlier rollbacks remain. No phone update required. Remaining Codex usage at review: approximately 15% short window / 25% weekly.

No Screensaver & Power, browser, file management or VLC work was started. Checkpoint ends here.
