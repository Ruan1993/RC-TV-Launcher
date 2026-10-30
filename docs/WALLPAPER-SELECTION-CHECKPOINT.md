# Phase B — first usable wallpaper selection

## Scope and feedback gate

30 October 2026. Continue from the verified `837db04` / 0.2.7 checkpoint on `feature/phase-b-cinematic-glass`. This increment ends at the user's explicit feedback gate: **install/test the first usable wallpaper selector on the Xiaomi, capture screenshots and pause before further personalisation**. Do not implement import, rotation, transitions, transparency/font choices or Reset to Defaults before feedback. RC Assist remains future/optional.

Started clean; the live branch and open draft PR #2 both pointed to `837db04`. Original 0.2.7 debug/profile SHA-256 matched the prior report. Exact rollback copies preserved as `releases/rollback/RC-TV-Launcher-837db04-{debug,profile}.apk`, alongside earlier checkpoints. No data clear, favourite reset, Home-default change, merge, public release or repository-visibility change.

Usage at start: 38% used in the five-hour window, 23% weekly (62% / 77% remaining). Current preferences snapshotted before testing (536 bytes), including eight favourites, latest user order and original appearance choices.

## Completed increments

### Navigation details — `ee89715` / 0.2.8

Removed only the bottom sidebar RC TV label; retained the official header branding/tagline. Replaced the target-like Settings mark with an original eight-tooth cog and central hole. Glass material, focus springs, Home cards, icons, wallpaper and layout are preserved. Builds/tests/lint passed; actual-TV navigation screenshots reviewed. Verified increment committed and pushed separately.

### Bundled wallpaper selection — 0.2.9, version code 11

- Dedicated Settings → Wallpaper section, with thumbnail selection and Current/Selected states.
- Original mountain stays default; two original generated landscapes, Tidal dusk and Desert afterglow, join it. No stock photographs downloaded. Exact prompts, source provenance, paths, encoding and redistribution caveats are in `WALLPAPER-ASSETS.md`.
- Preview shows the actual Home layout, with application launching disabled during preview and clear Apply wallpaper / Cancel preview controls. Back returns to the picker without saving; leaving the activity discards an unsaved preview.
- Apply persists a stable wallpaper ID to DataStore. Unknown/missing IDs fall back to mountain. No eager preference migration or unrelated preference rewrite.
- Current Cinematic/Minimalist behaviour is preserved. Minimalist keeps a plain background; a selected wallpaper is saved for Cinematic. Preview explicitly shows the scene without changing the theme preference.
- Full decoding runs on IO, reads bounds first and caps decoded dimensions; existing scrims are precomposited. A mutex prevents overlapping full-image decodes, with cancellation checks before publication. Only one full backdrop is cached; small dedicated thumbnails avoid decoding three full wallpapers for the picker. Published bitmaps are not manually recycled.
- No transition, timer, import permission, network image source or background service added. Selection uses an immediate change; transitions await feedback/performance work.

## Device verification

Xiaomi MiTV-AFKR0, Android 11, network ADB. Debug/profile assemble and lint pass (zero errors, seven existing advisory warnings); 14 tests per variant including wallpaper default/unknown-ID and stable-ID cases.

Actual-TV checks passed: all three thumbnail assets render; coast Home preview; preview/Back leaves the preference file byte-identical; remote Cancel preserves the applied scene; Home/background discards an unsaved preview; coast and desert each persist after process restart; original mountain restored. Current favourites, order, theme and motion are compared with the pre-test snapshot. Original motion was temporarily changed only for matched profiling and is restored at the end.

An initial pointer-to-remote picker test exposed focus remaining on the navigation rail. The picker now requests keyboard input mode and focus on its selected thumbnail; Down explicitly reaches Preview and Up returns to the selected scene. Failed setup attempts are not accepted as passed tests.

The Settings group moved Motion further down the scroll surface. The profiling script now finds the actual Motion row using the unique Balanced label, scrolls when necessary, and avoids confusing the two Cinematic labels (theme versus motion).

Screenshots, preference snapshots, memory/frame data and traces are local under ignored `releases/diagnostics/wallpaper-selection/`. Working and rollback APKs remain local under `releases/`.

## Release preparation and continuation

`RELEASE-READINESS.md` now covers core functionality, remote/device compatibility, persistence, wallpapers/appearance, errors, performance, accessibility/focus, signing/versioning and install/rollback/recovery. It explicitly lists incomplete master-spec features and does not call this 1.0 complete.

