# Phase B Back navigation and performance fix — 30 October 2026

## Back root cause and verified policy

On Xiaomi MiTV-AFKR0 / Android 11, the previous `BackHandler(section != "Home")` was disabled at Home. Android's default Back implementation finished MainActivity. With YouTube's task behind RC TV, the system resumed that existing task and showed its account picker; RC TV did not issue a new YouTube launch intent.

Evidence: reproduce YouTube → reopen RC TV → Back; system events show `wm_finish_activity` for RC TV (`app-request`), then `wm_set_resumed_activity` for YouTube. The user reproduced this with the physical remote. After the fix, profile-only public key callbacks recorded ordinary `KEYCODE_BACK` (4), down/up, without repeats, and the user confirmed Back no longer opens YouTube. The first raw getevent attempt produced no usable data, so it is not treated as key evidence.

The Compose BackHandler is now always enabled: non-Home sections return Home; Home consumes Back and stays open. We did not change activity/task flags, package affinity, external app intents or default Home selection. Physical Home still opens the stock Xiaomi/Google TV launcher. Existing Android settings and Home recovery controls remain available. Standard Android IME handling precedes the activity callback; a future app context menu must handle its own dismissal before this root navigation policy.

`scripts/verify_tv_back.py` passed 20 device checks in each of the debug and profile builds: normal Leanback launch/Back; actual YouTube tile launch; YouTube Back return; five consecutive RC Home Back presses; Search, Apps, Library and Settings with Back to Home; physical-Home-equivalent key to stock launcher; reopening RC TV and three further Back presses. The user independently confirmed physical Back and Home behaviour. No forced stock-launcher change or task-clearing workaround was used.

## Profiling method

Added an explicit `profile` variant: release-derived, **non-debuggable**, shell-profileable, unminified, signed with the existing development key. It is a development profiling artifact, not a production/signed release. Debug and normal release disable the counters. Profile-only counters record wallpaper/shell/tile composition, wallpaper decoding and icon rasterisation; public Activity key callbacks log Back events; no keyboard text or app data is logged.

Before and after used the same APK configuration, same hardware, Cinematic wallpaper, eight favourites, workload and motion mode. Installed each APK with `-r` and explicitly precompiled only RC TV using `cmd package compile -f -m speed`. One unmeasured 24-key pass warmed drawing/focus paths, then two measured passes used three repetitions of four Right and four Left events, with a device-side 300 ms sleep after each input command. Actual input intervals include Android input-command overhead. `gfxinfo` counters for RC TV were reset between passes; **device logcat buffers were never cleared**. No screenshots/UI hierarchy dumps were taken inside the measured passes.

Perfetto recorded scheduler, gfx, view, input and ART slices locally. The 30-second trace covers the first measured pass; the second is an untraced confirmation pass. Both before/after follow that same schedule. Official Perfetto v59.1 trace_processor analysed files locally; traces were not uploaded. Because firmware trace metadata omits some process names, analysis resolves app threads through the recorded app PID, not a name-only join.

## Findings before changes

- Repeated wallpaper decode and icon allocation were not the steady-state cause: counters showed two wallpaper compositions, **one decode and seven icon rasters** across warm-up plus both passes.
- Balanced animated rail width was read in the whole screen's composition: **554 shell compositions**, versus 43 in Minimal. Even with fixed shelf reservation, the rail also repeatedly remeasured its labels. Balanced trace averaged 1.782 ms in Recomposer recompose and 1.523 ms in measure/layout slices.
- Static wallpaper rendering drew the image plus two full-screen translucent scrims every frame. Drawing/buffer-queue slices were significant: Balanced DrawFrame averaged 8.018 ms and queueBuffer 2.914 ms. These wall-time slices include waits, overlap and are not additive CPU costs; they do not establish a driver defect.
- Cold/debug overhead mattered: initial release-like precompiled startup was 784 ms; subsequent baseline starts were 651 ms (Minimal) and 561 ms (Balanced), versus the earlier unrelated debug runs around 3–3.7 seconds. The old 39.5%/70.5% short debug percentages are **not** the before values in this comparison.

## Changes, preserving the visual design

1. Isolate focus/rail state in CinematicNavigation. Clip/draw the animated rail width against a fixed layout instead of remeasuring the screen or navigation labels on every animation frame.
2. Read animated button tint in drawing, not in composition. Keep existing scale, timing, theme, glass shapes and interactions; no new effect added.
3. Render the same horizontal and vertical wallpaper scrims once into the bounded bitmap on IO, then draw one wallpaper image. The private temporary source bitmap is released; retained pixel storage remains about 6 MiB. Minimalist continues without wallpaper decoding. App icon cache/loading policy is unchanged.

