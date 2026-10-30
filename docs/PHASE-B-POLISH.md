# Phase B — interface polish and application actions

## Checkpoint verification — 30 October 2026

Started clean on `feature/phase-b-cinematic-glass` at `f0f9d18`. GitHub's live branch and draft PR #2 both pointed to that commit. The previous checkpoint was complete. Existing 0.2.2 APK hashes match the Home review report; additional exact copies are preserved under `releases/rollback/RC-TV-Launcher-f0f9d18-{debug,profile}.apk`. PR remains draft and must not be merged.

Usage at start: 1% used in the five-hour window, 18% used weekly. Xiaomi connected at `192.168.1.39:5555`; installed checkpoint was 0.2.2 profile. Stock Google/Xiaomi Home remains default.

## Increment 1 — typography and Settings

Android high-contrast text was enabled (`high_text_contrast_enabled=1`). No application text outline or shadow styling was present. The user explicitly authorised switching this system setting off in this chat; it was changed to 0. This revealed inherited black text outside a Material Surface, corrected with an explicit light content colour.

Shared system sans-serif typography uses normal/medium weights, explicit line heights and restrained letter spacing. No font download or third-party asset. Settings now has separate Appearance, Motion and Recovery glass groups, more spacing and minimum 48dp controls. Home's existing cards and wallpaper are preserved.

Debug/profile builds, seven debug unit tests and lint pass. Actual Xiaomi Settings screenshots and D-pad traversal captured locally in `releases/diagnostics/premium-polish/`. Complete DataStore snapshot before/after is byte-identical (310 bytes); eight favourites and saved preferences preserved. No data clear, uninstall or default Home change.

## Scope retained for later

Wallpaper collections and downloads; USB/local SAF imports; adjustable rotation; selectable transitions; theme/font/transparency options; live appearance previews; optional RC TV profiles with user-chosen name/photo. No assumed YouTube account integration. RC Assist remains planned only, with permission-based actions and a secure Gemini Flash backend; no APK API keys or assistant implementation in this increment.

## Increment 2 — navigation and motion

Removed the unexplained RC avatar. Expanded rail retains icons beside labels; 24dp marks, cached layered glass material and illuminated edges. Width is read during drawing; rail focus remains locally scoped. Balanced uses high damping; Cinematic permits a small overshoot. Minimal snaps and no longer draws a focused Home halo. No blur or continuous animation. Debug/profile builds, unit tests and lint pass; actual-TV rail/focus screenshots captured and DataStore remains byte-identical. Performance comparison follows in final verification.

## Increment 3 — application actions (0.2.5 checkpoint)

Home, Apps and Search use the same physical Select/Enter handler. It tracks initial down, repeat, platform long-press timeout, cancelled release and focus loss; a handled hold never launches on release. The popup suppresses only events sharing the opening press timestamp. Accessibility long-click also opens options. Popup positioning uses actual tile bounds and clamps to the viewport; spring entrance is skipped in Minimal. Back closes the popup; leaving the activity closes panels.

Menu supports Favourite, Rearrange (when more than one favourite), Android App Information, and Android-confirmed Uninstall for non-system apps where user restrictions permit. Failure gives feedback; Android remains the final authority for uninstall restrictions. No silent uninstall API. REQUEST_DELETE_PACKAGES is declared; no broad package-delete permission.

Ordered favourites add an optional DataStore order key. Existing sets keep their original displayed order without an eager migration write; unavailable favourites remain preserved. Saving validates the entire membership inside the DataStore transaction to reject stale edits. Rearrange works on a local draft with explicit Save, Cancel and Back cancellation. Directional move is bounded; Down explicitly targets Save. Apps/Search Pin/Unpin controls were retained at this checkpoint pending physical remote acceptance; see increment 4 for their removal.

Twelve tests per variant pass, including five added tests for short/hold/repeat/cancel/focus-loss state and legacy/saved order resolution. Debug/profile builds and lint pass with zero errors and the same seven advisory warnings as before.

