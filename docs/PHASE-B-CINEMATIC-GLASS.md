# Cinematic Glass — first increment

This records the initial preview and its original debug measurements. The subsequent [Back/performance fix](PHASE-B-BACK-AND-PERFORMANCE.md) supersedes the performance status below with matched release-like results, the root Back policy, current APKs and updated rollback instructions. The visual direction is preserved. See [upcoming interaction requirements](INTERACTION-ROADMAP.md) for the future context menu and RC Assist.

## Checkpoint and scope

Phase A PR #1 was reviewed at `e051f51db9162a3d7f3ce792cbc8935ff74a9911`, with no review comments or configured remote checks. The clean local branch passed a fresh 53-task build/test/lint run and five tests. The user confirmed physical-remote functional acceptance. PR #1 was merged, with an expected-head SHA guard, as `af261a9000f5b3c62f42216b9498d2172b9591f1`. This preview starts from that main commit on `feature/phase-b-cinematic-glass`.

This is the first reviewable design-system increment, not completion of Phase B. The native screen now separates UI into the `ui` package while MainActivity retains platform/lifecycle actions. Reusable glass, focus button, navigation and app-tile components support subsequent changes.

Implemented:

- Original cinematic wallpaper, bounded bundled resolution, IO decoding, screen crop and readability scrims; provenance in ASSET-INVENTORY.md.
- Dark translucent glass surfaces, controlled borders, icon-led app cards, spacious wallpaper-led Home and a horizontal favourites shelf. Apps and Search retain the full application grid.
- Collapsible icon navigation with accessible names; reserved width keeps the content shelf stable while the rail expands.
- Interruptible spring focus scale and lightweight radial glow in Balanced/Cinematic modes. Minimal uses immediate states without scaling, glow or rail tweening. No delayed input handlers or continuous animation loop.
- Persistent Cinematic/Minimalist appearance and Minimal/Balanced/Cinematic motion, with the existing reduced-motion preference overriding intensity. Missing/unknown values safely fall back to Cinematic appearance and Minimal motion.
- Existing discovery, real error/retry state, favourites, cached installed icons, app launching, search, Back and Home recovery controls retained. No new permissions or network services.
- RC TV with “Your world. In focus.” in smaller typography. Wallpaper collection captions are separate from the brand.

## Incremental follow-up

Not implemented yet: user-selected wallpaper through SAF, multiple wallpapers/rotation, crossfades/parallax, staggered rows, opening animation, section transitions, custom app ordering/hiding, stable per-section focus restoration and capable-device blur. The current glass treatment is a translucent fallback on all devices, including Android 11; it is not backdrop blur. Do not enable additional expensive choreography until the frame budget improves.

## Verification and performance

Seven unit tests pass (five Phase A regressions plus preference validation/fallback tests). Debug assemble and lint pass; lint retains seven advisory warnings (five upgrade notices, TV banner size, Android 16 orientation). No warnings suppressed.

Actual Xiaomi MiTV-AFKR0 / Android 11 checks: successful cold starts; 25 discovered apps with no discovery error; existing eight favourites retained; Search via D-pad and text filtering; theme/motion persistence verified after restart and in app-private DataStore; screenshots taken through ADB. No default Home change or stock-launcher disable. The TV's existing high-contrast text setting is enabled, so screenshots show Android's text outlines; it was not changed.

Also verified YouTube unpin reduced Home from eight to seven favourites; repinning from Apps restored the original eight. The app tile launched the real YouTube activity, RC TV resumed successfully, Library rendered its honest unavailable-feature description, and Back returned Home. Final exit history shows intentional restart/install exits; no new RC TV fatal exception was captured. The installed preview is left in Cinematic appearance with Minimal motion and the user's favourites restored. APK SHA-256: `E3BF76BD39366C40DEE99C201165915574C2F1DE0E0FADE9782003BE4BA356EE`.

Performance is not yet acceptance-ready for animated modes. Recorded debug-build samples:

| Observation | Result |
| --- | --- |
| Cold launches | about 3.0–3.7 seconds |
| Cinematic appearance PSS | about 114–122 MiB in collected samples |
| Minimalist cold PSS | about 109 MiB |
| Balanced paced 24-key sample | 291 rendered frames, 70.45% janky, median 21 ms, p90 73 ms |
| Minimal paced 24-key sample | 172 rendered frames, 39.53% janky, median 14 ms, p90 34 ms |

The initial unpaced 19-frame sample was inadequate and heavily janky; it prompted removal of rail-driven shelf reflow and blurred tile shadows. The table records subsequent paced input at 300 ms intervals. These are short, sequential, unprofiled debug sessions with different warm-up states, not a controlled benchmark or a 60 fps claim. Historical Phase A counters were 31.28% janky and about 76 MiB PSS, also from a different workload. Firmware GPU percentile outliers include sentinel-like values and are not treated as reliable GPU timings. Minimal is the safe preview default; animated modes remain opt-in. Release/profileable measurements, long-session memory and physical-remote responsiveness remain gates before expanding effects.

Local screenshots and raw diagnostics (excluded from Git): `releases/diagnostics/phase-b-home.png`, `phase-b-search.png`, `phase-b-settings.png`, `phase-b-minimal-home.png`, frame/memory reports and ADB logs. The Home screenshot is from the installed preview, not a mockup.

## Rollback

The exact device-tested Phase A APK is preserved locally as `releases/rollback/RC-TV-Launcher-Phase-A-device-tested.apk`, SHA-256 `8B8E07216AD0528EA5E983FF7B1421B52BBC7E398415978A50FC70F869B68A40`. A separately rebuilt Phase A APK is also preserved. The preview is `releases/RC-TV-Launcher-0.2.0-cinematic-preview.apk` (versionCode 2).

To return to the same-signed debug Phase A APK without clearing preferences, from this project in Windows PowerShell:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s 192.168.1.39:5555 install -r -d 'releases/rollback/RC-TV-Launcher-Phase-A-device-tested.apk'
& $adb -s 192.168.1.39:5555 shell am start -W -n za.co.rc.tv/.MainActivity
```

`-d` requests a debug downgrade from versionCode 2 to 1. If firmware refuses, stop and inspect the error; do not uninstall or clear data as a workaround. Phase A ignores the new preference keys. For source rollback, main remains the merged Phase A checkpoint; build it from a clean checkout. Do not reset/force-push the Phase B branch. The stock Xiaomi launcher remains the default throughout.
