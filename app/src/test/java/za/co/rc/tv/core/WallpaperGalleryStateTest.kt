package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class WallpaperGalleryStateTest {
    private fun image(id: Long) = OnlineWallpaper(id, "revision$id", "Landscape $id", "", 1920, 1080, "Creator", "https://creativecommons.org/licenses/by/4.0/")
    private fun page(id: Long, next: String? = "cursor$id") = OnlineWallpaperPage(listOf(image(id)), next?.let { OnlineWallpaperCursor(it, "gcmcontinue||") })
    @Test fun revisitingLoadedPagesPreservesResultsCursorAndFocusedImage() {
        val history = WallpaperPageHistory()
        history.store(page(10)); history.entry().focused = 10
        assertTrue(history.next()); val secondCursor = history.entry().cursor
        history.store(page(20)); history.entry().focused = 20
        history.visit(1)
        assertEquals(10L, history.entry().focused)
        assertEquals(listOf(10L), history.entry().page!!.images.map { it.page })
        assertTrue(history.next())
        assertEquals(secondCursor, history.entry().cursor)
        assertEquals(20L, history.entry().focused)
        assertEquals(listOf(20L), history.entry().page!!.images.map { it.page })
        assertEquals(listOf(1, 2), history.numbers())
    }
    @Test fun historyNeverInventsRandomAccessPages() {
        val history = WallpaperPageHistory()
        assertFalse(history.next())
        assertThrows(IllegalArgumentException::class.java) { history.visit(3) }
        history.store(page(1, null)); assertFalse(history.next())
        assertEquals(listOf(1), history.numbers())
    }
    @Test fun historyIsBoundedAndAbsolutePageNumbersDoNotReset() {
        val history = WallpaperPageHistory(10)
        for (number in 1..25) {
            history.store(page(number.toLong()))
            if (number != 25) assertTrue(history.next())
        }
        assertEquals(25, history.current)
        assertEquals((16..25).toList(), history.numbers())
        assertThrows(IllegalArgumentException::class.java) { history.visit(1) }
    }
    @Test fun duplicateContinuationCannotProduceAnEndlessRepeatPage() {
        val history = WallpaperPageHistory()
        history.store(page(1, "same")); assertTrue(history.next())
        history.store(page(2, "same")); assertFalse(history.next())
        assertEquals(2, history.current)
    }
    @Test fun cachedPagesContainDistinctSourceImages() {
        val history = WallpaperPageHistory()
        history.store(page(1)); assertTrue(history.next())
        history.store(OnlineWallpaperPage(listOf(image(1), image(2), image(2)), null))
        assertEquals(listOf(2L), history.entry().page!!.images.map { it.page })
        history.visit(1); assertEquals(listOf(1L), history.entry().page!!.images.map { it.page })
    }
    @Test fun failedPageCanRetryUsingTheSameContinuationWithoutLosingEarlierPages() {
        val history = WallpaperPageHistory()
        history.store(page(1)); assertTrue(history.next())
        val cursor = history.entry().cursor
        assertNull(history.entry().page)
        history.visit(1); assertTrue(history.next())
        assertEquals(cursor, history.entry().cursor)
        history.store(page(2)); assertEquals(2, history.current)
    }
    @Test fun bulkRotationAddsWithoutTogglingOrRemovingExistingChoices() {
        val a = "local_" + "a".repeat(32); val b = "local_" + "b".repeat(32)
        val existing = setOf("mountain", "coast", a)
        assertEquals(setOf("mountain", "coast", a, b), addGalleryRotation(existing, setOf(a, b)))
        assertEquals(existing, addGalleryRotation(existing, emptySet()))
        assertThrows(IllegalArgumentException::class.java) { addGalleryRotation(existing, setOf("not-a-wallpaper")) }
    }
    @Test fun deletionProtectsCurrentHomeAndRotationImages() {
        assertFalse(canDeleteGalleryWallpaper("saved", "saved", emptySet()))
        assertFalse(canDeleteGalleryWallpaper("saved", "mountain", setOf("saved")))
        assertTrue(canDeleteGalleryWallpaper("saved", "mountain", setOf("coast")))
    }
}
