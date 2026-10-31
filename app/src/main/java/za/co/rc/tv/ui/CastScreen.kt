package za.co.rc.tv.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val RECEIVER = "com.google.android.apps.mediashell"

@Composable fun CastScreen(motion: String) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val receiver = remember(revision) { runCatching { context.packageManager.getApplicationInfo(RECEIVER, 0) }.getOrNull() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Cast to your TV", style = MaterialTheme.typography.headlineLarge)
        Text(when { receiver == null -> "Google Cast receiver not detected. Check your TV's built-in casting support."
            !receiver.enabled -> "Google Cast receiver is installed but disabled. Review its Android settings."
            else -> "Google Cast receiver is installed and enabled. A live casting connection has not been verified." })
        Text("Start on your phone or computer. Keep both devices on the same home network. RC TV uses your TV's existing receiver; it is not a Cast receiver itself.")
        if (receiver != null) FocusButton("Receiver app settings", motion) {
            runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$RECEIVER".toUri())) }
                .onFailure { Toast.makeText(context, "Open Android Settings → Apps → Show system apps → Chromecast built-in.", Toast.LENGTH_LONG).show() }
        }
        CastTip("Android phone", "Open a Google Cast-compatible app, tap its Cast icon, then choose this TV. The app controls playback.")
        CastTip("iPhone or iPad", "Use the Cast icon in a compatible app and allow Local Network access when iOS asks. AirPlay and Control Centre Screen Mirroring are different; AirPlay support has not been verified on this TV.")
        CastTip("Chrome on a computer", "In desktop Chrome, open the menu → Cast, save and share → Cast. Choose the TV and a tab or screen source. Menu names can vary by version.")
        CastTip("Screen mirroring", "On supported Android phones, use Google Home → this TV → Cast my screen and approve the phone's prompt. Other wireless display features may use incompatible protocols. Protected video may stay black.")
        CastTip("Local photos and videos", "Use a local-media app with Google Cast support. RC TV's wallpaper browser selects wallpapers; it does not stream phone files or bypass media protection.")
        Text("TV missing? Check receiver settings, the same Wi-Fi network and router guest-device isolation. Casting still depends on the sender app, receiver and supported media.", style = MaterialTheme.typography.bodyMedium)
    }
}

/** Read-only focus targets keep every instruction reachable with a D-pad. */
@Composable private fun CastTip(title: String, body: String) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
        .border(1.dp, if (focused) LocalAppearance.current.focus else Color.Transparent)
        .focusable().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body)
    }
}
