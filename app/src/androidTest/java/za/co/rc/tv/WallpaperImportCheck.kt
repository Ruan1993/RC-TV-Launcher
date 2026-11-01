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
    private var j1Args: Bundle?=null
    private var refinementMode = ""
    private var weatherLarge = false
    private var weatherLive = false
    private var weather = false
    private var icons = false
    private var overdue = false
    private var keyboard = false
    private var appearance: Bundle? = null
    override fun onCreate(arguments: Bundle?) { j1Args=arguments; refinementMode=arguments?.getString("mode").orEmpty(); weatherLarge = arguments?.getString("large") == "true"; weatherLive = arguments?.getString("mode") == "weatherlive"; weather = arguments?.getString("mode") == "weather"; icons = arguments?.getString("mode") == "icons"; overdue = arguments?.getString("mode") == "overdue"; keyboard = arguments?.getString("mode") == "keyboard"; appearance = arguments?.takeIf { it.getString("mode") == "appearance" }; super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        try {
            if(refinementMode=="j3cvisual") { J3CReview.largeTextAndStorageStats(this); result.putString("verification","Extra Large Settings, provider chooser and reset confirmation checked; own installed storage measured"); finish(Activity.RESULT_OK,result); return }
            if(refinementMode=="j3creset") { J3CReview.confirmedReset(this); result.putString("verification","Confirmed appearance reset preserved favourites/order, logical pairing, keyboard and screensaver settings; original appearance restored"); finish(Activity.RESULT_OK,result); return }
            if(refinementMode=="j3clauncher") { J3CReview.launcher(this); result.putString("verification","Launcher management, honest Home status, reset Cancel and supported Home selection entry passed"); finish(Activity.RESULT_OK,result); return }
            if(refinementMode=="j3cprovider") { J3CReview.providers(this); result.putString("verification","Provider switching, RC auto suppression, preview, saved choices and favourites passed"); finish(Activity.RESULT_OK,result); return }
            if(refinementMode=="j3cproviderpersist") { J3CReview.providerPersistence(this); result.putString("verification","Provider, interval, style and favourites persisted after process restart"); finish(Activity.RESULT_OK,result); return }
            if(refinementMode=="j3csettings") { J3CReview.settings(this,weatherLarge); result.putString("verification","Essential settings destinations, real storage, D-pad activation, Back return and preference preservation passed"); finish(Activity.RESULT_OK,result); return }
            if(refinementMode=="j3bsaver") { ScreensaverReview.run(this,weatherLarge); result.putString("verification","Screensaver settings persistence, three previews, focus restoration, power shortcuts, accelerated idle/Never and external-app suppression passed"); finish(Activity.RESULT_OK,result); return }
            if(refinementMode=="j1home") { HomeJ1Review.run(this,j1Args!!); result.putString("verification","Clean Home, original card dimensions and fixed Settings checked"); finish(Activity.RESULT_OK,result); return }
            if(refinementMode=="backdrops") { BackdropReview.run(this); result.putString("verification","Rendered all three modes of the same mountain wallpaper"); finish(Activity.RESULT_OK,result); return }
            if(refinementMode=="settings") { SettingsReviewCheck.run(this,weatherLarge); result.putString("verification","All Settings categories, existing control access, wallpaper/utility Back and category focus restoration passed"); finish(Activity.RESULT_OK,result); return }
            if (weatherLive) { WeatherLiveReview.run(this,weatherLarge); result.putString("verification", "Real Open-Meteo search and conditions rendered for Pretoria on isolated emulator; original weather restored"); finish(Activity.RESULT_OK, result); return }
            if (weather) { WeatherReviewCheck.run(this); result.putString("verification", "Weather off, cache, process-style reload, hourly throttle, offline fallback, stale cutoff and removal passed"); finish(Activity.RESULT_OK, result); return }
            if (icons) { IconReviewCheck.run(this); result.putString("verification", "Both adaptive and monochrome icons rendered"); finish(Activity.RESULT_OK, result); return }
            appearance?.let { AppearanceReviewCheck.run(this, it); result.putString("verification", "Saved appearance and unchanged, visible 216 x 190 dp card passed"); finish(Activity.RESULT_OK, result); return }
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
