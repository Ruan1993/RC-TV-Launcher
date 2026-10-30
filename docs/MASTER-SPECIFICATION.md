# RC TV Launcher
**Master Development Specification — Version 1.0**

**Project:** RC TV Launcher  
**Platform:** Native Android TV / Google TV  
**Primary test devices:** Two Xiaomi MiTV-AFKR0 TV boxes running Android 11  
**Initial distribution:** Free and open-source  
**Design:** Cinematic Glass  
**Motion:** Beautiful but lightweight  
**Development objective:** A polished, usable, feature-rich first release with an extensible architecture.

## 1. Project mission

Build a premium-quality, advertising-free Android TV launcher that prioritises visual beauty, instant navigation, reliability, privacy and complete user customisation.

This must be a real native Android application, not a website, WebView launcher or visual prototype.

Optimise for low-memory TV hardware, particularly the two existing Xiaomi Android 11 devices.

Build the project as a complete, maintainable codebase. Avoid placeholders, fake functionality, unnecessary services and dependencies on paid APIs.

## 2. Technology and architecture

Use Kotlin, Android SDK, Jetpack Compose with TV-compatible focus/navigation patterns, Coroutines, Flow, DataStore and a modular architecture.

Use Room where relational local data is appropriate.

Select currently supported, stable dependencies compatible with Android 11. Verify dependency licences before adoption.

Organise modules or clearly separated packages for:

- Core launcher and app discovery
- UI design system and navigation
- Wallpapers and ambient mode
- Profiles and preferences
- Local and network media
- Search and supported integrations
- Network diagnostics
- Device compatibility
- Backup and restore
- Optional phone companion
- Plugin and integration contracts

Avoid excessive abstraction, but ensure future device support does not require a rewrite.

## 3. Cinematic Glass design system

Create a stunning, minimalist cinematic home screen featuring:

- High-resolution landscape wallpapers
- Translucent dark glass panels
- Subtle blur and carefully controlled opacity
- Elegant typography readable from three metres
- Distinct, responsive D-pad focus states
- Soft focus glow, elevation and scaling
- Smooth sidebar transitions
- Attractive clock and optional weather widget
- Minimal clutter and zero advertising
- Optional minimalist theme
- Performance mode that reduces expensive effects

Support 1080p and 4K output layouts without assuming the UI must render every effect at native 4K resolution.

Animations must never delay input processing.

Respect accessibility settings and provide reduced-motion options.

## 4. Home screen and navigation

Create five main sections:

1. Home
2. Search
3. Apps
4. Library
5. Settings

Use a collapsible left sidebar that remains compact until focused.

Home must support configurable favourite apps, rows, shortcuts, clock, weather and optional Continue Watching integrations.

Provide full D-pad navigation, key-repeat handling, keyboard support and mouse interaction.

Keep navigation spatially predictable. Remember focus positions when returning from apps or submenus.

Aim to make launching favourite content possible within three deliberate actions after the home screen becomes interactive, where external app behaviour permits.

Provide app pinning, sorting, rearranging, hiding, custom labels and supported icon customisation.

Prioritise the user's favourite applications: YouCine, YouTube Music, YouTube and F1 TV, while discovering installed apps dynamically rather than hard-coding their availability.

## 5. Launcher integration and safety

Register the application as an Android Home launcher using the appropriate intent filters.

Implement a setup screen that explains the current default Home configuration.

First attempt standard Android launcher selection mechanisms.

Do not claim that manufacturer-specific Home-button overrides are guaranteed.

Do not silently disable, uninstall or modify the original Xiaomi launcher.

Provide a clearly accessible Restore Original Launcher guide and a recovery procedure.

If ADB is needed on a particular firmware version, document only reversible, explicitly authorised steps.

**Mandatory early milestone:** Build a minimal working launcher APK and test Home-button behaviour, reboot behaviour and recovery on the actual Xiaomi device before proceeding with risky system integrations.

If the default launcher cannot be changed through ordinary Android APIs, continue developing the app as a launchable application while documenting the limitation.

## 6. Wallpaper engine and ambient mode

Support:

- Bundled high-quality wallpapers with appropriate redistribution rights
- User-selected images
- USB and local storage image folders through Android's Storage Access Framework
- Scheduled wallpaper rotation
- Manual next/previous wallpaper controls
- Fit, fill, crop and dim options
- Per-profile wallpaper preferences
- Optional network wallpaper sources with caching and attribution
- Elegant ambient mode after configurable inactivity

Support animated wallpaper formats only when they can be implemented reliably and efficiently. Pause expensive rendering when the launcher is not visible.

Avoid persistent background processing solely for decoration.

## 7. Profiles

Implement launcher-level profiles with independent:

- Home layouts
- Favourite applications
- Wallpapers
- Themes
- Hidden applications
- Watchlists
- Launcher preferences

Explain that these are application profiles, not Android OS user accounts.

Do not claim to isolate other applications' private data, permissions or login sessions.

Provide optional PIN protection for launcher settings if feasible.

## 8. Search and content integrations

Search installed applications, local media, configured libraries and supported integration metadata.

Build a provider-based integration interface for future services.

Provide a local universal watchlist that users can manage manually and that supported providers may extend.

Do not scrape protected streaming services or invent access to private Netflix, Prime Video, F1 TV or other watch-history APIs.

Never display unavailable integrations as working features.

Support opening media through installed compatible applications and legitimate Android intents.

