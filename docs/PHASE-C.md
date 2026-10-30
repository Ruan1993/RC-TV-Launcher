# Phase C — everyday TV experience

## Verified entry checkpoint
The previous Back fix was uncommitted at a5dcbc9. It was retained, compiled and verified with Xiaomi native-key injection, then pushed as ab7560b (0.3.1). Search/Apps/Settings return Home; Home Back focuses first installed favourite; OK launches; repeated Back stays in RC TV; wallpaper preview cancels before leaving Settings. Empty/unavailable favourite handling reviewed without removing user favourites. Current Coast selection retained; no byte-identity claim for the interactive-session wallpaper timestamp.

## Supported Home selection — 0.4.0
RC TV retains ordinary app and Android Home intent registration. Settings clearly distinguishes actual Home-intent resolution from the HOME role. Supported RoleManager consent request is used if available/not held; otherwise Android Home settings opens if implemented. Stub/missing settings are explained. No package priority edits, forced defaults, stock-launcher disabling, root or destructive ADB changes.

Xiaomi MiTV-AFKR0 Android 11: HOME role holder is RC TV, while the resolved HOME activity remains com.google.android.apps.tv.launcherx/.home.HomeActivity at priority 2. Android Home settings lists Google TV, Projectivy, RC TV and Setup Wizard. Screen opened then cancelled; role and resolved Home route are unchanged. Two focused status-model tests pass, including role-granted-but-not-routed messaging. Actual Home-button/firmware behaviour must not be promised from role ownership alone.

FEATURE-ROADMAP.md audits the full master specification and newly requested optional capabilities. No RC Assist, profiles, phone server or forced default change introduced.

References: [Android RoleManager](https://developer.android.com/reference/android/app/role/RoleManager), [Home role eligibility](https://developer.android.com/reference/androidx/core/role/RoleManagerCompat), [IME framework](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method), [MediaStore](https://developer.android.com/training/data-storage/shared/media).

## Optional keyboard core � 0.4.1
Native Android IME plus isolated preview, sharing weighted D-pad navigation, letters/numbers/symbols, Shift, Space, code-point Backspace and editor actions. Password context changes the header only: no typed-content reads, logs, storage, suggestions, clipboard or network permission. Android alone enables/selects the IME; existing keyboard remains installed. Settings provides preview and Android enable/selection controls.

Two layout/navigation tests and debug compilation pass. Xiaomi registers the protected IME service; preview key actions and masked password field pass. First pointer click corrected to activate a key. Original default IME unchanged. Actual system-IME/cross-app acceptance pending user-controlled enable/selection; do not infer it from preview success.
