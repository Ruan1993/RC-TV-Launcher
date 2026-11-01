# Phase J3B — Checkpoint 1: Home D-pad

TV 1.0.0-rc1.10 (39), baseline fa0c6af. Phone 0.5.1-rc1 unchanged.

The Home navbar's LazyRow focus group previously relied on geometry for Home/Search/Apps/Cast Down. Speed Test and Settings explicitly targeted ClockNetwork. The distant bottom shelf therefore lost to navbar focus search. All six now explicitly target the favourite shelf while Home is showing real cards. The shelf requester attaches to its group, so it remains valid after horizontal scrolling. Outside normal Home (empty/loading shelf, rearrangement, wallpaper preview, other sections), Down retains a real clock target. Every normal favourite card explicitly sends Up to the fixed Settings button, available at every text size. Left/Right and OK handlers remain unchanged.

No Home styling changes. HomeAppCard is byte-identical to J3A (SHA256 57340A0268E445499609688E9150ADB031125C8C596BB90BC6DA0FD3A68C1508). Tests snapshot favourites and their saved order and assert neither changes; temporary text/motion choices restore in finally.

43 unit tests pass; lint has 0 errors and 27 existing warnings. TV and test APK builds pass. Physical Xiaomi MiTV-AFKR0 Android 11 testing uses authorized network ADB at 192.168.1.39:5555. Test evidence and final acceptance are appended below.

APK: releases/phase-j3b/checkpoint-1/RC-TV-Launcher-1.0.0-rc1.10-debug.apk.
Rollback: releases/rollback/RC-TV-Launcher-fa0c6af-debug.apk. Existing rollback files remain intact. Default Home unchanged; no phone edits, PR merge or public release.

Physical acceptance: all six navbar Down paths, Left/Right, shelf Up and Enhanced focus transform passed at Standard, Comfortable, Large and Extra Large. ADB key injection exercises the physical Xiaomi; human remote-button acceptance remains available to the user. OK opened the installed F1 app (TvSubscriptionWidgetActivity); Back and explicit return to RC TV worked. Saved favourites/order assertions passed in every run. Return screenshot: releases/review/phase-j3b/navigation-return.png. A transient network ADB interruption was recovered and the affected sizes rerun successfully.


## Checkpoint 2 — Screensaver & Power (TV 1.0.0-rc1.11 / 40)

Checkpoint 1 was committed as ba70fa1 before this work began.

Persistent options: 10/20/30 minutes (30 default), 1/2 hours, Never; Cinematic Wallpapers (default), Clock & Wallpaper, Black Screen. The category follows the existing glass subpage and scrollable controls. Preview is available regardless of the automatic timeout. D-pad/OK dismiss on release, preventing OK from also activating the underlying control; Back dismisses normally. Android's dialog window restores the existing focus. The TV-side phone-control handler consumes a dismissal command without forwarding it to the underlying page. The phone app is unchanged.

Automatic operation is explicitly **RC TV Home only**. Activity input and phone commands reset an elapsed-realtime deadline. Resume and focus regain start a fresh idle period. A single cancellable Compose delay runs only while Home is resumed, focused and not blocked by setup/companion approval. At its deadline, Android AudioManager.isMusicActive and PowerManager.isInteractive are checked once. Active media suppresses that idle session until a fresh interaction/resume; there is no periodic audio poll. Settings and other applications cannot activate it. Pause removes an existing saver. No overlay, background polling service, DreamService registration, wake lock, keep-screen-on flag, alarm or added permission is used. Android ambient mode/sleep can happen first; RC TV does not extend or change Android's timers.

The same cached wallpaper renderer/assets and saved visibility are reused. Wallpapers change only every two minutes; transitions respect interface motion and Reduced Motion freezes the scene. The clock uses the existing clock updates. Black Screen is visually black and does not power off a panel. No playback pause/stop, media session, audio focus or hardware power command is issued.

### Android 11 and Xiaomi capability evidence

The physical device reports MiTV-AFKR0, Android 11. Public ACTION_DREAM_SETTINGS and ACTION_DISPLAY_SETTINGS each resolve to no activity. ACTION_SETTINGS opens com.android.tv.settings/.MainSettings. Actual Settings navigation confirms System → Ambient mode and System → Power and energy → Turn off display; the latter currently reads After 4 hours. Read-only inspection: screen_off_timeout=600000; secure sleep_timeout=14400000. These are recorded device values, not RC TV settings, and were not changed.

The app tries the relevant public settings action and catches failure before falling back to general Android Settings. No private Xiaomi component is hardcoded. Sleep & power says Use Android setting; unsupported custom 1/2/4/6/8-hour/Never controls are deliberately absent. A normal launcher cannot promise system standby, HDMI-CEC TV shutdown, or physical panel power control. Android's screen-off timeout can begin dreaming instead of turning off the screen, so changing it would not implement the requested sleep policy. Special write-settings authorization and privileged secure/device-power access are not requested.

