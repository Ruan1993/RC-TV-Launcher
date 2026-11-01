# Continue Watching — rc1.22 (54)

## Established compatibility

On the physical Xiaomi MiTV-AFKR0 / Android 11 / firmware 774, a separate ordinary test app
received `android.permission.READ_TV_LISTINGS` through Android's user-consent flow and read
other packages' searchable TV-provider data. No privileged EPG permission or package/role
changes were used. The initial Kotlin standalone test packaging error was corrected with
a platform-Java test Activity; its successful query ran with the test app's UID.

Read-only result: 31 Watch Next rows (30 TCL Browser, 1 VLC), one marked CONTINUE, with
titles/links/artwork references but no usable position/duration pairs. The per-package
summary does not establish which package owns that single CONTINUE row. Preview programs
also exist, but they are recommendations, not viewing history. No YouCine Watch Next entry
was detected in the visible results. Metadata presence is not proof that a deep link or
artwork remains accessible; the product filters unusable launch targets separately.

## Product behaviour

- Reads `content://android.media.tv/watch_next_program` only; never writes TV-provider data.
- Normal runtime `READ_TV_LISTINGS` consent is required **for RC TV itself**. The test app's
  grant does not transfer. Request it explicitly in Settings -> Home & Apps; there is no
  automatic boot-time permission prompt or shell grant.
- Display preference `continue_watching` defaults to true and is stored in the existing
  launcher DataStore. No existing keys are renamed or migrated.
- Only browsable CONTINUE records with a real title and usable originating-app VIEW/MAIN
  intent are eligible. Other Watch Next types and all preview recommendations are excluded.
- Launch is restricted to the publishing package, an exported activity and permissions
  already held by RC TV. Selector, ClipData and provider-supplied task/grant flags are removed.
- Removed/disabled/suspended apps and unusable links are skipped. Failure at click time
  shows an error and leaves normal navigation available.
- Progress is shown only for consistent supplied position/duration values. Missing progress
  is omitted, never estimated. The compatibility query found no usable progress pairs.
- Artwork accepts only readable content URIs or HTTPS without embedded credentials;
  bounded reads, decoding, concurrency and a 4 MiB bitmap cache limit resource use. Missing,
  blocked or unsupported artwork falls back to the app icon.
- Work starts after the existing first-Home-draw gate. Refresh runs on resumed Home and
  provider notifications; observing stops when Home is paused/left or the option is off.
  No polling service, new dependency, accessibility scraping or watch-history inference.
- The row is hidden if permission/data is unavailable. When present, it follows the existing
  favourites at their original size; the Home content can scroll vertically when needed.
  D-pad Down enters it, Up returns to favourites, and existing Home Back behaviour remains.

## Compatibility and validation

The established schema-1 settings-backup format is unchanged. Its allowlist does not include
the new display toggle; restore preserves the current value. Watch history and TV-listings
permission are not exported. Existing favourites, order, weather, pairing, Home services,
DreamService and Android takeover settings are untouched.

Debug build and unit tests passed; Android test APK compiles. Focused policy tests cover
hidden/non-continue records, missing or invalid progress, malformed metadata and safe artwork
schemes. Product UI, originating-app launches and artwork still need physical verification
after a separately approved installation and RC TV permission grant. No product APK was
installed and no reboot performed while implementing this feature.

Sources:
- [Android Watch Next](https://developer.android.com/training/tv/discovery/watch-next-add-programs)
- [Android 11 provider access rules](https://android.googlesource.com/platform/packages/providers/TvProvider/+/refs/heads/android11-release/src/com/android/providers/tv/TvProvider.java)
- [Provider permission declarations](https://android.googlesource.com/platform/packages/providers/TvProvider/+/refs/heads/android11-release/AndroidManifest.xml)
