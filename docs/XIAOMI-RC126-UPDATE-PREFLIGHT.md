# Xiaomi rc1.26 supervised-update preflight — 2 November 2026

**NOT READY for installation yet.** Package compatibility, signer continuity, storage and off-device noncredential backups are verified. Settings focus/large-text acceptance and a proven data-preserving rollback are still missing. No TV installation or configuration change is authorised or performed.

## Verified installed build versus candidate

| Check | Actual installed working build | Prepared candidate |
| --- | --- | --- |
| Package | `za.co.rc.tv` | `za.co.rc.tv` |
| Version | `1.0.0-rc1.25` | `1.0.0-rc1.26-sprint` |
| Version code | 57 | 58 |
| Minimum / target API | 30 / 36 | 30 / 36 |
| Debuggable | Yes | Yes |
| APK size | 30,205,663 bytes | 30,572,167 bytes |
| Signer count / verified signature | One / APK Signature Scheme v2 | One / APK Signature Scheme v2 |
| Certificate SHA-256 | `2bedf618cf1d1a0b9e512ba160d6b9bade441cbce0a3993a97f9e568b037c773` | Same |
| APK SHA-256 | `71909a6dd70e34ef5453a7f6e7522a306b68ecc93ad1f0ef64612d24eb865eb8` | `e690369059bdce02f171ef01cfed3795722bd00d995d815fc475bf3123eceb8e` |

The previously authorised `192.168.1.39:5555` connection is online and authorised. Read-only identity confirms MiTV-AFKR0, Android 11; active user is 0. There is one installed base APK, no split set:

```text
/data/app/~~myZr0Mk6XLcIZSjmrulhPw==/za.co.rc.tv-6WZ9nwsOo840p_CB4PQCNg==/base.apk
```

That exact installed APK was pulled off-device and verified against its on-TV SHA-256. Only after extraction was it confirmed byte-identical to the saved local rc1.25 artifact. The candidate's prepared copy also matches the current build-output APK byte-for-byte. Its manifest declares the expected package, code, API levels and ARM-compatible native libraries.

