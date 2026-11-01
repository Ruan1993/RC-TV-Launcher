package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test
import za.co.rc.tv.ui.*

class SettingsNavigationTest {
    @Test fun categoriesAreDistinctAndOldRoutesRetainTheirDestination() {
        assertEquals(7, settingsCategories.size)
        assertEquals(settingsCategories.size, settingsCategories.map { it.key }.distinct().size)
        assertEquals(settingsCategories.size, settingsCategories.map { it.icon }.distinct().size)
        assertTrue(settingsCategories.all { it.description.isNotBlank() })
        assertEquals("apps", settingsCategoryKey("launcher"))
        assertEquals("weather", settingsCategoryKey("power"))
        assertNull(settingsCategoryKey("missing"))
        assertNull(settingsCategoryKey(null))
    }
    @Test fun categoryBackReturnsToItsOwnCategoryAndBackupReturnsToItsEntry() {
        settingsCategories.forEach { category ->
            assertEquals(SettingsReturnTarget(null, category.key), settingsBackTarget(category.key))
        }
        assertEquals(SettingsReturnTarget("system", "backup"), settingsBackTarget("backup"))
        assertEquals(SettingsReturnTarget(null, "apps"), settingsBackTarget("launcher"))
        assertEquals(SettingsReturnTarget(null, "weather"), settingsBackTarget("power"))
    }
    @Test fun utilityBackRestoresTheExactOriginatingEntry() {
        assertEquals(SettingsReturnTarget("apps", "apps"), settingsUtilityTarget("Apps"))
        assertEquals(SettingsReturnTarget("system", "speed"), settingsUtilityTarget("Speed Test"))
        assertEquals(SettingsReturnTarget("connections", "cast"), settingsUtilityTarget("Cast"))
        assertNull(settingsUtilityTarget("Home"))
        for (section in listOf("Apps", "Speed Test", "Cast")) {
            val target = settingsUtilityTarget(section)!!
            assertEquals(target.page, settingsEntryPage(target.entry!!))
        }
        assertEquals("appearance", settingsEntryPage("wallpaper"))
        assertEquals("system", settingsEntryPage("backup"))
        assertEquals("weather", settingsEntryPage("weather-details"))
        assertNull(settingsEntryPage("missing"))
    }
    @Test fun everyExistingAndroidSettingsDestinationHasOneHome() {
        assertEquals(setOf("android", "about", "display", "date", "storage", "updates", "accessibility"), essentialSystemDestinations.map { it.key }.toSet())
        essentialSystemDestinations.forEach { destination ->
            assertTrue(settingsCategories.any { it.key == systemDestinationCategory(destination.key) })
        }
        assertEquals("apps", systemDestinationCategory("storage"))
        assertEquals("remote", systemDestinationCategory("accessibility"))
        assertEquals("connections", systemDestinationCategory("display"))
        assertEquals("about", systemDestinationCategory("about"))
        assertEquals(listOf("android", "date", "updates"), essentialSystemDestinations.filter { systemDestinationCategory(it.key) == "system" }.map { it.key })
    }
}