Actual Xiaomi checks: generated short/long Select opens the correct launch/menu path; App Information reaches the selected package's Android screen. Uninstall displays “Do you want to uninstall this app?” with OK/Cancel; cancelled with Back and the package remains installed. D-pad-only Rearrange/Cancel restores the original tile focus without writing; Save survives force-stop/relaunch. Original eight-package order restored through Save. Moving the tile now explicitly requests its new list position so lazy-list anchoring cannot hide it. Focus restoration also returns to keyboard input after pointer interaction. A physical-remote check was requested at this checkpoint and subsequently confirmed by the user (increment 4).

The original preferences remain unchanged in meaning. The additional optional favourite_order key records the original displayed order after Save testing; the favourite set and theme/motion/reduced-motion values remain intact. At start the TV actually had Cinematic motion selected (the historical report's Minimal setting was not assumed).

## Increment 4 — user-requested direct shelf rearranging

The user physically tested the remote and reported “Everything works great”, specifically confirming menu/short press/Back/Home behaviour, but requested arrows and visible card movement instead of the separate rearrange dialog. Physical key logs include Select down/repeat and cancelled release when the popup takes focus. The earlier pending physical-menu gate is satisfied.

Rearrange now opens the Home shelf in a temporary move mode. Left/Right shifts the selected card among its neighbours; arrows show movement directions and fade at boundaries. Stable package keys plus interruptible item-placement springs show the actual movement. Minimal uses immediate placement. The viewport is explicitly kept stable while a visible card moves; it advances only as needed. Focus remains on the moving card. Select or Save commits; Back or Cancel discards. Leaving the activity discards an unsaved draft. Save validates current favourite membership transactionally.

The exact preference file after the user's physical test was snapshotted (536 bytes), preserving the user's latest order rather than overwriting it with the earlier order. Generated-key move/Back was byte-identical; Save then inverse-move/Save restored that snapshot byte-identically. Actual TV screenshots `inline-start.png` and `inline-moved.png` show card coordinates changing across the shelf. Temporary Apps/Search Pin/Unpin controls are now removed after the physical menu verification and discoverable hold hint. The new inline method has device-generated D-pad verification; user feedback on its feel remains welcome.

Debug/profile assemble, 12 tests per variant and lint pass (zero errors, seven existing advisories). No existing Home card dimensions, icons or wallpaper were redesigned.

Official Android references used for the diagnosis and supported intents: [high-contrast text](https://support.google.com/accessibility/android/answer/11183305), [application details settings](https://developer.android.com/reference/android/provider/Settings#ACTION_APPLICATION_DETAILS_SETTINGS), and [Intent API](https://developer.android.com/reference/android/content/Intent). No system accessibility preference was changed until the user explicitly approved it.

## Final verification and local artifacts

All 20 final profile navigation checks passed, including third-party launch/return, every section, repeated root Back, stock Xiaomi Home and reopening RC TV. The first YouTube cold-start Back attempt preceded its navigation readiness; the rerun waits three seconds after opening YouTube and passes. Both the failed first attempt and the final successful run are retained locally; no failed sample is labelled a pass.

Profiled inline-rearranging APKs are `releases/RC-TV-Launcher-0.2.6-{debug,profile}.apk`. SHA-256 debug: `5D0C449165B6A889F762FA735A6D4E9C9F2701982C53B6301860B0F25DE78723`; profile: `34415C4C71445209B8CDDFC12E14A6002225FAA9E6549C979899164196BAD121`. Exact Home-checkpoint and application-menu rollback APKs remain under `releases/rollback/`. The safe same-key rollback route (current debug → old debug with `-r -d` → old profile) was exercised for the fresh performance baseline without clearing data or changing Home. Return to 0.2.6 with a normal `-r` install.

Actual TV screenshots, preference snapshots, raw frame reports and Perfetto traces remain local in `releases/diagnostics/premium-polish/`. No full personalisation, RC Assist or account integration was added. Longer thermal sessions, both-box testing and feel of the newly requested inline rearranging remain future acceptance work; the user already confirmed physical menu operation.

## Profile measurements — 0.2.6 (Home unchanged in 0.2.7)

Same precompiled, non-debuggable profile method: one warm-up and two measured passes of 24 device-side Right/Left inputs with 300ms spacing; pass 1 traced, pass 2 untraced. Tests run sequentially. A fresh preserved 0.2.2 baseline was measured on the current TV with high-contrast text off, rather than relying only on the older report. Device temperature/background activity were not controlled. The user's latest favourite order differs from the old build's hard-coded display order, so these remain comparable navigation tasks, not an identical-content laboratory benchmark.

| Build / mode | Frames, passes 1 / 2 | Janky frames, passes 1 / 2 | p95, passes 1 / 2 |
| --- | --- | --- | --- |
| Home checkpoint / Minimal | 249 / 249 | 2.81% / 3.21% | 14 / 14 ms |
| Polish / Minimal | 249 / 249 | 4.02% / 5.62% | 14 / 16 ms |
| Polish / Balanced | 423 / 423 | 0.71% / 0.71% | 12 / 11 ms |
| Polish / Cinematic | 426 / 426 | 0.70% / 1.17% | 11 / 12 ms |

All polished modes retained five shell compositions, two wallpaper compositions, one wallpaper decode and seven icon rasters across warm-up plus measurement; 169 tile compositions. No return of the earlier broad recomposition/image-loading fault. Cold starts: Minimal 558ms, Balanced 576ms, Cinematic 557ms. PSS samples about 99MiB / 103MiB in Minimal/Balanced; no memory-leak or universal frame-rate claim.

**Minimal remains a known performance limitation.** The fresh baseline has 7/8 janky frames per 249 versus 10/14 in the polished build, a 1.21/2.41 percentage-point increase. Median remains 7ms; p95 rises to 16ms in the slower pass. Do not call this performance-neutral or claim the earlier Minimal cost is solved. Balanced/Cinematic stay near the earlier measured range, but extra animated frames change the denominator. Future work should profile static glass drawing/scroll cost before adding expensive effects. No live blur, background animation loop or additional wallpaper decoding was introduced.

The rapid-boundary setup attempt before profiling did not reach move mode and was discarded; final functional verification explicitly waits for the menu/move surface before exercising boundaries. No failed setup is included as a successful interaction check.

## Increment 5 — Search remote focus and final state

Final Search testing exposed that an editable field could consume Down as cursor movement. 0.2.7 adds an explicit D-pad Down path to the first search result and Up back to the field. Actual Xiaomi checks verified a YouTube query returns two applications; Down focuses the result, the context menu removes then restores its favourite, and Up returns to the field. The final decoded DataStore equals the post-physical-test snapshot exactly in meaning: eight favourites, latest user order, Cinematic theme/motion and reduced motion off. No preference or app data reset. High-contrast text remains off as approved.

0.2.7 debug/profile builds, 12 tests per variant and lint pass with zero errors/seven existing advisories. The 20 navigation checks and performance samples were run on 0.2.6; the only subsequent UI change is the Search key-focus path, separately verified on the TV. Home's rendering and motion code are unchanged. Final working APKs: `releases/RC-TV-Launcher-0.2.7-{debug,profile}.apk`; profile left installed. Exact 0.2.6 debug/profile rollback copies added under `releases/rollback/RC-TV-Launcher-819a534-{debug,profile}.apk`.

Final SHA-256 debug: `3AA7CFEFD53804395E91F3A8926E080AA42189DB7657D58EA4E2B140590E0B89`; profile: `B0E19EF1B5F9F122CD7C10B2164E7B9645C9EBA75B547D151F9D81AD4090B855`.

Continuation: review the feel of inline arrows/moving cards with the physical remote; profile Minimal glass/scroll cost before adding effects; then long-session/both-box testing and the documented personalisation roadmap. RC Assist remains planned only. Draft PR #2 stays open, draft and unmerged.
