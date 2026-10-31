package za.co.rc.tv.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import za.co.rc.tv.core.wallpaperScrimAlpha
import za.co.rc.tv.core.BundledWallpaper
import za.co.rc.tv.core.isImportedWallpaper
import za.co.rc.tv.core.WallpaperStore
import za.co.rc.tv.core.RenderDiagnostics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import za.co.rc.tv.R

internal fun BundledWallpaper.imageResource() = when (this) {
    BundledWallpaper.MOUNTAIN -> R.drawable.cinematic_horizon
    BundledWallpaper.COAST -> R.drawable.tidal_dusk
    BundledWallpaper.DESERT -> R.drawable.desert_afterglow
}
internal fun BundledWallpaper.thumbnailResource() = when (this) {
    BundledWallpaper.MOUNTAIN -> R.drawable.cinematic_horizon_thumb
    BundledWallpaper.COAST -> R.drawable.tidal_dusk_thumb
    BundledWallpaper.DESERT -> R.drawable.desert_afterglow_thumb
}

/** One retained full backdrop; serialised IO prevents overlapping preview decodes. Never recycle a published bitmap. */
internal object WallpaperCache {
    private val mutex = Mutex()
    private var cachedId: String? = null
    private var cached: Bitmap? = null
    suspend fun load(context: Context, id: String, visibility: String = "Balanced"): Bitmap? = mutex.withLock {
        currentCoroutineContext().ensureActive()
        val key = "$id:$visibility"
        if (cachedId == key && cached != null) return@withLock cached
        RenderDiagnostics.wallpaperDecoded()
        val result = renderCinematicBackdrop(context, id, visibility)
        currentCoroutineContext().ensureActive()
        if (result != null) { cachedId = key; cached = result }
        result
    }
}

/** Run on IO. The original scrims are baked once, avoiding two full-screen blends per frame. */
internal fun renderCinematicBackdrop(context: Context, id: String = "mountain", visibility: String = "Balanced"): Bitmap? {
    android.os.Trace.beginSection("RC.wallpaper.decodeAndScrim")
    try {
        val resource = BundledWallpaper.fromId(id).imageResource()
        val local = if (isImportedWallpaper(id)) WallpaperStore.image(context, id) else null
        if (local != null && !local.isFile) return renderCinematicBackdrop(context, "mountain", visibility)
        fun decode(options: BitmapFactory.Options): Bitmap? = if (local == null)
            BitmapFactory.decodeResource(context.resources, resource, options) else BitmapFactory.decodeFile(local.path, options)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decode(bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 1152) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample; inScaled = false }
        val source = decode(options) ?: return if (id != "mountain") renderCinematicBackdrop(context, "mountain", visibility) else null
        val result = createBitmap(source.width, source.height)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, 0f, 0f, paint)
        val ink = Color(0xFF0B131B)
        paint.shader = LinearGradient(0f, 0f, result.width.toFloat(), 0f,
            intArrayOf(ink.copy(alpha = wallpaperScrimAlpha(.9f, visibility)).toArgb(), ink.copy(alpha = wallpaperScrimAlpha(.15f, visibility)).toArgb()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), paint)
        paint.shader = LinearGradient(0f, 0f, 0f, result.height.toFloat(),
            intArrayOf(Color.Transparent.toArgb(), ink.copy(alpha = wallpaperScrimAlpha(.3f, visibility)).toArgb(), ink.copy(alpha = wallpaperScrimAlpha(.94f, visibility)).toArgb()),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), paint)
        source.recycle() // private temporary bitmap; never exposed to Compose or a cache
        return result
    } finally {
        android.os.Trace.endSection()
    }
}

internal fun wallpaperThumbnail(context: Context, id: String): Bitmap? = if (isImportedWallpaper(id))
    BitmapFactory.decodeFile(WallpaperStore.image(context, id, true).path)
else BitmapFactory.decodeResource(context.resources, BundledWallpaper.fromId(id).thumbnailResource())
