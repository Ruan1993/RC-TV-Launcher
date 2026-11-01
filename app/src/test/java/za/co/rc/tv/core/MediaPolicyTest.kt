package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class MediaPolicyTest {
    @Test fun recognisesCommonMediaWithoutDependingOnFilenameCase() {
        assertEquals(MediaKind.VIDEO, mediaKind("Episode.MKV", null))
        assertEquals(MediaKind.AUDIO, mediaKind("Concert.FLAC", null))
        assertEquals(MediaKind.IMAGE, mediaKind("Photo.HEIC", null))
        assertEquals(MediaKind.VIDEO, mediaKind("unknown", "video/mp4"))
        assertEquals(MediaKind.BACKUP, mediaKind("RC-TV-settings.json", "application/json"))
        assertNull(mediaKind("private.txt", "text/plain"))
    }
    @Test fun virtualFoldersStayWithinTheSelectedParent() {
        assertEquals("Shows", mediaChildFolder("Movies/", "Movies/Shows/Season 1/"))
        assertNull(mediaChildFolder("Movies/", "Movies/"))
        assertNull(mediaChildFolder("Movies/", "Other/Shows/"))
        assertNull(mediaChildFolder("Movies/", "Movies/../"))
    }
    @Test fun scopedPermissionsMatchPlatformVersions() {
        assertArrayEquals(arrayOf("android.permission.READ_EXTERNAL_STORAGE"), mediaReadPermissions(30))
        assertEquals(3, mediaReadPermissions(33).size)
        assertFalse(mediaReadPermissions(36).any { it.contains("MANAGE_EXTERNAL_STORAGE") })
    }
    @Test fun sortingIsStableWithFoldersFirstAndNewestFilesFirst() {
        val folder = MediaEntry("f", "Z folder", MediaKind.FOLDER, null)
        val old = MediaEntry("a", "Alpha.mp4", MediaKind.VIDEO, null, modified = 10)
        val newer = MediaEntry("b", "beta.mp3", MediaKind.AUDIO, null, modified = 20)
        assertEquals(listOf(folder, old, newer), sortedMedia(listOf(newer, old, folder), MediaSort.NAME))
        assertEquals(listOf(folder, newer, old), sortedMedia(listOf(old, folder, newer), MediaSort.DATE))
        assertEquals(3, sortedMedia(listOf(folder, newer, old, old), MediaSort.TYPE).size)
    }
}
