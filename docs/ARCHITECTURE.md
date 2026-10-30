# Architecture, dependencies and privacy

## Foundation boundaries

Home review 2.0 adds `ui/HomeAppCard.kt`, keeping larger material/icon rendering separate from the compact Apps/Search card. PM icon cache keys include raster size; requests are bounded to 192px and the existing 2 MiB LRU is retained. Home does not expose the permanent pin button; existing favourite management remains in Apps/Search pending the separately tested long-press context menu. Wallpaper, isolated rail, draw-phase motion and root Back optimisations remain intact. See GLASS-2-HOME-REVIEW.md for the measured cost and the user-requested design pause.

The current root Back handler always consumes Back: sections return Home and Home does not finish its activity or expose an unrelated Android task. System Home remains untouched. Rail animation state lives in its own composable, and animated width/tint are read during drawing rather than full-screen composition/layout. Static wallpaper scrims are precomposited once on IO. Profile-only counters and the same-signed, non-debuggable `profile` build support repeatable local measurements; debug and ordinary release keep diagnostics disabled. See PHASE-B-BACK-AND-PERFORMANCE.md for evidence and limitations.

Phase B introduces `ui/CinematicLauncher.kt` for the wallpaper renderer, translucent surfaces, focus-aware buttons/tiles and navigation shell. Platform actions and the proven discovery lifecycle remain in MainActivity. Appearance and motion extend the existing DataStore without renaming its file or favourite keys. Defaults validate unknown values; Minimal motion is the safe preview default. The renderer has no network dependency, continuous animation loop or live blur; see PHASE-B-CINEMATIC-GLASS.md for measured limits and the incremental roadmap.

`core/AppRepository` queries both TV and conventional MAIN launcher categories on an IO coroutine. It removes duplicate packages and uses supported launch intents. Availability is refreshed when the activity resumes. Package visibility is restricted to launcher/Home intent queries; QUERY_ALL_PACKAGES is not requested.

Discovery has one lifecycle-driven refresh while resumed. Cancellation propagates without becoming an error; genuine exceptions are logged and displayed with Retry, retaining the last successful app list. A successful refresh, including an empty result, clears the error. Installed icons use PackageManager off the main thread, rasterised at requested size (48dp for compact cards, 80dp for Home, maximum 192 pixels per side) with a 2 MiB LRU cache and lazy-grid loading. Icons are invalidated on refresh; unavailable icons use bundled RC artwork. No network image requests or bundled third-party app artwork are needed.

`core/Preferences` owns the application-scoped Preferences DataStore and exposes Flow state. Writes are asynchronous. Corrupted preferences reset to defaults. UI observes state only while lifecycle-active. Missing pinned packages remain stored but are not shown; app discovery never fabricates them.

`MainActivity` owns the singleTask Home entry, role request and settings fallback. `GlassButton` uses standard Compose buttons with keyboard, D-pad and pointer interaction and a high contrast focus border. Discovery work runs off the main thread. The clock coroutine pauses below the started lifecycle state. No persistent decoration service is used.

Future packages will separate wallpapers, profiles, media, integrations, diagnostics, compatibility and backup as features become real. Use Room only when relational media/watchlist/history data is implemented. Keep future provider contracts typed; no arbitrary executable plugin code. The mandatory hardware Home/recovery milestone precedes risky system integrations.

## Pinned dependency inventory

Initial reproducible baseline uses released, non-preview versions; this is not a claim that each is the latest release. Compatibility is established by compilation, followed by device tests. Upgrade assessment remains a release gate.

| Dependency | Version | Licence / source |
| --- | --- | --- |
| Android Gradle plugin | 8.13.1 | Apache-2.0; https://android.googlesource.com/platform/tools/base/ |
| Gradle | 8.13 | Apache-2.0; https://github.com/gradle/gradle |
| Kotlin Android / Compose compiler | 2.1.0 | Apache-2.0; https://github.com/JetBrains/kotlin |
| Compose BOM | 2025.04.01 | Apache-2.0; https://developer.android.com/develop/ui/compose/bom |
| AndroidX Activity Compose | 1.10.1 | Apache-2.0; https://android.googlesource.com/platform/frameworks/support/ |
| AndroidX Compose UI / Foundation / Material3 | BOM managed | Apache-2.0; same AndroidX source |
| AndroidX DataStore Preferences | 1.1.7 | Apache-2.0; same AndroidX source |
| AndroidX Lifecycle Runtime Compose | 2.9.0 | Apache-2.0; same AndroidX source |
| JUnit (test only) | 4.13.2 | EPL-1.0; https://github.com/junit-team/junit4 |

Coroutines are provided transitively by AndroidX. Full resolved transitive POM/licence verification and redistribution notices remain a public-release gate; this table is a direct dependency inventory, not a completed legal audit. Android platform fonts are used without redistributing font files. The original TV vectors and gradient are project artwork. MIT allows future commercial distribution subject to its notice requirement. Apache and other dependency obligations still apply independently.

## Permissions and security review

No runtime or uses-permission declarations. Intent queries allow app discovery; Home/Leanback filters register entry points. Touch hardware is optional. The exported activity accepts Home and app launch requests, and does not read external input as commands. No network endpoints, sockets, servers, credentials, broad storage access, telemetry, automatic backups or background services. Only local favourite package IDs and a motion preference are stored in private DataStore. Android automatic backup is disabled until a validated explicit backup format is implemented.

Before adding files: persist only user-granted document URIs, check provider capabilities and confirm destructive operations. Before adding network tests: disclose endpoint/bandwidth, require initiation, limit requests and implement cancellation. Before backup: validate version, schema, sizes and paths; never export secrets. Companion must require explicit pairing, encryption, authentication and revocation before opening a control listener.
