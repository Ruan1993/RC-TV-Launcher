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

## Application actions — implementation and acceptance boundary

Home, Apps and Search use the same physical Select/Enter handler. It tracks initial down, repeat, platform long-press timeout, cancelled release and focus loss; a handled hold never launches on release. The popup suppresses only events sharing the opening press timestamp. Accessibility long-click also opens options. Popup positioning uses actual tile bounds and clamps to the viewport; spring entrance is skipped in Minimal. Back closes the popup; leaving the activity closes panels.

Menu supports Favourite, Rearrange (when more than one favourite), Android App Information, and Android-confirmed Uninstall for non-system apps where user restrictions permit. Failure gives feedback; Android remains the final authority for uninstall restrictions. No silent uninstall API. REQUEST_DELETE_PACKAGES is declared; no broad package-delete permission.

Ordered favourites add an optional DataStore order key. Existing sets keep their original displayed order without an eager migration write; unavailable favourites remain preserved. Saving validates the entire membership inside the DataStore transaction to reject stale edits. Rearrange works on a local draft with explicit Save, Cancel and Back cancellation. Directional move is bounded; Down explicitly targets Save. Existing Apps/Search Pin/Unpin controls remain pending physical remote acceptance.

Twelve tests per variant pass, including five added tests for short/hold/repeat/cancel/focus-loss state and legacy/saved order resolution. Debug/profile builds and lint pass with zero errors and the same seven advisory warnings as before.

Actual Xiaomi checks: generated short/long Select opens the correct launch/menu path; App Information reaches the selected package's Android screen. Uninstall displays “Do you want to uninstall this app?” with OK/Cancel; cancelled with Back and the package remains installed. D-pad-only Rearrange/Cancel restores the original tile focus without writing; Save survives force-stop/relaunch. Original eight-package order restored through Save. Moving the tile now explicitly requests its new list position so lazy-list anchoring cannot hide it. Focus restoration also returns to keyboard input after pointer interaction. A physical-remote check was requested from the user; its result is not inferred from ADB input.

The original preferences remain unchanged in meaning. The additional optional favourite_order key records the original displayed order after Save testing; the favourite set and theme/motion/reduced-motion values remain intact. At start the TV actually had Cinematic motion selected (the historical report's Minimal setting was not assumed).

## Increment 4 — user-requested direct shelf rearranging

The user physically tested the remote and reported “Everything works great”, specifically confirming menu/short press/Back/Home behaviour, but requested arrows and visible card movement instead of the separate rearrange dialog. Physical key logs include Select down/repeat and cancelled release when the popup takes focus. The earlier pending physical-menu gate is satisfied.

Rearrange now opens the Home shelf in a temporary move mode. Left/Right shifts the selected card among its neighbours; arrows show movement directions and fade at boundaries. Stable package keys plus interruptible item-placement springs show the actual movement. Minimal uses immediate placement. The viewport is explicitly kept stable while a visible card moves; it advances only as needed. Focus remains on the moving card. Select or Save commits; Back or Cancel discards. Leaving the activity discards an unsaved draft. Save validates current favourite membership transactionally.

The exact preference file after the user's physical test was snapshotted (536 bytes), preserving the user's latest order rather than overwriting it with the earlier order. Generated-key move/Back was byte-identical; Save then inverse-move/Save restored that snapshot byte-identically. Actual TV screenshots `inline-start.png` and `inline-moved.png` show card coordinates changing across the shelf. Temporary Apps/Search Pin/Unpin controls are now removed after the physical menu verification and discoverable hold hint. The new inline method has device-generated D-pad verification; user feedback on its feel remains welcome.

Debug/profile assemble, 12 tests per variant and lint pass (zero errors, seven existing advisories). No existing Home card dimensions, icons or wallpaper were redesigned.

Official Android references used for the diagnosis and supported intents: [high-contrast text](https://support.google.com/accessibility/android/answer/11183305), [application details settings](https://developer.android.com/reference/android/provider/Settings#ACTION_APPLICATION_DETAILS_SETTINGS), and [Intent API](https://developer.android.com/reference/android/content/Intent). No system accessibility preference was changed until the user explicitly approved it.
