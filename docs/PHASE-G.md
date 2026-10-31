# Phase G — Easy setup and phone remote

## Checkpoint and delivery

Started with a clean tree at `4b15a70` on `feature/phase-b-cinematic-glass`. Implementation commits: `1d7da5c` (TV wizard) and `a57e42f` (phone remote and guarded editing). No push, merge or PR-state change was performed; PR #2 is untouched. Its remote draft state was not independently revalidated.

- TV: `0.8.0-easy-setup`, version code 25. Installed in place on the Xiaomi MiTV-AFKR0 / Android 11.
- Phone: `0.5.0`, version code 5. Built and tested in the Android phone emulator; no physical HONOR phone was connected for installation or UI testing.
- APKs: `releases/phase-g/RC-TV-Launcher-0.8.0-debug.apk` and `releases/phase-g/RC-TV-Remote-0.5.0-debug.apk`.
- Hashes/source commits: `releases/phase-g/release-manifest.json`.
- Rollback copies: `releases/rollback/RC-TV-Launcher-4b15a70-debug.apk` and `RC-TV-Remote-4b15a70-debug.apk`. All earlier working APKs remain.

These remain development-signed APKs. No public release, backend, AI integration or store upload is included.

## Review before coding

Existing phone screens were captured before product changes. Only the Xiaomi was physically connected; the installed Medium Phone API 36.1 emulator supplied phone screenshots. Test-only fixtures rendered the connected/reconnecting remote and pairing dialog without granting approval, sending commands or pretending to be a live paired phone. The normal emulator phone keyboard was used for the typing screenshot. The TV keyboard screenshot is the real Xiaomi's RC TV Keyboard preview, rather than a YouCine search capture.

Specific problems found:

1. Discovery, manual address entry and long guidance remained above the connected remote, pushing key controls below the fold.
2. Direction buttons formed a tall stack rather than a familiar, reachable D-pad.
3. Text entry was below media controls; opening the phone keyboard did not reliably expose its field/send control.
4. Connection feedback could be far from the user's current scroll position. Technical address information occupied the main screen.
5. Setup mixed pairing, permission instructions and phone management in one scrolling area. There was no first-run route through optional choices.

## Screenshots

All paths are relative to the repository root and exist locally under **`releases/review/phase-g/`**. Images are ignored by Git; this report does not imply that they were uploaded to GitHub. Private app-data snapshots are kept separately in `releases/diagnostics/phase-g/`.

| Review | Before | After | Evidence type |
| --- | --- | --- | --- |
| Phone remote | `before/phone-remote-emulator-fixture.png` | `after/phone-remote-360x800-emulator-fixture.png` | Emulator; simulated connected state |
| Phone typing | `before/phone-typing-emulator-fixture.png` | `after/phone-typing-320x640-emulator-fixture.png` | Emulator app with normal phone keyboard |
| Phone pairing | `before/phone-pairing-emulator-fixture.png` | `after/phone-pairing-360x800-emulator-fixture.png` | Emulator pairing UI fixture; no real approval |
| TV Settings | `before/xiaomi-settings.png` | `after/xiaomi-settings.png` | Real Xiaomi |
| TV pairing/control | `before/xiaomi-pairing.png`, `before/xiaomi-system-setup.png` | `after/xiaomi-pairing.png`, `after/xiaomi-wizard-phone.png` | Real Xiaomi |
| TV keyboard | `before/xiaomi-keyboard-preview.png` | Existing keyboard layout retained | Real Xiaomi preview |
| TV onboarding | No previous wizard | `after/xiaomi-wizard-welcome.png`, `after/xiaomi-wizard-home.png`, `after/xiaomi-wizard-phone-connected.png` | Real Xiaomi |

Additional phone images cover 320×640, 360×800 and 480×960 logical sizes, plus 360×800 with font scale 1.3. Their names include the size and `emulator-fixture`. The original before-phone images use the emulator's original resolution, not a matched after viewport. `after/phone-layout-checks.json` records the final assertions. `tv-wizard-*-emulator.png` and `tv-system-home-chooser-emulator.png` are emulator screenshots, not Xiaomi screens.

