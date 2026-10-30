# Release readiness and next milestones

Current checkpoint: the user approved the Home visual direction and authorised Phase B interface polish. Typography/Settings and sidebar/motion increments are committed separately; application actions are under actual-TV verification. See PHASE-B-POLISH.md for current evidence and remaining acceptance. GLASS-2-HOME-REVIEW.md is the historical checkpoint and its pause has been superseded by the user's new request.

Phase A is merged. This remains a Phase B preview. The user physically confirmed the new menu and Back/Home behaviour. Inline shelf rearranging then replaced the dialog at the user’s request and has generated-key device verification. Thermal/long-session checks, both-box recovery and licence audit remain release gates. Keep draft PR #2 unmerged. Full personalisation and RC Assist remain future work, including optional profiles and a secure Gemini Flash backend without APK keys.

- Phase A: obtain successful APK build, unit tests and lint; test Home/reboot/recovery on both Xiaomi boxes. Record compatibility limits.
- Phase B: landscape assets with rights inventory; bounded image decoding, SAF wallpaper choice/rotation, lifecycle-paused ambient, adaptive/collapsing sidebar, stable focus restoration, sorting/hiding/labels, settings and accessibility. Verify with remote UI tests and reduced system motion.
- Phase C: per-profile preferences and layouts; local watchlist and typed integrations; SAF file browse/copy/move/rename/delete/open; real opt-in throughput/latency test with transparent endpoints/history; versioned validated backup and cross-device missing-resource handling. Add business-logic and critical-flow tests with each feature.
- Phase D (optional): authenticated paired phone companion, evaluated SMB library, supported voice input and provider integrations. No protected-service scraping or unauthenticated server.
- Phase E: full licence notices, dependency update assessment, privacy/security review, visual/device QA, performance and accessibility measurements, release signing and reproducible release instructions.

Release gates: all implemented features compile/test/lint; no false working integrations; both device checklists completed; Home recovery proven; all assets and resolved dependencies audited; backup/file/network abuse cases tested when added; no secrets in distribution; APK checksum and signing provenance recorded.

Future-device enhancements: evaluate blur only on capable APIs/hardware, optional network wallpaper caching with attribution, supported media-provider channels and smart-home provider contracts. Never assume frame-rate, DRM, PiP, casting or Home overrides are universally controllable.
