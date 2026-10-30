package za.co.rc.tv.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import za.co.rc.tv.R

/** Run on IO. The original scrims are baked once, avoiding two full-screen blends per frame. */
internal fun renderCinematicBackdrop(context: Context): Bitmap? {
    android.os.Trace.beginSection("RC.wallpaper.decodeAndScrim")
    try {
        val source = BitmapFactory.decodeResource(context.resources, R.drawable.cinematic_horizon) ?: return null
        val result = createBitmap(source.width, source.height)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, 0f, 0f, paint)
        val ink = Color(0xFF0B131B)
        paint.shader = LinearGradient(0f, 0f, result.width.toFloat(), 0f,
            intArrayOf(ink.copy(alpha = .9f).toArgb(), ink.copy(alpha = .15f).toArgb()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), paint)
        paint.shader = LinearGradient(0f, 0f, 0f, result.height.toFloat(),
            intArrayOf(Color.Transparent.toArgb(), ink.copy(alpha = .3f).toArgb(), ink.copy(alpha = .94f).toArgb()),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), paint)
        source.recycle() // private temporary bitmap; never exposed to Compose or a cache
        return result
    } finally {
        android.os.Trace.endSection()
    }
}
