package za.co.rc.tv.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer

data class OnlineWallpaper(val page: Long, val revision: String, val title: String, val thumbnail: String,
    val width: Int, val height: Int, val credit: String, val license: String) {
    val id get() = "local_" + OnlineWallpaperPolicy.key("commons:$page:$revision").take(32)
    val attribution get() = "$title\n$credit\nhttps://commons.wikimedia.org/?curid=$page\n$license\nResized/re-encoded by RC TV. Image and modifications retain this licence."
}
data class OnlineWallpaperCursor(val member: String, val common: String)
data class OnlineWallpaperPage(val images: List<OnlineWallpaper>, val next: OnlineWallpaperCursor?)

/** Public Action API. No credentials, scraping, telemetry, polling or prefetching. */
class OnlineWallpapers(context: Context) {
    private val context = context.applicationContext
    private val cache = WallpaperThumbnailCache(java.io.File(context.cacheDir, "online-wallpaper-thumbnails"))
    private val downloads = Semaphore(2)
    private fun parameters(extra: Map<String, String>) = mapOf("action" to "query", "format" to "json", "formatversion" to "2",
        "prop" to "imageinfo", "iiprop" to "url|size|mime|sha1|extmetadata", "iiextmetadatafilter" to "Artist|Credit|Attribution|LicenseUrl|ObjectName") + extra
    private suspend fun query(extra: Map<String, String>): JSONObject {
        val url = "https://commons.wikimedia.org/w/api.php?" + parameters(extra).entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8") }
        val data = JSONObject(String(fetch(url, 1024 * 1024, false), Charsets.UTF_8))
        if (data.has("error")) throw IOException("Wikimedia is temporarily unavailable. Try again later.")
        return data
    }
    suspend fun page(continuation: OnlineWallpaperCursor?): OnlineWallpaperPage = withContext(Dispatchers.IO) {
        val data = query(mapOf("generator" to "categorymembers", "gcmtitle" to "Category:Featured pictures of landscapes",
            "gcmtype" to "file", "gcmlimit" to "12", "iiurlwidth" to "480") +
            (continuation?.let { mapOf("gcmcontinue" to it.member, "continue" to it.common) } ?: emptyMap()))
        val pages = data.optJSONObject("query")?.optJSONArray("pages")
        val images = (0 until (pages?.length() ?: 0)).mapNotNull { index ->
            val p = pages!!.getJSONObject(index)
            val info = p.optJSONArray("imageinfo")?.optJSONObject(0) ?: return@mapNotNull null
            val meta = info.optJSONObject("extmetadata") ?: return@mapNotNull null
            fun text(key: String) = Html.fromHtml(meta.optJSONObject(key)?.optString("value").orEmpty(), Html.FROM_HTML_MODE_LEGACY).toString().trim()
            val license = OnlineWallpaperPolicy.license(text("LicenseUrl")) ?: return@mapNotNull null
            val artist = text("Artist")
            val title = text("ObjectName").ifBlank { p.getString("title").removePrefix("File:").substringBeforeLast('.') }
            val credit = listOf(artist, text("Credit"), text("Attribution")).filter { it.isNotBlank() }.distinct().joinToString("\n")
            val thumb = info.optString("thumburl")
            val width = info.optInt("width"); val height = info.optInt("height")
            val revision = info.optString("sha1")
            if (artist.isBlank() || title.length > 512 || credit.length > 4096 || revision.isBlank() || !OnlineWallpaperPolicy.assetUrl(thumb) ||
                info.optString("mime") !in setOf("image/jpeg", "image/png", "image/webp") || width < height || height < 300) return@mapNotNull null
            OnlineWallpaper(p.getLong("pageid"), revision, title, thumb, width, height, credit, license)
        }
        val next = data.optJSONObject("continue")?.let {
            val member = it.optString("gcmcontinue"); val common = it.optString("continue")
            if (member.isNotBlank() && member.length < 2048 && common.isNotBlank() && common.length < 128) OnlineWallpaperCursor(member, common) else null
        }
        OnlineWallpaperPage(images, next)
    }
    suspend fun thumbnail(image: OnlineWallpaper): Bitmap = withContext(Dispatchers.IO) { downloads.withPermit {
        val key = OnlineWallpaperPolicy.key(image.thumbnail)
        val bytes = cache.read(key) ?: fetch(image.thumbnail, 384 * 1024, true).also {
            runCatching { cache.write(key, it, context.filesDir.usableSpace) }
        }
        try { decode(bytes, 320, 180) } catch (failure: Exception) { cache.remove(key); throw failure }
    } }
    suspend fun preview(image: OnlineWallpaper, width: Int, height: Int): Bitmap = withContext(Dispatchers.IO) {
        val target = OnlineWallpaperPolicy.fit(image.width, image.height, width.coerceIn(1, 3840), height.coerceIn(1, 2160))
        val data = query(mapOf("pageids" to image.page.toString(), "iiurlwidth" to target.first.toString(), "iiurlheight" to target.second.toString()))
        val info = data.optJSONObject("query")?.optJSONArray("pages")?.optJSONObject(0)?.optJSONArray("imageinfo")?.optJSONObject(0)
            ?: throw IOException("This wallpaper is no longer available.")
        check(info.optString("sha1") == image.revision) { "This image changed. Return to the gallery and reload before applying." }
        val url = info.optString("thumburl")
        check(OnlineWallpaperPolicy.assetUrl(url)) { "No compatible preview available." }
        decode(fetch(url, 8 * 1024 * 1024, true), target.first, target.second)
    }
    private fun decode(bytes: ByteArray, width: Int, height: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 1..32768 && bounds.outHeight in 1..32768 && bounds.outWidth.toLong() * bounds.outHeight <= 200_000_000 &&
            bounds.outMimeType in setOf("image/jpeg", "image/png", "image/webp")) { "Unsupported image." }
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            val target = OnlineWallpaperPolicy.fit(info.size.width, info.size.height, width, height)
            decoder.setTargetSize(target.first, target.second)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setOnPartialImageListener { false }
        }
    }
    private suspend fun fetch(url: String, max: Int, image: Boolean): ByteArray {
        currentCoroutineContext().ensureActive()
        if (System.currentTimeMillis() < limitedUntil) throw IOException("Wikimedia is busy. Wait a minute before trying again.")
        require(if (image) OnlineWallpaperPolicy.assetUrl(url) else url.startsWith("https://commons.wikimedia.org/w/api.php?"))
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 8000; connection.readTimeout = 8000
        connection.setRequestProperty("User-Agent", "RC-TV/1.0 (+https://github.com/Ruan1993/RC-TV-Launcher)")
        try {
            val status = connection.responseCode
            if (status == 429 || status == 503) {
                limitedUntil = System.currentTimeMillis() + (connection.getHeaderField("Retry-After")?.toLongOrNull()?.coerceIn(60, 3600) ?: 60) * 1000
                throw IOException("Wikimedia is busy. Please try again later.")
            }
            if (status != 200) throw IOException("Image service returned HTTP $status. Try again later.")
            require(connection.contentLengthLong <= max) { "Image response is too large." }
            if (image) require(connection.contentType?.substringBefore(';') in setOf("image/jpeg", "image/png", "image/webp")) { "Unsupported image response." }
            val deadline = android.os.SystemClock.elapsedRealtime() + 20_000
            return connection.inputStream.use { input -> ByteArrayOutputStream().use { output ->
                val buffer = ByteArray(16384)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    if (android.os.SystemClock.elapsedRealtime() > deadline) throw IOException("Image download timed out. Check Wi-Fi and try again.")
                    val count = input.read(buffer); if (count < 0) break
                    require(output.size() + count <= max) { "Image response is too large." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } }
        } finally { connection.disconnect() }
    }
    companion object { @Volatile private var limitedUntil = 0L }
}
