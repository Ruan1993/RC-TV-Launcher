# Efficient wallpaper personalisation sprint

From clean b88d107 / 0.2.9 on feature/phase-b-cinematic-glass. Exact debug/profile rollback APKs preserved. Initial allowance: 37% five-hour remaining. Approved Home/gallery styling retained; no stock Home or data reset.

## Implemented
- JPEG/PNG/WebP import via ACTION_OPEN_DOCUMENT. Provider access used only for the copy; original/USB image is never deleted or retained as an access dependency.
- Source cap 32MiB; valid MIME/header/dimension checks; ImageDecoder orientation-aware normalization bounded to 1920×1080; small WebP thumbnails; atomic private publication. Maximum 24 imports; pending imports cleaned safely. Imported IDs strictly validated.
- Existing gallery includes imported scenes; removal confirms deletion of private copy and falls back to mountain if current. Missing private image renders mountain.
- Persistent rotation switch, participants and 5/15/30 minute, 1/24 hour intervals. Rotates only at Home while RESUMED, excluding preview/rearrangement and Minimalist theme. Overdue return advances once; no background service or exact inactive timing promise.
- Instant, Crossfade and Gentle Slide. Local 180ms Balanced / 320ms Cinematic transitions; Minimal/reduced motion instant. Old/new backdrops can coexist briefly during transition; no blur.

## Targeted evidence / limitation
Five wallpaper unit tests cover stable defaults/IDs, pool exclusion/missing entries, overdue deadlines/clock rollback, intervals and unsafe IDs. Separate test APK exercises platform decoding, bounded JPEG/PNG/WebP normalization, thumbnails and corrupt input rejection. Test code is not in the launcher APK.

Xiaomi Android 11 only resolves document intents to com.google.android.tv.frameworkpackagestubs/.Stubs$DocumentsStub, which cancels immediately. Launcher explains the missing compatible picker. No third-party picker installed and no USB-provider success claimed. Private import pipeline verified with device-generated test images; original image-selection UI needs a real picker/provider.

Imported selection/restart, Cancel and confirmed removal checked on device; disposable test imports removed. Prior launcher/navigation evidence reused where unchanged. No new full profiling or screenshot sweep; old Minimal regression remains open for dedicated optimisation.

## Remaining release priorities
Working picker/USB-provider acceptance on suitable firmware; long-session rotation/transition/memory and second-device tests; focus/error recovery; dedicated performance checkpoint; production signing/installation. RC Assist and optional profiles are not 1.0 blockers. Wider master-spec capabilities remain roadmapped.

## Verified checkpoint
0.3.0-wallpaper-personalisation, code 12. Targeted wallpaper unit tests pass; platform test APK passed all three image formats, bounded output/thumbnail checks and corrupt input rejection. Imported selection survived restart; removal Cancel/confirmation passed and all test imports were removed. Rotation enable, every interval, membership, Crossfade/Gentle Slide/manual next and restart persistence passed. A test-APK-only overdue timestamp fixture exercised the real Home scheduler without changing the TV clock; it advanced once. All original preference values and eight favourites/order match the pre-sprint snapshot. Rotation left off, interval 15 minutes, bundled pool and Instant selected. Test fixtures/test code are not included in the launcher APK.
Working debug APK: releases/RC-TV-Launcher-0.3.0-debug.apk; SHA-256 48614F693C8517D3A2F421576739EE18D66BD77A8A7F328082AD2BC10EDF3513. Exact 0.2.9 rollback debug/profile preserved.

Final meaningful-checkpoint pass: all 17 debug unit tests pass; lint zero errors/eight advisories (seven existing plus a stylistic Bitmap.scale KTX suggestion). No repeated profiling/navigation sweeps. Last allowance check 11% remaining, so optional fonts/glass controls were not started. Temporary source fixtures removed; private test imports removed through confirmation UI.