## Phone experience

The connected remote separates TV discovery from control. A compact TV name/connection header, visible Reconnect and Change TV actions, a familiar five-button pad and labelled Back, Home, Keyboard and media controls replace the vertical developer layout. The pad adapts to screen height as well as width; its controls remain at least 48 dp. Button labels can grow with text size. The small-screen correction was made after screenshots exposed partially hidden navigation.

Glass-like gradients, mint selection, strong text contrast and native ripple feedback use no blur or continuous rendering effects. Brief press scaling respects Android's animation-disable setting. Optional button vibration is off by default, respects system haptics and adds no vibration permission. Manual address entry, launcher app search, setup close and explicit Forget are under More. Cancelling a new TV connection no longer immediately revokes the old TV approval or destroys its saved credentials.

The Keyboard button opens a dedicated sheet and the phone's own keyboard. Send and Delete sit together; Done is at the top. Text is sent only after an explicit Send, stays in the field until accepted, and remains available after a failed request while that sheet stays open. Errors appear in the sheet and as a toast. Typed text is not logged, saved as view state or persisted; closing/backgrounding clears the sheet. Personalized keyboard learning is discouraged using the supported flag, subject to the phone keyboard honoring it.

Explicit **Delete on TV** removes a selected range or one preceding Unicode code point through the active keyboard connection. It rejects unknown selection positions or unsupported operations rather than falling back to unsafe UTF-16 deletion. Target-field checks are shared with text insertion: current foreground package, focused enabled editable non-password node, matching active RC TV Keyboard editor, valid non-password input type, current authentication and command lease. Passwords and protected/system screens remain blocked.

Automatic transmission of every phone keystroke was evaluated and not added. Without a stable target-editor identity and transactional edit acknowledgements across arbitrary apps, reconnects and focus changes could duplicate or misdirect edits. Explicit send/delete is the supported bounded editing model. A successful input-connection reply is acceptance by the app connection, not proof that every custom editor visibly changed.

## Optional TV setup

New installations see Welcome → Home choice → Phone control → Connect phone → Finish. Existing installations with launcher preferences are not interrupted. Settings → Easy setup reopens it; Finish later and Back provide recovery. Skipping or declining permissions does not block ordinary launcher use. The existing Remote and Keyboard navbar entries remain; no additional navbar item or Home redesign was added.

- **Home:** opens the existing supported Home-role/default-app flow only after the user chooses it. Completion requires the actual Home resolver to select RC TV; a held role alone is insufficient. Xiaomi currently reports a held role while still resolving Home to Google TV, and the wizard accurately shows that discrepancy. The stock launcher stays installed and selected. Emulator Home chooser cancellation was tested. No Home choice was granted on either device.
- **Phone control:** a short explanation leads to an explicit disclosure and Android's ordinary Accessibility settings page. The user must enable **RC TV Remote system control** and approve Android's warning using the physical remote. Both the app opt-in and a connected Android service are required before automatic advancement. Existing valid consent is recognized. No hidden service-details shortcut is used.
- **Connect phone:** opens the existing fingerprint/code/physical-TV-approval flow. Successful authenticated reconnect closes the pairing screen and advances to Finish; the TV shows Phone connected. Existing approved phones can reconnect, and adding another remains optional.
- **Finish:** gives the Remote/Keyboard/Settings entry points and explains incomplete optional steps. It does not imply that stock Home or declined permission was changed.

## Simple instructions for a family member

1. Install the TV APK and open RC TV from Apps. New users see Easy setup; existing users can open Settings → Easy setup.
2. Choose whether RC TV should be your Home screen. Follow Android's chooser only if you want that change; Continue keeps your current Home.
3. To control other apps, choose Set up phone control. In Android Accessibility, scroll to Services, choose RC TV Remote system control, turn it On and approve. Return to RC TV.
4. Install the supplied RC TV Remote APK on your phone. Keep both devices on the same Wi-Fi/local network. Choose the TV, compare the complete security fingerprint, enter the code and approve the phone on TV.
5. Use the arrows and OK. Back and Home are labelled; the physical TV remote still works.
6. For typing, use Keyboard on the TV to enable/select RC TV Keyboard if needed. Select the app's search/text field on TV, tap Keyboard on the phone, type and tap Send to TV. Use Delete on TV to edit TV text. Password fields and some custom keyboards are unsupported.

