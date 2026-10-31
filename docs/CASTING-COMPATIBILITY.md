# Xiaomi casting compatibility — Phase D

Observed MiTV-AFKR0 Android 11: Google receiver package `com.google.android.apps.mediashell` is installed, enabled, not suspended/stopped, version 3.72.446070. It exposes no normal Leanback launcher activity. Presence does not prove a functioning casting session. A brief standard mDNS probe from this PC received no Cast advertisement; discovery/network isolation and actual playback remain unverified until a phone sender test. No receiver/default/network settings were changed.

The simplest supported path is the Cast button in an existing compatible phone application, selecting this TV on the same permitted LAN. RC TV can launch installed sender/media applications through normal app discovery, but cannot invent a generic receiver launch activity or start a third-party Cast session without that application's supported integration. No unsupported direct receiver button was added.

Google Cast uses sender/receiver applications; Cast Connect can integrate an Android TV media app with existing Cast infrastructure. It does not make a launcher a universal receiver. Provider accounts, supported receiver IDs, codecs, subscriptions, geo restrictions and DRM remain controlled by the streaming app/provider. YouCine's private keyboard and content flows are not replaced or scraped. Miracast, AirPlay and DLNA receiver support were not established by the package audit.

A separate optional DLNA/UPnP media module is feasible in principle: standards-based discovery, an explicit local media library and a compatible player/renderer. It needs a focused device/protocol test, URL/network validation, metadata/parser bounds and dependency/licence review; it would not supply Google Cast or protected-service rights. Cast SDK/receiver registration and applicable Google terms, plus any media-provider licences, must be checked for a chosen integration. No proprietary protocol emulation, DRM bypass or universal casting claim.

Next acceptance: use a supported phone sender on the same LAN, confirm this TV appears, play permitted media, stop/disconnect, then return to RC TV with the physical remote. Preserve Xiaomi's existing receiver and Home route.

References: [Google Cast overview](https://developers.google.com/cast/docs/overview), [Android TV Cast Connect](https://developers.google.com/cast/docs/android_tv_receiver).
