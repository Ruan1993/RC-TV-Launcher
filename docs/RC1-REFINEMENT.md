# RC1 visual and Settings refinement

TV **1.0.0-rc1.1 (30)**, continuing clean `123163b` on `feature/phase-b-cinematic-glass`. Release-candidate status remains; no Phase J feature work, public release, merge, push or PR #2 change occurred. Phone 0.5.1-rc1 and its UI/client are unchanged and need no update.

## Changes

### Wallpaper visibility

The renderer decodes a bounded wallpaper on IO, then precomposites horizontal and vertical scrims into one cached image. There was no second full-screen Compose scrim; the two gradients compounded strongly, and the previous Vivid factor of 0.78 left the result close to Balanced. Theme glass tint affects controls/panels, not the underlying backdrop. Bundled wallpapers also intentionally contain dark scenic areas; no wallpaper asset was replaced or duplicated.

Balanced retains the original scrim colour and four opacity values exactly. Vivid now uses 0.18 of those opacities, with local contrast behind Home header/shelf labels/footer and text pages. Favourite-card opacity, geometry, styling and animations were not touched. Subtle uses a deeper navy scrim tint with the existing darker alpha factor, making all three modes distinct. Cache identity already includes visibility; changes persist with the existing key and preview immediately. Reset behaviour remains scoped.

**Actual rendered pixel comparison**, same mountain asset, same emulator/Android decoder, baseline RC1 APK versus refinement (not a calculation using opacity constants):

| Mode | Mean RGB luma (0–255) | Mean linear relative luminance |
| --- | ---: | ---: |
| RC1 Balanced | 29.38 | 0.0181 |
| RC1 Vivid | 31.68 | 0.0218 |
| Refined Subtle | 20.40 | 0.0122 |
| Refined Balanced | 29.38 | 0.0181 |
| Refined Vivid | 38.91 | 0.0383 |

Every decoded Balanced pixel is identical to RC1. Refined Vivid has about 32% greater average RGB luma and 2.1 times the linear relative luminance of Balanced on this asset; old Vivid differed by only about 8% RGB luma. These are image-pixel measurements, not calibrated physical TV nits or a guarantee for every image. The user compared on the physical Xiaomi and explicitly confirmed: **Vivid is clearly brighter and readable**.

Comparisons: [Balanced Home](../releases/review/rc1-refinement/home-balanced.png), [Vivid Home](../releases/review/rc1-refinement/home-vivid.png), [Balanced backdrop](../releases/review/rc1-refinement/refined-balanced.png), [Vivid backdrop](../releases/review/rc1-refinement/refined-vivid.png). Home comparison uses the same mountain wallpaper on emulator; clock time differs. Raw pixel measurements exclude UI/clock.

### Settings

The old long combined page is replaced with a readable category list and one focused subpage per category:

- **Appearance & Personalisation:** wallpaper picker/import/rotation/transition, accent themes, visibility, fonts, sizes, appearance reset, layout and motion/reduced motion.
- **Connections & Devices:** paired-phone management and existing Speed Test. No pretend Wi-Fi/Bluetooth controls.
- **Apps & Media:** installed-app browsing and public Android app settings. No browser/file manager/VLC integration added.
- **Weather & Widgets:** weather On/Off, location/change/remove, details and compact privacy/source information. Existing media restrictions remain.
- **System & Updates:** version, Easy Setup, Android settings, phone-control permission guidance, keyboard preview/enable/chooser, current Home and recovery/chooser controls. No updater is invented.

Screensaver & Power is omitted until it has real controls. The category icons follow the existing navbar stroke style. Returning from a subpage restores focus to its category; Back through wallpaper preview/picker returns through Appearance, then categories, then Home. Speed Test or Apps opened from Settings returns to the originating subpage. Selecting Settings in the navbar opens the category list. No preference migration/reset is performed. All prior controls remain reachable; new future-feature placeholders were not added.

Screenshots: [categories at Extra Large](../releases/review/rc1-refinement/settings-categories-xl.png), [Weather settings](../releases/review/rc1-refinement/settings-weather-xl.png), [System/recovery](../releases/review/rc1-refinement/settings-system-xl.png).

### Weather

The existing header card now has a larger temperature, native condition icon, separate readable location/condition and an update/offline line. Select it for a separate details dialog. It uses the existing header space: fixed-size favourite cards remain fully visible at Extra Large/Bold, and their component is byte-identical to RC1.

The same Open-Meteo current-weather request includes apparent temperature, relative humidity and wind speed in km/h. These fields are optional, unit/range checked and cached; missing/invalid/old-cache values are omitted rather than invented. Opening details makes no network request. Provider, location selection, one-hour refresh throttle, offline fallback, 24-hour stale cutoff, no GPS and no continuous/background polling remain. Extra measurements on the TV can appear after its next normal scheduled refresh; no forced extra refresh was introduced.

