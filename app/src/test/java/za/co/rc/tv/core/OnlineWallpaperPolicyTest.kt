package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class OnlineWallpaperPolicyTest {
    @Test fun acceptsLicenceUrlsCapturedFromRealCommonsGalleryPages() {
        val rows = javaClass.getResourceAsStream("/commons-wallpaper-licences.tsv")!!.bufferedReader().use { reader ->
            reader.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map { it.split('\t') }
        }
        assertEquals(14, rows.size)
        assertTrue(rows.any { !it[2].endsWith("/") })
        rows.forEach { row -> assertEquals("Commons page ${row[1]}", row[3], OnlineWallpaperPolicy.license(row[2])) }
        val pages = rows.groupBy { it[0] }
        assertTrue(pages.getValue("1").map { it[1] }.toSet().intersect(pages.getValue("2").map { it[1] }.toSet()).isEmpty())
        listOf("https://creativecommons.org/licenses/by-nc/4.0", "https://creativecommons.org/licenses/by-nd/4.0",
            "https://creativecommons.org.evil.test/licenses/by-sa/3.0").forEach { assertNull(OnlineWallpaperPolicy.license(it)) }
    }
    @Test fun onlyExplicitSupportedLicencesAreAccepted() {
        assertEquals("https://creativecommons.org/licenses/by-sa/4.0/", OnlineWallpaperPolicy.license("http://creativecommons.org/licenses/by-sa/4.0/"))
        listOf("https://creativecommons.org/licenses/by-nc/4.0/", "https://creativecommons.org/licenses/by-nd/4.0/",
            "https://creativecommons.org.evil.test/licenses/by/4.0/", "https://evil.test@creativecommons.org/licenses/by/4.0/",
            "https://creativecommons.org:443/licenses/by/4.0/", "", "Public domain", "https://creativecommons.org/licenses/by/4.0/?bad=1").forEach {
            assertNull(it, OnlineWallpaperPolicy.license(it))
        }
    }
    @Test fun imageRequestsCannotEscapeOfficialHttpsAssetHosts() {
        assertTrue(OnlineWallpaperPolicy.assetUrl("https://thumb.wikimedia.org/wikipedia/commons/thumb/a/ab/file.jpg/500px-file.jpg"))
        assertTrue(OnlineWallpaperPolicy.assetUrl("https://upload.wikimedia.org/wikipedia/commons/a/ab/file.jpg"))
        listOf("http://upload.wikimedia.org/wikipedia/commons/a.jpg", "https://upload.wikimedia.org.evil.test/wikipedia/commons/a.jpg",
            "https://evil.test@upload.wikimedia.org/wikipedia/commons/a.jpg", "file:///data/data/a.jpg", "https://upload.wikimedia.org/other/a.jpg").forEach {
            assertFalse(it, OnlineWallpaperPolicy.assetUrl(it))
        }
    }
    @Test fun freeSpaceReserveIncludesTheEntirePendingWrite() {
        val reserve = OnlineWallpaperPolicy.RESERVE_BYTES
        assertFalse(OnlineWallpaperPolicy.hasSpace(reserve - 1, 0))
        assertFalse(OnlineWallpaperPolicy.hasSpace(reserve + 1024, 1025))
        assertTrue(OnlineWallpaperPolicy.hasSpace(reserve + 1024, 1024))
        assertFalse(OnlineWallpaperPolicy.hasSpace(Long.MAX_VALUE, -1))
    }
    @Test fun displayFitDoesNotUpscaleOrDistortPanoramas() {
        assertEquals(1920 to 960, OnlineWallpaperPolicy.fit(6000, 3000, 1920, 1080))
        assertEquals(1920 to 192, OnlineWallpaperPolicy.fit(20000, 2000, 1920, 1080))
        assertEquals(800 to 600, OnlineWallpaperPolicy.fit(800, 600, 3840, 2160))
    }
    @Test fun thumbnailEvictionIsBoundedAndKeepsRecentlyReadImages() {
        val root = Files.createTempDirectory("wallpaper-cache").toFile()
        try {
            val cache = WallpaperThumbnailCache(root, 12)
            val a = OnlineWallpaperPolicy.key("a"); val b = OnlineWallpaperPolicy.key("b"); val c = OnlineWallpaperPolicy.key("c")
            cache.write(a, ByteArray(6), Long.MAX_VALUE)
            cache.write(b, ByteArray(6), Long.MAX_VALUE)
            java.io.File(root, b).setLastModified(1)
            assertNotNull(cache.read(a))
            cache.write(c, ByteArray(6), Long.MAX_VALUE)
            assertNotNull(cache.read(a)); assertNull(cache.read(b)); assertNotNull(cache.read(c))
            assertTrue(root.listFiles()!!.sumOf { it.length() } <= 12)
            assertFalse(root.listFiles()!!.any { it.name.endsWith(".part") })
        } finally { root.deleteRecursively() }
    }
    @Test fun cacheSkipsLowStorageAndOversizeWritesAndCleansInterruptedParts() {
        val root = Files.createTempDirectory("wallpaper-cache").toFile()
        try {
            val cache = WallpaperThumbnailCache(root, 10)
            val key = OnlineWallpaperPolicy.key("image")
            cache.write(key, ByteArray(11), Long.MAX_VALUE)
            assertNull(cache.read(key))
            cache.write(key, ByteArray(5), OnlineWallpaperPolicy.RESERVE_BYTES)
            assertNull(cache.read(key))
            java.io.File(root, "$key.part").writeBytes(ByteArray(3))
            cache.read(key)
            assertTrue(root.listFiles()!!.isEmpty())
        } finally { root.deleteRecursively() }
    }
    @Test fun stableKeysDeduplicateIdenticalSourceRevision() {
        assertEquals(OnlineWallpaperPolicy.key("commons:12:abc"), OnlineWallpaperPolicy.key("commons:12:abc"))
        assertNotEquals(OnlineWallpaperPolicy.key("commons:12:abc"), OnlineWallpaperPolicy.key("commons:12:def"))
        assertTrue(isImportedWallpaper("local_" + OnlineWallpaperPolicy.key("commons:12:abc").take(32)))
    }
}