Android's installation, Home and Accessibility confirmations remain authoritative. There is no silent default replacement, automatic accessibility grant, warning suppression, root/ADB runtime control path or hidden API workaround.

## Verification and scope

| Check | Result and boundary |
| --- | --- |
| Builds / static checks | Both APK builds and both lint tasks pass; 31 unit tests pass, including first-install/upgrade handling, actual-Home gating, both permission gates and deletion payload restrictions. |
| Phone layouts | Final emulator assertions pass at all four size/font configurations. D-pad/OK/Back/Home/Keyboard targets are at least 48 dp and fully visible. Send remains visible with the normal phone keyboard open; typed view state is disabled. |
| Native editing | Emulator native-editor checks pass for insertion, cursor movement, repeat, selection deletion, empty fields, Unicode code-point deletion and rejection of unknown selection positions. No production permission is granted by this test. |
| Wizard | Emulator first launch, explicit cancellation of the Android Home chooser, skipped optional steps, finish persistence, Settings reopen and cancellation pass. |
| Xiaomi installation | In-place TV update succeeds. Existing user is not forced through first-run setup. Settings reopen and actual Google TV Home status were captured. Final inspection shows the control service bound, no crashed service, stock Google TV Home still selected and RC TV Keyboard still selected. |
| Xiaomi live phone connection | Real Phone connected notification and automatic wizard Finish transition observed/captured. A temporary RC review phone was physically approved and then explicitly removed; both original phone approvals remain. |
| Full live harness | Inconclusive: the initial UI-inspection tool temporarily suppressed the accessibility service and interrupted the test client's polling. No end-to-end D-pad/media/text pass is claimed from this harness. Subsequent Xiaomi verification used screenshots without that inspection tool. |
| YouCine / HONOR | Earlier user-confirmed system control and visible-RC-keyboard YouCine search typing remain the accepted Phase F baseline. The new phone 0.5.0 interface and Delete on TV need physical HONOR/YouCine acceptance. No new universal app-compatibility claim. |
| Physical remote | Production physical-key handlers are unchanged. Ordinary TV navigation and Back were exercised with development input events; that is not a fresh handheld-remote hardware acceptance test. |

The screenshot fixture is confined to the separate test APK. Its final implementation uses `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`. Default Android UI automation can otherwise suspend other accessibility services, which explains the live-harness interference; it is not evidence of an ordinary-app permission regression. No test-only command endpoint is included in either product APK.

## Data and security preservation

Private before/after snapshots were compared. Eight favourites and their exact order remain unchanged. The only launcher preference values that changed were `wallpaper` and `wallpaper_rotated_at`, consistent with the already-enabled rotation running during the review; rotation pool, interval and other saved settings were not changed by setup. The encrypted grants file changed because the temporary review approval was added and removed; the two original approvals remain. No original snapshot was restored over newer user data.

Application IDs, existing development signing, device-bound encrypted credentials, certificate pinning, bounded private-LAN listener, authenticated leases, replay checks, grant expiry and explicit revocation remain. No new product dependency or permission, foreground service, boot receiver, cloud endpoint, assistant component or AI feature was added. RC Assist classes are absent from the packaged APKs.

Implementation/source references:

- [Android Home role and explicit consent](https://developer.android.com/reference/android/app/role/RoleManager)
- [Public Accessibility settings action](https://developer.android.com/reference/android/provider/Settings#ACTION_ACCESSIBILITY_SETTINGS)
- [InputConnection text/deletion operations](https://developer.android.com/reference/android/view/inputmethod/InputConnection)
- [Android UI automation and service suppression](https://developer.android.com/reference/android/app/UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)

No performance benchmark, physical HONOR layout test, universal third-party keyboard support or guaranteed background uptime is claimed. Media controls still depend on the player and report dispatch rather than confirmed playback. Usage at final review: 49% short-window allowance and 46% weekly allowance remaining; query again if continuing development.
