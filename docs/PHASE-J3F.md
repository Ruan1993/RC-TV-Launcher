# J3F — RC TV Backup & Restore

Target: physical Xiaomi MiTV-AFKR0 / jaws, Android 11 build 774; continued from `2ea7868`. New TV candidate **1.0.0-rc1.16 (45)**. Phone companion and Xiaomi Home routing remain untouched.

## Outcome and acceptance boundary

Backup & Restore is implemented under **Settings → System & Recovery → Backup & Restore**. Versioned settings validation, preview, explicit confirmation and atomic restore are implemented. The physical Xiaomi passes real **TV Downloads** export/readback and confirmed restore using D-pad navigation. **External USB/HDD acceptance remains blocked**, not passed: the firmware supplies only a DocumentsStub for both required SAF picker actions, and the attached HDD was not exposed as a mounted Android public volume during the test. The user's prior HDD media/USB host operation remains physically verified; this result does not invalidate it or prove USB device-mode capability.

No system picker was installed or replaced. No Home role, preferred activity, system component/package, ADB transport setting, system partition or Android permission setting was changed. No reset or application-data clearing was used. A successful settings backup does not authorise a launcher takeover; the J3E routing/recovery blockers remain.

## Checkpoints

### A — audit

Committed separately as `e0c5e04`. [Backup format/data audit](BACKUP-FORMAT.md) maps all current stores, safe portable fields and exclusions. Weather location/enabled configuration now migrates into the existing launcher DataStore once, allowing one atomic transaction for all supported settings; conditions cache remains separate. No changes to credential storage.

### B — implementation

Initial implementation committed as `9184af3`; final refinement adds readable favourite labels and D-pad scrolling of long previews.

- Standard SAF `CreateDocument` / `OpenDocument` on supporting devices, with MIME `application/json`, no new broad storage permission or USB path assumption.
- Closed typed schema 1, size/UTF-8/nesting/trailing-content checks, strict version/enums/ranges/order validation, SHA-256 corruption detection.
- Preview includes saved date/version, favourites in order, appearance, wallpaper/rotation, motion, screensaver, weather and missing-resource warnings. Cancel receives initial focus. Back cancels before confirmation.
- Single durable DataStore edit restores supported settings together; unrelated stores remain untouched. Failed validation/I/O cannot partially apply a restore.
- Export is reported successful only after actual destination readback and integrity validation.
- Known firmware stub is detected and explained. An explicit scoped **Save a copy to TV Downloads** / **Restore a TV Downloads copy** fallback provides the same validation/confirmation flow without broad permission. The UI states that this is local storage, not USB/HDD protection.

### C — recovery package

Prepared at `releases/recovery/j3f/` (local, ignored by Git):

- `apks/`: original verified-working rc1.15, relevant rc1.14 rollback and final rc1.16 candidate; originals remain in their release folders.
- `RECOVERY.md`, `original-home-state.json`, Home diagnostics/readiness/acceptance and J3E records, backup-format and J3F notes.
- `SHA256-MANIFEST.json` for bundle files.
- `private-settings/RC-TV-verified-physical-backup.json`: a verified copy of the real TV Downloads file, copied through its app-authorised provider readback. Treat the bundle as private because this file includes weather location.

Tracked source documentation is in [the recovery checklist](recovery/XIAOMI-J3F-RECOVERY.md) and [original state](recovery/xiaomi-original-home-state.json). Prepared ADB package-restoration guidance is not executed or claimed tested after a takeover. This is not a full Android image, a portable Keystore backup or guaranteed rescue from an unbootable box.

Recovery source documentation committed as `284f2d7`. Final rc1.16 APK: `releases/phase-j3f/RC-TV-Launcher-1.0.0-rc1.16-debug.apk`; SHA-256 `30A086B559D0B6BA6E2DA0FD9A5144FA90313D120868D4B6E44CB177AAACDA9A`. APK signature verification passed with the existing Android debug signer. The bundle manifest records this final APK alongside the preserved earlier versions.

### D — tests

