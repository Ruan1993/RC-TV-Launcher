# Phase J3A — Final Home branding and safe checkpoint

TV **1.0.0-rc1.9 (38)**; phone **0.5.1-rc1 unchanged**. Baseline `afbc7c3`, branch `feature/phase-b-cinematic-glass`. Release candidate; PR #2 remains draft and unmerged. No public release/push.

## Completed: branding

Original native Compose vector lettering replaces the plain heading. RC and TV share matching geometric strokes/cut corners, a white/theme-accent metallic gradient, a restrained halo and dark contour for wallpaper contrast. The user approved the geometric design and requested slightly more spacing; the final revision increases the group gap from 5 sp to 9 sp. The tagline is unchanged; the wordmark is horizontally centred above it within the same header column. No image assets, external fonts, new permissions, services or background work.

Wordmark bounds are 84 sp wide by 25 sp high, scaled through the existing text density; the 40-unit lettering occupies 20 sp vertically. This stays within the existing tagline column width at Standard and Extra Large. Navbar, weather, clock, shelf and favourite component were not redesigned. HomeAppCard remains byte-identical, SHA256 `57340A0268E445499609688E9150ADB031125C8C596BB90BC6DA0FD3A68C1508`.

## Verification

Targeted actual saved-appearance instrumentation passed Standard/Vivid/default accent and Extra Large/Subtle/Midnight Black. Original 216 × 190 dp favourite cards remain visible. Final product build/lint passed: 0 errors, 27 advisory warnings. No unrelated full test suite was run for this drawing-only change. Physical Xiaomi APK installation and screenshot review performed; saved preference audit confirmed exact eight favourites/order and no changed launcher preference keys. Stock Google Home remains default. Pairing/keyboard/phone code unchanged; no new pairing test is claimed.

[Standard emulator](../releases/review/phase-j3a/branding-standard.png), [Extra Large emulator](../releases/review/phase-j3a/branding-xl.png), [Xiaomi Home](../releases/review/phase-j3a/branding-xiaomi.png).

[Final TV APK](../releases/phase-j3a/RC-TV-Launcher-1.0.0-rc1.9-debug.apk). Same development signing key. J2 rollback preserved at `releases/rollback/RC-TV-Launcher-afbc7c3-debug.apk`; intermediate branding builds also preserved locally. Current implementation commits include `b6e64d1` and `dac67d5`, followed by the requested spacing adjustment.

## Deferred, unstarted: Screensaver & Power

Usage was limited, so no category, selectable placeholder, timer or power behaviour was added. Future work must first verify documented Android 11 capabilities and actual Xiaomi settings destinations/user permissions. Do not introduce a screensaver that interrupts other applications' playback.

Agreed future screensaver intervals: 10 minutes, 20 minutes, 30 minutes, 1 hour, 2 hours, Never. Agreed sleep intervals: Use Android setting, 1 hour, 2 hours, 4 hours, 6 hours, 8 hours, Never. Only expose choices once their behaviour can be supported and tested safely. No claim of device power support is made here.

Final centring adjustment: build/lint and Xiaomi screenshot review; prior Standard/Extra Large checks cover the unchanged drawing. No full suite rerun for alignment-only change.
