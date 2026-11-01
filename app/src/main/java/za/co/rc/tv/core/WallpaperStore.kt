package za.co.rc.tv.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.util.UUID

/** Only normalized images survive import; provider access is never needed afterwards. */
class WallpaperStore(context: Context) {
    private val context = context.applicationContext
    private val root = File(context.filesDir, "wallpapers")
    private val mutex = Mutex()
    suspend fun list(): List<WallpaperOption> = withContext(Dispatchers.IO) { mutex.withLock {
        root.listFiles().orEmpty().filter { it.name.startsWith(".pending_") && isImportedWallpaper(it.name.removePrefix(".pending_")) }.forEach { it.deleteRecursively() }
        bundledWallpapers + root.listFiles().orEmpty().filter {
            isImportedWallpaper(it.name) && File(it, "image.webp").isFile
        }.sortedBy { it.name }.map { WallpaperOption(it.name, File(it, "title.txt").takeIf { f -> f.isFile && f.length() <= 4096 }?.readText()?.take(40) ?: "Imported ${it.name.takeLast(6)}", onlineWallpaperCredit(it)) }
    } }
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) { mutex.withLock {
        require(root.listFiles().orEmpty().count { isImportedWallpaper(it.name) } < 24) { "Remove an imported wallpaper first (maximum 24)." }
        check(root.exists() || root.mkdirs()) { "Wallpaper storage unavailable." }
        val id = "local_" + UUID.randomUUID().toString().replace("-", "")
        val pending = File(root, ".pending_$id")
        check(pending.mkdir()) { "Cannot create wallpaper storage." }
        try {
            val source = File(pending, "source")
            context.contentResolver.openInputStream(uri)?.use { input -> source.outputStream().use { out ->
                val buffer = ByteArray(32768); var total = 0L
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    total += n; require(total <= 32L * 1024 * 1024) { "Image exceeds 32 MB." }
                    out.write(buffer, 0, n)
                }
            } } ?: throw IOException("Cannot open the selected image.")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path, bounds)
            require(bounds.outMimeType in setOf("image/jpeg", "image/png", "image/webp") &&
                bounds.outWidth in 1..32768 && bounds.outHeight in 1..32768 &&
                bounds.outWidth.toLong() * bounds.outHeight <= 200_000_000L) { "Choose a valid JPEG, PNG or WebP image." }
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(source)) { decoder, info, _ ->
                val scale = minOf(1.0, 1920.0 / info.size.width, 1080.0 / info.size.height)
                decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setOnPartialImageListener { false }
            }
            try {
                File(pending, "image.webp").outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 88, it)) }
                val scale = minOf(320f / bitmap.width, 180f / bitmap.height, 1f)
                val thumb = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
                try { File(pending, "thumb.webp").outputStream().use { require(thumb.compress(Bitmap.CompressFormat.WEBP_LOSSY, 82, it)) } }
                finally { if (thumb !== bitmap) thumb.recycle() }
            } finally { bitmap.recycle() }
            val title = runCatching {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0)?.substringBeforeLast('.')?.filterNot { it.isISOControl() }?.take(40) else null
                }
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Imported ${id.takeLast(6)}"
            File(pending, "title.txt").writeText(title)
            check(source.delete())
            check(pending.renameTo(File(root, id))) { "Could not save wallpaper." }
            id
        } finally { if (pending.exists()) pending.deleteRecursively() }
    } }
    suspend fun remove(id: String) = withContext(Dispatchers.IO) { mutex.withLock {
        require(isImportedWallpaper(id))
        val target = File(root, id)
        check(!target.exists() || target.deleteRecursively()) { "Could not remove wallpaper." }
    } }
    companion object {
        fun image(context: Context, id: String, thumbnail: Boolean = false): File {
            require(isImportedWallpaper(id))
            return File(File(File(context.filesDir, "wallpapers"), id), if (thumbnail) "thumb.webp" else "image.webp")
        }
    }
}
