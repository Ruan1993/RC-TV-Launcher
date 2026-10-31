package za.co.rc.tv

import android.app.Instrumentation
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import java.io.File

/** Installed Android drawables, not a separately drawn approximation. Emulator only. */
object IconReviewCheck {
    fun run(test: Instrumentation) {
        val canvasBitmap = Bitmap.createBitmap(960, 420, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap); canvas.drawColor(Color.rgb(9,21,31))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 24f }
        listOf("za.co.rc.tv", "za.co.rc.tv.phone").forEachIndexed { row, pkg ->
            val resources = test.targetContext.packageManager.getResourcesForApplication(pkg)
            val id = resources.getIdentifier("ic_launcher", "mipmap", pkg)
            val icon = resources.getDrawable(id, test.targetContext.theme) as AdaptiveIconDrawable
            check(icon.monochrome != null) { "Missing monochrome" }
            for (column in 0..3) {
                val x = 25 + column * 235; val y = 25 + row * 200
                canvas.save(); canvas.translate(x.toFloat(), y.toFloat())
                val mask = Path().apply { if (column == 0) addCircle(80f,80f,80f,Path.Direction.CW) else addRoundRect(0f,0f,160f,160f,if (column == 1) 32f else 12f,if (column == 1) 32f else 12f,Path.Direction.CW) }
                canvas.clipPath(mask)
                if (column == 3) {
                    canvas.drawColor(Color.rgb(202,233,225))
                    icon.monochrome!!.mutate().apply { setTint(Color.rgb(16,39,51)); setBounds(-40,-40,200,200); draw(canvas) }
                } else {
                    icon.background.setBounds(-40,-40,200,200); icon.background.draw(canvas)
                    icon.foreground.setBounds(-40,-40,200,200); icon.foreground.draw(canvas)
                }
                canvas.restore()
            }
            canvas.drawText(if (row == 0) "RC TV Launcher" else "RC TV Remote", 25f, 210f + row * 200, paint)
        }
        File(test.targetContext.getExternalFilesDir(null), "icon-review.png").outputStream().use { canvasBitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; canvasBitmap.recycle()
    }
}
