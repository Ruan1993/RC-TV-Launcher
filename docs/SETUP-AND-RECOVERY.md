# Setup and recovery — rc1.26 sprint candidate

This is a development candidate, not a physically accepted Xiaomi update. See [sprint results](IMPLEMENTATION-SPRINT.md), [approved architecture](../RC-TV-SETUP-COMPANION-RECOVERY-REPORT.md) and the [preserved Xiaomi-774 takeover](XIAOMI-774-WORKING-TAKEOVER.md).

## Easy setup

New installations offer Easy setup; existing installations can open **Settings → System & Recovery → Easy setup**. Ten stages cover compatibility, appearance, favourites/order, optional features, permissions/keyboard, phone pairing, Home selection, a physical-Home trial, recovery readiness and a truthful summary.

Progress and selected feature requirements are stored independently of Activity saved state. Unexpected process termination resumes an active setup on reopen. **Finish later** pauses automatic reopening but preserves the step for manual resume. Completing setup resets the wizard to Welcome for a future rerun; existing launcher preferences are retained. Skip is recorded independently of verification.

Appearance reuses existing preference setters and immediately reflects colours/fonts/text choices. Bundled wallpaper choices are available in setup; import, online galleries and advanced rotation remain in Settings. The same favourites editor is available directly under **Home & Apps → Manage favourites**. Existing long-OK actions remain available.

Only enabled optional features request related access. Other-app control needs explicit RC TV consent plus the Android Accessibility service. Phone typing also needs RC TV Keyboard enabled and selected. The wizard cannot grant either authority. Returning from Android Settings refreshes status; Continue does not mean the permission was granted.

Home selection uses Android's existing role/chooser. No system package is disabled. The neutral Home trial observes a genuine MAIN/HOME delivery during a short test window and requires owner confirmation plus correct resolution. Enabled RC Home-button interception makes verification incomplete; setup never disables it automatically. A restart is never performed or claimed verified. App mode is a valid completion outcome.

## Original Home recovery

Open **Settings → System & Recovery → Restore Original Launcher**, or the contextual original-Home action in Home & Apps. Choose a credible original system Home, review the plan, inspect independent Home-button interception, open original app details/Home selection and test stock Home. Setup/recovery screens share readable controls and predictable Back navigation.

The wizard saves first-observed device/user/candidate/override information and durable recovery step/target. These are current observations, **not an original pre-takeover baseline**; externally configured activation is reported as unknown. No shell runner or package-changing operation is enabled. Recovery verification requires an enabled selected component, matching Home resolution, interception absent and explicit owner confirmation after an original-launcher test. Confirmation is session-local; reboot acceptance remains pending.

For the preserved Xiaomi-774 installation, Google TV and setupwraith are documented as disabled for user 0. Normal Home selection cannot enable them. Detailed device-specific guidance remains accessible in Home & Apps, but this sprint never executes it. The documented restoration order is Google TV before setupwraith, with original DEFAULT overrides restored. Restoring a historical role baseline that named RC TV is different from finishing with original Google Home selected.

Never disable/uninstall RC TV before working stock Home is confirmed. This candidate intentionally offers no self-uninstall shortcut: verified external settings/image preservation and independent recovery acceptance are still required. Switching through the supported settings screens does not clear RC preferences or images. Android uninstall would remove private RC data.

## Safety and rescue limits

- Existing Xiaomi Home/package/Accessibility/keyboard/debugging configuration was not changed during this sprint.
- Android Settings entry points are resolved to supported exported handlers where possible, with manual menu guidance otherwise.
- A Settings shortcut inside RC is not an independent rescue path if RC cannot open.
- Existing TCP ADB is not guaranteed after debugging shutdown, reboot or network changes. QR pairing cannot grant shell authority.
- Recovery when RC and wireless ADB both fail remains unverified on Xiaomi. No new stock-disabling activation may ship before independent rescue passes.
- A local settings backup is not a system image and excludes imported image bytes, phone keys, permissions, Home and package states.

Physical activation/restoration/reboot tests require a separately authorised owner session. Keep the working Xiaomi installation until this candidate has been reviewed.