Primary references consulted before implementation:

- [Activity input/window-focus callbacks](https://developer.android.com/reference/android/app/Activity#onUserInteraction()).
- [AudioManager.isMusicActive](https://developer.android.com/reference/android/media/AudioManager#isMusicActive()).
- [Public settings destinations and optional availability](https://developer.android.com/reference/android/provider/Settings#ACTION_DREAM_SETTINGS).
- [SCREEN_OFF_TIMEOUT may begin dreaming](https://developer.android.com/reference/android/provider/Settings.System#SCREEN_OFF_TIMEOUT).
- [WRITE_SETTINGS special approval; WRITE_SECURE_SETTINGS unavailable to third-party apps](https://developer.android.com/reference/android/Manifest.permission#WRITE_SETTINGS).
- [AOSP goToSleep requires DEVICE_POWER and is outside normal public launcher control](https://android.googlesource.com/platform/frameworks/base/+/2f431e264d886fd33d537802095e98e9c3425b7c/core/java/android/os/PowerManager.java).

### Verification scope

The separate test APK adjusts only RC TV's own private monotonic interaction state to 9m59s idle, then waits for the actual production delay and eligibility checks. No Android clock or system timer is changed. This is accelerated inactivity verification, not a claim of a completed 10-minute-to-2-hour endurance test. A real silent looped AudioTrack verifies active-media suppression without subscription content or audible playback. Foreground Android Settings verifies lifecycle suppression. Individual Netflix/YouTube/VLC/YouCine long-playback sessions and fresh physical-phone pairing remain manual acceptance items; no claim that those sessions were watched is made.

### Final verified result

- TV 1.0.0-rc1.11 (40), same development signing key; installed on physical Xiaomi. Phone 0.5.1-rc1 and its source unchanged.
- 46 unit tests passed, including monotonic deadlines, Never and foreground/audio eligibility. Final lint: 0 errors, 27 pre-existing advisory warnings. Debug and separate instrumentation APK builds passed.
- Physical Home D-pad acceptance passed all six navbar Down paths, Left/Right and fixed Settings return at all four font sizes with Enhanced focus. Repeated on the final build. OK launched the F1 application and return worked.
- Physical screensaver integration passed Standard and Extra Large. Final Extra Large also used Reduced Motion. All three previews, OK/Back dismissal, focus restoration, saved choices, accelerated idle activation, input resetting inactivity, Never, active silent-media suppression and external-foreground suppression passed. Both system shortcuts opened Android Settings through their public-action fallback.
- Existing physical Settings regression passed categories/control access, wallpaper and utility Back, category focus restoration, remote/keyboard controls, weather settings and Speed Test access. No fresh phone pairing or live online speed/weather measurement is claimed.
- Final preference audit: eight favourites; exact saved order unchanged; Comfortable restored; Reduced Motion false restored; screensaver 30 minutes / Cinematic Wallpapers restored. HomeAppCard hash remains the J3A hash. RC TV Keyboard remains selected; stock Google Home remains default. Android timeout values remained 600000 / 14400000.
- Pairing persistence is not byte-stable evidence: the existing CompanionServer deliberately re-encrypts migrated/unexpired approvals when each new process loads them. No stop/revoke/new-approval action was performed. The TV-side dismissal handler now accepts an approved phone's command while the saver owns focus; an incoming approval automatically dismisses the saver. Fresh phone/device acceptance remains pending.

Final APK: `releases/phase-j3b/checkpoint-2/RC-TV-Launcher-1.0.0-rc1.11-debug.apk`.
Rollback to Checkpoint 1: `releases/rollback/RC-TV-Launcher-ba70fa1-debug.apk` (rc1.10).
Rollback to baseline: `releases/rollback/RC-TV-Launcher-fa0c6af-debug.apk` (rc1.9).

Physical screenshots under `releases/review/phase-j3b/`: `screensaver-settings.png`, `screensaver-settings-xl.png`, `screensaver-cinematic.png`, `screensaver-clock.png`, `screensaver-black.png`, and Extra Large equivalents. Black preview screenshot pixel extrema are exactly RGB 0 throughout. Preference evidence: `preference-preservation.txt`.

Next acceptance: use the physical remote for a brief comfort check, observe an unaccelerated idle interval with Android's own ambient timeout taken into account, and run long playback/return sessions in the streaming apps plus a fresh phone reconnect/pairing check. No system sleep scheduling, privileged standby, physical TV power, default-Home change, PR merge, public release or phone changes were made.

Scrolled power-section screenshot: releases/review/phase-j3b/screensaver-power-xl.png. Final APK SHA256: 7CCCFF0D1C33E73703F3209F22179BD8880DE774E5E55B170F0F074C36B95BBB.

