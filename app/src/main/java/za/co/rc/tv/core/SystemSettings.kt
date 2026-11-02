package za.co.rc.tv.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.Toast

data class SystemDestination(val key: String, val label: String, val actions: List<String>, val route: String)
val androidSettingsDestination = SystemDestination("android", "Android settings", listOf(Settings.ACTION_SETTINGS), "Android Settings")
val appSettingsDestination = SystemDestination("apps", "Android app settings", listOf(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS), "Apps → See all apps")
val storageSettingsDestination = SystemDestination("storage", "Storage & USB settings", listOf(Settings.ACTION_INTERNAL_STORAGE_SETTINGS, Settings.ACTION_MEMORY_CARD_SETTINGS), "System → Storage")
val essentialSystemDestinations = listOf(
    androidSettingsDestination,
    SystemDestination("about", "Device information / About", listOf(Settings.ACTION_DEVICE_INFO_SETTINGS), "System → About"),
    SystemDestination("display", "Display & sound settings", listOf(Settings.ACTION_DISPLAY_SETTINGS, Settings.ACTION_SOUND_SETTINGS), "Display and sound"),
    SystemDestination("date", "Date & time settings", listOf(Settings.ACTION_DATE_SETTINGS), "System → Date and time"),
    storageSettingsDestination,
    // Documented settings action, also exported by this Android 11 Xiaomi firmware.
    SystemDestination("updates", "System updates", listOf("android.settings.SYSTEM_UPDATE_SETTINGS"), "System → About → System update"),
    SystemDestination("accessibility", "Accessibility settings", listOf(Settings.ACTION_ACCESSIBILITY_SETTINGS), "System → Accessibility"))

fun usableSystemSettingsTarget(packageName: String, className: String, exported: Boolean, system: Boolean) =
    exported && system && packageName != "com.google.android.tv.frameworkpackagestubs" &&
        !className.contains("EmptyStubActivity") && !className.endsWith("ResolverActivity")

/** Resolve exported system handlers only; never rely on an OEM private class name. */
fun resolvedSystemSettings(context: Context, actions: List<String>, data: android.net.Uri? = null): Intent? {
    val pm = context.packageManager
    for (action in actions) {
        val candidates = pm.queryIntentActivities(Intent(action, data), PackageManager.MATCH_DEFAULT_ONLY)
            .filter { info -> info.activityInfo?.let { activity ->
                usableSystemSettingsTarget(activity.packageName, activity.name, activity.exported,
                    activity.applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) &&
                    (action != "android.settings.SYSTEM_UPDATE_SETTINGS" || info.priority > 0)
            } == true }.sortedByDescending { it.priority }
        candidates.firstOrNull()?.activityInfo?.let {
            return Intent(action, data).setClassName(it.packageName, it.name)
        }
    }
    return null
}

fun openSystemDestination(context: Context, destination: SystemDestination): Boolean {
    val direct = resolvedSystemSettings(context, destination.actions)
    if (direct != null && runCatching { context.startActivity(direct); true }.getOrDefault(false)) return true
    val fallback = resolvedSystemSettings(context, listOf(Settings.ACTION_SETTINGS))
    if (fallback != null && runCatching { context.startActivity(fallback); true }.getOrDefault(false)) {
        Toast.makeText(context, "In Android Settings: ${destination.route}", Toast.LENGTH_LONG).show()
        return true
    }
    Toast.makeText(context, "Android Settings unavailable. Open Settings from the original launcher.", Toast.LENGTH_LONG).show()
    return false
}

data class InternalStorageSpace(val availableBytes: Long, val totalBytes: Long)
fun readInternalStorage(): InternalStorageSpace? = runCatching {
    val stats = StatFs(Environment.getDataDirectory().absolutePath)
    InternalStorageSpace(stats.availableBytes, stats.totalBytes)
}.getOrNull()
