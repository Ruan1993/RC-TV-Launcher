package za.co.rc.tv.core

import org.junit.Assert.assertEquals
import org.junit.Test
import za.co.rc.tv.ui.orderedFavouritePackages

class FavouriteOrderTest {
    private val apps = listOf(LauncherApp("a", "A"), LauncherApp("b", "B"), LauncherApp("c", "C"))
    @Test fun legacyFavouritesKeepTheirExistingDisplayOrderIncludingMissingPackages() {
        assertEquals(listOf("a", "c", "missing"), orderedFavouritePackages(
            LauncherPreferences(favourites = setOf("missing", "c", "a")), apps))
    }
    @Test fun savedOrderFiltersRemovedEntriesAndAppendsNewFavourites() {
        assertEquals(listOf("c", "a", "b"), orderedFavouritePackages(
            LauncherPreferences(favourites = setOf("a", "b", "c"), favouriteOrder = listOf("c", "removed", "c", "a")), apps))
    }
}
