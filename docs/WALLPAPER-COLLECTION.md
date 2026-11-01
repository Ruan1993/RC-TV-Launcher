# Wallpaper collection and page history — rc1.25 (57)

Baseline: working rc1.24 (56), `cd22535`. This replaces the earlier one-at-a-time online-image cleanup described in ONLINE-WALLPAPERS.md. Provider requests, supported licences and attribution checks remain unchanged.

## User flow

- Online preview now offers **Add to Gallery** alongside Apply. Save writes the existing optimised offline copy and its full credit without editing Home or rotation preferences. Apply saves/reuses the same copy, then applies it. Neither operation deletes older gallery images.
- **My Wallpaper Gallery** is available directly in Wallpaper Settings and from Online Wallpapers. It lists only saved online images; bundled wallpapers and custom imports remain in their existing places.
- Highlight an image with the D-pad; OK toggles its checkmark. Fullscreen preview, Apply to Home and Remove act on the highlighted image shown above the grid. Preview reuses the existing fullscreen/credit interface and reads the local image without a provider download. Selection and highlighted-image focus survive preview/Back.
- **Add Selected to Rotation** unions the selected IDs into the existing rotation pool in one DataStore edit. Existing choices, interval and rotation On/Off remain unchanged. Clear selection affects only the temporary checkmarks.
- Removal requires confirmation. An image currently used by Home or included in rotation cannot be deleted: apply a different Home image and remove that image from the existing rotation controls first. The same guard also protects removal from the original wallpaper interface and checks current persisted preferences. Custom-import removal retains its existing behaviour.
- Existing online images automatically appear as collection images; their IDs, files, credits and preferences require no migration. Images remain offline across ordinary app restarts/reboots; clearing app data or uninstalling still removes private files. Settings-only backup compatibility is unchanged and still excludes image files.

## Page navigation

Numbered buttons directly revisit known pages. Next obtains only the continuation cursor returned by the current page; it never guesses offsets or fetches intervening pages to simulate random access. Returning to a loaded page reuses its metadata. Reload explicitly replaces that page's metadata.

The launcher keeps the last **10 pages** of metadata, continuation cursors and focused-image IDs in RAM. Absolute page numbers are preserved when older pages leave the window. Duplicate continuation cursors cannot create repeated endless pages; duplicate source IDs within retained pages are filtered. The current page/focused wallpaper survives preview, My Gallery, closing/reopening Online Wallpapers and leaving/re-entering Wallpaper Settings during the same launcher session. Lazy-grid state and focus restoration preserve the relevant image when returning. No bitmaps or browsing history are persisted by this history object; process/activity recreation resets browsing history to page 1. This session boundary is intentional.

## Storage

- **16 MiB** total saved online library, including thumbnails and credit metadata; no automatic collection eviction.
- **4 MiB** maximum per full WebP image; saved thumbnail at most 256 KiB, metadata at most 16 KiB.
- Existing **10 MiB** disposable thumbnail cache and **384 MiB** free-space reserve remain unchanged. Save also retains its 6 MiB staging allowance. Failed staging is removed; published collection images are never silently deleted to make room.
- Existing **24 combined imported/saved-image** limit remains in place, alongside the byte limit. Full-library errors ask the user to remove an unused image explicitly.
- Existing source/revision IDs deduplicate saving and applying. Preview checks for a local copy before requesting a full image; already-saved online thumbnails also reuse local thumbnails. Browsed full previews remain memory-only.

## Verification and release

Passed app build, full JVM unit suite and existing Android test-APK compilation using `:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --offline`.

**87 tests passed**, zero failures/errors/skips. Eight new tests cover cursor reuse, remembered page/focus, bounded absolute page numbering, prevention of unsupported random navigation and cursor loops, distinct cached images, retry cursors, additive/idempotent bulk rotation, and protection of images used by Home/rotation. Existing real-API licence fixtures and storage/cache tests also pass. No provider implementation changes were needed.

APK: `releases/phase-online-wallpapers/RC-TV-Launcher-1.0.0-rc1.25-debug.apk`.
Verification record: `releases/phase-online-wallpapers/verification-rc1.25.json`.
Rollback build rc1.24 remains in the same release directory. No installation, ADB commands, reboot or Xiaomi configuration changes were performed. Home handling, Accessibility, DreamService, favourites, phone pairing and unrelated UI are unchanged.

Physical checks after a separately approved installation: collect several images without changing Home; reopen My Gallery offline; preview/Back; select several and bulk-add to rotation; apply one; verify protected deletion and explicit unused-image removal; revisit numbered pages and confirm page/image focus after previews, My Gallery and closing/reopening the browser. Physical D-pad/rendering/offline-persistence checks are pending, not implied by JVM results.
