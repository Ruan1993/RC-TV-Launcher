# Cinematic Glass 2.0 — Home review checkpoint

## Scope and stopping point

The user explicitly requested a pause after the first substantial Home redesign, before expanding the visual changes across the application. This checkpoint completes that Home increment and stops for design feedback. It does not claim completion of the context menu, premium Settings or personalisation features.

Started from clean, remote-synchronised `638d2af` on `feature/phase-b-cinematic-glass`. Reviewed draft PR #2 and the Back/performance report. Preserved exact working 0.2.1 debug/profile APKs under `releases/rollback/RC-TV-Launcher-638d2af-{debug,profile}.apk`. Account usage was available through the host and checked before work and during verification; no allowance was guessed or inferred from token counts.

## Completed Home design

- Preserved the original mountain artwork, precomposited wallpaper scrims, official RC TV branding and “Your world. In focus.” tagline.
- Removed Quiet horizons, Blue Hour and Cinematic Collection headings from the runtime UI. No replacement decorative wallpaper heading.
- Dedicated Home cards: 232dp footprint, 216 × 190dp glass button, versus the previous 172dp footprint / 122dp height. Icons are 80dp versus 44dp. Labels use 19sp, with more breathing room and clear single-line truncation.
- Rounded 26dp edges, tonal translucent material, an internal reflection, layered edge lighting and a luminous focus outline. Brushes are cached; no backdrop blur, blurred shadow, new ongoing animation or fullscreen effect was added. Existing interruptible motion scales are retained.
- Quieter navigation highlighting and slightly larger navigation marks. Rail state remains isolated; width and tint still update during drawing without remeasuring the screen.
- Home has no permanent Pin/Unpin controls. **Temporary Apps/Search controls remain available**, preserving favourite management while the long-press menu awaits its own tested increment.
- PM icons are now cached by package and requested raster size. Home requests 80dp, bounded to 192px; the existing cache remains bounded to 2 MiB. Compact Apps/Search icons retain their previous requested size.

The strip remains a lazy, package-keyed horizontal list and adapts to the filtered favourites count. The actual user's eight favourites were preserved; broader zero/one/many visual acceptance remains future testing. Settings and Search have not received a broad visual redesign at this checkpoint.

## Actual Xiaomi testing

Xiaomi MiTV-AFKR0 / Android 11, network ADB. Debug and profile assemble, unit tests and lint pass: **seven tests per variant, zero errors / seven existing advisory lint warnings**. Twenty Back/navigation checks passed on the profile Home redesign and on the final debug APK, covering YouTube tile launch and Back return, repeated root Back, every launcher section, stock Home and reopening RC TV.

Initial UI capture encountered the normal system screensaver, which was dismissed with ordinary Wake/Back input; no timeout or power setting was changed. A timed-out/overlapping setup attempt was discarded before accepting performance measurements. Final samples were run sequentially with no concurrent TV tests.

The complete DataStore file is byte-identical before/after testing: eight favourites, theme, motion and reduced-motion preferences are retained. The final TV review build is 0.2.2-glass-home-review, versionCode 4, profile variant, with Cinematic appearance and the original Minimal motion restored. Stock Xiaomi/Google TV remains the default Home. The TV's high-contrast text preference is unchanged.

## Performance observations

Used the existing warmed, precompiled, non-debuggable profile workload: three repetitions of four Right/four Left inputs, 300ms delay after each device input command, one warm-up and two measured passes. First pass traced, second untraced, as in the previous checkpoint. **The wider cards change scrolling distance and frame count**, so these are comparable navigation tasks, not identical rendering work or controlled hardware/thermal benchmarks.

| Mode / pass | Prior 0.2.1 frames / jank | Home 2.0 frames / jank | Home 2.0 p50 / p90 / p95 |
| --- | --- | --- | --- |
| Minimal 1 | 204 / 1.96% | 249 / 4.82% | 8 / 13 / 16 ms |
| Minimal 2 | 204 / 0.49% | 249 / 3.61% | 7 / 10 / 14 ms |
| Balanced 1 | 381 / 1.05% | 414 / 0.72% | 8 / 10 / 11 ms |
| Balanced 2 | 382 / 0.52% | 415 / 0.48% | 7 / 9 / 11 ms |

