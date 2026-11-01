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
