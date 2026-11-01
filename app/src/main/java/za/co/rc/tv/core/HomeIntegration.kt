package za.co.rc.tv.core

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

data class HomeIntegrationState(val resolvedPackage: String?, val resolvedLabel: String,
    val roleAvailable: Boolean, val roleHeld: Boolean, val homeSettingsAvailable: Boolean) {
    fun description(ownPackage: String): String = when {
        resolvedPackage == null -> "Current Home: unknown. Android has not exposed a reliable default. Open Home selection to check."
        resolvedPackage == ownPackage -> "RC TV currently handles Android Home."
        roleHeld -> "Android grants RC TV the Home role, but Home still resolves to $resolvedLabel. This firmware may override launcher selection."
        roleAvailable -> "Current Home: $resolvedLabel. Android can request your consent to choose RC TV."
        homeSettingsAvailable -> "Current Home: $resolvedLabel. Choose a launcher in Android Home settings, if this firmware permits it."
        else -> "Current Home: $resolvedLabel. This firmware does not expose supported Home selection. RC TV remains available as a normal app."
    }
}

object HomeIntegration {
    fun originalHomeRecovery(context: Context): OriginalHomeRecovery {
        fun disabled(packageName: String): Boolean? = try {
            !context.packageManager.getApplicationInfo(packageName, PackageManager.MATCH_DISABLED_COMPONENTS).enabled
        } catch (_: PackageManager.NameNotFoundException) { null }
          catch (_: SecurityException) { null }
        return OriginalHomeRecovery(disabled("com.google.android.apps.tv.launcherx"),
            android.os.Process.myUserHandle() == android.os.UserHandle.getUserHandleForUid(0) &&
                android.os.Build.MODEL == "MiTV-AFKR0" && android.os.Build.FINGERPRINT ==
                "Xiaomi/jaws/jaws:11/RTT0.211222.001/774:user/release-keys",
            disabled("com.google.android.tungsten.setupwraith") == true)
    }
    fun homeSettings(context: Context): Intent? {
        return resolvedSystemSettings(context, listOf(Settings.ACTION_HOME_SETTINGS))
    }
    fun inspect(context: Context): HomeIntegrationState {
        val current = context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
        val roles = context.getSystemService(RoleManager::class.java)
        val available = runCatching { roles.isRoleAvailable(RoleManager.ROLE_HOME) }.getOrDefault(false)
        val activity = current?.activityInfo?.takeIf { reliableHomeTarget(it.packageName, it.name) }
        return HomeIntegrationState(activity?.packageName, activity?.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: "Unknown / Android chooser",
            available, available && runCatching { roles.isRoleHeld(RoleManager.ROLE_HOME) }.getOrDefault(false), homeSettings(context) != null)
    }
}

/** Read-only guidance: package mutation is deliberately outside this interface. */
data class OriginalHomeRecovery(val googleTvDisabled: Boolean?, val verifiedXiaomi774: Boolean, val setupDisabled: Boolean) {
    val useHomeSelection get() = googleTvDisabled != true
    val buttonLabel get() = when (googleTvDisabled) {
        true -> "Original Google TV recovery guidance"
        false -> "Switch to Original Xiaomi Home"
        null -> "Open Android Home selection"
    }
    val description get() = when (googleTvDisabled) {
        true -> "Google TV is installed but disabled for this Android user. Home selection cannot restore a disabled launcher. Open recovery guidance; RC TV will not enable packages automatically."
        false -> "Google TV is enabled. Choose it in Android’s Home selection to switch launchers. This does not change package states or RC TV’s Accessibility Home handling."
        null -> "Home selection lists available, enabled launchers. If the original launcher is missing, check Android Settings → Apps → Show system apps. Selecting Home does not enable disabled packages."
    }
    val recoverySteps get() = if (verifiedXiaomi774 && setupDisabled) {
        "On this Xiaomi firmware 774, Google TV and Android TV Setup are disabled for user 0. " +
        "Use the documented recovery procedure from an authorised ADB computer. Restore Google TV first, then Setup; keep RC TV installed.\n\n" +
        "Run these commands through adb shell, in order:\n" +
        "pm enable --user 0 com.google.android.apps.tv.launcherx\n" +
        "pm default-state --user 0 com.google.android.apps.tv.launcherx\n" +
        "pm enable --user 0 com.google.android.tungsten.setupwraith\n" +
        "pm default-state --user 0 com.google.android.tungsten.setupwraith\n\n" +
        "The original package states were DEFAULT. The saved Home role already named RC TV. If needed, restore that baseline with:\n" +
        "cmd package set-home-activity --user 0 za.co.rc.tv/.MainActivity\n" +
        "am start --user 0 -n com.google.android.apps.tv.launcherx/.home.HomeActivity\n" +
        "am start --user 0 -n com.android.tv.settings/.MainSettings\n\n" +
        "If offline, use Android Settings → Network & Internet to reconnect, or Apps → Show system apps to inspect disabled apps. " +
        "The exact Enable controls have not been physically tested. Do not reboot on a recovery screen or unstable Home. " +
        "This restores the documented package/Home baseline; it does not disable RC TV’s Accessibility Home interception. " +
        "See docs/XIAOMI-774-WORKING-TAKEOVER.md for the full procedure."
    } else {
        "Open Android Settings → Apps → Show system apps and inspect Google TV. " +
        "A disabled launcher must be enabled through Android Settings or an authorised administrator before Home selection can offer it. " +
        "Use recovery instructions specific to this device; do not copy Xiaomi firmware 774 commands onto other devices. " +
        "No package or Accessibility changes are made by this guidance."
    }
}

fun reliableHomeTarget(packageName: String, className: String) = packageName != "android" &&
    packageName != "com.google.android.tv.frameworkpackagestubs" &&
    !className.endsWith("ResolverActivity") && !className.endsWith("ChooserActivity") && !className.contains("EmptyStubActivity")
