package za.co.rc.tv.core

import java.io.File
import java.net.URI
import java.security.MessageDigest

/** Bounds apply before allocating, downloading or writing untrusted provider content. */
object OnlineWallpaperPolicy {
    const val CACHE_BYTES = 10L * 1024 * 1024
    const val RESERVE_BYTES = 384L * 1024 * 1024
    const val IMAGE_BYTES = 4 * 1024 * 1024
    const val LIBRARY_BYTES = 16L * 1024 * 1024
    fun hasSpace(free: Long, write: Long) = write >= 0 && free >= RESERVE_BYTES && write <= free - RESERVE_BYTES
    fun assetUrl(value: String): Boolean = runCatching {
        val u = URI(value)
        u.scheme == "https" && u.host in setOf("upload.wikimedia.org", "thumb.wikimedia.org") &&
            u.userInfo == null && u.port == -1 && u.path.startsWith("/wikipedia/commons/")
    }.getOrDefault(false)
    fun license(value: String): String? = runCatching {
        val u = URI(value)
        // Commons LicenseUrl metadata commonly omits the canonical trailing slash.
        val path = if (u.path.endsWith("/")) u.path else "${u.path}/"
        if (u.scheme !in setOf("http", "https") || u.host != "creativecommons.org" || u.userInfo != null || u.port != -1 || u.query != null) null
        else path.takeIf { it in setOf("/licenses/by/3.0/", "/licenses/by/4.0/", "/licenses/by-sa/3.0/", "/licenses/by-sa/4.0/", "/publicdomain/zero/1.0/") }?.let { "https://creativecommons.org$it" }
    }.getOrNull()
    fun fit(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Pair<Int, Int> {
        require(width > 0 && height > 0 && maxWidth > 0 && maxHeight > 0)
        val scale = minOf(1.0, maxWidth.toDouble() / width, maxHeight.toDouble() / height)
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }
    fun key(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}

/** Private thumbnail directory only. Eviction happens before writes; no fullscreen images enter it. */
class WallpaperThumbnailCache(private val root: File, private val limit: Long = OnlineWallpaperPolicy.CACHE_BYTES) {
    fun remove(key: String): Unit = synchronized(lock) {
        require(key.matches(Regex("[a-f0-9]{64}")))
        File(root, key).delete()
    }
    fun read(key: String): ByteArray? = synchronized(lock) {
        require(key.matches(Regex("[a-f0-9]{64}")))
        trim(0)
        return File(root, key).takeIf { it.isFile && it.length() <= 384 * 1024 }?.let { it.setLastModified(System.currentTimeMillis()); it.readBytes() }
    }
    fun write(key: String, bytes: ByteArray, free: Long): Unit = synchronized(lock) {
        require(key.matches(Regex("[a-f0-9]{64}")))
        if (bytes.size > 384 * 1024 || bytes.size > limit || !OnlineWallpaperPolicy.hasSpace(free, bytes.size.toLong())) return
        if (!root.exists() && !root.mkdirs()) return
        File(root, key).delete()
        trim(bytes.size.toLong())
        val part = File(root, "$key.part")
        try { part.writeBytes(bytes); check(part.renameTo(File(root, key))) } finally { part.delete() }
    }
    private fun trim(incoming: Long) {
        root.listFiles().orEmpty().filter { it.name.endsWith(".part") }.forEach { it.delete() }
        val files = root.listFiles().orEmpty().filter { it.isFile }.sortedBy { it.lastModified() }
        var size = files.sumOf { it.length() }
        for (file in files) { if (size + incoming <= limit) break; val n = file.length(); if (file.delete()) size -= n }
        check(size + incoming <= limit) { "Thumbnail cache unavailable." }
    }
    companion object { private val lock = Any() }
}
