> Phase G: TV 0.8.0 / Remote 0.5.0 adds Easy setup and the redesigned phone remote. Existing unexpired approvals are retained. Use [the current setup and verification guide](PHASE-G.md); historical instructions below describe earlier layouts.

> Phase F update: use TV 0.7.3 and Remote 0.4.0. The foreground-only limits below describe the earlier checkpoint. See [Phase F setup and current restrictions](PHASE-F.md) for the opt-in system-control path. Existing 0.2 approved pairing records are retained; no compulsory re-pairing.

# Current upgrade - TV 0.5.2 / Remote 0.2.0

Install both new APKs and pair once. Approved credentials are now encrypted and retained for up to seven days. Opening YouCine pauses the listener; return to RC TV using the physical remote and the phone reconnects automatically. There is no YouCine/system-wide control or always-running service. The phone retries only while visible, up to every 15 seconds. Backgrounding the phone retains approval.

Disconnect & forget TV, TV-side Revoke, expiry and Stop & disconnect all remove approval. New pairing code adds/retries pairing without forgetting other approved devices. Manual address fallback remains available; discovery/address updates require physical-phone/network acceptance.

The Phase D description below is historical and superseded for credential/session lifetime by PHASE-E.md.

# RC TV Remote — Android companion

## Connect
Install releases/RC-TV-Remote-0.1.0-debug.apk on an Android 8+ phone. On TV: Settings > Phone companion > Start pairing. Keep RC TV visible; both devices need the same trusted LAN without client isolation/VPN. Find RC TV in the phone list or enter the private address:port shown on TV.

Compare **every fingerprint group** on both screens before Matches my TV. Enter the six-digit code, approve the phone using the physical TV remote, then close the TV dialog with Done. Phone controls provide D-pad/OK/Back, RC TV Home and bounded app-search text. Never enter passwords. Physical Home still follows Xiaomi's existing route; phone Home operates within foreground RC TV only. Use the physical remote to dismiss modal dialogs.

Disconnect on phone, revoke a phone on TV, or Stop & disconnect all. Codes expire after two minutes, pending approvals after one minute, grants after 30 minutes. Stop/start to pair again after success. Leaving either app ends the phone session; leaving RC TV closes the listener and clears all TV grants. No persistent pairing credentials or cloud account.

## Security / scope
Off by default; no background service, boot receiver or router/UPnP configuration. Binds one active Wi-Fi/Ethernet private IPv4 address and accepts only same-subnet peers. VPN and IPv6-only networks are unsupported initially. Android DNS-SD advertises _rctv._tcp.; manual address entry is the fallback for filtered multicast. Do not configure router port forwarding.

TLS 1.2 uses a P-256 Android Keystore identity. Initial bootstrap retrieves public hello/identity only; user compares the full SHA-256 certificate fingerprint. Every PIN, approval ticket, token and command then requires that exact certificate pin. Bootstrap trust is not general browser trust. The initial RSA identity failed Xiaomi's signing path; the tested P-256 identity supports Conscrypt's prehashed signing.

Frames are length-prefixed JSON, maximum 4 KiB, one request/reply per connection, two workers/four queued connections and a four-second socket timeout. PIN attempts limited to five/minute. Pairing requires a valid PIN plus separate TV approval. Approval tickets and grants use 256-bit random secrets, held in memory (hash-keyed grants on TV). Increasing sequences reject replay; commands are rate limited. Stop/expiry/disconnect/revocation invalidate grants. No payload/credential logging or saved phone text. Networking permissions support the companion; keyboard code makes no network calls.

Allowlist: Up/Down/Left/Right/OK/Back/Home and app-search text up to 160 characters. Dispatch is confined to resumed RC TV's own window; modal windows reject phone commands. No system-wide injection, shell, arbitrary intents, package management, preference edits, file access, IME/password-field entry or accessibility service.

## Evidence and remaining acceptance
Gate tests cover approval, replay, expiry, rate limiting, rejection, disconnect and revocation. Xiaomi LAN tests verify TLS identity, unauthorised/bad-PIN/replay rejection, TV approval, Home/Search/Back/favourite focus and disconnect. Actual Android client code passed pinned TLS, TV approval, Home/Search/Back/disconnect from a separate test process; its disposable installations were removed. This is client/transport evidence, not physical-phone UI acceptance.

User will test the phone later. Phone layout/touch/lifecycle, actual phone/router discovery, newer Android LAN permissions, varied networks and longer sessions remain gates. A short PC multicast probe received no advertisements, so automatic discovery is not claimed verified on this LAN.

Reference: [Android NSD](https://developer.android.com/develop/connectivity/wifi/use-nsd).
