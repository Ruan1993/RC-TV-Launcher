# Phase F — system-wide remote compatibility

## Checkpoint and preservation

Started from clean `e8e20c4`, branch `feature/phase-b-cinematic-glass`, TV 0.6.0 / Remote 0.2.0. Existing checkpoint APKs remain in `releases/rollback/RC-TV-{Launcher,Remote}-e8e20c4-debug.apk`; none were overwritten. No uninstall, app-data clear, default Home change, IME change, root, permission grant through ADB or hidden API was used. New versions are TV `0.7.0-system-remote` (21) and Remote `0.3.0` (3), using the same application IDs/signing configuration.

Device: Xiaomi MiTV-AFKR0, Android 11/API 30, reached through its existing authorized development connection. Before update, default Home was `com.google.android.apps.tv.launcherx/.home.HomeActivity`, active IME was RC TV Keyboard, and enabled accessibility services was null. YouCine (`com.world.youcinetv`), YouTube (`com.google.android.youtube.tv`), YouTube Music and Google's TV Remote Service are installed.

A private, ignored pre-update snapshot of `shared_prefs` and `files` is stored under `releases/diagnostics/phase-f/`. All four original files were byte-identical after in-place installation and first launch. This preserves persisted favourites/order, wallpaper configuration and encrypted approvals. Device-bound Keystore keys are not exportable: the snapshot is not an uninstall/reinstall recovery guarantee. No phone was connected for installation; existing phone records are untouched. No Home redesign, wallpaper/browser change or physical-key handler change was made. No new performance benchmark is claimed.

PR #2 was not modified or merged. The connected GitHub metadata request returned 404, so current remote draft state could not be independently revalidated. Keep it draft and unmerged; no push or PR state transition was performed.

## Supported APIs and decision

| Mechanism | Android 11 result / decision |
| --- | --- |
| Accessibility Home / Back | Public `performGlobalAction(GLOBAL_ACTION_HOME/BACK)`. Android returns whether it accepted the action. Home uses the existing selected system Home; it does not select RC TV as default. |
| Accessibility D-pad global actions | `GLOBAL_ACTION_DPAD_*`, including center, require API 33. They are never called on this API 30 TV. Version-gated support exists for newer Android but was not device-tested. |
| Accessibility focus / selection | `rootInActiveWindow`, `findFocus(FOCUS_INPUT)`, `focusSearch(direction)`, `ACTION_FOCUS`, `ACTION_CLICK`. Only works when the target app exposes useful input focus and actions. No synthetic D-pad key injection or touch/gesture fallback. No focus target, refused action or missing window produces an explicit phone error. |
| Media session controllers | Reading other apps' active sessions requires privileged `MEDIA_CONTENT_CONTROL` or a separately enabled notification listener. No such permission/service is added. |
| Media buttons | Public `AudioManager.dispatchMediaKeyEvent` sends Play/Pause, Next and Previous to Android's media-key consumer. This API returns no playback acknowledgement. Phone explicitly reports request sent and inability to confirm response. An app with no suitable media consumer can ignore it; routing can go to another active player. |
| Text / IME | Existing optional RC TV Keyboard uses `currentInputConnection.commitText`. Requires visible keyboard, an active editor belonging to the accessible foreground package, non-null connection, non-password input type and at most 160 UTF-16 code units. Inserts at current selection rather than replacing the full field. No editor action or password input is automated. |
| Accessibility text replacement | Not implemented: would depend on editable nodes and replace rather than insert text. The IME path is narrower and avoids reading field text. |
| IME D-pad events | `sendDownUpKeyEvents` targets the current input connection, not a general cross-app remote. Not used to claim global key injection. |
| Android TV Remote Service | Installed. Google's documented supported consumer path is the Google TV phone remote with on-TV pairing. No public third-party SDK/protocol contract was established by the official sources reviewed. RC TV does not impersonate Google's client or reuse its privileged service. Google TV remote remains a separate option for broader navigation. |

Public API acceptance is not proof that the UI visibly moved or playback changed. YouCine/YouTube custom surfaces, WebViews, games, inaccessible controls, custom keyboards and protected/system interfaces may reject or expose no actions. No universal compatibility claim. Firmware-specific acceptance requires actual device testing after consent.

Sources reviewed 31 October 2026:

