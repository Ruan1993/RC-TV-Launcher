package za.co.rc.tv.core

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

data class HomeIntegrationState(val resolvedPackage: String?, val resolvedLabel: String,
    val roleAvailable: Boolean, val roleHeld: Boolean, val homeSettingsAvailable: Boolean) {
    fun description(ownPackage: String): String = when {
        resolvedPackage == ownPackage -> "RC TV currently handles Android Home."
        roleHeld -> "Android grants RC TV the Home role, but Home still resolves to $resolvedLabel. This firmware may override launcher selection."
        roleAvailable -> "Current Home: $resolvedLabel. Android can request your consent to choose RC TV."
        homeSettingsAvailable -> "Current Home: $resolvedLabel. Choose a launcher in Android Home settings, if this firmware permits it."
        else -> "Current Home: $resolvedLabel. This firmware does not expose supported Home selection. RC TV remains available as a normal app."
    }
}

object HomeIntegration {
    fun homeSettings(context: Context): Intent? {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        val activity = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo ?: return null
        return intent.takeUnless { activity.name.endsWith("EmptyStubActivity") || activity.packageName == "com.google.android.tv.frameworkpackagestubs" }
    }
    fun inspect(context: Context): HomeIntegrationState {
        val current = context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
        val roles = context.getSystemService(RoleManager::class.java)
        val available = runCatching { roles.isRoleAvailable(RoleManager.ROLE_HOME) }.getOrDefault(false)
        return HomeIntegrationState(current?.activityInfo?.packageName, current?.activityInfo?.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: "System chooser / unavailable",
            available, available && runCatching { roles.isRoleHeld(RoleManager.ROLE_HOME) }.getOrDefault(false), homeSettings(context) != null)
    }
}
