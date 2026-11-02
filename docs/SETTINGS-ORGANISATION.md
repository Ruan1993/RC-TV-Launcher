# Settings organisation — rc1.26 sprint update

The current implementation uses the seven-category persistent rail/two-panel shell in `SettingsHub.kt`, with a full-width category/page fallback for narrow viewports or large type. **Phone & Keyboard** replaces the display label Remote & Input; its stable route stays `remote`. Category browsing previews content without writes; OK/Right enters it and Back returns to the rail. Page scroll is retained through saveable page state. Existing utility return routes and preference keys remain intact.

The approved [S01–S39 preservation map](../RC-TV-SETUP-COMPANION-RECOVERY-REPORT.md) is the audit baseline. Added direct entries include Manage favourites, Home-button controls and System & Recovery → Restore Original Launcher. Existing callbacks/screens remain, with shared setup/recovery controls, 64 dp Settings targets, two-line labels, Cancel-focused scoped reset confirmation and no extra runtime UI dependency. See [current results and remaining acceptance](IMPLEMENTATION-SPRINT.md). The earlier results below are historical and do not certify the new shell's physical D-pad behaviour.

## Historical rc1.21 organisation

Baseline: physically verified rc1.20 (52), `577f2e1`. Organisation/navigation only;
the installed Xiaomi build and takeover configuration are untouched.

## Audit and mapping

| Previous location | New location |
| --- | --- |
| Appearance & Personalisation | Appearance |
| Apps & Media | Home & Apps |
| System -> Launcher Management | Home & Apps, directly on the category page |
| Weather & Widgets; Screensaver & Power | Weather & Screensaver, except power controls |
| Remote & Keyboard | Remote & Input |
| Connections & Devices | Connections & Devices |
| Connections -> Internet Speed Test | System & Recovery -> Performance & diagnostics |
| System -> display/sound | Connections & Devices |
| System -> accessibility | Remote & Input |
| System -> device information; version label | About RC TV |
| Duplicate System storage link | Home & Apps, alongside existing storage summary |
| Screensaver -> power controls | System & Recovery |
| Launcher Management -> full appearance reset | System & Recovery |
| System -> easy setup, backup, Android Settings, date/time, updates | System & Recovery |

Seven categories keep the proposed Performance & Diagnostics content merged into System
because it currently provides one speed-test utility. Cast is reachable from Connections
using the existing Cast screen; navbar shortcuts remain unchanged. About contains the
existing version and Android device-information entry.

## Retained controls

- Appearance: wallpaper selection/import/removal/preview/rotation/pool/transitions; accent,
  font, text size, wallpaper visibility and its individual reset; partial appearance reset;
  layout; focus animation; interface motion; Reduced Motion.
- Home & Apps: actual Home status, default-Home selection, conditional original-Home recovery
  guidance, installed apps/favourite context actions, media browser, Android app management,
  storage summary and Storage & USB settings.
- Weather & Screensaver: weather on/off/location/search/details/privacy; original provider,
  idle interval, style, preview and Ambient Mode settings controls.
- Remote & Input: pairing/download, typing, phone-control permissions; keyboard preview,
  enabling/selection; Android Accessibility settings.
- Connections & Devices: Wi-Fi/status, Bluetooth, existing Cast guidance, display/sound.
- System & Recovery: easy setup, backup/restore, general Android Settings, date/time,
  updates, speed test, full appearance-reset confirmation, Android power settings/restart help.
- About: RC TV version and Android device information.

The former `Reset to Original` button is labelled `Reset colours, fonts & focus`, with its
existing callback and a precise description. The separate full reset remains unchanged.
The `Animation Intensity` heading now correctly describes its options as `Focus animation`.

## Navigation and compatibility

- Back from a category focuses that category on the main list.
- Backup Back focuses its System entry. Wallpaper Back focuses Choose wallpaper.
- Installed apps, speed test and Cast return to their respective originating entries.
- Weather-details Back from Settings focuses its details entry; Home weather behaviour stays intact.
- Existing remembered UI routes `launcher` and `power` map to Home & Apps and Weather & Screensaver.
  These are UI routes, not preference-key or backup migrations.
- Every existing essential Android Settings destination belongs to exactly one category.
- The Home interception, startup code, manifest, DreamService, preferences, weather repository,
  remote/keyboard logic, pairing, backup format and provider integrations were not edited.

## Validation

- Debug application build and 67 JVM tests pass (four focused Settings navigation tests added).
- Updated Settings, backup and screensaver UI fixtures compile in the Android test APK.
- Static inventory comparison retained every original literal action label except the removed
  Launcher Management submenu entry and renamed partial-reset label; their controls/callbacks remain.
- No device/emulator installation or UI test execution was performed. D-pad traversal, scrolling,
  Standard/Extra Large text and focus return still require an approved physical review.
- Saved candidate: `releases/phase-settings/RC-TV-Launcher-1.0.0-rc1.21-debug.apk`.
