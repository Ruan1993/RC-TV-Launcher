# J4 recovery bundle

Local bundle: `releases/recovery/j4/`.

- `apks/RC-TV-Launcher-1.0.0-rc1.17-debug.apk`: final J4 candidate, version code 46.
- `apks/RC-TV-Launcher-1.0.0-rc1.16-debug.apk`: verified J3F backup/restore candidate.
- `apks/RC-TV-Launcher-1.0.0-rc1.15-debug.apk`: preserved working pre-backup launcher.
- `SHA256-MANIFEST.json`: integrity hashes for bundle files. Verify before reinstalling an APK.
- `docs/`: J4 results, schema-1 backup specification and original Home diagnostics/readiness records.
- `BASE-RECOVERY.md` and `original-home-state.json`: Xiaomi-specific original states, prepared restoration commands, limitations and data-preservation checklist.
- `private-settings/RC-TV-verified-settings.json`: real provider-read backup from the final physical backup test. Digest and eight favourite entries checked without displaying personal values. **Contains weather location; keep the whole bundle private.** Pairing credentials/keys are excluded.

This bundle is not an Android firmware image or guaranteed recovery from an unbootable device. The existing `releases/recovery/j3f/` bundle remains intact, including its rc1.14 rollback. No user HDD media was modified; J4's synthetic shared-storage files were deleted by their exact owned media URIs after testing.

Repair the app by reinstalling the same/newer signed APK with `adb install -r`, preserving app data. Do not uninstall or clear data to force a downgrade. Import settings only through RC TV's validated preview/confirmation UI. Local Downloads files are accessible to the same installed app; restoring an arbitrary off-TV file still needs a supported picker/granted folder. Tree grants, Android permissions and pairing are not portable; securely set them up on another device.

USB host/media use was historically confirmed, but in the current J4 check two known-working HDDs showed no contents even in Xiaomi Media Player. No public removable volume or OPEN_DOCUMENT_TREE handler was exposed. Wired USB ADB remains unverified. No host-to-host USB-A cable or adapter purchase is recommended.

A proposed normal-settings USB-debugging-off test is optional and requires separate user approval: it temporarily disconnects the existing ADB connection. If approved, change only that setting manually, keep the original Home/Settings packages intact, stop playback before restoring USB debugging, and do not revoke debugging authorisations or change Default USB configuration. Approval, execution and outcome must be recorded in PHASE-J4.md; a proposal is not a completed test.

Home remains exactly as before J4: RC TV saved role/preference, Google TV operative routing. No launcher takeover is authorised by this bundle or by successful media/backup tests.
