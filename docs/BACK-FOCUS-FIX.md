# Focused Back correction — 0.3.1

From clean a5dcbc9, feature/phase-b-cinematic-glass. Root Back now returns Settings/Search/Apps to Home; a further Home Back scrolls to/focuses the first installed favourite in the saved order without launching. Repeated Back remains consumed. Empty/unavailable favourites yield no focus request and never exit. Existing later preview/rearrange handlers and native popup/dialog windows keep priority. The wallpaper Settings page no longer traps Back. Explicit picker Back still closes the picker normally.

Small UI-only navigation change; compilation and focused Xiaomi native-key checks, no full profiling/test sweep. 0.3.0 debug rollback is preserved; working 0.3.1-debug APK saved. Preferences are not written by this change.

## Future selectable default Home — document only

Audit the existing Android Home intent declarations and launcher-selection helpers, then provide a clear user-controlled setup/default-Home selection flow using supported Android Home/role settings. Verify firmware compatibility and stock-launcher recovery on both TVs. Require explicit user approval before changing the current default or physical Home route. Do not disable Xiaomi’s launcher, use root, or use ADB/manufacturer hacks to force selection. No default-Home implementation or system change is part of this fix.

Verified at Phase C start on Xiaomi with native-key injection: Settings Back → Home Back focuses YouCine without launching; OK launches it; repeated root Back stays in RC TV; Search/Apps return and preview-first dismissal pass. External-app startup was allowed to settle before asserting RC foreground. Favourites/order and other settings matched; the observed wallpaper/apply timestamp changed from the initial snapshot during the interactive session, so byte identity is not claimed. Current Coast selection retained. Empty/unavailable favourite path reviewed (no preference mutation to simulate emptiness).

Phase C later added supported user-controlled Home selection; see PHASE-C.md. No current default or physical Home route was changed.