An ordinary replacement update is technically compatible: same application ID/certificate, higher code, supported API and ABI. It should preserve the existing app UID/data directory and Keystore identity. This is an expectation of an in-place update, not a guarantee that new application code or OEM service rebinding has no regression. [Android update identity rules](https://developer.android.com/google/play/app-updates), [Android app signing](https://developer.android.com/studio/publish/app-signing.html).

## Storage readings

- `/data` free from `df`: **710,080 KiB = 693.44 MiB**, out of 4,519,032 KiB. Usage: 85%.
- Android storage monitor: **NORMAL**, last usable 722,546,688 bytes; low threshold 231,374,438 bytes, full threshold 1,048,576 bytes. These are separate samples and naturally differ slightly.
- RC TV private directory: **32,468 KiB = 31.71 MiB**; `files` 23,632 KiB, `shared_prefs` 16 KiB, `cache` 8,808 KiB, `code_cache` 4 KiB. Directory accounting adds minor overhead.
- Readable installed code directory: **at least 29,516 KiB**. Shell could not traverse its `oat` directory; no attempt was made to override access. Complete installed code/ART storage is therefore not measured. Do not present the APK plus private-data sum as a complete Android app-storage total.
- The candidate is 366,504 bytes larger than the working APK. Current free space is comfortably above its raw size and the device's reported low-space threshold, but actual installer staging/dex requirements and reserved-space policy have not been exercised. No cache/data was deleted to create space.

## Off-device backup preparation

All copies are under ignored, private local directory `releases/supervised-update-preflight/`. They must not be committed or pushed; they contain personal app settings and potentially weather location. Nothing was written to the TV to create these backups.

| Copy | Verification and limits |
| --- | --- |
| `RC-TV-installed-working-rc1.25-code57.apk` | Actual installed APK, verified SHA-256 and signer above. This is the working binary, not its data |
| `RC-TV-live-private-files-and-preferences.tar` | 117 files, 23,797,760 bytes; final sanitized archive SHA-256 `bc29ca352662798bbd8ed2c7d3eba3b863230850936d7b5485d7a96b57bf09da`; **encrypted pairing records excluded** |
| `RC-TV-critical-state-verification.tar` | Second read-only copy of DataStore, wallpapers and shared preferences; SHA-256 `d17f42bc6a114825638d06214046fc62b4a53c59456f10aac1064732dc3ad13d`; no pairing credentials or keys |
| `existing-TV-Downloads-backups/` | Five pre-existing JSON exports copied off-device; all five passed schema-1 envelope/payload SHA-256 checks. No new export was generated on the TV |
| `read-only-home-and-input-baseline.txt` | Home resolution, current system-launcher overrides, enabled RC accessibility service IDs and selected IME; reference only, never automatically replayed |
| Integrity/coverage JSON receipts | Local archive/file hashes and aggregate coverage counts, without printing preference values or pairing material |

The installed debug build supports normal `run-as` reads as **its own UID 10084**. Root, permission bypass and privileged extraction were not used. DataStore is present and parsable: eight favourites, eight ordered favourites and 20 stored keys. Wallpaper coverage is **14 locally stored asset directories, 56 files, including 28 full-image/thumbnail files**, totalling 5,066,557 bytes. All 68 image files in the broader copy, including historical test screenshots, passed local image-decoder verification. No database directory was present at the inspected location.

Live-copy consistency is limited: the original preference-file hash initially matched a separate live read. In the later copy, 59 of 60 critical files were identical. Only `launcher.preferences_pb` changed, specifically the selected `wallpaper` and runtime `wallpaper_rotated_at` keys; favourites and their order did not change. This is consistent with an active rotating wallpaper while RC TV remains open. The copies are **live observations, not an atomic frozen application snapshot**. No force-stop or app-data modification was used to freeze them. Refresh the noncredential backup during the supervised session if exact current selections matter.

### Pairing and backup restrictions

Automatic approval review rejected a proposed explicit second export of `companion-grants.enc` because exporting pairing credentials had not been specifically authorised. A first broad archive had already included that encrypted record; the archive was replaced locally with a validated sanitized copy excluding all `.enc` records, and its file-hash receipt was updated. The TV record was untouched. The owner then explicitly selected **Exclude pairing credentials; continue**. Pairing-record export is excluded from this task, and no encrypted pairing record remains in the prepared backup.

Android Keystore private keys were not extracted and are not in these backups. URI grants, system Home/Accessibility/IME state and other Android-managed authority are not restored by copying the tar or importing settings JSON. Even an approved encrypted-pairing-record copy would not prove post-uninstall recovery, because its Keystore keys are nonexportable. Same-UID in-place update should retain them; uninstall/data clearing must not be used as a rollback workaround.

## Current Home/input baseline and update implications

- Effective resolved Home remains `za.co.rc.tv/.MainActivity`.
- Current installed/updated Google TV and setupwraith user-0 records are DISABLED_USER (`enabled=3`). Inactive factory records also appear with DEFAULT; those do not override the current installed records.
- Enabled RC services are `HomeKeyExperimentService` and `RemoteAccessibilityService`. Selected IME is `za.co.rc.tv/.keyboard.RcInputMethodService`.
- These existing component names, Home intent registration, preference stores, keyboard service and Home-button service implementation are retained by rc1.26. Existing `Preferences.kt` and backup schema were not changed by the sprint.
- No update code runs package takeover/restoration or changes Home, Accessibility, IME or debugging settings automatically. Nevertheless installation replaces running app code and may restart/rebind its processes/services. Home routing and input continuity must be checked by the owner after installation; they cannot be guaranteed from manifest comparison alone.
- Independent emergency recovery with both RC TV and network ADB unavailable remains unverified. Stock launchers are still disabled in the existing configuration. This risk predates the candidate and is not solved by preserving the old APK.

## Local tests and outstanding presentation checks

The clean sprint branch rebuilt successfully with offline TV/phone debug checks and lint. A fresh `:app:testDebugUnitTest --rerun-tasks` executed **91 tests, zero failures/errors**. TV lint has zero errors/47 warnings; phone lint zero errors/11 warnings. Candidate and prepared APK checksums agree. Signatures were verified with installed Android build tools.

No emulator is currently attached. Earlier emulator persistence instrumentation passed, but Settings/full UI and short UI attempts were interrupted by emulator disconnection. No new APK installation, either physical or emulator, was attempted in this preflight. Local navigation policies and source checks confirm category/return-route retention and adaptive layout rules; they do **not** establish actual focus traversal, screenshot correctness, Extra Large clipping, or three-metre readability. Those remain the principal UI acceptance blocker.

## Supervised install plan — not executed

1. First complete reliable isolated Settings/large-text UI checks, or explicitly agree to a supervised exploratory review with these risks. Keep the owner at the PC and physical remote. Confirm ADB still authorised, free space adequate and the installed package/code/signer still match this baseline. If they differ, stop and reassess.
2. Review the saved working APK and private noncredential backup receipts. Refresh copies if needed. Do not assume a tar can be restored safely into a running app; no automated data-restoration procedure has been validated.
3. Obtain explicit approval for **only** the specific code-58 APK and its SHA-256 above. A normal replacement installation would use `adb -s 192.168.1.39:5555 install -r <verified-candidate.apk>` after approval. Do not add downgrade, grant-all-permissions or uninstall flags. This command was not run.
4. Confirm installation success before proceeding. If Android rejects it, stop and record the error; do not clear data/uninstall/change packages to force success. A successful install is not acceptance.
5. With the owner present, inspect launch, physical short/long Home behaviour, app opening and existing Accessibility/IME functionality; compare the recorded baseline using read-only checks. Do not choose Home or modify services as part of this validation. Do not reboot.
6. Review Settings at Standard and Extra Large: browse categories with D-pad, OK/Right into content, Left among options, Back to the rail and originating Home/utility. Check wallpaper/gallery, favourite membership/order, fonts, confirmations, backup pages and current-status text. Restore only deliberately changed app appearance choices with the owner's agreement.
7. Inspect setup/recovery UI without selecting another Home, enabling stock packages, granting permissions or disabling interception. Verify the wizard reports incomplete Home verification when interception is enabled. Test phone reconnect only with the owner's agreement, preserving approvals.
8. Accept only after failures and measurements are recorded. Reboot/default-Home/restoration experiments remain separate approvals.

## Rollback plan and limits

The exact old binary is available and signer-compatible. However returning code 58 to code 57 is a **version downgrade**, so a normal `install -r` is not an established rollback. ADB documents an explicit downgrade option for debuggable packages; both these APKs are debuggable, making a data-preserving replacement a possible future recovery route, **not a tested guarantee**. Firmware enforcement, app-state compatibility and live app behaviour remain unverified. No downgrade has been attempted or approved. [Official ADB install/downgrade options](https://android.googlesource.com/platform/packages/modules/adb/+/refs/heads/main/docs/user/adb.1.md).

If a supervised update fails, keep ADB and RC TV installed; collect read-only evidence and stop. A separately approved rollback could evaluate an explicit same-signed downgrade, or a locally rebuilt known-working source at a higher version code if exact source provenance and data compatibility are first verified. Neither procedure is preapproved. Do not uninstall, clear data, restore raw files into a running app, enable stock packages or alter Home to improvise a rollback.

## Branch and approval status

All private backups remain ignored. Preflight documentation may be committed and pushed only to the already existing local development branch `codex/settings-setup-recovery` under that same remote branch name, with no force push, merge, main update, release or deployment. Push result is reported in the final response.

Required owner decisions: explicit installation approval; separate rollback approval if needed. Pairing credentials are excluded at the owner's explicit direction. No installation/system-change approval is inferred from leaving the PC/TV running.
