# Primary Home & startup acceptance — J3C NOT ACCEPTED

Latest TV candidate: 1.0.0-rc1.15 (44), from 4ba8375 / rc1.14. Physical target: Xiaomi MiTV-AFKR0, Android 11. This mandatory gate takes precedence over additional Settings work. The requested seamless primary-launcher behaviour is **not achieved on this tested firmware**. No forced-launch workaround is included.

## Acceptance results (1 November 2026)

| Requirement | Physical result |
| --- | --- |
| Choose RC TV using supported Android UI | The chooser accepts RC TV; the saved preferred Home and Home role name RC TV. This is a stored selection, not proof of operative routing. |
| Recovery to original Home | Selecting Google TV in Android's visible chooser changed the Home role to com.google.android.apps.tv.launcherx; Home opened Google TV correctly. Original package remained installed/enabled. |
| Select RC TV again | The role changed back to za.co.rc.tv, but default resolution remained Google TV. No privileged role/preferred-activity command was used. |
| Physical remote Home | User opened another app, pressed the Xiaomi remote Home button repeatedly, and confirmed Google TV appeared. FAIL. |
| Home from other apps | Three Android Home-key events from Android Settings and from YouTube on the final candidate each returned to Google TV. FAIL. |
| Full reboot, first Home | Full device reboot performed. User observed the original launcher first. Boot event trace shows Android FallbackHome, then Google TV HomeActivity. RC TV MainActivity was not auto-launched. FAIL. |
| Reboot persistence | RC TV's role survived the reboot, but Google TV remained the resolved and foreground Home. The role was not simply reset to stock; operative Home routing ignores the saved RC selection. FAIL for primary Home. |
| Standby / physical wake | Android sleep input put the real device into Asleep (sleep_button). Wireless ADB disconnected. User woke it with the physical remote and saw RC TV; after reconnect, Awake and RC TV MainActivity foreground were verified. PASS for resuming an already-open RC TV activity. |
| Home after wake | Home immediately returned to Google TV; resolution still selected priority-2 Google TV. FAIL for operative RC default. Wake resume does not establish default-Home acceptance. |
| Repeated chooser | No repeated launcher chooser observed during boot/Home/wake; it appears only when deliberately opening Home settings. This does not make wrong-launcher routing acceptable. |
| Original Home intermediate screen | Google TV is the actual boot/Home destination, not an intermediate screen followed by an RC forced launch. FAIL for required direct RC startup. |
| Data preservation | Startup instrumentation confirmed all eight favourites/order/provider, logical pairing grants and selected keyboard were unchanged. Phone app untouched. |

ADB commands in the acceptance session only tested a physical device. They are not bundled app behaviour. The APK has no boot receiver, forced-start service, new permission, custom splash/animation or timer to take over from Google TV. No system package was disabled, removed, suspended or modified. Role changes were made only by selecting rows in Android's supported visible chooser, as explicitly requested by the test brief.

## Why the saved selection does not take effect

Read-only Home-handler inspection reports:

- Google TV HomeActivity: priority 2.
- System Setup Wizard RecoveryActivity: priority 1.
- RC TV MainActivity: priority 0.
- Android FallbackHome: priority -1000.

The preferred-activity record and Home role both name RC TV. Android's documented priority rules consider higher-priority handlers first and cap positive priorities requested by non-privileged applications to zero. The observed priorities are consistent with the mismatch. No ordinary-app manifest-priority adjustment is represented as a supported fix; ROM-specific source was not available to prove every internal policy. This result applies to the tested firmware/device, not every Xiaomi model.

[Android intent-filter priority and ordinary-app cap](https://developer.android.com/guide/topics/manifest/intent-filter-element#priority), [Home role API](https://developer.android.com/reference/android/app/role/RoleManager#ROLE_HOME).

No supported way was found to meet direct boot/Home replacement on this firmware while keeping the original launcher installed and respecting the no-system-package/privileged-workaround constraints. J3C remains blocked on that mandatory criterion.

## Lightweight startup change

Essential installed-app discovery no longer waits behind the wallpaper-library scan. A one-time ViewTreeObserver draw callback releases optional activity-owned work only after Home app content has drawn. It never delays drawing, blocks input or displays a startup screen. Weather HTTP refresh eligibility, network-indicator registration, activity-owned companion resume, wallpaper catalogue scan and keyboard/Home metadata refresh are deferred behind that gate. Existing explicitly enabled system-control services are unchanged.

Physical startup trace verified order: Home content drawn → network indicator enabled → optional resume work. Navigation and protected settings remained working. Cached weather remains displayable while the normal hourly/offline policy is preserved. This follows [Android's advice to defer nonessential initialization](https://developer.android.com/topic/performance/appstartup/best-practices).

Development explicit-open samples, in milliseconds:

| Build | COLD sample 1 | WARM sample | COLD sample 2 |
| --- | ---: | ---: | ---: |
| rc1.14 | 3448 | 3391 | 3372 |
| rc1.15 candidate | 3518 | 3417 | 3502 |

These small debug-build samples do **not** establish a speed improvement; the startup ordering is improved, but the observed explicit-open times were slightly higher. They are not physical boot-to-RC measurements or a faster-than-stock comparison. RC TV did not become the boot-selected launcher. No artificial delay was added to produce the deferral.

## Power off / reboot follow-up

The user and a physical screenshot/XML inspection confirmed that holding the remote Power button opens a system menu offering Restart only. Android About did not offer Restart. RC TV cannot directly invoke shutdown/reboot with normal app privileges: REBOOT is not available to third-party applications, and Android 11's shutdown/reboot activity is protected by SHUTDOWN.

Screensaver & Power now includes **Power off & restart help**, with a focused Close button and verified instructions for holding the physical remote Power button and selecting Restart. The working Android power-settings shortcut remains. This is explicitly guidance, not nonfunctional shutdown/reboot buttons. Standby is distinguished from cutting physical power. No permission request, hidden intent, root, device-owner action or accessibility workaround is introduced.

[Android REBOOT restriction](https://developer.android.com/reference/android/Manifest.permission#REBOOT), [Android 11 protected shutdown activity](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android11-release/core/res/AndroidManifest.xml).

## Verification and evidence

50 unit tests pass. Debug/test APK builds pass. Final lint: 0 errors, 27 existing warnings. Startup-order/D-pad/power-help instrumentation passed on the physical Xiaomi; logical pairing records and selected keyboard were checked without exposing credentials. Approved Home card source geometry and design are unchanged.

Local evidence: releases/review/phase-j3c/primary-home-selection.xml, original-home-selected.xml, preferred-before.xml, home-handlers-before-reboot.txt, role-before-reboot.txt, role-after-reboot.txt, activity-after-reboot.txt, boot-activity-events.txt, after-reboot.png, standby.txt, wake.txt, activity-after-wake.txt, power-menu.png, power-menu.xml, power-help.png, startup-order-final.txt, startup-before.txt and startup-after.txt. User's physical remote/boot/wake observations are recorded in this chat and summarized above.

Final APK: releases/phase-j3c/RC-TV-Launcher-1.0.0-rc1.15-debug.apk. Rollback: releases/rollback/RC-TV-Launcher-4ba8375-debug.apk (rc1.14); all earlier checkpoints remain preserved. PR #2 remains unmerged. The user-managed usage reset was not initiated.

Delivered APK SHA256: 395538656B044832B5ABC88CDFB3F1AE23174BA6B5C9AAC4F6F72DC9E6BDD947. The Android manifest is byte-identical to 4ba8375; no receivers, services, permissions or Home-filter priorities changed. Final saved APK matches the built and installed candidate.