- [AccessibilityService: global actions, version requirements and lifecycle](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [AccessibilityNodeInfo: input focus search, focus and click actions](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo)
- [AudioManager: dispatchMediaKeyEvent](https://developer.android.com/reference/android/media/AudioManager)
- [MediaSessionManager: active-session access requirements](https://developer.android.com/reference/android/media/session/MediaSessionManager)
- [InputMethodService: current input connection and IME event scope](https://developer.android.com/reference/android/inputmethodservice/InputMethodService)
- [Google's supported Android TV phone remote setup](https://support.google.com/androidtv/answer/6122465)
- [Android TV Remote Service consumer listing](https://play.google.com/store/apps/details?id=com.google.android.tv.remote.service)

## Consent, setup and recovery

1. Install both new APKs in place. TV APK is already installed on the Xiaomi for this checkpoint; phone installation remains user work. Existing unexpired approvals are retained. If pairing is absent/expired, start pairing in Phone companion, compare the complete fingerprint on both screens, enter the code and approve on TV with the physical remote.
2. With the physical remote, open RC TV Settings → Phone companion → Set up system control. Read the disclosure and choose Allow system control. This expands authority for **all existing approved phones**, explicitly disclosed in that confirmation. Revoke any untrusted phone first.
3. Choose Open Android Accessibility. Select RC TV Remote system control and accept Android's own warning yourself. This application never toggles the secure setting or dismisses/suppresses the warning. Xiaomi may require navigating Device Preferences → Accessibility manually.
4. Return to RC TV and close the companion dialog. Use the phone controls; open YouCine or YouTube normally. Test focus in each direction, OK, Back, system Home and media separately. App search remains RC TV-only.
5. For text, enable and choose RC TV Keyboard using the existing keyboard Settings actions, focus a normal field in the target app, and make the keyboard visible. Use Send text on the phone. Password fields, custom in-app keyboards and other IMEs are unsupported by this path. Do not send passwords.
6. Turn system control off in Phone companion, or disable the accessibility service in Android to stop background remote authority. The former leaves Android's service enabled but prevents app inspection/commands and closes off-screen connections; disable it in Android to fully unbind. Stop & disconnect all additionally clears companion approvals; per-phone revoke and phone forget remain available. Service disable invalidates active leases without deleting approved records.

The accessibility service is system-bound and does not intercept physical keys. No gestures, screenshots, clipboard, node text, screen history, notification access, microphone, assistant backend, wake lock, boot receiver or foreground service is added. It queries the active tree only for authenticated explicit commands and does not send that tree to the phone. System settings, permission controllers, package installers, System UI and RC TV's own dialogs reject accessibility focus/click/text requests; use the physical remote. Home/Back remain ordinary public global actions.

One process-wide companion listener shares the original TLS identity and encrypted approval store between foreground UI and service. With both consent gates enabled and service connected, it remains available outside RC TV. Otherwise the old pause/resume behaviour applies. Existing certificate pinning, private-LAN/subnet binding, bounded workers/messages, credential expiry/revocation, fresh reconnect leases and replay sequencing are retained. Permission changes invalidate leases. No cloud connection is introduced. Network changes or process/service loss can require returning to RC TV/re-enabling the service to resume; no permanent uptime guarantee or always-on polling service is added.

## RC Assist postponement

Removed Settings entry, conversation state, panel invocation, local action implementation, backend adapter and assistant-only tests from the active source. Implementation remains in Git history at `e8e20c4` and rollback APKs. Existing assistant code had no dedicated manifest permissions, services or third-party dependency to remove. INTERNET remains necessary for encrypted LAN companion communication. Gemini backend was neither developed nor deployed.

## Verification and remaining acceptance

- Both debug APKs build; 28 unit tests pass, including protected-interface/payload restrictions and existing pairing expiry/replay/restore checks. Both module lint tasks pass. Deprecated node recycling is retained for API 30; compilation also reports pre-existing discovery API deprecations.
- Xiaomi: in-place install succeeded, version 21/0.7.0-system-remote confirmed, MainActivity cold launch completed. All four original app data files unchanged. TLS hello remains protocol 2; unauthenticated HOME and INPUT_TEXT are rejected before dispatch.
- No foreground service observed; only the existing keyboard service was bound before accessibility consent.
- **User consent is pending.** No accessibility service was enabled through ADB or automation. No system-wide command has yet been verified on the Xiaomi. API reasoning and automated checks are not physical-device control acceptance.

| Target | Navigation / OK | Back / Home | Media | Text |
| --- | --- | --- | --- | --- |
| YouCine installed on Xiaomi | Pending consent and actual control test | Pending | Pending supported media consumer | Pending active normal editor |
| YouTube installed on Xiaomi | Pending consent and actual control test | Pending | Pending supported media consumer | Pending active normal editor |
| Other installed apps | Not tested | Not tested | Not tested | Not tested |

APKs: `releases/phase-f/RC-TV-Launcher-0.7.0-debug.apk` and `RC-TV-Remote-0.3.0-debug.apk`. Earlier working and rollback files are preserved. These are development-signed previews, not a public release. Usage at start: short window 1% used, weekly 46% used; query again for delivery.

## Pairing visibility fix — TV 0.7.1 (22)

User reported the pairing code was clipped after the first Phase F update. Device screenshot confirmed focus-driven scrolling placed it partly above the scroll viewport. Address and pairing code now live in the fixed dialog title area; instructions and controls use a bounded 200 dp scroll region. No communication or consent behaviour changed.

Targeted TV assembleDebug and lintDebug passed. Installed in place on the Xiaomi and visually verified the complete address/code both before and after scrolling the instructions. Phone remains 0.3.0. APK: `releases/phase-f/RC-TV-Launcher-0.7.1-debug.apk`; 0.7.0 and earlier rollback files preserved. Android accessibility consent and third-party control acceptance remain pending.

## Pairing renewal fix — TV 0.7.2 (23)

The no-active-code state after reconnect was ambiguously labelled expired/used, with renewal inside the scrolled content. Opening the companion dialog now prepares a fresh code if none is valid, no approval/poll is pending and capacity is available. New pairing code is pinned under the fixed header. A dialog-scoped one-second refresh removes expired codes from display without automatically rotating them or weakening the two-minute expiry/rate limits. Existing grants and pending approval exchanges are preserved.

Targeted PairingGate tests (5) and TV build/lint pass, including renewal after expiry/reconnect with an existing approved credential and preservation of a pending pairing. TV APK `releases/phase-f/RC-TV-Launcher-0.7.2-debug.apk` installed in place.

Xiaomi UI verification: reopening Phone companion displayed a fresh six-digit code and visible fixed-header New pairing code button. No pairing code or credential is recorded in this report.

## Remote usability and YouCine typing — TV 0.7.3 (24), Remote 0.4.0 (4)

User physically confirmed pairing and subsequent remote use worked after enabling Android Accessibility. Treat this as user-reported YouCine control acceptance, not a complete automated per-button compatibility matrix. Password/custom-surface and Android 11 focus limitations remain.

Navbar now includes Remote and Keyboard shortcuts using the existing glass rail. Remote opens short Pair phone, Other apps and Typing pages; pairing code/renewal and the missing-permission action remain outside scrolling instructions. Other apps reports separate app opt-in and Android service connection status, with one action opening the ordinary Android consent page. Guidance retains physical remote approval and stock Home recovery. When authenticated reconnect obtains a lease, the TV shows Phone connected successfully and closes pairing setup. If permission setup has not been offered and is incomplete, it offers Set up other apps or Later once. Phone gains a scoped Close TV setup command; it cannot dismiss pending phone approval or the other-app consent offer.

YouCine search investigation on the actual Xiaomi showed an active EditableInputConnection for `com.world.youcinetv` / `etSearchInput`, input type `0x80001`, but `mInputShown=false`. The old implementation both registered only in onStartInputView and required isInputViewShown, incorrectly rejecting this legitimate hidden-keyboard editor. The optional IME now tracks onStartInput independently of its view. Text still requires a focused, enabled, editable, non-password accessibility node belonging to the active app, plus an active RC TV Keyboard input connection with matching EditorInfo package and valid non-password type. Maximum length remains 160. No arbitrary keys, hidden APIs or ADB typing are added.

Phone 0.4.0 shows current setup readiness from authenticated resume/status replies, a connection toast, clearer typing guidance and Send text feedback. It keeps entered text until an accepted reply; rejected commands show a visible toast even when the connection status is scrolled off screen. Text insertion still uses public IME commitText, not field replacement or screen scraping. The on-screen keyboard need not be visible. Some app editors may acknowledge without updating; physical/user verification remains authoritative.

Build verification: both APKs, 29 unit tests and both lint checks pass. New CLOSE_SETUP payload rules are covered by the policy test (rerun separately after the additional assertions). TV 0.7.3 installed in place. Phone APK 0.4.0 is built for user installation; no physical phone is connected for installation.

A separate on-device instrumentation attempt was inconclusive: instrumentation restarted RC TV, Android marked its accessibility service unbound/crashed, and the test exited before sending any text. No application fatal exception appeared in the inspected AndroidRuntime logs. The temporary invasive test was removed; no test-only endpoint or runtime control bypass is shipped. User was asked to toggle the service Off/On once and retry from the real paired phone. No permission setting was toggled by automation. Actual YouCine text insertion is awaiting that physical check.

APKs are preserved under `releases/phase-f/RC-TV-Launcher-0.7.3-debug.apk` and `RC-TV-Remote-0.4.0-debug.apk`; all earlier rollback APKs remain. No changes to wallpaper/favourites persistence, default Home, physical-key handlers, encryption, grant expiry or foreground-service policy.
