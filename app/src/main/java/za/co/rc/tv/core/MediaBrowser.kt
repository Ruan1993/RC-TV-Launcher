package za.co.rc.tv.core

import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class MediaSource(val id: String, val label: String, val volume: String? = null, val tree: Uri? = null)
data class MediaLocation(val source: MediaSource, val directory: String, val label: String)
data class MediaEntry(val id: String, val name: String, val kind: MediaKind, val uri: Uri?, val directory: String? = null,
    val mime: String? = null, val bytes: Long = 0, val modified: Long = 0)
data class MediaPage(val entries: List<MediaEntry>, val hasNext: Boolean)
fun sortedMedia(entries: List<MediaEntry>, sort: MediaSort): List<MediaEntry> = entries.distinctBy { it.id }.sortedWith(
    compareBy<MediaEntry> { it.kind != MediaKind.FOLDER }.thenComparator { a, b ->
        val order = when (sort) { MediaSort.NAME -> 0; MediaSort.DATE -> b.modified.compareTo(a.modified); MediaSort.TYPE -> a.kind.compareTo(b.kind) }
        if (order != 0) order else a.name.compareTo(b.name, ignoreCase = true).takeIf { it != 0 } ?: a.id.compareTo(b.id)
    })

/** Only public MediaStore and explicitly granted document trees. Never scans private/raw mounts. */
class MediaBrowser(context: Context) {
    private val context = context.applicationContext
    private val resolver = context.contentResolver
    private val saved = context.getSharedPreferences("media-folders", Context.MODE_PRIVATE)
    fun canReadIndexed() = mediaReadPermissions(Build.VERSION.SDK_INT).any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    fun folderPickerAvailable(): Boolean {
        val target = context.packageManager.resolveActivity(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo ?: return false
        return target.packageName != "com.google.android.tv.frameworkpackagestubs" && !target.name.contains("EmptyStubActivity")
    }
    fun rememberTree(uri: Uri) {
        require(DocumentsContract.isTreeUri(uri))
        val current = saved.getStringSet("trees", emptySet()).orEmpty()
        require(current.size < 8 || uri.toString() in current) { "Eight folders are already connected." }
        try { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        catch (_: SecurityException) { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        check(resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }) { "Folder access was not retained." }
        check(saved.edit().putStringSet("trees", current + uri.toString()).commit()) { "Could not save folder access." }
    }
    suspend fun sources(includeIndexed: Boolean = true): List<MediaSource> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MediaSource>()
        if (includeIndexed) {
            val volumes = context.getSystemService(StorageManager::class.java).storageVolumes
            MediaStore.getExternalVolumeNames(context).sorted().forEach { volume ->
                val description = volumes.firstOrNull { if (volume == MediaStore.VOLUME_EXTERNAL_PRIMARY) it.isPrimary else it.uuid.equals(volume, true) }?.getDescription(context)
                result += MediaSource("index:$volume", description ?: "Shared media ($volume)", volume = volume)
            }
        }
        saved.getStringSet("trees", emptySet()).orEmpty().sorted().forEach { raw ->
            currentCoroutineContext().ensureActive()
            val uri = Uri.parse(raw)
            if (resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }) {
                val label = runCatching {
                    resolver.query(DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri)), arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
                }.getOrNull() ?: "Connected folder (currently unavailable)"
                result += MediaSource("tree:$raw", label, tree = uri)
            }
        }
        result
    }
    fun root(source: MediaSource) = MediaLocation(source, source.tree?.let(DocumentsContract::getTreeDocumentId) ?: "", source.label)
    suspend fun page(location: MediaLocation, page: Int): MediaPage = withContext(Dispatchers.IO) {
        require(page >= 0 && page <= 10000)
        if (location.source.tree != null) documents(location, page) else indexed(location, page)
    }
    private suspend fun indexed(location: MediaLocation, page: Int): MediaPage {
        check(canReadIndexed()) { "Allow media access to browse Android’s indexed media." }
        check(location.source.volume in MediaStore.getExternalVolumeNames(context)) { "This storage is disconnected. Return to Storage and reconnect it." }
        val prefix = location.directory
        val escaped = prefix.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val args = Bundle().apply {
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, "media_type IN (1,2,3) AND is_pending=0 AND is_trashed=0 AND relative_path LIKE ? ESCAPE '\\'")
            putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("$escaped%"))
            putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns._ID))
            putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, android.content.ContentResolver.QUERY_SORT_DIRECTION_ASCENDING)
        }
        val projection = arrayOf("_id", "_display_name", "relative_path", "mime_type", "_size", "date_modified")
        val items = linkedMapOf<String, MediaEntry>(); var seen = 0
        val cursor = resolver.query(MediaStore.Files.getContentUri(location.source.volume!!), projection, args, null) ?: error("Media storage is unavailable.")
        cursor.use {
            // Cursor windows stay provider-backed; explicit positioning also works on OEMs that ignore paging arguments.
            if (page > 0) it.moveToPosition(page * 500 - 1)
            while (it.moveToNext() && seen < 501) {
                currentCoroutineContext().ensureActive(); seen++
                if (seen > 500) break
                val path = it.getString(2).orEmpty()
                val folder = mediaChildFolder(prefix, path)
                if (folder != null) {
                    val directory = "$prefix$folder/"
                    items[directory] = MediaEntry("folder:$directory", folder, MediaKind.FOLDER, null, directory)
                } else if (path == prefix) {
                    val name = it.getString(1) ?: "Unnamed media"; val mime = it.getString(3)
                    val kind = mediaKind(name, mime) ?: continue
                    if (kind == MediaKind.BACKUP) continue
                    val collection = when (kind) {
                        MediaKind.VIDEO -> MediaStore.Video.Media.getContentUri(location.source.volume!!)
                        MediaKind.AUDIO -> MediaStore.Audio.Media.getContentUri(location.source.volume!!)
                        else -> MediaStore.Images.Media.getContentUri(location.source.volume!!)
                    }
                    val uri = ContentUris.withAppendedId(collection, it.getLong(0))
                    items[uri.toString()] = MediaEntry(uri.toString(), name, kind, uri, mime = mime, bytes = it.getLong(4), modified = it.getLong(5) * 1000)
                }
            }
        }
        return MediaPage(items.values.toList(), seen > 500)
    }
    private suspend fun documents(location: MediaLocation, page: Int): MediaPage {
        val tree = location.source.tree!!
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, location.directory)
        val projection = arrayOf("document_id", "_display_name", "mime_type", "_size", "last_modified")
        val result = mutableListOf<MediaEntry>(); var seen = 0
        val cursor = resolver.query(children, projection, null, null, null) ?: error("Folder unavailable. Check the drive and its folder permission.")
        cursor.use {
            if (page > 0) it.moveToPosition(page * 200 - 1)
            while (it.moveToNext() && seen < 201) {
                currentCoroutineContext().ensureActive(); seen++; if (seen > 200) break
                val id = it.getString(0); val name = it.getString(1) ?: "Unnamed media"; val mime = it.getString(2)
                val kind = if (mime == DocumentsContract.Document.MIME_TYPE_DIR) MediaKind.FOLDER else mediaKind(name, mime) ?: continue
                result += MediaEntry(id, name, kind, DocumentsContract.buildDocumentUriUsingTree(tree, id), if (kind == MediaKind.FOLDER) id else null,
                    mime, it.getLong(3), it.getLong(4))
            }
        }
        return MediaPage(result, seen > 200)
    }
    fun vlcIntent(entry: MediaEntry): Intent {
        require(entry.kind == MediaKind.VIDEO || entry.kind == MediaKind.AUDIO)
        val uri = requireNotNull(entry.uri); require(uri.scheme == "content")
        return Intent(Intent.ACTION_VIEW).setPackage("org.videolan.vlc").setDataAndType(uri, entry.mime?.takeIf { it.startsWith("video/") || it.startsWith("audio/") }
            ?: if (entry.kind == MediaKind.VIDEO) "video/*" else "audio/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply { clipData = ClipData.newRawUri(entry.name, uri) }
    }
    suspend fun image(uri: Uri): android.graphics.Bitmap {
        var decoded: android.graphics.Bitmap? = null
        try { return withContext(Dispatchers.IO) {
        resolver.openFileDescriptor(uri, "r")?.use { require(it.statSize <= 64L * 1024 * 1024) { "Image exceeds the 64 MB preview limit." } }
            ?: error("Image unavailable. Check the drive.")
        android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            require(info.size.width.toLong() * info.size.height <= 200_000_000L) { "Image dimensions are too large." }
            val scale = minOf(1.0, 1920.0 / info.size.width, 1080.0 / info.size.height)
            decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setOnPartialImageListener { false }
        }.also { decoded = it }
        } } catch (e: Exception) { decoded?.recycle(); throw e }
    }
}
