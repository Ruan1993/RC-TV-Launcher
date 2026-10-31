# Phase H — Cinematic Glass Home and essential utilities

TV preview **0.9.2-utilities (28)**; phone **0.5.0 remains unchanged**. Baseline TV 0.8.0 / phone 0.5.0 at `32decbe`. Work remains on `feature/phase-b-cinematic-glass`; no push, merge or PR metadata change was performed. PR #2 was reported draft/unmerged at the baseline; the final connector read returned 404, so remote status could not be independently refreshed.

## Saved checkpoints

- A: `25ece6b` — horizontal navigation and full-width favourites. APK `releases/phase-h/RC-TV-Launcher-0.9.0-checkpoint-A-debug.apk`.
- B: `c73229e` — bounded Speed Test, themes and typography. APK `releases/phase-h/RC-TV-Launcher-0.9.1-checkpoint-B-debug.apk`.
- C: final Phase H commit — Cast guidance and permanent pairing approvals with renewable leases. APK `releases/phase-h/RC-TV-Launcher-0.9.2-debug.apk`.
- Original same-signed TV and phone rollback APKs are preserved under `releases/rollback/*32decbe-debug.apk`. Unchanged phone APK: `releases/phase-g/RC-TV-Remote-0.5.0-debug.apk`.

Private app-data snapshots are retained locally under ignored `releases/diagnostics/phase-h/`. They are not committed or published. Do not clear app storage or uninstall to update. A rollback APK may require Android's explicit downgrade support; it is not a reason to uninstall and lose pairing/data.

## Home, appearance and preservation

The top bar contains Home, Search, Apps, Remote, Keyboard, Cast, Speed Test and Settings. Icons and labels keep their size; focus brings off-screen controls into view, with a visible Left/Right cue. The favourite shelf uses the freed full width and scrolls horizontally.

The original card structure is preserved: 232 dp outer width, 216 x 190 dp button, 80 dp icon, original padding, glass layers, spacing and focus animation. Checkpoint A kept the card file byte-identical. B only substitutes accent/focus tokens and optional bold text; no card geometry was changed. Original (Default), Modern, Standard reproduces the existing palette and typography. Longer labels keep the original single-line ellipsis, especially at larger text sizes; cards are never squeezed to expose all eight at once.

Settings → Appearance & Themes previews and saves Original plus Emerald, Ocean, Rose, Ruby, Midnight and Slate; Modern, Classic and Bold / High Readability; Standard, Comfortable, Large and Extra Large. Reset to Original clears only those three appearance choices. It does not reset layout/motion, wallpaper or favourites.

Private before/after comparison confirms the exact same eight favourites and order. All other original launcher values are unchanged except wallpaper and rotation timestamp, consistent with already-enabled automatic rotation. Wallpaper collections and settings were not replaced. Android's selected RC TV Keyboard and user-enabled RC TV Remote accessibility service remain selected. Default Home still resolves to `com.google.android.apps.tv.launcherx/.home.HomeActivity`. No permission grants, default Home change, foreground service, new product dependency, paid backend, RC Assist or Gemini deployment was added.

## Speed Test

Choose Speed Test → Start Test. Nothing runs at startup or periodically. Download/upload are decimal Mbps measured over HTTPS against Cloudflare's documented public speed-test endpoints (`__down` and `__up`). HTTP latency is the median of five requests after warm-up, not ICMP ping. Basic information shows Wi-Fi/Ethernet, validated Internet and metering without collecting SSID, MAC, public IP or location.

Warm-up is excluded. Reported throughput requires at least two transfers each lasting 1.2 seconds, at least 1 MiB per transfer and at least 8 MiB combined. The byte-weighted rate includes request/TLS overhead; it describes this TV-to-Cloudflare path, not a calibrated line-capacity or ISP-maximum test. Very fast connections may leave a metric blank rather than report a tiny-file estimate. Concurrent traffic and Wi-Fi affect results.

Limits: under 125 MB application transfer data per run (network protocol overhead is additional), 55-second monotonic budget with five-second connection/read timeouts, one-minute in-process cooldown, single worker, explicit Cancel and cancellation on leaving the screen/backgrounding. Blocking network operations may add timeout time to the nominal budget. No redirects or compressed responses; exact download byte count is checked. Truncation, unexpected status/size, timeout and rate limit produce visible errors; short/insufficient samples remain blank. Completed stages may remain visible after a later-stage error. Results are not saved. No scheduled work or always-running speed service.

Endpoint review: Cloudflare's [official open-source engine](https://github.com/cloudflare/speedtest) documents these public default endpoints, provides MIT-licensed integration code and has optional authorization disabled by default. This app adds no SDK or copied implementation, account, key or billing resource. [Website terms](https://www.cloudflare.com/website-terms/) still govern public services; no guaranteed quota/SLA or unlimited commercial permission is claimed. The personal preview bounds and spaces requests, and respects HTTP 429 without bypass. Recheck provider terms/capacity before distributing at scale. The [provider test/privacy information](https://speed.cloudflare.com/) explains network-address processing; the TV screen discloses that the provider receives the network address.

Actual Xiaomi tests, started with development input events:

| Run | Download | Upload | HTTP latency | Interpretation |
| --- | --- | --- | --- | --- |
| Initial B | 52.9 Mbps | blank | 369.4 ms | Insufficient sustained upload samples; correctly withheld |
| Final B algorithm | 87.7 Mbps | 75.2 Mbps | 389.3 ms | Both stages met sustained sample rules |

