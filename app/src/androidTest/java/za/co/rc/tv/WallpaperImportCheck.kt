package za.co.rc.tv

import android.app.Instrumentation
import android.app.Activity
import android.os.Bundle
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import za.co.rc.tv.core.WallpaperStore
import java.io.File

/** Targeted platform-decoder check; lives only in the separate test APK. */
class WallpaperImportCheck : Instrumentation() {
    private var overdue = false
    private var keyboard = false
    override fun onCreate(arguments: Bundle?) { overdue = arguments?.getString("mode") == "overdue"; keyboard = arguments?.getString("mode") == "keyboard"; super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        try {
            if (keyboard) {
                KeyboardEditingCheck.run(this)
                result.putString("verification", "Native editor: cursor movement, middle insertion, selection deletion, Unicode, empty fields and repeat passed")
                finish(Activity.RESULT_OK, result); return
            }
            if (overdue) {
                runBlocking {
                    val job = Job(); val scope = CoroutineScope(Dispatchers.IO + job)
                    val data = PreferenceDataStoreFactory.create(scope = scope) { targetContext.preferencesDataStoreFile("launcher") }
                    data.edit { it[longPreferencesKey("wallpaper_rotated_at")] = System.currentTimeMillis() - 600_000L }
                    scope.cancel(); job.join()
                }
                result.putString("verification", "App-only overdue timestamp fixture set; original preferences preserved")
                finish(Activity.RESULT_OK, result); return
            }
            runBlocking {
                val store = WallpaperStore(targetContext)
                val fixture = Bitmap.createBitmap(4000, 2200, Bitmap.Config.RGB_565)
                try {
                    for ((ext, format) in listOf("jpg" to Bitmap.CompressFormat.JPEG, "png" to Bitmap.CompressFormat.PNG, "webp" to Bitmap.CompressFormat.WEBP_LOSSY)) {
                        File(targetContext.filesDir, "import-fixture.$ext").outputStream().use { fixture.compress(format, 85, it) }
                    }
                } finally { fixture.recycle() }
                File(targetContext.filesDir, "import-fixture-corrupt.jpg").writeText("invalid image")
                val ids = mutableListOf<String>()
                for (ext in listOf("jpg", "png", "webp")) {
                    val source = File(targetContext.filesDir, "import-fixture.$ext")
                    val id = store.import(Uri.fromFile(source))
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(WallpaperStore.image(targetContext, id).path, bounds)
                    check(bounds.outWidth in 1..1920 && bounds.outHeight in 1..1080)
                    check(WallpaperStore.image(targetContext, id, true).isFile)
                    ids += id
                }
                check(runCatching { store.import(Uri.fromFile(File(targetContext.filesDir, "import-fixture-corrupt.jpg"))) }.isFailure)
                check(store.list().count { it.id in ids } == 3)
                result.putString("imports", ids.joinToString(","))
                result.putString("verification", "JPEG/PNG/WebP normalization, bounded dimensions, thumbnail generation and corrupt input rejection passed")
            }
            finish(Activity.RESULT_OK, result)
        } catch (failure: Throwable) {
            result.putString("failure", failure.stackTraceToString());finish(Activity.RESULT_CANCELED, result)
        }
    }
}
