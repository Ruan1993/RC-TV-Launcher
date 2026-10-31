package za.co.rc.tv

import android.app.Instrumentation
import android.graphics.Bitmap
import java.io.File
import za.co.rc.tv.ui.renderCinematicBackdrop

/** Same renderer call can run against baseline/new APKs. No preferences or cached product images are changed. */
object BackdropReview {
    fun run(test: Instrumentation) {
        for(mode in listOf("Subtle","Balanced","Vivid")) {
            val bitmap=renderCinematicBackdrop(test.targetContext,"mountain",mode) ?: error("No backdrop")
            File(test.targetContext.getExternalFilesDir(null),"backdrop-${mode.lowercase()}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
        }
    }
}
