package za.co.rc.tv.core
import org.junit.Assert.*
import org.junit.Test
class SystemConnectionsTest {
    @Test fun directDestinationDoesNotOpenAnotherScreen() {
        val attempts=mutableListOf<String>()
        assertTrue(launchSettingsWithFallback("wifi") { attempts+=it; true })
        assertEquals(listOf("wifi"),attempts)
    }
    @Test fun missingDestinationFallsBackOnceAndReportsFailure() {
        val attempts=mutableListOf<String>()
        assertTrue(launchSettingsWithFallback("bluetooth") { attempts+=it; it!="bluetooth" })
        assertEquals(listOf("bluetooth","android.settings.SETTINGS"),attempts)
        assertFalse(launchSettingsWithFallback("bluetooth") { false })
    }
}