| Check | Result |
| --- | --- |
| Debug build and test APK build | Passed |
| Existing JVM suite | 50 tests passed, zero failures/errors |
| Physical codec and I/O | Valid roundtrip; malformed JSON/UTF-8, oversize/deep/trailing input, unsupported version, missing/unknown fields, wrong types/ranges/order and corrupt digest rejected; provider failure preserved settings |
| Physical controlled restore | Accent, screensaver interval and weather enabled state changed reversibly, then restored via production service; finally block restores baseline even if assertion fails |
| Physical all-supported state | Effective appearance, wallpaper/rotation, motion, screensaver, weather configuration and all eight favourite IDs/order matched original after restore (runtime rotation timestamp intentionally restarted) |
| Pairing/keyboard safety | Logical encrypted grants, companion configuration and Android selected keyboard remained unchanged. No plaintext credentials exported. A new phone-to-TV command session was not independently tested; existing grants were preserved and restore on a new TV requires secure re-pairing |
| Physical UI local file | Real MediaStore Downloads row saved, closed, read back, decoded and checksum-verified before success; same file inspected for preview and confirmed restore |
| Physical preview safety | Cancel initially focused; Back kept the controlled changed setting; subsequent explicit confirmation restored baseline; no silent restore |
| Physical D-pad / Back | Settings entry, action activation, local backup list, preview details scrolling, Cancel/confirm and Back-to-entry focus exercised; screenshots inspected |
| Off-TV copy | Exact provider-read bytes copied through the app-private test capture to PC/recovery folder; 1452-byte recorded copy decoded, digest verified and eight favourites checked without displaying personal values |
| Physical invalid-file preservation | Broken-file inspect and invalid supported values rejected before edit; canonical supported state unchanged |
| SAF on this Xiaomi | **Blocked:** both actions resolve to `com.google.android.tv.frameworkpackagestubs/.Stubs$DocumentsStub`; initial physical create attempt closed without a document. Final UI detects the stub and reports unavailability |
| External HDD file create/read | **Not completed:** no public HDD volume exposed and no functional firmware picker; no USB/debugging settings changed to force it |
| Genuine SAF emulator end-to-end | Attempted on API 36.1 emulator with real DocumentsUI; emulator/host ADB interruptions prevented a completed result. **Not claimed passed.** The test mode remains available as `j3fsaf` |

Instrumentation modes are `j3fcore` and `j3fui` in the separate test APK. UI automation focuses controls and sends D-pad/Back events; these are physical-device automated tests, not new user-operated remote confirmations. They do not affect the phone APK. Test captures/receipts and private backup copies remain under ignored `releases/review/phase-j3f/`; no personal settings or raw private snapshots are committed.

## File locations and portability

Physical local files use **TV Downloads / RC TV / RC-TV-settings-<date-time>.json**. The PC copy is `releases/review/phase-j3f/RC-TV-verified-physical-backup.json`; the independent recovery-folder copy is listed above. Android-assigned media URIs identify local rows; no hardcoded USB path is used.

Includes favourites/order, theme/accent/font/text/visibility, wallpaper selection/rotation/pool/transition, Reduced Motion and animation/focus, screensaver provider/style/interval, weather location and enabled state. Excludes imported image files, app APKs/data, weather cache, runtime timestamps, credentials, pairing grants/hashes/IDs, TLS keys, listener/system-control opt-ins, Android permissions/keyboard activation, onboarding, accounts/Wi-Fi and Home/system configuration. Unavailable apps retain package IDs/order; absent imported images produce a warning and bundled fallback. See [schema documentation](BACKUP-FORMAT.md).

Existing same-device pairing is not replaced. A different TV must securely pair again and receive Android keyboard/accessibility consent. A local backup alone does not protect against device reset/failure. Restoring copied files from other devices requires a working document picker; the local list is deliberately restricted to files this installation owns.

## Remaining limitation

J3F's portable-settings implementation and physical local recovery path are delivered, but **the requested end-to-end external HDD checkpoint is incomplete on this firmware**. It requires a separately established supported storage/picker route. Do not bypass this with broad storage permissions, system package changes or a claim that TV Downloads is external HDD storage. Do not begin a launcher experiment based on this backup alone.