## 9. File management and media

Implement practical local file browsing using Android's supported storage permissions and document APIs.

Support permitted operations including browse, copy, move, rename, delete and open-with.

Support USB drives and portable HDDs through user-granted access.

Provide image previews and supported media metadata.

Investigate optional SMB network-share browsing using a suitable open-source library.

Integrate with installed media players where that provides better playback support than building a full decoder pipeline.

Do not promise system-wide automatic frame-rate matching, DRM playback control or universal picture-in-picture management.

Preserve existing Android casting services and provide shortcuts to supported casting and receiver settings.

## 10. Input and companion features

Support D-pad remotes, Bluetooth and USB keyboards, and compatible mice.

Use supported Android speech-recognition services for optional voice input, with clear fallback when unavailable.

Design a secure optional local-network phone companion for:

- Remote navigation
- Phone-keyboard text entry
- Sending links
- Supported file transfers

Require explicit pairing and authorisation. Do not expose an unauthenticated control server on the local network.

## 11. Network speed test and device diagnostics

Create a polished Performance & Network dashboard.

Show available device information such as Android version, model, storage, memory and network connection details, subject to Android permissions.

Implement a real, user-initiated internet speed test reporting:

- Download throughput
- Upload throughput
- Latency
- Test timestamp
- Connection type
- Wi-Fi signal or link information when accessible

Use transparent, documented test endpoints and account for bandwidth consumption. Do not run speed tests automatically in the background.

Explain that internet throughput is different from Wi-Fi signal quality and local UI performance.

Provide local test history and optional comparison between test sessions.

Never advertise fake RAM cleaning, automatic performance boosts or unsafe task killing.

## 12. Settings, customisation and backup

Provide comprehensive but easy-to-navigate settings.

Include theme controls, animation intensity, wallpaper behaviour, layout preferences, profile management, app visibility, network diagnostics, accessibility and launcher setup.

Support exporting and importing launcher configurations between devices.

Use a versioned backup format with schema validation.

Offer optional password-protected encryption using established cryptographic libraries and secure key derivation.

Never export Android account credentials, private application databases or device-bound secrets.

Handle missing apps and unavailable wallpaper paths gracefully when importing a backup onto another device.

## 13. Extensibility and open-source distribution

Design documented interfaces for future media providers, smart-home dashboards and other integrations.

For the first release, prefer a safe, typed integration API over executing arbitrary untrusted JavaScript or Python inside the launcher.

Do not create an unrestricted code-execution plugin system.

Choose an open-source licence deliberately, considering potential future commercial distribution.

Audit all dependencies, artwork, fonts and bundled assets for redistribution rights.

Do not include advertising SDKs, tracking SDKs, subscriptions, paywalls or premium-locked features.

## 14. Performance requirements

Optimise specifically for Android 11 TV hardware with approximately 2 GB RAM.

Use lazy rendering, cached assets, controlled image decoding, background I/O and lifecycle-aware resource management.

Do not block the main thread with network, storage or media operations.

Measure cold start, warm start, memory usage, navigation latency and wallpaper transition performance.

Target smooth 60 Hz interactions where hardware permits. Do not guarantee a five-second device boot time, since Android system startup is outside the launcher's control.

Ensure the launcher remains useful without internet connectivity.

## 15. Quality assurance

Provide automated tests for settings, backup validation, app discovery, navigation state, profiles and relevant business logic.

Add UI tests for critical navigation flows where practical.

Build a debug APK and provide reproducible build instructions.

Verify that the project compiles without errors.

Document all required permissions and explain why each is necessary.

Perform a security and privacy review of network features, backups, file access and optional companion services.

Provide a device-test checklist covering:

- Initial installation
- Default Home selection
- Home button
- Restart and reboot
- Restoring the stock launcher
- Remote focus navigation
- Keyboard and mouse
- Wallpaper rotation
- USB file access
- App launching
- Network speed testing
- Profile switching
- Backup and restore
- Low-memory behaviour

Clearly distinguish automated verification from tests that require physical hardware.

## 16. Development execution

Work in coordinated phases within one consistent architecture.

**Phase A — Foundation:** Initialise the project, validate dependencies and build the minimum real Home launcher.

**Phase B — Core experience:** Implement Cinematic Glass UI, navigation, installed apps, settings and wallpapers.

**Phase C — Full functionality:** Implement profiles, file management, search, diagnostics, backup and supported integrations.

**Phase D — Advanced features:** Implement the secure phone companion, optional network libraries and extensibility interfaces as time and compatibility permit.

**Phase E — Polish:** Optimise performance, accessibility, animations, error handling, documentation and release readiness.

Do not replace working architecture between phases.

Do not fabricate successful tests or mark unfinished features as complete.

At each milestone, report what was implemented, what was verified and what remains dependent on real-device testing.

## 17. Deliverables

Produce:

1. Complete Android Studio source project.
2. Documented architecture and dependency/licence inventory.
3. Buildable debug APK.
4. Installation and setup instructions.
5. Safe default-launcher and recovery instructions.
6. Testing checklist for both Xiaomi boxes.
7. Known limitations and compatibility report.
8. Release-readiness checklist.
9. Open-source README and licence files.
10. A clear list of optional enhancements for future devices.

**Final objective:** RC TV Launcher should feel like a beautiful, responsive, complete TV experience that the owner can confidently use every day, not merely a demonstration of an attractive interface.
