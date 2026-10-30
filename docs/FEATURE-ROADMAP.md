# Complete feature roadmap — Phase C

The approved Home/materials and existing preferences are baseline constraints. Heavy capabilities stay optional modules; confirmed requirements are distinguished from platform/provider feasibility questions. See MASTER-SPECIFICATION.md for the full specification and RELEASE-READINESS.md for release gates.

| Feature | Requirement / implemented baseline | Next increment / feasibility boundary |
| --- | --- | --- |
| Core launcher | Implemented discovery, favourites/order, app actions, Search, glass Home, safe Back | Remote/focus/error recovery and everyday reliability remain critical |
| Default TV Home | Confirmed: supported Home registration and user-controlled selection; normal app entry retained | Surface role versus actual resolved Home; Xiaomi may override. No forced priorities, root, package disabling or automatic default change |
| RC TV keyboard | Confirmed optional IME: glass keys, D-pad, letters/numbers/symbols, secure password handling | Reliable core first; user enables/selects it, existing keyboard remains fallback; cross-app behaviour depends on editor support |
| Phone companion | Confirmed optional navigation/text entry | Pairing, authenticated encrypted local transport and revocation required; no open control server |
| Casting / screen sharing | Confirmed preserve system casting; optional supported integration | Audit Android/receiver capabilities, DRM/provider limits; do not promise universal mirroring or casting |
| Local / USB files | Confirmed supported browsing and later permitted file operations | Scoped MediaStore images first; document/tree-provider support and USB visibility vary by firmware; no broad all-files access |
| Wallpapers | Bundled/import pipeline, rotation and lightweight transitions implemented | Native image browser for broken picker; online curated sources need explicit redistribution rights, offline cache/quota/removal and cancellation |
| Appearance | Existing themes/motion implemented; practical font/glass controls confirmed | Persisted, functional controls and confirmed reset; preserve approved defaults |
| User profiles | Optional, outside initial 1.0 critical path | Launcher-level profiles only; cannot isolate other apps' sessions or infer YouTube accounts |
| RC Assist | Future optional, outside initial 1.0 path | Secure Gemini Flash backend, authenticated permission-based actions; no APK API keys or keyboard dependency |
| Voice / accessibility | Confirmed supported services and accessible focus/text/motion | Speech provider/microphone availability and consent; safe fallback; no background listening |
| Network media / browser | Confirmed typed provider contracts, installed-player/browser handoff | Local watchlist and configured libraries; SMB feasibility/licence/security review; no protected-service scraping |
| Backup / restore | Confirmed versioned schema validation and missing-resource handling | No account credentials/device secrets; bounded imports and optional vetted encryption |
| Weather / ambient | Optional provider weather and lifecycle-bound ambient screensaver | Endpoint/privacy/rights, inactivity detection and output/device limitations; no decorative service loop |
| Diagnostics / recovery | Confirmed real metrics/recovery and opt-in network tests | Disclosed endpoints/bandwidth; no fake cleaning or task killing; dedicated Minimal/GPU optimisation |
| Production release | Confirmed signing, privacy/licences and reproducible install/recovery | Both-device acceptance, long sessions/low-memory, production key backup; approval before merge/publication/default changes |

Priority: default-Home clarity → optional reliable keyboard checkpoint → bounded native wallpaper browser. Do not rush these into incomplete UI. Online collections, companion, profiles, RC Assist and broad file management remain separate increments.
