package za.co.rc.tv.core

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

fun imageReadPermissions(sdk: Int): Array<String> = when {
    sdk >= 34 -> arrayOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
    sdk >= 33 -> arrayOf("android.permission.READ_MEDIA_IMAGES")
    else -> arrayOf("android.permission.READ_EXTERNAL_STORAGE")
}
data class MediaImage(val uri: Uri, val name: String, val bucket: String, val bytes: Long)
data class MediaImagePage(val images: List<MediaImage>, val hasNext: Boolean)

/** Read-only MediaStore, bounded pages and thumbnails. No raw filesystem or all-files access. */
class MediaImages(context: Context) {
    private val context = context.applicationContext
    private val resolver = context.contentResolver
    private val decoding = Semaphore(2)
    private val thumbnails = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    fun canRead(): Boolean = imageReadPermissions(Build.VERSION.SDK_INT).any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    suspend fun page(page: Int): MediaImagePage = withContext(Dispatchers.IO) {
        require(page >= 0)
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.SIZE)
        val args = Bundle().apply {
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Images.Media.IS_PENDING}=0 AND ${MediaStore.Images.Media.IS_TRASHED}=0 AND ${MediaStore.Images.Media.MIME_TYPE} IN (?,?,?)")
            putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("image/jpeg", "image/png", "image/webp"))
            putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media._ID))
            putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, 101)
            putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, page * 100)
        }
        val result = mutableListOf<MediaImage>()
        resolver.query(collection, projection, args, null)?.use { cursor ->
            while (cursor.moveToNext() && result.size < 101) {
                val id = cursor.getLong(0)
                result += MediaImage(ContentUris.withAppendedId(collection, id), cursor.getString(1) ?: "Image $id", cursor.getString(2) ?: "Shared images", cursor.getLong(3))
            }
        }
        MediaImagePage(result.take(100), result.size > 100)
    }
    suspend fun thumbnail(uri: Uri, preview: Boolean = false): Bitmap? = withContext(Dispatchers.IO) { decoding.withPermit {
        val key = "$uri/$preview"
        thumbnails.get(key) ?: runCatching {
            resolver.loadThumbnail(uri, if (preview) Size(960, 540) else Size(320, 180), null)
        }.getOrNull()?.also { thumbnails.put(key, it) }
    } }
}
