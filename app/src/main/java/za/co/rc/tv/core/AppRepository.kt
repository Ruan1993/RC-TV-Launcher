package za.co.rc.tv.core

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LauncherApp(val packageName: String, val label: String)

object AppOrdering {
    private val preferred = listOf("youcine", "youtube music", "youtube", "f1 tv")
    fun sort(apps: List<LauncherApp>): List<LauncherApp> = apps.distinctBy { it.packageName }
        .sortedWith(compareBy<LauncherApp> { app -> preferred.indexOfFirst { app.label.lowercase().contains(it) }.let { if (it < 0) 100 else it } }.thenBy { it.label.lowercase() })
}

class AppRepository(private val context: Context) {
    suspend fun discover(): List<LauncherApp> = withContext(Dispatchers.IO) {
        AppOrdering.sort(listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER).flatMap { category ->
            context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .map { LauncherApp(it.activityInfo.packageName, it.loadLabel(context.packageManager).toString()) }
        })
    }
    fun launch(app: LauncherApp): Boolean = runCatching {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(app.packageName) ?: pm.getLaunchIntentForPackage(app.packageName)
        requireNotNull(intent)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}
