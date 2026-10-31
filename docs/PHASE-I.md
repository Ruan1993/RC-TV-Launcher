# Phase I — 1.0 release candidate for user acceptance

**TV 1.0.0-rc1 (29), Remote 0.5.1-rc1 (6). Not a public or accepted final 1.0 release.** The TV candidate is installed on the Xiaomi. The phone APK is supplied locally; no physical HONOR update was performed. It changes only the app icon/version, preserving the approved 0.5.0 phone UI and client implementation.

## Checkpoints and packages

Baseline: TV 0.9.2 / Remote 0.5.0, clean `0572377` on `feature/phase-b-cinematic-glass`.

- A: `bc5be75` — corrected Cast mark, wallpaper visibility and adaptive icons.
- B: `4948439` — inactive download flow, optional cached weather, permission-constrained media fallback.
- C: final Phase I commit — D-pad-readable speed guidance, regression evidence and RC packaging.

Final local packages:

- `releases/phase-i/RC-TV-Launcher-1.0.0-rc1-debug.apk`
- `releases/phase-i/RC-TV-Remote-0.5.1-rc1-debug.apk`

Checkpoint A/B TV packages are retained in the same folder. Working Phase H TV/phone copies are preserved as `releases/rollback/RC-TV-{Launcher,Remote}-0572377-debug.apk`; older rollbacks remain. Application IDs are unchanged. All four baseline/final APKs pass signature verification with the same development certificate (SHA-256 `2bedf618cf1d1a0b9e512ba160d6b9bade441cbce0a3993a97f9e568b037c773`). Update in place; do not uninstall or clear storage. Production signing/secure key backup remains a public-release gate. These development-signed APKs are for private acceptance, not a store release.

APK SHA-256:

| Package | SHA-256 |
| --- | --- |
| TV | `030211323233a4cd848966c2d4580c32f81b915942f5a633ac2ebee0bfb8fe3b` |
| Phone | `79bf78d8aa2c339140d52134339b15c7f9de6184f77299a333f016c3dbd375b1` |

No push, PR merge/metadata change, public source/APK publication, repository visibility change, paid infrastructure or download deployment occurred. The GitHub connector returned 404 for this repository, so the baseline draft/unmerged PR #2 status could not be refreshed remotely. PR #2 remains untouched by this work.

## Preserved experience and appearance

The favourite-card file is byte-identical to Phase H: SHA-256 `57340a0268e445499609688e9150adb031125c8c596bb90bc6da0fd3a68c1508`. Dimensions, glass, icon presentation, padding, spacing, focus glow, animation and ordering interactions were not edited. The same eight favourites and exact order were verified against a private before/after DataStore snapshot. Private wallpaper image files are unchanged. No snapshot was restored over newer user choices.

The final TV keeps Emerald Green, Bold / High Readability and Comfortable, as visible during the review. These three appearance values differ from the initial private snapshot; production TV test actions did not select themes/fonts/sizes, and emulator preference fixtures were confined to `emulator-5554`. Current TV choices were retained rather than overwritten. Wallpaper/rotation timestamp also changed as already-enabled rotation ran. Other original launcher preferences are unchanged; no wallpaper-visibility key was set on the TV, so it remains Balanced. Weather is unconfigured/off on the TV. Existing Easy Setup and companion configuration files remain unchanged. The temporary test phone was removed; the TV again lists only the original BRP-NX1 approval, and the encrypted grants file returned to its original 181-byte length (ciphertext changes when approvals are saved).

Android still selects `za.co.rc.tv/.keyboard.RcInputMethodService`, with the user-enabled RC TV Remote accessibility service present. Default Home remains `com.google.android.apps.tv.launcherx/.home.HomeActivity`. No permission was silently granted, no warning suppressed, no device Home changed and no existing credential cleared. RC Assist remains absent from active code/runtime; no Gemini work was performed.

The Cast mark now has an open lower-left screen corner, broadcast dot and two quarter-circle waves using the same stroke and 24 dp footprint as the other navbar marks. Navbar structure, spacing and focus behaviour remain unchanged.

Settings → Appearance & Themes → Wallpaper visibility offers Subtle, Balanced and Vivid. A centralized alpha function adjusts the existing precomposited scrims; Balanced uses exactly the old four alpha values. Cache identity includes visibility, so selection previews immediately without duplicate assets or live per-frame blending. Only one full backdrop is retained, with serialized IO decoding. Reset visibility to Balanced changes only visibility; Reset to Original clears accent/font/size/visibility without touching wallpaper, favourites or layout/motion. Minimalist still hides the wallpaper. Card opacity/design is unaffected.

