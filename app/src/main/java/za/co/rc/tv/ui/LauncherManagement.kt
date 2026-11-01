package za.co.rc.tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import za.co.rc.tv.core.*

@Composable internal fun LauncherManagement(motion: String, chooseHome: () -> Unit, restoreHome: () -> Unit,
    androidSettings: () -> Unit, reset: suspend () -> Boolean) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val home by produceState(HomeIntegration.inspect(context), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { value = HomeIntegration.inspect(context) }
    }
    Text("Current Home Launcher", style = MaterialTheme.typography.titleMedium, color = LocalAppearance.current.accent)
    Text(home.resolvedPackage?.let { home.resolvedLabel } ?: "Unknown — Android has not exposed a reliable default")
    Text(home.description(context.packageName), style = MaterialTheme.typography.bodyMedium)
    FocusButton("Set RC TV as Default Home", motion, onClick = chooseHome)
    FocusButton("Switch to Original Xiaomi Home", motion, onClick = restoreHome)
    Text("In Android’s Home selection, choose the original Google TV / Xiaomi launcher to switch back. Android confirms the change. The original launcher stays installed.")
    FocusButton("Open Android Settings", motion, onClick = androidSettings)
    HorizontalDivider()
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val cancelFocus = remember { FocusRequester() }
    val input = LocalInputModeManager.current
    val scope = rememberCoroutineScope()
    FocusButton("Reset RC TV Appearance", motion) { confirm = true; failed = false }
    Text("Restore wallpaper, colours, text, layout and focus animation defaults. Favourites and order, pairing, keyboard, Reduced Motion and screensaver choices stay saved.", style = MaterialTheme.typography.bodyMedium)
    if (confirm) {
        LaunchedEffect(Unit) { withFrameNanos { }; input.requestInputMode(InputMode.Keyboard); cancelFocus.requestFocus() }
        AlertDialog(onDismissRequest = { if (!busy) confirm = false }, title = { Text("Reset RC TV appearance?") },
            text = { Text(if (failed) "Could not save the reset. Try again or cancel." else
                "Reset wallpaper, colours, text, layout and animations? Your favourites and order, phone pairing, keyboard, Reduced Motion and screensaver choices will be kept. Imported wallpapers will stay in your library.") },
            confirmButton = { FocusButton(if (busy) "Resetting…" else "Reset appearance", motion) {
                if (!busy) { busy = true; scope.launch { if (reset()) confirm = false else failed = true; busy = false } }
            } },
            dismissButton = { FocusButton("Cancel", motion, Modifier.focusRequester(cancelFocus)) { if (!busy) confirm = false } })
    }
}
