# Phase D — connected experience

## Cursor editing — 0.5.0
Dedicated cursor-left/right keys in every keyboard layout. Remote OK down/repeats move the insertion point; release does not duplicate movement. D-pad arrows still navigate keys. InputConnection.setSelection and Android selection offsets avoid moving editor focus at empty-field boundaries. A bounded transient read of at most two UTF-16 code units preserves surrogate pairs; no content is logged, persisted or kept as a composing buffer. Selection deletion replaces the selection; ordinary Backspace deletes the code point before the cursor. Unsupported cursor metadata/text access safely yields no movement.

Xiaomi standard native editor checks pass: middle insertion, cursor movement, before-cursor deletion, selected text, Unicode, empty fields and repeated cursor activation. Targeted keyboard layout tests and debug/test-APK compilation pass. Separate instrumentation uses synthetic text only; no IME/default change or preference edits. Working/rollback APKs retained. YouCine's application keyboard is untouched.

## Companion direction
Android phone confirmed. First delivery will use an optional native Android client, local discovery, pinned TLS after explicit fingerprint comparison, short-code pairing and TV approval. Commands are restricted to RC TV's own surfaces. No cloud account or system-wide key injection.

## Local companion - 0.5.1
TV-side foreground pairing/revocation and a separate native Android phone APK implemented. Pinned TLS after fingerprint comparison, short code plus TV approval, expiring in-memory grants, bounded scoped commands and foreground-only listener. See COMPANION.md for usage and limitations. User deferred physical-phone acceptance; actual Android client and Xiaomi LAN transport tests pass separately.

Casting findings: CASTING-COMPATIBILITY.md. Google receiver installed; functional sender playback remains unverified. RC Assist interface, confirmation/execution boundaries and backend requirements: RC-ASSIST-ARCHITECTURE.md. No backend deployed.

TV-side revocation and listener shutdown pass. Eight favourites/order and all stored settings match; only the current scene and rotation timestamp advanced through the existing rotation scheduler during the interactive session. No preference reset or rollback of that normal advancement.

Final safeguards: initial pairing-approval focus defaults to Decline (touch-to-remote handoff corrected). Listener shuts down on Activity pause as well as stop, including translucent Xiaomi Settings. Companion is left off. Broader unit/lint checkpoint passed; subsequent narrow focus/lifecycle corrections compiled and received focused device checks.

Final navigation: phone Home/Back focuses the first favourite, Right visibly focuses YouTube, OK launches it. Launching an external app pauses RC TV and may close that command reply socket; the phone reports connection ended, and the physical remote remains the fallback. Returned to RC TV Home with companion off.
