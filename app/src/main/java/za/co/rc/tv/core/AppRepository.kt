package za.co.rc.tv.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LauncherApp(val packageName: String, val label: String)

object AppOrdering {
    private val preferred = listOf("youcine", "youtube music", "youtube", "f1 tv")
    fun sort(apps: List<LauncherApp>): List<LauncherApp> = apps.distinctBy { it.packageName }
        .sortedWith(compareBy<LauncherApp> { app -> preferred.indexOfFirst { app.label.lowercase().contains(it) }.let { if (it < 0) 100 else it } }.thenBy { it.label.lowercase() })
}

class AppRepository(private val context: Context) {
    // At most 2 MiB of rasterised icons; no full-size drawables are retained.
    private val icons = object : LruCache<String, Bitmap>(2 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    fun invalidateIcons() = icons.evictAll()

    suspend fun icon(packageName: String, sizeDp: Int = 48): Bitmap? = withContext(Dispatchers.IO) {
        val size = (sizeDp * context.resources.displayMetrics.density).toInt().coerceIn(48, 192)
        val cacheKey = "$packageName@$size"
        icons.get(cacheKey) ?: try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            RenderDiagnostics.iconRasterized()
            val bitmap = createBitmap(size, size)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
            icons.put(cacheKey, bitmap)
            bitmap
        } catch (failure: Exception) {
            android.util.Log.w("RcIcons", "Unable to load installed app icon", failure)
            null
        }
    }
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