Both apps have original vector adaptive foreground/background artwork and API 33 monochrome variants, with a related mint/glass identity: a TV/play symbol and a distinct remote/D-pad symbol. Legacy icon resources remain available; the existing card fallback graphic was not replaced. Installed Android drawables were rendered on the emulator with circle, rounded-square, square and monochrome masks. See [Android adaptive icon guidance](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).

## Download flow and distribution

Download RC TV Remote is reachable from Easy Setup's Connect your phone step and the companion Pair phone screen. The dialog gives the exact Remote version, Android 8+ compatibility, three installation/network steps, Back and Return to pairing. Read-only instructions are focusable, so a physical D-pad can scroll them without clipping at larger text sizes. Return to pairing was observed on the Xiaomi.

**Online download and QR are deliberately inactive.** No public HTTPS destination was verified; no fake URL or QR is rendered. A stable owner-approved public HTTPS download page is needed before activation. A dedicated public binary/download repository or static HTTPS page can distribute versioned APKs/checksums without changing the private source repository's visibility; GitHub private release URLs require authentication and are unsuitable for an unauthenticated TV download QR. Before enabling QR, publish only with explicit approval, verify unauthenticated phone access and the APK/checksum, then encode the verified page URL. No access token belongs in the page, QR or APK. Android installation security prompts remain intact.

For this private RC, transfer the supplied phone APK manually. Existing paired 0.5.0 phones continue working, so the branding update is optional.

## Weather and media

Settings → Optional weather → enter a city → Search city → choose the matching location. Choosing a result is the explicit opt-in. Turn weather off and remove location deletes only its saved location/cache. No GPS/location permission, API key, geolocation lookup, job, boot receiver or continuous polling is added.

The card fits inside the existing Home header above the favourites, using the selected glass tokens. It does not insert a new shelf or change card size. Long location names use one-line ellipsis. Weather is loaded asynchronously after Home is visible; normal unconfigured Home starts without a network request. Returning to Home or RESUMED may refresh once per hour, with the attempt timestamp persisted even on failure. No hourly timer or background refresh service runs while away. A successful cache shows its provider timestamp; failures label cached weather offline/unavailable. Conditions older than 24 hours are hidden. Location changes invalidate old responses. Responses are bounded to 64 KiB with connection/read timeouts, a transfer deadline, no redirects and validated coordinates, temperature units/range and timestamps. The provider receives the search/location coordinates and IP address; setup explains that.

