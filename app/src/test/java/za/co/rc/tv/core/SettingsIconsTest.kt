package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test
import za.co.rc.tv.ui.settingsCategories

class SettingsIconsTest {
    @Test fun remoteAndConnectionsRemainSeparateCategoriesWithDistinctIcons() {
        val remote = settingsCategories.single { it.key == "remote" }
        val connections = settingsCategories.single { it.key == "connections" }
        assertNotEquals(remote.icon, connections.icon)
        assertEquals("Phone & Keyboard", remote.title)
        assertEquals("Connections & Devices", connections.title)
    }
}