The upload ladder was corrected to repeat the largest bounded sample. These are observed network measurements, not a laboratory calibration or proof of speed accuracy against a reference instrument. Calculation tests cover weighted units, sample rejection and latency median. Offline, throttled-server and cancellation paths were code-reviewed, not fault-injected on the Xiaomi.

## Cast hub

The Xiaomi MiTV-AFKR0 / Android 11 has enabled `com.google.android.apps.mediashell` version 3.72.446070. The hub detects known receiver installation/enabled state through ordinary package visibility and opens public Android app-details settings. It does not invoke private receiver activities, start a fake session or implement a receiver. Instructions are D-pad focusable and scroll into view; reaching the last section was observed on the TV.

Instructions distinguish Google Cast from screen mirroring, AirPlay and local media. Android/iPhone users start from a compatible sender app; iOS may require Local Network consent. Desktop Chrome can cast a tab/screen; supported Android devices can use Google Home screen casting. Miracast/AirPlay support is not established. DRM, sender compatibility, router isolation and provider restrictions still apply. RC TV's wallpaper browser does not stream local phone files.

No actual sender-to-TV Cast session was completed in Phase H. Installed/enabled receiver status is not proof of discovery, playback or mirroring. References: [Google sender guidance](https://support.google.com/googlecast/answer/3006709), [Cast design basics](https://developers.google.com/cast/docs/design_checklist/cast-basics), [Android screen casting](https://support.google.com/googlecast/answer/6050397).

## Pairing expiry correction

The audit found a real seven-day lifetime on persistent approval credentials, not just a connection lease. New approvals now remain until TV-side Revoke, Disconnect & forget, Stop & disconnect all, or app credential removal. During load, still-valid legacy records migrate to no scheduled expiry with the same hash/ID/name and are saved encrypted again. Already-expired records are discarded; revoked records are never recovered. Existing grants were not cleared and no disposable phone was added in Phase H.

Connections use fresh random in-memory leases lasting 30 minutes; the existing phone reconnect path renews them using the durable token without another pairing code. Restart/pause invalidates old leases. Pinning, encrypted storage, local subnet restriction, explicit TV approval, attempt limiting, command rate limits, increasing sequences and explicit revocation remain. An action arriving exactly at lease expiry can trigger reconnect; it must be retried after reconnect rather than silently replayed.

Six targeted PairingGate tests pass, including legacy migration, rejecting expired records, durable authentication beyond 30 days, lease expiry/renewal, replay and revoke. This is automated gate verification. A fresh physical-phone reconnect and a 30-minute live expiry soak were not performed in Phase H. The user's earlier successful YouCine control and RC TV Keyboard typing are retained evidence, not freshly retested claims. The approved phone layout/source was not changed and needs no update for this fix.

## Optional widgets and remaining acceptance

Weather/media widgets were deliberately deferred as usage became low, per checkpoint priorities. No blank/fake widget, GPS permission, notification listener or extra startup work was added. [Open-Meteo terms](https://open-meteo.com/en/terms) allow limited noncommercial hobby use with attribution but require a paid arrangement for commercial use; provider choice and caching still need a separate implementation checkpoint. Media history must come from genuinely available metadata; no YouCine/Netflix/YouTube history is fabricated and no intrusive permission was requested merely to fill a card.

Build/unit/lint evidence: checkpoint B passed app build, full app unit suite and lint during development; after the sampling revision, targeted measurement tests, build and lint passed. C passed six pairing tests, app build and lint; the final read-only Cast focus fix was rebuilt and linted. Latest lint: zero errors, 21 advisory warnings (dependency updates, existing platform/style/localization advisories). No new product library or permission was added; the known receiver package query is not a permission.

Emulator appearance review saved real preferences, restarted the activity, asserted fully visible original 216 x 190 dp cards, saved screenshots and restored preferences. Original/Standard passed; all seven accents passed at Extra Large with the three font styles represented; Comfortable and Large also passed. This is a representative matrix, not all 84 combinations or every TV resolution. Emulator-only discoverable fixture and instrumentation remain in the separate test APK, never the installed product. A Bluetooth crash dialog initially blocked the emulator; it was cleared on the emulator and the checks rerun. No UI automation was used on the Xiaomi to avoid suppressing its accessibility service.

Actual TV evidence uses screenshots and development input events: horizontal bar focus/scroll, long-press menu, moving a card and cancelling without saving, Home/Back handling, final Home, two bounded speed runs, receiver status and D-pad instruction scrolling. These are device checks, not handheld physical-remote or new phone hardware acceptance. A fresh performance benchmark, casting playback and every theme/size on the Xiaomi remain unverified. Screenshots: `releases/review/phase-h/final-home.png`, `speed-result-final.png`, `final-cast.png`, `final-cast-dpad.png`, `theme-*-xl.png`.

Usage at checkpoint review: 11% short-window and 40% weekly remaining; recheck before adding features. Optional work is stopped to preserve a stable checkpoint.

Final APK SHA-256: TV `C23707568630F5D89F8A9CB2618735B22222EC43270ED0EBA90746A6874BEF83`; unchanged phone `0DF4C9EB971185488071EA8FD0226D712F1ACDF50BC786A86B804B81CAE431CC`.
