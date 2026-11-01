# RC TV backup data audit — J3F checkpoint A

## Existing data and scope

| Data | Current storage | Backup decision |
| --- | --- | --- |
| Favourite package IDs and saved order | `files/datastore/launcher.preferences_pb` | Include; keep unavailable package IDs for portability, do not install apps |
| Theme, colours, font, text size, wallpaper selection/rotation/pool/transition, motion/focus, Reduced Motion | Same DataStore | Include validated settings; exclude rotation runtime timestamp |
| Screensaver provider, style and interval | Same DataStore | Include RC TV's choice only; do not write Android's screensaver settings |
| Imported wallpaper image/thumbnail/title files | `files/wallpapers/local_*/` | Images excluded from settings backup; retain IDs only where assets already exist; preview missing assets and fall back safely |
| Weather location and enabled state | `shared_prefs/weather.xml`, JSON record | Include location/coordinates and enabled state; exclude conditions cache, requests and runtime timestamps. Location is personal information; explain before export |
| Phone listener opt-in/port and system-control opt-in | `shared_prefs/companion-connection.xml` | Exclude device-specific listener/permission state; never enable remote access during restore |
| Phone approvals/credential hashes | Encrypted `SecureRecord("companion-grants")`; Android Keystore | Exclude entirely, including device names/IDs and hashes; existing pairing stays untouched, new devices require secure pairing |
| TLS private identity | Android Keystore alias `rc-companion-local-tls-ec-v2` | Never export or replace |
| Keyboard/accessibility enabled state and selected input method | Android secure settings | Exclude; restore cannot grant Android permissions or change keyboard selection |
| Keyboard text, transient navigation/search/remote messages | Memory/editor state | Exclude |
| Easy-setup completed flag | `shared_prefs/easy-setup.xml` | Exclude device onboarding state |
| Android Home role, package states, Wi-Fi, accounts, other apps | Android/system stores | Exclude; no system restoration |

Weather configuration will move into the launcher DataStore, migrating the legacy configuration once without losing the cached report. This lets a supported restore replace all portable settings, including weather, in one durable DataStore transaction. Weather cache remains separate and nonauthoritative for configuration. Pairing stores are never opened by the backup implementation.

This is a launcher-settings backup, not a full app-data/Keystore backup, APK backup, Android image or guarantee of recovery from an unbootable device. Detailed format, validation and restore semantics are recorded with implementation below.

## Schema 1 — rc1.16

UTF-8 JSON, MIME `application/json`, suggested filename `RC-TV-settings-<date-time>.json`, maximum 256 KiB. Envelope fields are exactly:

- `format`: `rc-tv-settings`.
- `schemaVersion`: integer `1`; unknown versions are rejected, not guessed or partially imported.
- `createdAt`: epoch milliseconds; `appVersion`: originating RC TV version.
- `payload`: a JSON **string**, retained byte-for-byte as UTF-8 for its integrity digest.
- `sha256`: lowercase SHA-256 of the UTF-8 payload string. This detects corruption, **not authenticity or encryption**. Only restore a file you trust.

Payload is a closed typed allowlist: `favourites` (unique package-ID array, maximum 256), `favourite_order` (newline-delimited permutation of that set), `reduced_motion`, `theme`, `animation_intensity`, `wallpaper`, `wallpaper_rotation`, `wallpaper_interval`, `wallpaper_pool`, `wallpaper_transition`, `accent_theme`, `font_style`, `text_size`, `wallpaper_visibility`, `focus_animation`, `screensaver_provider`, `screensaver_minutes`, `screensaver_style`, `weather_configuration`.

Weather configuration is a JSON string with exactly `enabled` and `place`; `place` is null or `{label, latitude, longitude}`. Latitude/longitude are finite and range-checked; labels are bounded and cannot contain control characters. Export UI discloses that location is personal information. There are no secrets, pairing IDs/hashes, private keys, device IP addresses or credentials in this format.

Enums, booleans, integer ranges, list uniqueness, package IDs, cross-field favourite order, supported keys and version, UTF-8, envelope integrity, parser nesting and trailing content are validated before a preview. A strict streaming JSON reader rejects comments, single-quoted fields and duplicate keys, bounds nesting to 12 and list length to 256 before constructing objects. Oversize streams stop at the byte bound. An invalid file cannot reach the DataStore edit. Preview retains the validated in-memory snapshot; changing the source file afterward does not change what is confirmed.

Missing installed apps retain their IDs and saved positions; restore does not install apps. Imported images are not copied: existing local IDs are reused only if their files exist on the destination; missing selected wallpaper falls back to Mountain and missing rotation images are removed, with preview warnings. The asset check is repeated before applying. No original image or app file is deleted.

## Atomicity and weather migration

Weather location/enabled state is migrated once from `shared_prefs/weather.xml` into `weather_configuration` in the same launcher DataStore. The older weather record remains as a report cache; the DataStore is authoritative for production configuration. The UI repository observes committed changes and invalidates old refresh generations. Hourly refresh and offline-cache policies remain intact.

All supported fields are restored in **one DataStore edit**, with validation outside and before that edit. Existing unrelated keys and all other stores remain untouched. DataStore commits durably as one transaction; a failed edit does not leave half the supported configuration applied. The local wallpaper rotation timestamp is restarted, not imported. After commit, weather's nonauthoritative cache/UI converges to the new configuration. A process interruption cannot split weather configuration from favourites/appearance.

## Storage and firmware support

Primary create/open actions use Storage Access Framework contracts, without new broad storage permissions, a fixed USB path or a persistent directory grant. Export opens the chosen URI, closes/flushed output, reads the actual file back and validates matching bytes before reporting success. Provider write failure can leave an incomplete destination file; app settings remain unchanged and the UI says so. Picker cancellation changes nothing.

Xiaomi build 774 resolves both actions to `com.google.android.tv.frameworkpackagestubs/.Stubs$DocumentsStub`; the observed stub closes without returning a document. RC TV detects this known stub and explains that USB/HDD picker backup is unavailable on that firmware. No system picker is installed, replaced or enabled by RC TV.

An additional **TV Downloads** route uses `MediaStore.Downloads` for files this app creates, requiring no broad permission. Files are pending until write/readback verification succeeds, then published under Android's Downloads collection in `RC TV/`. Failed creations remove the newly inserted row where possible. A list of this app's published backups provides the identical preview/confirmation/restore path. This is local storage, not USB/HDD, a cross-device import replacement, or independent recovery: copy it off the TV with a trusted file app or PC. Reinstall/reset ownership and data-loss caveats apply; arbitrary files require a working SAF picker.

References: [Android SAF](https://developer.android.com/training/data-storage/shared/documents-files), [app-owned MediaStore files](https://developer.android.com/training/data-storage/shared/media#access-own-media-files).
