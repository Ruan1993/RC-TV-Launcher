package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class WallpaperPreferencesTest {
    @Test fun missingOrUnknownSelectionKeepsTheApprovedMountainDefault() {
        assertEquals("mountain", LauncherPreferences().wallpaper)
        assertEquals("mountain", validWallpaper(null))
        assertEquals("mountain", validWallpaper("removed-collection-item"))
    }
    @Test fun stableIdsRoundTripAndAreDistinct() {
        val ids = BundledWallpaper.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertEquals(it, validWallpaper(it)) }
    }
}
