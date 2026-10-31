# Release readiness — 1.0 gates

Current work is a **Phase D development preview**, not a feature-complete 1.0 release. The approved Home is preserved. See WALLPAPER-SELECTION-CHECKPOINT.md and PHASE-B-POLISH.md for evidence. Keep draft PR #2 unmerged, repository visibility unchanged and stock Home as default.

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
- [x] Original Home, navigation and first selector approved. Current high-efficiency sprint supersedes the old feedback pause; see SPRINT-WALLPAPERS.md.
- [x] System document-picker import; JPEG/PNG/WebP validation, normalized private copies, bounded dimensions/source size/count, corrupt input errors and confirmed removal.
- [ ] Physical picker/USB provider acceptance: this Xiaomi exposes only Google’s DocumentsStub; no working picker was installed or substituted. Import pipeline tested separately on device.
- [x] Rotation enable/disable, participants and 5/15/30 minute, 1/24 hour intervals persist; Home/RESUMED scheduling, overdue return handled once, no service.
- [ ] Longer real-time rotation/power/lifecycle sessions across devices.
- [x] Instant/Crossfade/Gentle Slide implemented; short local transitions, Minimal/reduced motion instant.
- [ ] Final-release transition GPU/memory acceptance; prior Minimal regression reserved for dedicated optimisation.
- [x] Existing Cinematic/Minimalist theme and motion controls remain functional.
- [ ] Glass transparency, a small system/licensed font selection, live appearance previews.
- [ ] Confirmed Reset to Defaults with clear scope; never unexpectedly erase favourites or imported content.
- [ ] Optional profiles/per-profile appearance are outside the 1.0 critical path.

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

Prioritise the 1.0 critical path: reliable everyday launch/return, remote focus, wallpaper/appearance persistence, error recovery, targeted performance optimisation, production signing and installation. Optional user profiles and RC Assist are outside that path. Scope other broader capabilities separately before declaring 1.0 feature-complete. No fake integrations, protected-service scraping, device account assumptions or unauthenticated companion server.

**RC Assist stays future and optional:** permission-based actions and a secure Gemini Flash backend, no APK credentials and no implementation here.

## Phase C follow-up gates
- [x] Pending Back fix verified/pushed; supported Home-selection screen verified without changing defaults.
- [x] Optional offline keyboard core/preview and native image browser implemented; see PHASE-C.md.
- [ ] Real IME Search entry/Backspace/Back and exact Gboard restoration now verified with user approval; cross-app/password/numeric/editor-action and physical-remote acceptance remain.
- [ ] Xiaomi Home role/resolution mismatch: actual Home selection/reboot requires explicit user approval and firmware acceptance.
- [ ] Android 13/14 image permissions/selected access, USB providers, revoked permissions, large libraries and second-device acceptance.

## Phase D follow-up gates
- [x] Keyboard cursor controls/repeat and native editor insertion, selection, empty-field and Unicode checks.
- [x] Optional Android companion: pinned TLS, TV approval, authenticated RC TV-only controls and disconnect/revocation.
- [ ] Physical-phone UI/discovery/router/lifecycle acceptance, longer sessions and security review before production distribution. Automatic discovery is not yet verified on this LAN; manual address fallback implemented.
- [ ] Google Cast sender discovery/playback/stop acceptance. Installed receiver alone is not functional-casting evidence.
- [x] RC Assist contract/security architecture documented; no assistant/backend implementation this sprint.
