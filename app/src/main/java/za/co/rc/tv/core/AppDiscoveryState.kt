package za.co.rc.tv.core

import kotlinx.coroutines.CancellationException

data class AppDiscoveryState(
    val apps: List<LauncherApp> = emptyList(),
    val loaded: Boolean = false,
    val error: String? = null
)

suspend fun refreshDiscovery(
    previous: AppDiscoveryState,
    discover: suspend () -> List<LauncherApp>,
    reportFailure: (Exception) -> Unit = {}
): AppDiscoveryState = try {
    AppDiscoveryState(apps = discover(), loaded = true)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    reportFailure(failure)
    previous.copy(loaded = true, error = "App discovery failed. Retry to refresh installed apps.")
}
