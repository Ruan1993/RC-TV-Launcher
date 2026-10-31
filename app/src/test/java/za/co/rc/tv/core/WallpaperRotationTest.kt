package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class WallpaperRotationTest {
    @Test fun rotationSkipsExcludedMissingAndCurrentScenes() {
        assertEquals("desert", nextWallpaper("mountain", setOf("mountain", "desert", "missing"), listOf("mountain", "coast", "desert")))
        assertEquals("mountain", nextWallpaper("desert", setOf("mountain", "desert"), listOf("mountain", "desert")))
        assertNull(nextWallpaper("mountain", setOf("mountain", "missing"), listOf("mountain", "coast")))
    }
    @Test fun overdueResumeRotatesOnceAndClockRollbackWaits() {
        assertEquals(0L, rotationDelay(1_000_000L, 600_000L, 5))
        assertEquals(200_000L, rotationDelay(1_000_000L, 900_000L, 5))
        assertEquals(300_000L, rotationDelay(1_000_000L, 0L, 5))
        assertEquals(300_000L, rotationDelay(1_000_000L, 2_000_000L, 5))
    }
    @Test fun onlySafePrivateIdsAndSupportedSettingsAreAccepted() {
        val id = "local_" + "a".repeat(32)
        assertEquals(id, validWallpaper(id))
        assertFalse(isImportedWallpaper("local_../../other"))
        assertEquals("mountain", validWallpaper("local_invalid"))
        assertEquals(15, validRotationInterval(-1))
        assertEquals(1440, validRotationInterval(1440))
        assertEquals("Instant", validWallpaperTransition("unknown"))
    }
}
