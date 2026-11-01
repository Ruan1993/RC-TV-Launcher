package za.co.rc.tv.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import za.co.rc.tv.core.*

@Composable internal fun ScreensaverSettings(prefs: LauncherPreferences, motion: String,
    minutes: (Int) -> Unit, style: (String) -> Unit, preview: () -> Unit, provider: (String) -> Unit) {
    val context = LocalContext.current
    var choice by remember { mutableStateOf<String?>(null) }
    var powerHelp by remember { mutableStateOf(false) }
    Text("Screensaver Provider", style = MaterialTheme.typography.titleMedium, color = LocalAppearance.current.accent)
    FocusButton(prefs.screensaverProvider, motion) { choice = "provider" }
    Text(if (prefs.screensaverProvider == ANDROID_SCREENSAVER)
        "Android manages ambient mode using its own settings. RC TV’s automatic timer is off; your RC TV options stay saved."
        else "RC TV runs only on idle Home. Android ambient mode takes priority if it starts first.")
    FocusButton("Android ambient mode settings", motion) {
        openSystemDestination(context, SystemDestination("ambient", "Android ambient mode settings", listOf(Settings.ACTION_DREAM_SETTINGS), "System → Ambient mode"))
    }
    Text("RC TV screensaver", style = MaterialTheme.typography.titleMedium, color = LocalAppearance.current.accent)
    Text("Runs only while RC TV Home is open and idle. Other apps and Android’s own ambient mode take priority.")
    FocusButton("Start after: ${screensaverIntervalLabel(prefs.screensaverMinutes)}", motion) { choice = "time" }
    FocusButton("Style: ${prefs.screensaverStyle}", motion) { choice = "style" }
    Text(when (prefs.screensaverStyle) {
        "Black Screen" -> "A black picture; the physical display stays on."
        "Clock & Wallpaper" -> "A clear clock over your cinematic wallpaper."
        else -> "Cinematic wallpapers change gently every two minutes. Reduced Motion keeps one still image."
    }, style = MaterialTheme.typography.bodyMedium)
    FocusButton("Preview Screensaver", motion, onClick = preview)
    Text("Preview shows the RC TV style without changing your provider.", style = MaterialTheme.typography.bodyMedium)
    HorizontalDivider()
    Text("Sleep & power", style = MaterialTheme.typography.titleMedium, color = LocalAppearance.current.accent)
    Text("Use Android setting", style = MaterialTheme.typography.titleMedium)
    Text("Android manages sleep and standby. Choose System → Power and energy in Android Settings. RC TV cannot set a system sleep timer or switch off TV hardware.")
    FocusButton("Open Android power settings", motion) {
        openSaverSystemSettings(context, Settings.ACTION_DISPLAY_SETTINGS)
    }
    FocusButton("Power off & restart help", motion) { powerHelp = true }
    if (powerHelp) {
        val closeFocus = remember { FocusRequester() }
        val input = LocalInputModeManager.current
        LaunchedEffect(Unit) { withFrameNanos { }; input.requestInputMode(InputMode.Keyboard); closeFocus.requestFocus() }
        AlertDialog(onDismissRequest = { powerHelp = false }, title = { Text("Power off & restart") },
        text = { Text("To restart this Xiaomi, hold the remote’s Power button and choose Restart in Android’s power menu. Use the physical remote’s power control for standby; standby does not disconnect physical power. Android restricts direct shutdown and reboot to system apps, so RC TV cannot perform those actions itself. Android power settings remain available for sleep options.") },
        confirmButton = { FocusButton("Close", motion, Modifier.focusRequester(closeFocus)) { powerHelp = false } })
    }
    if (choice != null) {
        val times = choice == "time"
        val providers = choice == "provider"
        val options = if (providers) screensaverProviders.map { it to it }
            else if (times) screensaverIntervals.map { it.toString() to screensaverIntervalLabel(it) } else screensaverStyles.map { it to it }
        val selected = if (providers) prefs.screensaverProvider else if (times) prefs.screensaverMinutes.toString() else prefs.screensaverStyle
        SaverChoice(if (providers) "Screensaver Provider" else if (times) "Start after" else "Screensaver style", options, selected, motion,
            { value -> if (providers) provider(value) else if (times) minutes(value.toInt()) else style(value); choice = null }, { choice = null })
    }
}

