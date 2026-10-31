package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test
import za.co.rc.tv.assist.*

class AssistActionsTest {
    private val state = AssistSnapshot(listOf(AssistApp("yt", "YouTube"), AssistApp("music", "YouTube Music")), listOf("music"), "mountain", true)
    @Test fun actualInstalledAppsAndFavourites() {
        assertTrue(AssistActions.interpret("What applications are installed?", state).text.contains("YouTube Music"))
        assertTrue(AssistActions.interpret("Find YouTube.", state).text.contains("YouTube"))
        val launch = AssistActions.interpret("Open YouTube.", state).action!!
        assertEquals(AssistKind.LAUNCH, launch.kind); assertEquals("yt", launch.value)
        assertNull(AssistActions.interpret("Open missing app", state).action)
        assertTrue(AssistActions.interpret("Show my favourites.", state).text.contains("YouTube Music"))
        assertEquals("Settings", AssistActions.interpret("Open Settings.", state).action!!.value)
    }
    @Test fun writesAreProposalsAndRejectStaleState() {
        val wallpaper = AssistActions.interpret("Change my wallpaper to Tidal Dusk.", state).action!!
        assertEquals("coast", wallpaper.value); assertTrue(wallpaper.valid(state))
        assertFalse(wallpaper.valid(state.copy(wallpaper = "desert")))
        assertFalse(wallpaper.valid(state, wallpaper.expires))
        val rotation = AssistActions.interpret("Turn off wallpaper rotation.", state).action!!
        assertEquals("false", rotation.value); assertFalse(rotation.valid(state.copy(rotation = false)))
        assertFalse(AssistAction(AssistKind.WALLPAPER, "file:///secret", "bad").valid(state))
        assertFalse(AssistAction(AssistKind.LAUNCH, "missing", "bad").valid(state))
    }
    @Test fun unsupportedCommandsCannotBecomeActions() {
        for (text in listOf("run shell rm", "disable Xiaomi", "delete wallpapers", "open intent://evil", "Open YouTube; uninstall apps")) assertNull(AssistActions.interpret(text, state).action)
        assertTrue(AssistActions.interpret("Explain quantum mechanics", state).needsOnline)
        assertNull(AssistActions.interpret("Open YouTube", state.copy(apps = emptyList())).action)
    }
}
