package za.co.rc.tv.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AppDiscoveryStateTest {
    @Test fun cancellationIsRethrownWithoutReportingFailure() = runBlocking {
        val cancellation = CancellationException("refresh replaced")
        var reported = false
        try {
            refreshDiscovery(AppDiscoveryState(), { throw cancellation }, { reported = true })
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        assertFalse(reported)
    }

    @Test fun realFailureRetainsAppsAndSuccessfulRetryClearsError() = runBlocking {
        val oldApps = listOf(LauncherApp("old", "Old"))
        val failure = SecurityException("package query failed")
        var reported: Exception? = null
        val failed = refreshDiscovery(AppDiscoveryState(oldApps, true), { throw failure }, { reported = it })
        assertSame(failure, reported)
        assertEquals(oldApps, failed.apps)
        assertNotNull(failed.error)
        assertTrue(failed.loaded)
        val recovered = refreshDiscovery(failed, { listOf(LauncherApp("new", "New")) })
        assertNull(recovered.error)
        assertEquals("new", recovered.apps.single().packageName)
    }

    @Test fun successfulEmptyDiscoveryClearsPreviousError() = runBlocking {
        val result = refreshDiscovery(AppDiscoveryState(error = "failed"), { emptyList() })
        assertTrue(result.loaded)
        assertTrue(result.apps.isEmpty())
        assertNull(result.error)
    }
}
