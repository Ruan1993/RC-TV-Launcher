package za.co.rc.tv.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.URL
import java.nio.ByteBuffer
import javax.net.ssl.HttpsURLConnection

/** Public searchable Watch Next data only. No writes, preview/history inference or privileged permission. */
internal class WatchNextRepository(context: Context) {
    private val context = context.applicationContext
    private val resolver = context.contentResolver
    private val uri = Uri.parse("content://android.media.tv/watch_next_program")
    private val images = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private val decoding = Semaphore(2)
    fun permitted() = context.checkSelfPermission(TV_LISTINGS_PERMISSION) == PackageManager.PERMISSION_GRANTED
    fun changes() = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        resolver.registerContentObserver(uri, true, observer)
        trySend(Unit)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }
    suspend fun entries(): List<ContinueItem> = withContext(Dispatchers.IO) {
        if (!permitted()) return@withContext emptyList()
        val result = mutableListOf<ContinueItem>()
        val projection = arrayOf("_id", "package_name", "title", "intent_uri", "poster_art_uri", "watch_next_type", "browsable", "last_playback_position_millis", "duration_millis")
        val cancellation = CancellationSignal()
        val job = currentCoroutineContext()[kotlinx.coroutines.Job]
        val cancellationHandle = job?.invokeOnCompletion { if (job.isCancelled) cancellation.cancel() }
        try {
            // Unprivileged readers must not supply SQL selection; filter the bounded cursor locally.
            resolver.query(uri, projection, null, null, "last_engagement_time_utc_millis DESC", cancellation)?.use { cursor ->
                var read = 0
                while (read++ < 200 && result.size < 16 && cursor.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    val item = continueItem(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4),
                        if (cursor.isNull(5)) null else cursor.getInt(5), cursor.getInt(6) == 1,
                        if (cursor.isNull(7)) null else cursor.getLong(7), if (cursor.isNull(8)) null else cursor.getLong(8)) ?: continue
                    if (launchIntent(item) != null) result += item
                }
            }
            result.distinctBy { it.id }
        } finally { cancellationHandle?.dispose() }
    }
    private fun launchIntent(item: ContinueItem): Intent? = runCatching {
        val app = context.packageManager.getApplicationInfo(item.packageName, 0)
        require(app.enabled && app.flags and android.content.pm.ApplicationInfo.FLAG_SUSPENDED == 0)
        val intent = Intent.parseUri(item.launchUri, Intent.URI_INTENT_SCHEME or Intent.URI_ANDROID_APP_SCHEME)
        require(intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_MAIN)
        require(intent.component == null || intent.component!!.packageName == item.packageName)
        intent.selector = null
        intent.clipData = null
        intent.setPackage(item.packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // Never forward provider-controlled grant/task flags.
        val target = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
        require(target != null && target.exported && target.packageName == item.packageName &&
            (target.permission == null || context.checkSelfPermission(target.permission) == PackageManager.PERMISSION_GRANTED))
        intent
    }.getOrNull()
    fun launch(item: ContinueItem): Boolean = launchIntent(item)?.let { runCatching { context.startActivity(it) }.isSuccess } ?: false
    fun appLabel(item: ContinueItem): String = runCatching {
        context.packageManager.getApplicationInfo(item.packageName, 0).loadLabel(context.packageManager).toString()
    }.getOrDefault(item.packageName)

    suspend fun artwork(value: String?): Bitmap? = withContext(Dispatchers.IO) { decoding.withPermit {
        if (value == null) return@withPermit null
        images.get(value)?.let { return@withPermit it }
        var connection: HttpsURLConnection? = null
        try {
            val art = Uri.parse(value)
            val input = when (art.scheme) {
                "content" -> resolver.openInputStream(art)
                "https" -> {
                    val url = URL(value)
                    require(url.userInfo == null)
                    connection = (url.openConnection() as HttpsURLConnection).apply {
                        connectTimeout = 4000; readTimeout = 4000; instanceFollowRedirects = false
                    }
                    require(connection!!.responseCode == 200)
                    connection!!.inputStream
                }
                else -> null
            } ?: return@withPermit null
            val bytes = input.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val block = ByteArray(8192)
                val deadline = System.nanoTime() + 8_000_000_000L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    check(System.nanoTime() < deadline)
                    val count = stream.read(block); if (count < 0) break
                    require(output.size() + count <= 2 * 1024 * 1024)
                    output.write(block, 0, count)
                }
                output.toByteArray()
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0 && bounds.outWidth.toLong() * bounds.outHeight <= 40_000_000)
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
                val scale = minOf(1.0, 480.0 / info.size.width, 270.0 / info.size.height)
                decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setOnPartialImageListener { false }
            }
            try { currentCoroutineContext().ensureActive() }
            catch (e: kotlinx.coroutines.CancellationException) { bitmap.recycle(); throw e }
            images.put(value, bitmap)
            bitmap
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
          catch (_: Exception) { null }
        finally { connection?.disconnect() }
    } }
}
