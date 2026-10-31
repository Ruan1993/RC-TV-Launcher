# Phase C â€” everyday TV experience

## Verified entry checkpoint
The previous Back fix was uncommitted at a5dcbc9. It was retained, compiled and verified with Xiaomi native-key injection, then pushed as ab7560b (0.3.1). Search/Apps/Settings return Home; Home Back focuses first installed favourite; OK launches; repeated Back stays in RC TV; wallpaper preview cancels before leaving Settings. Empty/unavailable favourite handling reviewed without removing user favourites. Current Coast selection retained; no byte-identity claim for the interactive-session wallpaper timestamp.

## Supported Home selection â€” 0.4.0
RC TV retains ordinary app and Android Home intent registration. Settings clearly distinguishes actual Home-intent resolution from the HOME role. Supported RoleManager consent request is used if available/not held; otherwise Android Home settings opens if implemented. Stub/missing settings are explained. No package priority edits, forced defaults, stock-launcher disabling, root or destructive ADB changes.

Xiaomi MiTV-AFKR0 Android 11: HOME role holder is RC TV, while the resolved HOME activity remains com.google.android.apps.tv.launcherx/.home.HomeActivity at priority 2. Android Home settings lists Google TV, Projectivy, RC TV and Setup Wizard. Screen opened then cancelled; role and resolved Home route are unchanged. Two focused status-model tests pass, including role-granted-but-not-routed messaging. Actual Home-button/firmware behaviour must not be promised from role ownership alone.

FEATURE-ROADMAP.md audits the full master specification and newly requested optional capabilities. No RC Assist, profiles, phone server or forced default change introduced.

References: [Android RoleManager](https://developer.android.com/reference/android/app/role/RoleManager), [Home role eligibility](https://developer.android.com/reference/androidx/core/role/RoleManagerCompat), [IME framework](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method), [MediaStore](https://developer.android.com/training/data-storage/shared/media).

## Optional keyboard core — 0.4.1
Native Android IME plus isolated preview, sharing weighted D-pad navigation, letters/numbers/symbols, Shift, Space, code-point Backspace and editor actions. Password context changes the header only: no typed-content reads, logs, storage, suggestions, clipboard or network permission. Android alone enables/selects the IME; existing keyboard remains installed. Settings provides preview and Android enable/selection controls.

Two layout/navigation tests and debug compilation pass. Xiaomi registers the protected IME service; preview key actions and masked password field pass. First pointer click corrected to activate a key. Original default IME unchanged. Actual system-IME/cross-app acceptance pending user-controlled enable/selection; do not infer it from preview success.

## Native image browser — 0.4.2
Wallpaper Import now opens a native read-only MediaStore image browser instead of Xiaomi's DocumentsStub. Permissions are requested only on explicit Allow image access: Android 11/12 legacy media-read permission (maxSdk32), Android 13 image-only permission, Android 14+ full/selected-image access. Only image queries are made; no raw paths, all-files or write access. Bounded 100-item pages, two concurrent thumbnail loads, 4 MiB thumbnail cache and 960×540 previews; private import remains the existing validated/downsampled pipeline. Back cancels preview before closing browser. System document-picker fallback remains where working. USB is limited to images indexed by Android or exposed by a working provider; arbitrary USB folders are not promised.

Xiaomi checks: permission denial/retry, image listing, preview/Back cancellation, import to private storage, imported gallery selection, confirmed removal and return to existing gallery pass. A disposable shell-created image initially had IS_PENDING=1 and was correctly excluded; publishing that fixture made it visible. Fixture/private copy removed after test. Parsed preference values, all eight favourites/order and wallpaper settings matched the pre-browser snapshot (shell newline conversion normalized for comparison).

Final checkpoint: 22 unit tests pass; lint passes with 15 warnings (dependency updates, TV landscape declarations, existing vector/scale advice, native-view tooling and untranslated preview strings). One IME MissingSuperCall error corrected. No performance profiling repeated; no rendering change to approved Home. Actual system-IME cross-app acceptance, physical remote feedback, selected-photo access on newer Android, USB provider and second-device acceptance remain open.

Final-device follow-up: mixed pointer/D-pad entry and focus retention through Shift/mode changes pass on 0.4.2. Original system IME and resolved Google TV Home are unchanged. User enable/selection approval was requested for a temporary actual-IME test; without an answer the service remains disabled.