Cold starts measured 552ms (Minimal), 578ms (Balanced); the initial post-install launch was 762ms. PSS samples: 105,656 / 108,215 KiB (about 103 / 106 MiB), versus about 95 / 98 MiB previously. No leak or general memory improvement is claimed.

Both modes retained five shell compositions, two wallpaper compositions, one wallpaper decode and seven icon rasters across warm-up plus measurement. Tile compositions rose 151 → 169, with the wider shelf and additional scrolling/prefetch work. Minimal trace: Recomposer recompose average 0.394ms, measure/layout 0.836ms, DrawFrame 6.113ms versus 5.421ms in the previous Minimal trace, and queueBuffer 1.781ms versus 0.820ms. There is no return of the broad recomposition/image-loading fault; the increased Minimal cost is in the wider, more layered drawing/scroll workload. Balanced samples stayed near the prior range, but their lower percentages do not establish a universal advantage over Minimal.

Minimal's regression is a **known review limitation**, not hidden by the good Balanced sample. Do not add further expensive effects. After the user approves the material/layout, investigate reducing glass overdraw or caching a shared material before expanding animation. Long-session, thermal, all-app-grid, Cinematic-intensity, 4K and physical-remote feel remain acceptance items.

## Local review artifacts and rollback

- `releases/RC-TV-Launcher-0.2.2-profile.apk` — final TV review build.
- `releases/RC-TV-Launcher-0.2.2-debug.apk` — same-key development build.
- `releases/diagnostics/glass-2-review/home-focused.png` — actual TV, Balanced focus state.
- `releases/diagnostics/glass-2-review/final-home.png` — final installed build and restored preferences.
- Same directory contains raw traces, frame/memory reports, screenshots, logs and preference verification; all excluded from Git.

Rollback: first install same-version 0.2.2 debug with `adb install -r`, then downgrade to the preserved 0.2.1 debug with `adb install -r -d`. If desired, install the preserved 0.2.1 profile APK at that same version afterward. Keep the same signing key; do not uninstall or clear data if firmware refuses. Prior Phase A and Phase B rollback APKs remain intact. Downgrade was not exercised during this review.

## Continuation plan — after design feedback

1. Review actual glass reflections, icon/card size, spacing, sidebar and overall composition. Resolve the Minimal-mode cost before adding effects. Keep a clean pushed checkpoint while waiting.
2. Implement the separate long-press OK/Select context menu: Favourite, Move/Rearrange, App Information and Android-confirmed Uninstall. Test physical short/hold/repeat/cancel interactions, prevent launch after a handled hold, preserve focus and root Back, then remove the remaining Apps/Search Pin/Unpin controls. Persist ordered favourites with migration and explicit commit/cancel for rearranging.
3. Apply approved typography, spacing, grouped surfaces and focus components to premium Settings and Search. Test couch readability and remote traversal.
4. Add personalisation in small tested increments: bundled wallpaper selection, SAF local/USB imports with persisted grants and bounded decoding; lifecycle-bound rotation intervals; performance-gated Instant/Crossfade/Gentle Slide; typography and transparency controls with apply/cancel preview. No background service or online wallpaper service.
5. RC Assist remains architecture only: floating glass surface and future menu, secure Gemini Flash backend, permission-based actions, no APK API keys or AI integration. See INTERACTION-ROADMAP.md.

Draft PR #2 must remain unmerged. Do not start the context menu, Settings redesign or personalisation implementation until the requested Home design feedback arrives.

Final artifact SHA-256: debug 9E0966B0B531C6ACF58F725F2AE3FE282EAE28FE028D4929D169B57AD1FE5512; profile 365E36C821E8D813086F8B694E313DAC027C36E86806E092EAB46186B4801576. Final precompiled installed launch: 589ms. Final allowance check reached 1% in the five-hour window, so development stopped at this tested checkpoint and the requested design-feedback pause.
