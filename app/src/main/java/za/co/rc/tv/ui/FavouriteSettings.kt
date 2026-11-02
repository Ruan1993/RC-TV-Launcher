package za.co.rc.tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import za.co.rc.tv.core.*

/** One editor shared by Settings and setup; unavailable saved favourites are never discarded. */
@Composable internal fun FavouriteSettings(preferences: Preferences, prefs: LauncherPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var retry by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    LaunchedEffect(retry) {
        runCatching { AppRepository(context).discover() }.onSuccess { apps = it; loaded = true }
            .onFailure { message = "Apps could not load. Retry discovery." }
    }
    fun save(action: suspend () -> Unit) { scope.launch { runCatching { action() }
        .onSuccess { message = "Saved" }.onFailure { message = "Could not save. Retry the action." } } }
    Text("Add/remove apps with OK. Order changes save immediately; unavailable saved apps keep their positions.")
    if (message.isNotBlank()) Text(message, color = LocalAppearance.current.accent)
    if (!loaded) FocusButton("Retry app discovery", "Minimal") { retry++ }
    val order = (prefs.favouriteOrder.filter { it in prefs.favourites } + prefs.favourites.sorted()).distinct()
    order.forEachIndexed { index, pkg ->
        Text(apps.firstOrNull { it.packageName == pkg }?.label ?: "$pkg · unavailable")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (index > 0) FocusButton("Move up", "Minimal") {
                val next = order.toMutableList(); java.util.Collections.swap(next, index, index - 1)
                save { preferences.reorderFavourites(next) }
            }
            if (index < order.lastIndex) FocusButton("Move down", "Minimal") {
                val next = order.toMutableList(); java.util.Collections.swap(next, index, index + 1)
                save { preferences.reorderFavourites(next) }
            }
            if (apps.none { it.packageName == pkg }) FocusButton("Remove saved favourite", "Minimal") { save { preferences.toggleFavourite(pkg) } }
        }
    }
    apps.forEach { app -> FocusButton("${if (app.packageName in prefs.favourites) "✓" else "+"} ${app.label}", "Minimal") { save { preferences.toggleFavourite(app.packageName) } } }
}
