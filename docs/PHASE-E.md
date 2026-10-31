# Phase E - RC Assist and companion reliability

## Companion checkpoint - TV 0.5.2 / Remote 0.2.0
Phase D intentionally cleared grants when MainActivity paused. This caused the reported YouCine disconnect; no Android background restriction or unsupported YouCine integration is needed to explain it. The listener still pauses off-screen, but approved grants now persist encrypted with device-bound Android Keystore AES-GCM. TV stores credential hashes; phone stores its pin/address/credential. Pairing lasts seven days or until explicit forgetting/revocation. Every reconnect obtains a fresh in-memory lease with independent replay sequence; stale leases cannot operate after pause/process recreation.

TV automatically resumes on foreground return and reuses its last port where available. Phone preserves credentials through its own backgrounding/restart, shows Reconnecting while TV is unavailable and retries with bounded exponential backoff (up to 15 seconds) only while phone UI is active. Certificate failures are distinct from temporary TLS/network closures. No foreground/background service or system-wide remote injection. YouCine remains controlled by its own supported UI/physical remote, not RC TV Remote.

Both APKs must update and pair once: 0.1 credentials existed only in RAM and cannot safely migrate through an APK replacement. New approved credentials survive interruptions. Disconnect & forget, TV revoke, expiry and Stop remain authoritative.

Verified: targeted gate tests including record restore/fresh leases; Xiaomi launch YouCine, listener pause, return RC and resume same credential; process restart restores encrypted approval; stale lease and revoked credential rejected. Existing physical-remote behaviour reused. Phone backoff/UI hardware feedback and varied networks remain acceptance gates.

## Backend inspection
Connected Vercel inspected read-only: RC-related projects are unrelated websites; no Assist project. No local Gemini credential or configured backend found. No deployment, billing or public endpoint authorised/created. RC Assist local layer is the allowed first increment; online integration must remain explicitly unavailable until configured.
