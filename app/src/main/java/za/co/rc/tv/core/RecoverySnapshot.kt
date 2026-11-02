package za.co.rc.tv.core

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityManager
import org.json.JSONObject

data class OriginalHome(val component: String, val label: String, val enabled: Boolean, val override: Int, val componentOverride: Int)

object RecoverySnapshot {
    fun candidates(context: Context): List<OriginalHome> = runCatching {
        val pm = context.packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { result -> result.activityInfo?.takeIf {
                it.packageName != context.packageName && reliableHomeTarget(it.packageName, it.name) &&
                    it.applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            }?.let { info -> OriginalHome(ComponentName(info.packageName, info.name).flattenToString(),
                info.loadLabel(pm).toString(), info.enabled && info.applicationInfo.enabled,
                pm.getApplicationEnabledSetting(info.packageName),
                pm.getComponentEnabledSetting(ComponentName(info.packageName, info.name))) } }.distinctBy { it.component }
    }.getOrDefault(emptyList())

    fun interceptionEnabled(context: Context): Boolean = runCatching {
        context.getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.let { service ->
                service.packageName == context.packageName && service.name.endsWith("HomeKeyExperimentService")
            } }
    }.getOrDefault(true) // Unknown is not safe evidence for a Home verification.

    /** Preserve observations only. Current state is never mislabelled as a pre-takeover baseline. */
    fun capture(context: Context): String {
        val storage = context.getSharedPreferences("recovery-observation-v1", Context.MODE_PRIVATE)
        storage.getString("snapshot", null)?.let { return it }
        val home = HomeIntegration.inspect(context)
        val record = JSONObject().put("schema", 1).put("fingerprint", android.os.Build.FINGERPRINT)
            .put("user", android.os.Process.myUserHandle().toString()).put("capturedAt", System.currentTimeMillis())
            .put("resolvedHome", home.resolvedPackage ?: JSONObject.NULL).put("activationMethod", "unknown")
            .put("provenance", "Observed on first recovery inspection; not an original package baseline")
            .put("candidates", org.json.JSONArray().apply { candidates(context).forEach {
                put(JSONObject().put("component", it.component).put("enabled", it.enabled).put("override", it.override).put("componentOverride", it.componentOverride))
            } }).toString()
        check(storage.edit().putString("snapshot", record).commit()) { "Recovery information could not be saved." }
        return record
    }
}