The original artwork file and branding are unchanged. Actual-TV screenshot inspection preserved the atmosphere; a text-free sky-region comparison averaged less than 0.5/255 absolute difference per RGB channel, consistent with rasterisation rounding. The TV's existing high-contrast text setting was not changed.

## Matched frame results

| Motion / pass | Frames before → after | Jank before → after | p50 ms before → after | p90 ms before → after |
| --- | --- | --- | --- | --- |
| Minimal / 1 (traced) | 204 → 204 | 23.53% → 1.96% | 9 → 7 | 20 → 10 |
| Minimal / 2 | 205 → 204 | 12.20% → 0.49% | 7 → 7 | 18 → 9 |
| Balanced / 1 (traced) | 381 → 381 | 12.86% → 1.05% | 10 → 8 | 18 → 10 |
| Balanced / 2 | 382 → 382 | 11.52% → 0.52% | 9 → 7 | 17 → 10 |

Balanced shell compositions fell **554 → 5**; Minimal fell 43 → 5. Wallpaper remained two compositions/one decode; icon rasters remained seven and tile compositions 151. In the 30-second Balanced traces, main-thread CPU fell 2372.77 → 1396.81 ms; Recomposer recompose average 1.782 → 0.253 ms, measure/layout 1.523 → 0.629 ms, and DrawFrame 8.018 → 6.156 ms. RenderThread CPU was broadly unchanged (1891.92 → 1880.01 ms), while queueBuffer wall time dropped 2.914 → 1.229 ms.

After-change precompiled cold starts measured 543 ms (Minimal) and 560 ms (Balanced). PSS: Minimal 98,786 → 97,240 KiB; Balanced 94,462 → 100,715 KiB. Memory varies by warm-up/GC and no general memory reduction is claimed. No long-session leak test or thermal-control experiment was performed.

These are two sequential samples per mode on one device, with synthetic input overhead and no R8/baseline-profile production setup. They demonstrate a large improvement for this workload, not a universal 60fps guarantee. Cinematic intensity, all-app grids and long-session behaviour need broader acceptance. Minimal remains the original/default setting; no extra expensive effects were enabled.

## Preservation, builds and artifacts

Both debug and profile assemble, lint and unit tests pass. **Seven unit tests in each variant; lint 0 errors / 7 existing advisory warnings in each.** Both variants passed the 20 navigation checks. One offline Wi-Fi ADB connection required reconnecting the session; the TV was not restarted and data was not cleared.

Restored the user's Minimal motion setting after profiling. The entire DataStore file before/after testing is **byte-for-byte identical**, preserving all eight favourites, theme, motion and reduced-motion settings. New version: 0.2.1-cinematic-fixes, versionCode 3. The final TV review build is the non-debuggable profile variant; the ordinary debug APK is also provided for development. Both use the same development signing key and preserve app data.

- `releases/RC-TV-Launcher-0.2.1-debug.apk`: SHA-256 `E48F19776A229E9C6B3E37B21808D95E22DD149FD86A528E20BE71FF64BA2BDB`.
- `releases/RC-TV-Launcher-0.2.1-profile.apk`: SHA-256 `E36ED23B8BC2094922AE6EBF1F735D6A2AFC72AEF58541E89FE4C8E84A829888`.
- Device screenshots, task stacks, raw logs, traces, preference verification and per-pass reports: local, Git-excluded `releases/diagnostics/back-performance/`.
- Previous Phase B debug APK is preserved as `releases/rollback/RC-TV-Launcher-Phase-B-34f16b8.apk`; the exact Phase A APK also remains intact.

If a version downgrade is refused while the non-debuggable profile variant is installed, first install the **same-version** 0.2.1 debug APK with `install -r`, then request the previous debug version with `install -r -d`. Do not uninstall or clear data to work around a refusal. That rollback sequence is documented, not exercised during this fix.

Repeat measurements with the matching profile APK already installed and precompiled:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
python scripts/measure_tv.py --adb $adb --serial 192.168.1.39:5555 --label review-minimal --motion Minimal
python scripts/verify_tv_back.py --adb $adb --serial 192.168.1.39:5555
```

The measurement script temporarily selects the requested motion setting; restore the user's original setting afterward. Context-menu interactions and RC Assist are recorded separately in [INTERACTION-ROADMAP.md](INTERACTION-ROADMAP.md), not implemented here. PR #2 remains draft and unmerged.

Method references: [Android task/Back lifecycle](https://developer.android.com/guide/components/activities/tasks-and-back-stack), [Compose performance](https://developer.android.com/develop/ui/compose/performance), [Perfetto tracing](https://developer.android.com/topic/performance/tracing), [official trace processor](https://perfetto.dev/docs/analysis/trace-processor).
