# Online wallpapers — rc1.23 (55)

Entry: Settings → Appearance → Choose wallpaper → Online Wallpapers.
Baseline: rc1.22 (54), `1da57ad`. No device installation or configuration changes were made during this batch.

## Source and licence

Uses Wikimedia Commons' public, keyless Action API, with a descriptive RC-TV User-Agent, to browse `Category:Featured pictures of landscapes`. No scraping, account, private API key, new dependency or backend is involved.

- [Commons reuse requirements](https://commons.wikimedia.org/wiki/Commons:Reusing_content_outside_Wikimedia)
- [Imageinfo API](https://www.mediawiki.org/wiki/API:Imageinfo)
- [Categorymembers and continuation](https://www.mediawiki.org/wiki/API:Categorymembers)
- [Wikimedia API access policy](https://www.mediawiki.org/wiki/Wikimedia_APIs/Access_policy)

Only landscape JPEG/PNG/WebP entries with creator credit and an explicit recognised Creative Commons URL are offered: CC BY or BY-SA 3.0/4.0, or CC0 1.0. Unknown, noncommercial, no-derivatives and GFDL-only licences are excluded. Commons metadata is supplied by contributors, not a rights warranty. The file page remains the source of provenance.

Preview includes the title, full supplied artist/credit/attribution text, stable Commons file-page URL, licence URL and resize/re-encoding notice. The same attribution is saved beside the applied image and displayed in the existing wallpaper interface offline. Image licences remain separate from RC TV's software licence; BY-SA image modifications retain the corresponding image licence. Oversized credits are rejected rather than silently truncated.

## Behaviour and storage

- Pages request 12 source files; incompatible entries are filtered, so visible page sizes vary. Previous/Next and Reload are explicit remote controls. No polling or next-page prefetch.
- Only visible thumbnails are loaded, with two concurrent thumbnail downloads. API responses are limited to 1 MiB; thumbnail transfers to 384 KiB each. Decoded thumbnails fit 320×180.
- Dedicated private thumbnail disk cache: **10 MiB maximum**, least-recently-used eviction before writes, including temporary write space. Corrupt decoded entries are evicted. Cache operations share a lock across gallery instances. Android may also evict this disposable cache.
- Full previews are requested only after selecting an image. They stay in RAM, never in the disk cache. Transfers are capped at 8 MiB; decoding fits the device's actual app display metrics, bounded at 3840×2160, without upscaling. Originals are not downloaded. Closing the preview drops its bitmap reference.
- Apply reuses the preview bitmap, saving an optimised WebP (quality 86, at most **4 MiB**), a thumbnail (at most 256 KiB) and credit metadata (at most 16 KiB). No second fullscreen download or original copy is retained.
- Image writes preserve at least **384 MiB free**; Apply requires an additional 6 MiB staging allowance before starting. A low-space thumbnail cache write is skipped; low-space Apply leaves the old preference untouched.
- Published online copies are capped at **16 MiB**. A replacement may temporarily stage another image before its quota check; unsuccessful staging is removed. Earlier online copies are removed only after the new image and existing DataStore preference commit succeed, except images explicitly included in the rotation pool. Custom imports and bundled images are never pruned. Existing 24-import limit still applies. If storage is full, remove a saved image before applying another.
- Stable source/revision-derived `local_…` IDs deduplicate reapplication and reuse the existing renderer, rotation, removal and preference paths. Only files marked `online.json` are eligible for automatic pruning. Imported files, all preference keys and rotation selections remain unchanged.
- The selected private copy loads without networking after leaving Settings or restarting. Existing settings-only backups still exclude private image files, as they already do for custom imports; no backup format changes.
- Failed downloads, unsupported images, timeouts and low space are reported. Requests have connection/read timeouts, byte limits, cancellation checks and a download deadline. HTTP 429/503 honours a bounded Retry-After cooldown; no automatic retries. Only official HTTPS asset hosts are accepted, with redirects disabled.
- Saving is staged before the preference edit; failure cleans the new copy and preserves the prior preference. Back cannot interrupt the short save transaction. A process interruption before commit can leave an unreferenced copy; a later successful Apply prunes it. An interrupted staging directory is cleaned on the next Apply.

## Verification

Passed: `:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --offline`.
**78 unit tests, zero failures/errors/skips**, including seven new checks for licensing, URL boundaries, display sizing, storage reserve, actual LRU eviction, oversized/low-space cache writes, interrupted cache parts and source-revision identity. Existing Android test APK compiles; it was not installed or run.

Public endpoint smoke checks from the PC: first page 12 source files; returned continuation tokens retrieve a different second page of 12; thumbnail HTTP 200/JPEG (78,471 bytes); display-bounded preview HTTP 200/JPEG (174,309 bytes), matching source revision. The initial paging smoke check exposed an incorrect continuation constant; implementation now uses both continuation values returned by the API, and the corrected check passed.

Saved APK: `releases/phase-online-wallpapers/RC-TV-Launcher-1.0.0-rc1.23-debug.apk`.
SHA-256: `cf5ee3e37835cdc41cbf6ba275a0d230b082d1c55be6a74c011a1d33e5041cca`.
Local verification record: `releases/phase-online-wallpapers/verification.json`.
Rollback APK remains `releases/phase-continue-watching/RC-TV-Launcher-1.0.0-rc1.22-debug.apk`.

## Physical checks pending approval to install

1. Open Online Wallpapers; check D-pad focus, scrolling, paging, Reload and Back returning to its originating control.
2. Select a landscape; verify preview, readable/scrolled credits, Apply feedback and Back returning to the selected tile. Check another preview can be dismissed without changing Home.
3. Apply two different images; confirm the current one remains in the local gallery and the previous unpinned online copy is removed. Confirm custom imports, bundled scenes and rotation selections remain intact.
4. Include an online image in rotation, then apply another; confirm the pinned image survives and rotates normally.
5. With Wi-Fi unavailable, confirm the applied image and credit remain visible while gallery requests fail gracefully. Confirm persistence across a later separately approved normal restart.
6. Check low-storage failure preserves the current image, and cache eviction remains bounded during ordinary paging. Do not fill the physical TV's storage merely to test this; policy and file-cache boundaries are covered by unit tests.

No takeover, Home handling, Accessibility, DreamService, phone companion, favourites, weather-provider or system-package changes are part of this feature. Physical rendering, remote navigation and device-specific network behaviour are not claimed verified by the PC checks.