After user feedback: SAF local/USB image import with validation/downsampling/access recovery; lifecycle-bound rotation and participating scenes; performance-gated Instant/Crossfade/Gentle Slide; transparency, limited fonts and confirmed Reset to Defaults. Broader profiles, media/files, watchlist/search providers, genuine diagnostics and validated backup remain unresolved master-spec scope. No RC Assist implementation.

PR #2 must stay draft and unmerged. The exact next step is the user's review of the first wallpaper collection, picker readability, preview behaviour and remote feel.

## Matched Minimal performance investigation

Precompiled non-debuggable profile builds, identical eight-favourite order and Home geometry, one warm-up and two 24-key passes at 300ms device-side spacing; first pass traced, second untraced. No concurrent TV tests. Sequential samples are not thermally controlled.

| Build | Frames, passes 1 / 2 | Jank | p50 | p95 |
| --- | --- | --- | --- | --- |
| Existing 0.2.7 | 249 / 249 | 4.02% / 2.81% | 7 / 7ms | 15 / 14ms |
| Wallpaper 0.2.9 | 249 / 250 | 6.02% / 4.80% | 8 / 7ms | 16 / 16ms |

Cold start 557ms for the new profile. PSS baseline 105,950KiB versus 105,606KiB (both about 103MiB). Both runs retain wallpaper=2, shell=5, tiles=169, decodes=1, icons=7. No focus-triggered wallpaper decode or broad recomposition has returned.

Local Perfetto v59.1 analysis (no trace uploads): Recomposer recompose average 0.378 → 0.388ms; measure/layout 0.894 → 0.904ms; DrawFrame 5.874 → 6.171ms; queueBuffer 1.425 → 1.566ms. Recorded main-thread CPU over the trace 969.25 → 1017.70ms; RenderThread 1154.27 → 1198.03ms. These wall-time slices overlap/include waits and are not additive GPU or CPU costs. Android reports no slow bitmap uploads in the second new pass; its extreme GPU histogram bucket is not treated as a real 4.95-second frame.

**The Minimal regression is not fixed.** The measured increase is about two percentage points in these passes. Stable composition/decode counts and small main/layout changes point away from repeated image loading; the additional cost is concentrated more in frame drawing/buffering, but the trace does not isolate one definitive cause. Glass/scroll overdraw and scheduling/GPU variance require further controlled profiling before 1.0. Do not claim performance neutrality or a universal frame rate. No further visual effects were added to hide this limitation; pause at the requested wallpaper feedback checkpoint.

## Working APKs and rollback

0.2.9-wallpaper-selection, versionCode 11. `releases/RC-TV-Launcher-0.2.9-debug.apk` SHA-256 `567CD4EF24FE41B8CE6826C762422AD318807F765920FA79307D95C5CD32EFD1`; profile SHA-256 `54751C2D8F7C58A13B74D41D4223B5A575B598FA91D9DD7B1842F806CF6D2378`. Same debug signing identity as the existing installation; not a production signing key. Exact 0.2.7 and 0.2.8 rollback builds are preserved.

For a downgrade from profile: first install the same-version 0.2.9 debug with `-r`, then the preserved 0.2.7 debug with `-r -d`; optionally install 0.2.7 profile at its matching version. Never uninstall or clear data if firmware refuses. The optional wallpaper key is ignored by old builds; favourited packages and all existing keys are retained. This increment did not exercise a downgrade, relying on the already-verified same-key route documented in PHASE-B-POLISH.md.

The final 20 profile navigation checks passed: launch/Back return from YouTube, every launcher section, repeated root Back, stock Home and reopening RC TV. An initial ADB connection closed before the script began; reconnecting allowed the full run. No failed setup is reported as a passed test.

Three debug-build desert preview/cancel PSS samples: 144,351 / 127,969 / 127,662KiB (about 141 / 125 / 125MiB). These include the picker, thumbnails and transient old/new backdrops; they are short samples, not proof of absence of leaks or the complete low-memory release gate.

Final decoded preferences match the original snapshot for every existing key; only the new wallpaper key is present, set to mountain after testing. All eight favourites and their exact latest order are retained. Original Cinematic theme/motion and reduced-motion setting are restored. The final profile is left on Settings → Wallpaper for user feedback, with mountain still applied.

Unobstructed Home background comparison against the previous 0.2.7 actual-TV screenshot (x400–1850, y180–380): mean absolute RGB difference [0.0, 0.0, 0.0]. This checks the unchanged mountain/scrim rendering in that region; it is not a whole-screen or performance equivalence claim. Final post-install profile launch was 708ms; the warmed profiling cold-start sample above was 557ms.

Final allowance check: 60% used in the five-hour window and 27% weekly (40% / 73% remaining). Work stops at the requested feedback gate, not because allowance is exhausted.