Open-Meteo is used only under its **free personal/noncommercial** terms, not an unlimited commercial licence. Its published ceilings are below 10,000 calls/day, 5,000/hour and 600/minute; this explicit-search/hourly-refresh home preview stays far below these. A commercial distribution would require revisiting provider licensing/hosting before publication. UI attribution credits Open-Meteo and GeoNames, identifies summarized conditions and links/displays CC BY 4.0. Sources: [provider terms](https://open-meteo.com/en/terms), [forecast schema and WMO codes](https://open-meteo.com/en/docs), [GeoNames-backed geocoding](https://open-meteo.com/en/docs/geocoding-api).

Real forecast/geocoding calls for the explicitly chosen test city Pretoria succeeded on the emulator, and real returned conditions were displayed. This was not the user's configured location and was never written to the Xiaomi. The test restored all emulator weather and font settings. A Windows source-encoding fault initially corrupted the degree symbol used for unit validation; source encoding and Unicode-safe unit checks were corrected, then the real endpoint test passed. No invented weather is included in product UI.

The media card uses only genuine MediaSessionManager/MediaController metadata when Android already authorizes access, registers callbacks only while visible and hides with no metadata or permission. **An ordinary install on this Xiaomi cannot enumerate third-party sessions without MEDIA_CONTENT_CONTROL privilege or enabled notification-listener access. Neither permission/service is requested.** The card is therefore hidden on this TV; it is not a working cross-app Now Playing feature here. The privileged rendering path was not verified on a privileged device. No watch history is fabricated, no media content scraped and no metadata stored. [Android's documented session access restrictions](https://developer.android.com/reference/android/media/session/MediaSessionManager#getActiveSessions(android.content.ComponentName)) explain this limit. Existing phone media dispatch remains unchanged and player-dependent.

## Verification evidence

| Area | Evidence and limits |
| --- | --- |
| Build and tests | Final app unit suite: 37 tests, zero failures/errors. Both APKs build; app lint zero errors/26 advisories, phone lint zero errors/11 advisories. After the small speed-guidance change the app was rebuilt/linted; no unnecessary repeat full-suite sweep. Separate regression fixtures compile. |
| Favourite cards | File hash identical to baseline, exact eight/order saved comparison, emulator visible 216 x 190 dp button bounds verified. Existing long-press/reorder handlers unchanged; unit RemotePress/order cases pass. No fresh handheld reorder test was claimed. |
| Visibility/theme/type | All three visibility modes were saved, activity restarted, screenshot taken and preferences restored on emulator. Vivid/Ruby/Bold/Extra Large passed fixed-card visibility. Real-weather header also passed full-card bounds at Extra Large/Bold. This is a representative 1080p matrix, not all combinations/devices. |
| Icons | Actual installed TV/phone adaptive and monochrome drawables rendered and visually reviewed on API 36 emulator. Physical HONOR launcher-mask acceptance remains outstanding. |
| Pairing/reconnection | User physically approved temporary RC review on the Xiaomi. Actual phone Android client/TLS code in emulator connected, a fresh client resumed the same token, stale lease was rejected, Home/Back passed. This is real TV transport evidence, not a fresh physical HONOR layout test or 30-minute lease soak. |
| Test cleanup | The instrumentation initially reported completion before its finally block ran. Audit caught the leftover RC review grant; it was removed through its specific TV Remove control, leaving BRP-NX1 untouched. The separate test runner now cleans up before Instrumentation.finish and compiles; that cleanup correction was not relive-paired a second time. No product pairing protocol change was needed. |
| Keyboard | Native EditText/InputConnection test passed cursor movement, middle insertion, selected/Unicode deletion, empty fields and repeated controls on emulator. Actual TV RC keyboard selection is preserved. New physical YouCine typing was not repeated; earlier user acceptance remains historical evidence. |
| Speed Test | One explicit bounded Xiaomi run: 74.1 Mbps down, 75.0 Mbps up, 46.2 ms median HTTP latency to Cloudflare. No ISP capacity/calibration claim. Final guidance is D-pad focusable and scrolled into view on TV; measurement code is unchanged. |
| Weather | Deterministic isolated Android tests passed off/no-request, cache, repository reload, hourly throttle, offline fallback, stale cutoff and removal. Live Open-Meteo search/current conditions and Standard/Extra Large header screenshots passed on emulator. Physical TV manual-city setup/network/offline acceptance remains. |
| Media | Ordinary emulator permission denial verified; unsupported card hidden, no intrusive access requested. Live third-party metadata display is not verified or claimed on Xiaomi. |
| Download | Actual Xiaomi entry, inactive QR message, D-pad instruction scrolling and Return to pairing observed. No public QR target exists to validate. Easy Setup wiring compiled; complete handheld wizard acceptance remains. |
| Startup | Debug launches immediately after updates reported roughly 4.26–4.66 s cold and one 2.859 s warm activity display. A later HOT bring-to-front wait was 33 ms; its TotalTime repeated an old display time and is not a fresh startup benchmark. No matched baseline, profile/thermal benchmark or startup regression fix is claimed; cold-start responsiveness remains an acceptance item. New optional weather adds no startup network wait when off. |
| Home/security | Actual default stock Home, selected keyboard and enabled accessibility service rechecked. No test UiAutomation was attached to Xiaomi; emulator automation used DONT_SUPPRESS_ACCESSIBILITY_SERVICES. No root, bypass or silent permission changes. |
| Casting | Corrected icon and receiver guidance observed. Receiver installation/enabled evidence carries from Phase H. Actual casting playback remains unverified. |

Screenshots, retained locally under `releases/review/phase-i/`: `final-home.png`, `checkpoint-A-cast.png`, `app-icons.png`, `visibility-0.png` / `1.png` / `2.png`, `vivid-ruby-bold-xl.png`, `weather-live-emulator.png`, `weather-bold-xl-emulator.png`, `download-dpad-tv.png`, `speed-result-tv.png`, `speed-guidance-dpad-tv.png`. Weather and icon images are emulator evidence; Home/download/speed/Cast are actual Xiaomi screenshots. Pairing diagnostics contain private LAN information and are retained only under ignored local diagnostics.

## Acceptance and release gates

Ready for **private user acceptance**, with explicit limitations above. Please review Cast icon, all three wallpaper visibility choices, both app icons, actual-phone reconnect/typing, manual weather setup/offline view and everyday startup/navigation. Keep stock Home as default until separately approved. Real casting, second-device/reboot/long-session testing, complete contrast/resolution matrix, production signing and dependency/distribution review remain before public 1.0. No final 1.0 release, merge or publication is declared.

Last usage review: 80% short-window and 36% weekly allowance remaining. No reset credit was used.
