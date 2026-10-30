# Release readiness — 1.0 gates

Current work is a **Phase B development preview**, not a feature-complete 1.0 release. The approved Home is preserved. See WALLPAPER-SELECTION-CHECKPOINT.md and PHASE-B-POLISH.md for evidence. Keep draft PR #2 unmerged, repository visibility unchanged and stock Home as default.

## Core and device acceptance

| Area | Implemented / verified | Remaining release gate |
| --- | --- | --- |
| Core launcher | Dynamic installed-app discovery, cached icons, launch, discovery error/retry, favourites, direct shelf ordering and short/long Select | Missing/disabled/removed apps, zero/one/many favourites, interrupted saves and longer sessions |
| Search | Installed-app filtering and explicit Down/Up result focus | IME, external keyboard/mouse, empty/error cases and second device |
| TV remote | Physical Xiaomi menu/short Select/Back/Home confirmed; new wallpaper flow tested through device input | Physical feedback on picker, second Xiaomi, repeated/cancelled keys and accessibility matrix |
| Home / recovery | Home intent registration, current default status, setup/recovery controls; stock Home unchanged | Both-box reboot/recovery and firmware-specific default-Home selection acceptance |
| Persistent settings | DataStore theme/motion/reduced motion/favourites/order; stable wallpaper IDs and default fallback | Migration/read/write failure cases and validated backup/restore |
| Errors | Retry/failure feedback and unknown wallpaper fallback | Storage exhaustion, corruption and external-provider failures when imports are implemented |

## Wallpaper and appearance

- [x] Preserve original mountain default, large cards, icons, glass and Home layout.
- [x] Original bundled collection; asset paths, exact generation prompts and provenance in WALLPAPER-ASSETS.md.
- [x] Dedicated thumbnail previews, Home preview, explicit Apply/Cancel and persistent selection.
- [x] Background bounds-first decoding, serialized full-image loads, one cached backdrop and precomposited scrims; no live blur.
- [ ] **Feedback gate:** pause after the first usable selector is installed/tested. Do not implement the following personalisation increments before feedback.
- [ ] SAF local/USB imports; JPEG/PNG/WebP checks; unsupported/corrupt images; private copies or persistent grants; bounded cache and storage errors.
- [ ] Rotation enable/disable, participating scenes, 5/15/30 minutes and 1/24 hours; lifecycle-bound scheduling with honest inactive-launcher timing.
- [ ] Instant/Crossfade/Gentle Slide with motion settings and GPU/memory verification; no decorative background service.
- [x] Existing Cinematic/Minimalist theme and motion controls remain functional.
- [ ] Glass transparency, a small system/licensed font selection, live appearance previews.
- [ ] Confirmed Reset to Defaults with clear scope; never unexpectedly erase favourites or imported content.
- [ ] Per-profile appearance after launcher profiles exist.

## Performance, accessibility and distribution

- [x] Builds, unit tests, lint, device screenshots and raw profiling records for implemented increments.
- [ ] Minimal glass/scroll regression remains an investigation gate. Compare equivalent geometry/order, warm-up and input pacing; distinguish thermal/trace/background variance from a proven fix.
- [ ] Long-session/thermal, low-memory/process recreation, 1080p/4K and both-box compatibility. No universal 60fps claim.
- [ ] Complete wallpaper preview peak-memory/decoding acceptance; ensure focus does not trigger full-image decoding.
- [ ] TV-distance typography, font scaling/high contrast, reduced motion, semantic selection and clear focus on both devices. System accessibility changes require explicit approval.
- [x] Increasing development version codes, working/rollback APKs and SHA-256 records; no application data clear.
- [ ] Production signing key/secure backup; debug-key builds are not public release artifacts.
- [ ] Transitive dependency/licence/notice audit and current dependency assessment before distribution.
- [ ] Final installation, same-key rollback, recovery, privacy/permissions and reproducible signed-release documentation.
- [ ] User approval before merging PR #2, public release or repository visibility changes.

## Broader master specification still incomplete

Do not silently label these complete or remove them from 1.0 scope: app hiding/custom labels/icon customisation and configurable rows; launcher profiles; local file/media browsing and permitted operations; local watchlist/provider search; genuine user-initiated network diagnostics/history; versioned validated configuration export/import with missing-app/resource handling. Weather/Continue Watching require supported optional integrations. Optional voice, paired phone companion, SMB and advanced integrations need separate design/security/device acceptance.

Decide the release scope explicitly before declaring 1.0 feature-complete. No fake integrations, protected-service scraping, device account assumptions or unauthenticated companion server.

**RC Assist stays future and optional:** permission-based actions and a secure Gemini Flash backend, no APK credentials and no implementation here.
