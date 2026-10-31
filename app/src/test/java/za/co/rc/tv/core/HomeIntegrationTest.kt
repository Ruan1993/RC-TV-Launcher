package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class HomeIntegrationTest {
    @Test fun roleGrantDoesNotMisreportActualHomeRouting() {
        val state = HomeIntegrationState("stock.home", "Stock Home", true, true, true)
        assertTrue(state.description("rc.home").contains("still resolves to Stock Home"))
        assertFalse(state.description("rc.home").contains("currently handles"))
    }
    @Test fun unavailableSelectionAndActualHomeHaveClearMessages() {
        assertTrue(HomeIntegrationState("rc.home", "RC TV", true, true, true).description("rc.home").contains("currently handles"))
        assertTrue(HomeIntegrationState("stock", "Stock", false, false, false).description("rc.home").contains("does not expose"))
    }
}
