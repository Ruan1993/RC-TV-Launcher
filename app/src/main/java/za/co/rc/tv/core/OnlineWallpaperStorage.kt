package za.co.rc.tv.core

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FilterOutputStream
import java.io.IOException

/** Private offline collection. Saving never applies or evicts an existing image. */
class OnlineWallpaperStorage(context: Context) {
    private val root = File(context.applicationContext.filesDir, "wallpapers")
    suspend fun save(image: OnlineWallpaper, bitmap: Bitmap): String =
        withContext(Dispatchers.IO + NonCancellable) { mutex.withLock {
            check(root.exists() || root.mkdirs()) { "Wallpaper storage unavailable." }
            val target = File(root, image.id)
            if (target.exists()) {
                check(File(target, "image.webp").isFile && File(target, "online.json").isFile) { "Saved wallpaper is incomplete. Remove it and try again." }
                return@withLock image.id
            }
            check(OnlineWallpaperPolicy.hasSpace(root.parentFile!!.usableSpace, 6L * 1024 * 1024)) {
                "Not enough free storage. RC TV leaves 384 MB free; remove other files before saving."
            }
            val pending = File(root, ".online_pending")
            if (pending.exists()) check(pending.deleteRecursively())
            val previous = root.listFiles().orEmpty().filter { isImportedWallpaper(it.name) && File(it, "online.json").isFile }
            try {
                if (!target.exists()) {
                    check(root.listFiles().orEmpty().count { isImportedWallpaper(it.name) } < 24) { "Wallpaper gallery is full (maximum 24 saved/imported images). Remove an unused image first." }
                    check(pending.mkdir()) { "Wallpaper storage unavailable." }
                    writeImage(File(pending, "image.webp"), bitmap, 86, OnlineWallpaperPolicy.IMAGE_BYTES)
                    val size = OnlineWallpaperPolicy.fit(bitmap.width, bitmap.height, 320, 180)
                    val thumb = Bitmap.createScaledBitmap(bitmap, size.first, size.second, true)
                    try { writeImage(File(pending, "thumb.webp"), thumb, 78, 256 * 1024) } finally { if (thumb !== bitmap) thumb.recycle() }
                    check(OnlineWallpaperPolicy.hasSpace(root.parentFile!!.usableSpace, 16L * 1024)) { "Storage is too low. Existing wallpaper unchanged." }
                    File(pending, "title.txt").writeText(image.title.take(40))
                    val metadata = JSONObject().put("provider", "Wikimedia Commons").put("page", image.page)
                        .put("revision", image.revision).put("attribution", image.attribution).toString()
                    check(metadata.toByteArray(Charsets.UTF_8).size <= 16 * 1024) { "Image credits are too large to preserve safely. Please choose another image." }
                    File(pending, "online.json").writeText(metadata)
                    // Collection images are never evicted to make room.
                    val retained = previous.sumOf { folderSize(it) }
                    check(retained + folderSize(pending) <= OnlineWallpaperPolicy.LIBRARY_BYTES) { "Online wallpaper storage is full (16 MB). Remove an unused image from My Wallpaper Gallery first." }
                    check(pending.renameTo(target)) { "Could not save wallpaper." }
                }
                image.id
            } finally { pending.deleteRecursively() }
        } }
    private fun folderSize(file: File) = file.listFiles().orEmpty().sumOf { it.length() }
    private fun writeImage(file: File, bitmap: Bitmap, quality: Int, limit: Int) {
        check(OnlineWallpaperPolicy.hasSpace(root.parentFile!!.usableSpace, limit.toLong())) { "Storage is too low. Existing wallpaper unchanged." }
        file.outputStream().use { raw ->
            val bounded = object : FilterOutputStream(raw) {
                var written = 0
                override fun write(b: Int) { if (++written > limit) throw IOException("Optimised image exceeds storage limit."); out.write(b) }
                override fun write(b: ByteArray, off: Int, len: Int) {
                    if (len > limit - written) throw IOException("Optimised image exceeds storage limit.")
                    out.write(b, off, len); written += len
                }
            }
            check(bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, bounded)) { "Could not encode wallpaper." }
            raw.fd.sync()
        }
    }
    companion object {
        private val mutex = Mutex()
        fun isOnline(context: Context, id: String) = isImportedWallpaper(id) &&
            File(context.filesDir, "wallpapers/$id/online.json").isFile
    }
}

fun onlineWallpaperCredit(folder: File): String? = runCatching {
    File(folder, "online.json").takeIf { it.isFile && it.length() <= 16 * 1024 }?.let {
        JSONObject(it.readText()).optString("attribution").takeIf { text -> text.isNotBlank() }
    }
}.getOrNull()