private fun openSaverSystemSettings(context: android.content.Context, action: String) {
    // Let Android resolve public actions; this also handles package-visibility filtering.
    val opened = listOf(action, Settings.ACTION_SETTINGS).distinct().any { candidate ->
        runCatching { context.startActivity(Intent(candidate)); true }.getOrDefault(false)
    }
    if (!opened) android.widget.Toast.makeText(context, "Open System settings from your original launcher.", android.widget.Toast.LENGTH_LONG).show()
}

@Composable private fun SaverChoice(title: String, options: List<Pair<String, String>>, selected: String,
    motion: String, choose: (String) -> Unit, close: () -> Unit) {
    val focus = remember { FocusRequester() }
    val input = LocalInputModeManager.current
    LaunchedEffect(Unit) { withFrameNanos { }; input.requestInputMode(InputMode.Keyboard); focus.requestFocus() }
    AlertDialog(onDismissRequest = close, title = { Text(title) }, text = {
        Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                FocusButton(label, motion, Modifier.fillMaxWidth().then(if (value == selected) Modifier.focusRequester(focus) else Modifier), selected = value == selected) { choose(value) }
            }
        }
    }, confirmButton = { FocusButton("Back", motion, onClick = close) })
}

/** In-app preview/idle wrapper; the system dream reuses the same content without a dialog. */
@Composable internal fun Screensaver(prefs: LauncherPreferences, clock: String, dismiss: () -> Unit) {
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        ScreensaverContent(prefs, clock, dismiss)
    }
}

@Composable internal fun ScreensaverContent(prefs: LauncherPreferences, clock: String, dismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    val input = LocalInputModeManager.current
    var wallpaper by remember { mutableStateOf(prefs.wallpaper) }
    val black = prefs.screensaverStyle == "Black Screen"
    LaunchedEffect(black, prefs.reducedMotion) {
        if (!black && !prefs.reducedMotion) while (true) {
            delay(120_000)
            val pool = bundledWallpapers.map { it.id }
            wallpaper = pool[(pool.indexOf(wallpaper) + 1).mod(pool.size)]
        }
    }
        LaunchedEffect(Unit) { withFrameNanos { }; input.requestInputMode(InputMode.Keyboard); focus.requestFocus() }
        Box(Modifier.fillMaxSize().background(Color.Black)
            .semantics { contentDescription = "RC TV screensaver" }
            .focusRequester(focus).onPreviewKeyEvent { event ->
                val key = event.nativeKeyEvent
                if (key.keyCode in listOf(19, 20, 21, 22, 23, 66, 160)) {
                    // Dismiss on release so OK's release cannot activate the control underneath.
                    if (key.action == android.view.KeyEvent.ACTION_UP) dismiss()
                    true
                } else false
            }.focusable().clickable(onClick = dismiss)) {
            if (!black) {
                Wallpaper(false, wallpaper, if (prefs.reducedMotion) "Instant" else "Crossfade",
                    if (prefs.reducedMotion) "Minimal" else prefs.motion, prefs.wallpaperVisibility)
                if (prefs.screensaverStyle == "Clock & Wallpaper") {
                    Column(Modifier.align(Alignment.BottomEnd).padding(48.dp)
                        .background(Color.Black.copy(alpha = .65f), androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
                        .padding(horizontal = 28.dp, vertical = 16.dp)) {
                        Text(clock, fontSize = 64.sp, color = Color.White)
                    }
                }
            }
        }
}
