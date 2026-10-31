# Phase J1 — Cinematic Home polish and navigation

TV **1.0.0-rc1.4 (33)**; phone **0.5.1-rc1 unchanged**. Baseline `06a1434`; implementation `f136a79`, followed by the clearer spring focus refinement. Branch `feature/phase-b-cinematic-glass`. Release candidate; user physically approved the rc1.4 Enhanced pop effect on Xiaomi: “Yes, this is strong enough.” PR #2 remains draft and unmerged; no push, public publication or default Home change.

## Changes

Home no longer shows the navbar hint or footer instructions. The favourites heading and clock have floating text with local text shadows. The favourite shelf keeps its previous position (first card edge y537 in both baseline/new emulator captures). The original HomeAppCard source is byte-identical, SHA256 `57340A0268E445499609688E9150ADB031125C8C596BB90BC6DA0FD3A68C1508`; its geometry, material, icon sizes and launch handler remain intact.

Home/Search/Apps/Cast/Speed Test remain in the horizontal navbar. Settings is a separate fixed, prominent control at the right. Left returns to Speed Test; down reaches the clock/network control; up returns to Settings. Remote & Keyboard now groups existing phone pairing/download/manage/typing, accessibility setup and keyboard preview/enable/chooser controls. Easy Setup retains direct access. Connections retains the existing Speed Test and now displays actual network status.

A shared focus modifier animates only the visual layer: Subtle 2.5%, Standard 5%, Enhanced 10% scale, a short spring, 1.3–6 dp lift, elevation and an outer theme outline. Neighbouring items do not reflow. The initial soft transition was insufficient for the user's physical review, so rc1.3 adds the spring/lift/clearer outline, then rc1.4 strengthens Enhanced following a second physical request. Off and Reduced Motion disable all lift/scaling while existing focus indicators remain. Existing Minimal maps to Off; Balanced to Subtle; Cinematic to Standard until a separate focus choice is saved. Fresh installs default to Standard. The user's saved choice is never silently replaced.

## Network API and privacy

The visible activity observes [ConnectivityManager.NetworkCallback](https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback), [NetworkCapabilities](https://developer.android.com/reference/android/net/NetworkCapabilities) and protected Wi-Fi state/RSSI broadcasts through [WifiManager](https://developer.android.com/reference/android/net/wifi/WifiManager). It unregisters on pause/disposal. No polling, foreground service or startup internet request is added. ACCESS_WIFI_STATE is a normal permission; no new sensitive permission prompt is needed. No SSID, BSSID, IP or location is read/stored.

Wi-Fi uses available RSSI for bars; invalid/redacted signal remains unknown rather than invented. Ethernet takes priority when detected; Wi-Fi disconnected/off and neutral unknown/other states are distinct. Lack of Android internet validation is described honestly. Selecting the indicator opens Connections. Wi-Fi switching/Ethernet state transitions were covered by classifier tests, not physically toggled on Xiaomi.

## Verification

- Full J1 unit suite: 41 passed before spring refinement. Targeted FocusAnimation tests passed after refinement; no unrelated full rerun. Final lint: 0 errors, 27 advisory warnings.
- Emulator: normal and Extra Large Home, fixed Settings transfers, Settings categories and Back/category restoration, persistence, unchanged 216 × 190 dp card geometry, real weather rendering, wallpaper layout. After refinement, Enhanced/normal and Standard/Extra Large checks also assert actual transformed on-screen card bounds; Reduced Motion asserts the original bounds. The stronger rc1.4 Enhanced also passed Extra Large navigation/bounds checks. These are emulator checks, not physical acceptance.
- Xiaomi: update installed successfully; moved Remote & Keyboard permissions page and existing connected accessibility status observed. User reported initial focus effect too subtle; rc1.4 installed and the user confirmed the stronger Enhanced effect is strong enough. No UiAutomation tests ran on Xiaomi. Pairing/keyboard protocol implementations and phone APK were unchanged; no new pairing roundtrip is claimed.
- Private before/after audit: exact eight-favourite set and order unchanged, encrypted paired grants remain present (181 bytes; ciphertext changes, so byte equality is not claimed). Saved weather location and effective enabled state are unchanged (missing legacy enabled defaults to true); its reading/cache timestamps can refresh. Changes were wallpaper rotation/current image and the user's new focus choice. Stock Google Home, RC keyboard and user-enabled accessibility service remain selected/enabled.
- Same-emulator cold starts, two samples each: baseline 2942/3068 ms; J1 2547/2546 ms. Small diagnostic sample, not a benchmark or guarantee. Xiaomi first J1 update warm launch 2773 ms; rc1.3 update cold launch was 4108 ms, including the first start after installation; this is not comparable to the emulator pair. Focus uses GPU transforms/native shadow, with no layout animation, bitmap blur, timer or background job.

Implementation commits: `f136a79`, `1a17ce5`, `33bac9a`. Codex usage at delivery review: approximately 23% short window and 27% weekly remaining.

## Artifacts

- [TV rc1.3 APK](../releases/phase-j1/RC-TV-Launcher-1.0.0-rc1.4-debug.apk).
- Rollback baseline: `releases/rollback/RC-TV-Launcher-06a1434-debug.apk`; pre-spring J1: `releases/rollback/RC-TV-Launcher-f136a79-debug.apk`.
- [Home layout — emulator](../releases/review/phase-j1/home-standard.png), [focused card — Enhanced](../releases/review/phase-j1/focus-enhanced.png), [Standard/Extra Large](../releases/review/phase-j1/focus-standard-xl.png), [stronger Enhanced/Extra Large](../releases/review/phase-j1/focus-enhanced-xl.png).
- [Remote & Keyboard Settings](../releases/review/phase-j1/settings-remote.png), [Extra Large Settings](../releases/review/phase-j1/settings-remote-xl.png), [Xiaomi Appearance review](../releases/review/phase-j1/appearance-xiaomi.png).

Emulator has a different installed-app list, so its shelf caption is Your applications; the Xiaomi's eight saved favourites are verified separately. [Physical Xiaomi Home](../releases/review/phase-j1/xiaomi-review-latest.png) was captured during the user’s navigation in rc1.3; rc1.4 Enhanced focus-effect acceptance was then explicitly confirmed by the user. Prior casting/media/accessibility limitations and release blockers are unchanged. This checkpoint adds no phone, weather, media, browser or storage features.
