# Phase E - RC Assist and companion reliability

## Companion checkpoint - TV 0.5.2 / Remote 0.2.0
Phase D intentionally cleared grants when MainActivity paused. This caused the reported YouCine disconnect; no Android background restriction or unsupported YouCine integration is needed to explain it. The listener still pauses off-screen, but approved grants now persist encrypted with device-bound Android Keystore AES-GCM. TV stores credential hashes; phone stores its pin/address/credential. Pairing lasts seven days or until explicit forgetting/revocation. Every reconnect obtains a fresh in-memory lease with independent replay sequence; stale leases cannot operate after pause/process recreation.

TV automatically resumes on foreground return and reuses its last port where available. Phone preserves credentials through its own backgrounding/restart, shows Reconnecting while TV is unavailable and retries with bounded exponential backoff (up to 15 seconds) only while phone UI is active. Certificate failures are distinct from temporary TLS/network closures. No foreground/background service or system-wide remote injection. YouCine remains controlled by its own supported UI/physical remote, not RC TV Remote.

Both APKs must update and pair once: 0.1 credentials existed only in RAM and cannot safely migrate through an APK replacement. New approved credentials survive interruptions. Disconnect & forget, TV revoke, expiry and Stop remain authoritative.

Verified: targeted gate tests including record restore/fresh leases; Xiaomi launch YouCine, listener pause, return RC and resume same credential; process restart restores encrypted approval; stale lease and revoked credential rejected. Existing physical-remote behaviour reused. Phone backoff/UI hardware feedback and varied networks remain acceptance gates.

## Backend inspection
Connected Vercel inspected read-only: RC-related projects are unrelated websites; no Assist project. No local Gemini credential or configured backend found. No deployment, billing or public endpoint authorised/created. RC Assist local layer is the allowed first increment; online integration must remain explicitly unavailable until configured.

## RC Assist - local first increment (0.6.0)
Settings > Open RC Assist opens a lightweight floating glass panel with current-session history (20 entries), the active Android IME, loading/error/unavailable states and Back/Close dismissal. No Home redesign or conversation persistence. Local responses are labelled Local, never Gemini.

Supported commands use actual discovery/preferences: list installed apps, Find/Open an app, Show my favourites, Open Settings/Home, select one of the three bundled wallpapers and toggle wallpaper rotation. Internal navigation/read queries run directly; external launches and preference writes require confirmation, defaulting to Cancel. Proposals expire after two minutes; app availability/value allowlists are rechecked, and preference writes atomically reject stale expected values. Backend text cannot execute actions. Shell/intents/delete/package/default/IME/system controls are unsupported.

The HTTPS backend adapter accepts only a runtime short-lived authenticated session, with prompt/response limits, timeouts, no redirects and no payload logs. There is no bundled key, endpoint or active session provider. Unknown local requests clearly explain that online AI is unavailable. Server-side authentication/rate limiting/model access and authorised deployment are still required before activation; this sprint does not claim a live Gemini service.

Optional Remote prompt entry investigated: it can reuse the authenticated allowlist/lease channel with a separate bounded ASSIST_PROMPT command that opens this panel. TV confirmation must remain authoritative. No second channel or optional phone prompt control was added in this increment.

## Supported broader-control investigation
No general third-party Android remote-control path was added. Google's supported Google TV phone remote is a separate user-controlled option ([official setup](https://support.google.com/androidtv/answer/6122465)). Media-session playback control is narrower: accessing other apps' active sessions requires privileged MEDIA_CONTENT_CONTROL or an explicitly enabled notification listener ([Android API](https://developer.android.com/reference/android/media/session/MediaSessionManager)); it is not arbitrary D-pad/text control and depends on the target app publishing supported sessions. No notification/accessibility privileges were requested, and YouCine support is not established. A foreground service would keep a socket alive but would not grant that control; pausing and reconnecting meets this increment without a permanent service.

## Verification checkpoint
29 unit tests and TV/phone lint pass. Xiaomi: actual installed lookup, unavailable-backend message, unsupported action rejection, cancellation with no preference writes, Back close, confirmed wallpaper persistence across process restart, confirmed rotation writes and restoration to original values, external launch cancellation, active Android IME Send and confirmed YouTube launch. Original favourites/order and settings restored; preference apply timestamps reflect actual verified writes. No repeated profiling or system default changes.

Remote-entry polish: the panel initially focuses Send so the session history remains visible without auto-opening the IME; moving to the input uses the active keyboard. Back closes the panel; an explicitly opened keyboard retains normal first-Back dismissal.
