package za.co.rc.tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp
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
    var recovery by remember { mutableStateOf(HomeIntegration.originalHomeRecovery(context)) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { recovery = HomeIntegration.originalHomeRecovery(context) }
    }
    var recoveryOpen by remember { mutableStateOf(false) }
    Text("Current Home Launcher", style = MaterialTheme.typography.titleMedium, color = LocalAppearance.current.accent)
    Text(home.resolvedPackage?.let { home.resolvedLabel } ?: "Unknown — Android has not exposed a reliable default")
    Text(home.description(context.packageName), style = MaterialTheme.typography.bodyMedium)
    FocusButton("Set RC TV as Default Home", motion, onClick = chooseHome)
    FocusButton(recovery.buttonLabel, motion) {
        // Recheck at activation as package state may have changed while this page was open.
        recovery = HomeIntegration.originalHomeRecovery(context)
        if (recovery.useHomeSelection) restoreHome() else recoveryOpen = true
    }
    Text(recovery.description)
    if (recoveryOpen) {
        val closeFocus = remember { FocusRequester() }
        val detailsFocus = remember { FocusRequester() }
        val detailsScroll = rememberScrollState()
        val detailsScope = rememberCoroutineScope()
        LaunchedEffect(Unit) { withFrameNanos { }; closeFocus.requestFocus() }
        AlertDialog(onDismissRequest = { recoveryOpen = false }, title = { Text("Restore original Google TV") },
            text = {
                Column(Modifier.heightIn(max = 280.dp).verticalScroll(detailsScroll)
                    .focusRequester(detailsFocus).onPreviewKeyEvent { event ->
                        val key = event.nativeKeyEvent
                        val delta = when (key.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> if (detailsScroll.canScrollForward) 140f else null
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> if (detailsScroll.canScrollBackward) -140f else null
                            else -> null
                        }
                        if (delta != null) {
                            if (key.action == android.view.KeyEvent.ACTION_DOWN) detailsScope.launch { detailsScroll.scrollBy(delta) }
                            true
                        } else false
                    }.focusable()) {
                    Text(recovery.description)
                    Spacer(Modifier.height(12.dp))
                    Text(recovery.recoverySteps, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { FocusButton("Open Android Settings", motion) { recoveryOpen = false; androidSettings() } },
            dismissButton = { FocusButton("Back", motion, Modifier.focusRequester(closeFocus).focusProperties { up = detailsFocus }) { recoveryOpen = false } })
    }
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
