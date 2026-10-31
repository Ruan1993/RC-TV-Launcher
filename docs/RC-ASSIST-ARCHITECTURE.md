# RC Assist — proposed contract, no backend deployed

RC Assist is an optional in-app panel, not an accessibility service or unrestricted Android overlay. Home, app launch, Settings, keyboard and companion stay fully usable offline. No model dependency, API credential, microphone listener or assistant server is added in this sprint.

## Interface and flow
`AssistRequest(v=1, requestId, message, optionalContext)` is sent only after an explicit user action over authenticated HTTPS to our backend. Context is a deliberately small, consented snapshot: current RC TV section, selected app labels/IDs for lookup and supported preference values. Never include IME/companion input buffers, passwords, accounts, tokens, arbitrary files, screenshots or another application's content. No automatic conversation retention; provide clear cancel/error/offline states.

`AssistReply(v=1, requestId, answer, proposals[])` separates prose from suggested actions. A proposal contains a typed action, bounded arguments and a human-readable explanation. Model output is untrusted data. A local validator independently checks the allowlist, schema, current app discovery and current preference values. Prose never becomes executable code, an intent URI, package command or shell instruction.

## Allowed action vocabulary
- `Navigate(section)` — Home/Search/Apps/Library/Settings only.
- `FindInstalledApp(query)` — local lookup; show results instead of inventing availability.
- `ExplainSetting(settingId)` — local supported-setting catalogue.
- `ProposePreference(key, value)` — only validated RC TV theme, motion, reduced motion, wallpaper/rotation settings supported by the current version.
- `ProposeLaunch(appId)` — installed launchable app only, shown for user selection.
- `ShowTroubleshooting(topic)` — curated, non-destructive help with an explicit diagnostic action if available.

All proposed writes and app launches require an explicit TV confirmation showing the exact effect. Bind approval to the proposal hash, current preference revision, one-use nonce and short expiry; changed state requires a fresh review. Reuse the existing preference/repository API and report actual completion/failure. Reject arbitrary intents, broadcasts, settings writes, package install/uninstall/disable, launcher/IME defaults, file operations, network commands, account actions and system-wide key injection. Reset/delete operations are outside the first action set. Companion authentication never authorises assistant actions.

## Backend requirements
Keep a supported Gemini Flash model configurable on the server; verify current model availability and terms before implementation. Store Google credentials only in a backend secret manager with least privilege and rotation. Authenticate clients using short-lived, revocable backend sessions; never rely on an extractable APK shared secret. Add per-device/user rate limits, payload bounds, timeouts, request deduplication, spending caps and a global disable switch. Do not relay arbitrary URLs or executable tools. Restrict outbound access to the model provider, validate structured replies and redact credentials/content from logs. Define provider data handling, retention/deletion and privacy consent before shipping.

The backend can suggest actions but has no direct launcher execution capability. A deterministic local executor owns validation and confirmation. Future voice uses a supported Android speech service with explicit microphone consent and a visible listening state, never continuous background capture.

Reference: [Google API-key security guidance](https://ai.google.dev/gemini-api/docs/api-key). This is an architecture boundary, not a deployed assistant or a promise of model availability.