Weather settings now show a clear On/Off control, location and Change location/Details actions. Location search opens only when requested, keeping an unconfigured page clean. Off retains the saved city/cache for easy re-enabling; the separate explicit Remove saved location action retains the former delete-location behaviour. Existing records without an enabled flag load as enabled. Larger privacy/licence explanations are behind About weather & privacy, with attribution/source access preserved. Free provider usage is still personal/noncommercial; no key, paid infrastructure or permission was added.

[Weather Home at Extra Large](../releases/review/rc1-refinement/weather-home-xl.png), [weather details](../releases/review/rc1-refinement/weather-details-xl.png). Both show real provider data for the named emulator test city Pretoria, not the user's location or fabricated values. The fixture restores all emulator weather/font preferences after review. Source for the additional fields/units: [Open-Meteo API documentation](https://open-meteo.com/en/docs).

## Verification and preserved data

- **37 app unit tests pass**, zero failures/errors. Lint passes with zero errors and 26 existing advisories. Targeted appearance/weather tests ran during development; the full small suite ran once at the final checkpoint.
- Emulator interaction checks pass at Standard and Extra Large/Bold: all five categories, every moved settings control, wallpaper Back, Speed Test/Apps Back and category focus restoration. Early fixture failures were caused by stale Compose accessibility nodes and off-screen queries; the fixture now reads actual nodes after clearing its API-appropriate cache and scrolls with D-pad events. No UiAutomation attached to Xiaomi; emulator automation preserves accessibility services.
- Saved appearance modes were tested across activity restart. Same-wallpaper Balanced/Vivid Home screenshots and actual backdrop pixel comparisons pass. Extra Large/Bold weather/card bounds pass at the original 216 x 190 dp button dimensions.
- Isolated Android weather tests pass for legacy enabled migration, On/Off cache/location retention, hourly throttling, reload, offline cache, stale cutoff, removal, missing extras and invalid units/ranges. Live provider retrieval and opening details passed on emulator. Physical TV brightness/readability is user-confirmed; new detailed weather interactions on Xiaomi still merit normal acceptance review.
- Private before/after snapshots confirm exactly eight favourites in the same order. Every original launcher preference is unchanged except wallpaper/rotation timestamp from already-enabled rotation. The selected Vivid mode, themes/fonts/sizes, keyboard, current weather location/enabled state and private wallpaper files are retained. Weather settings XML is byte-identical across this review; a newer detailed cache will be saved only at a normal refresh. Encrypted grants remain the same length (181 bytes) with no pairing added or revoked; re-encryption can change ciphertext. Phone/companion/security implementation files are unchanged.
- Favourite component SHA-256 remains `57340a0268e445499609688e9150adb031125c8c596bb90bc6da0fd3a68c1508`. No redesign, resizing, relocation or spacing change to the favourite shelf/component was made.
- Stock Home still resolves to `com.google.android.apps.tv.launcherx/.home.HomeActivity`; RC TV Keyboard and the user-enabled RC TV Remote accessibility service remain selected. No permission grants, warning suppression, Home switch, root or hidden API use.
- Small matched emulator debug cold-start sample: baseline 3083/2687/2651 ms, refinement 2782/2574/2576 ms. No measured slowdown in this sample; it is not a thermal/long-session or physical-TV benchmark. Final Xiaomi post-update warm activity display was 2858 ms, similar to the RC1 development-build observation. No startup network wait is added; weather scheduling is unchanged.

## Delivery and limits

Installed TV APK: `releases/rc1-refinement/RC-TV-Launcher-1.0.0-rc1.1-debug.apk`.

SHA-256: `5c9cf0d7ef694927c845b549070ab39cfc05a0f8eaa002f4c51b85e2fcf53963`.

It verifies with the same development signing certificate as RC1, so it updates in place. Exact previous working APK: `releases/rollback/RC-TV-Launcher-123163b-debug.apk`; other rollbacks and private snapshots remain. No uninstall/data clear was performed. APKs/screenshots/private diagnostics are local, ignored by Git and unpublished.

The phone APK remains the Phase I 0.5.1-rc1 build. Casting playback, broad physical-device/font/contrast testing, long sessions, production signing and public download activation retain their existing RC limitations. No new media, browser, file, networking, screensaver or power features were developed. Keep PR #2 unmerged and the product at release-candidate status pending acceptance of the full refinement; only Vivid brightness/readability has explicit physical approval so far.

Last usage check: **53% short-window, 31% weekly remaining**. No reset credit was used.
