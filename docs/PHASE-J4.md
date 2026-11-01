# J4 — media browser and VLC

Continued from `9e07ab5` / rc1.16. Candidate: **RC TV 1.0.0-rc1.17 (46)**. No Home redesign, phone companion change, launcher takeover, system-package change, root or raw/private storage bypass.

## Implemented

- Entry: **Settings → Apps & Media → Media Browser**. Existing Library placeholder also links to it; approved Home navigation/layout is unchanged.
- Public mounted-volume/MediaStore discovery and permission-scoped indexed-media folder hierarchy. Reads run off the UI thread with provider-backed cursors and bounded batches (500 indexed rows or 200 document children). Scan more exposes additional results; name/newest/type sorting applies to loaded entries, with folders first.
- Connected document trees on devices with a genuine folder picker, persisted user grants, nested folders, metadata, clear loading/empty/permission/disconnected states, storage broadcasts and Refresh.
- D-pad controls, breadcrumbs, Back/Up, source/file focus return after playback and images, page-to-page focus on newly discovered entries. Lost sources return safely to Storage.
- Video/audio go **only to installed VLC**, using canonical media/document content URIs, ClipData and temporary read flags. Missing/unresolvable VLC produces an explicit message; no silent substitute. Opening an intent is not treated as playback proof.
- Bounded fullscreen image decoding (maximum 1920×1080 output; compressed-file and pixel guards), Back/Close focus and cancellation cleanup.
- **Xiaomi Media Player** fallback uses the app's public leanback launcher intent; no private component or provider interface is used. This is delegated browsing, not a claim RC TV natively reads proprietary USB storage.
- Backup connected-folder export/import reuses J3F's exact validation, checksum, preview and explicit confirmation. TV Downloads backup remains available. Device-specific tree links/grants are excluded from portable backups.

## Physical storage findings — bounded investigation

Xiaomi MiTV-AFKR0 / jaws, Android 11 build 774. One read-only check exposed only internal emulated storage through `sm list-volumes` / mount information; no public removable volume was listed. The installed VLC package is `org.videolan.vlc`. OPEN_DOCUMENT_TREE resolves to **no activity**; J3F already verified CREATE/OPEN_DOCUMENT resolve only to DocumentsStub.

The user confirms that the connected HDD's contents are visible through **Media Player**. Its public leanback entry is in `com.xiaomi.mitv.mediaexplorer`. Thus physical host/media support is confirmed, but it does not establish a public mounted MediaStore volume or document-tree grant for RC TV. No undocumented OEM provider contract was invoked and no private mount was scanned. No USB/debugging switch was changed to force access.

**Native real-HDD browsing/playback and HDD JSON backup through RC TV remain blocked by the firmware's public storage/picker exposure.** No repeated USB-controller investigation or attempt to weaken scoped storage is appropriate. The OEM Media Player route is retained for the user's existing HDD access. The absence of a public volume in this session does not prove every drive/filesystem is unsupported.

## Verification status

- Debug APK/test APK builds and **54 JVM tests** pass (50 existing plus media policy/path/sort/permission tests).
- Physical automated checks use **three labelled synthetic files in internal shared Movies/Music/Pictures**, not the HDD: nested media folders, sorting, canonical content URIs/read grants, a playing VLC video session, active audio output plus VLC audio session, fullscreen image rendering, D-pad/Back and returning file focus.
- Hardware VLC video is black in captured images on this device; captured-frame delta was zero. A playing session alone is not claimed as visible playback. **User visual video confirmation pending.**
- Physical automated checks compare eight favourites/order, other effective preferences (allowing normal wallpaper rotation), and logical encrypted pairing grants before/after. They do not change launcher settings. A new phone command session is not independently verified.
- Real HDD video in VLC, an HDD audio/image, unplug/reconnect stability and the OEM fallback's user-operated return path require explicit physical observations and are recorded separately when received. No intent dispatch will be represented as actual HDD playback.
- J3F backup regression is run separately from the user's manual media check to avoid interrupting it. External-folder backup cannot be verified on this firmware; schema/confirmation safeguards remain unchanged.

Test-only generated files use **RC TV J4 Check** folders and are tracked by exact owned media URIs. Only those fixtures can be removed by `j4cleanup`; no user HDD content, original media or app data is removed. Tests and screenshots/receipts live locally under ignored `releases/review/phase-j4/`.

## Storage/security boundaries

Android 11 uses the existing media-read permission. Android 13+ uses video/audio/images permissions; Android 14+ also recognises user-selected visual access. No MANAGE_EXTERNAL_STORAGE, legacy-target workaround, fixed USB path, root, file:// handoff or new background service is added. MediaStore reveals indexed media only, not arbitrary files or empty/unindexed folders. Document-tree access needs a real provider and explicit user grants. Large-provider queries may depend on OEM responsiveness; bounded cursor consumption and IO dispatch keep filesystem reads out of Compose rendering.

References: [Android shared media](https://developer.android.com/training/data-storage/shared/media), [SAF folder grants](https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory), [Android 11 scoped-storage boundaries](https://developer.android.com/about/versions/11/privacy/storage).

## Release boundary

This is the best supported native browser plus OEM fallback under the stated firmware constraints. Do not declare the full HDD acceptance checklist complete while public access and real-HDD VLC playback remain unverified. Further work should address essential stability/results, not repeat inconclusive storage diagnostics or begin launcher takeover. Detailed final artifact hashes, commit and completed physical observations are appended with the release.

Prepared signed candidate: `releases/phase-j4/RC-TV-Launcher-1.0.0-rc1.17-debug.apk`, SHA-256 `DD800060A00B470D6584FBB016FA3D844B941FEE23C285D78BA4B6D0148179CE`. Existing signer verification passed. Local recovery copies include rc1.17, rc1.16 and rc1.15 under `releases/recovery/j4/apks/`. Final install/regression recording waits while the user performs the manual Media Player check; no playback is interrupted for that purpose.
