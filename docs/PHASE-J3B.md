# Phase J3B — Checkpoint 1: Home D-pad

TV 1.0.0-rc1.10 (39), baseline fa0c6af. Phone 0.5.1-rc1 unchanged.

The Home navbar's LazyRow focus group previously relied on geometry for Home/Search/Apps/Cast Down. Speed Test and Settings explicitly targeted ClockNetwork. The distant bottom shelf therefore lost to navbar focus search. All six now explicitly target the favourite shelf while Home is showing real cards. The shelf requester attaches to its group, so it remains valid after horizontal scrolling. Outside normal Home (empty/loading shelf, rearrangement, wallpaper preview, other sections), Down retains a real clock target. Every normal favourite card explicitly sends Up to the fixed Settings button, available at every text size. Left/Right and OK handlers remain unchanged.

No Home styling changes. HomeAppCard is byte-identical to J3A (SHA256 57340A0268E445499609688E9150ADB031125C8C596BB90BC6DA0FD3A68C1508). Tests snapshot favourites and their saved order and assert neither changes; temporary text/motion choices restore in finally.

43 unit tests pass; lint has 0 errors and 27 existing warnings. TV and test APK builds pass. Physical Xiaomi MiTV-AFKR0 Android 11 testing uses authorized network ADB at 192.168.1.39:5555. Test evidence and final acceptance are appended below.

APK: releases/phase-j3b/checkpoint-1/RC-TV-Launcher-1.0.0-rc1.10-debug.apk.
Rollback: releases/rollback/RC-TV-Launcher-fa0c6af-debug.apk. Existing rollback files remain intact. Default Home unchanged; no phone edits, PR merge or public release.

Physical acceptance: all six navbar Down paths, Left/Right, shelf Up and Enhanced focus transform passed at Standard, Comfortable, Large and Extra Large. ADB key injection exercises the physical Xiaomi; human remote-button acceptance remains available to the user. OK opened the installed F1 app (TvSubscriptionWidgetActivity); Back and explicit return to RC TV worked. Saved favourites/order assertions passed in every run. Return screenshot: releases/review/phase-j3b/navigation-return.png. A transient network ADB interruption was recovered and the affected sizes rerun successfully.

