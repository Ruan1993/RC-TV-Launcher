package za.co.rc.tv.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import za.co.rc.tv.companion.*
import za.co.rc.tv.core.HomeIntegration
import za.co.rc.tv.core.SetupProgress

/** Optional setup: only Android's own chooser and consent screens can change system settings. */
@Composable fun SetupWizard(server: CompanionServer, chooseHome: () -> Unit, restoreHome: () -> Unit, finish: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var step by rememberSaveable { mutableIntStateOf(0) }
    var home by remember { mutableStateOf(HomeIntegration.inspect(context)) }
    val access by RemoteRuntime.access.collectAsState()
    val connection by server.state.collectAsState()
    var seenConnection by remember { mutableLongStateOf(connection.connectionEvent) }
    var phoneConnected by rememberSaveable { mutableStateOf(false) }
    var consent by remember { mutableStateOf(false) }
    var pairing by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) home = HomeIntegration.inspect(context) }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(step, home.resolvedPackage, access) {
        step = SetupProgress.automaticStep(step, home.resolvedPackage == context.packageName, access.optedIn, access.connected)
    }
    LaunchedEffect(connection.connectionEvent) {
        if (connection.connectionEvent > seenConnection) {
            seenConnection = connection.connectionEvent
            if (step == 3) { phoneConnected = true; pairing = false; step = 4 }
        }
    }
    if (pairing) {
        CompanionSettings(server) { pairing = false }
        return
    }
    if (consent) {
        AlertDialog(onDismissRequest = { consent = false }, title = { Text("Allow your phone to control compatible apps?") },
            text = { Text("Your approved phones can move between controls, select, go Back or Home, and send non-password text through RC TV Keyboard. Android's Accessibility service can inspect the active screen. Screen content is not stored or sent to your phone. Some apps may not support every control. Only approve phones you trust. Android will ask you to confirm using the physical remote.") },
            confirmButton = { FocusButton("Allow and open Android", "Minimal") {
                RemoteRuntime.setOptIn(context, true); consent = false
                runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }.onFailure { help = true }
            } }, dismissButton = { FocusButton("Not now", "Minimal") { consent = false } })
        return
    }
    val titles = listOf("Welcome to RC TV", "Choose your Home screen", "Use your phone in other apps", "Connect your phone", "You're ready")
    AlertDialog(onDismissRequest = finish, modifier = Modifier.widthIn(max = 680.dp),
        title = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("EASY SETUP  ·  ${step + 1} OF 5", style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth())
            Text(titles[step])
        } }, text = {
            Column(Modifier.heightIn(max = 230.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (step) {
                    0 -> Text("Your favourite apps, wallpapers and a phone remote, together on this TV. We'll help you get started. Every step is optional.")
                    1 -> {
                        Text("Would you like RC TV to open when you press Home? Android will ask you to choose. Your original Home screen stays installed.")
                        Text("Current Home: ${home.resolvedLabel}")
                        if (!home.roleAvailable && !home.homeSettingsAvailable) Text("This TV doesn't provide a supported Home chooser. You can keep opening RC TV from Apps.")
                        else FocusButton("Choose RC TV as Home", "Minimal", onClick = chooseHome)
                        if (home.roleHeld && home.resolvedPackage != context.packageName) Text("This firmware still opens the original Home. RC TV can continue as an app.")
                        FocusButton("How to return to original Home", "Minimal") { help = !help }
                        if (help) {
                            Text("Open Android Home settings and select your original launcher. On some Xiaomi TVs this chooser is unavailable; keep the stock launcher and open RC TV from Apps.")
                            FocusButton("Open Home settings", "Minimal", onClick = restoreHome)
                        }
                    }
                    2 -> {
                        Text("Allow your phone to control compatible apps on this TV. Your physical remote keeps working.")
                        FocusButton(if (access.optedIn) "Open Android permission" else "Set up phone control", "Minimal") {
                            if (!access.optedIn) consent = true
                            else runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }.onFailure { help = true }
                        }
                        Text("In Android Accessibility, scroll to Services. Select RC TV Remote system control, turn it On and approve the message. Return here to continue automatically.")
                        if (help) Text("Open Android Settings > Device Preferences > Accessibility using your physical remote. RC TV cannot turn this permission on for you.")
                    }
                    3 -> {
                        Text("Install the RC TV Remote phone app supplied with RC TV. Open it on your Android phone. Connect both devices to the same Wi-Fi or local network.")
                        FocusButton(if (connection.devices.isEmpty()) "Connect phone" else "Connect or add a phone", "Minimal") { server.start(); pairing = true }
                        Text("Choose this TV on your phone, compare its security fingerprint, enter the TV code, then approve on this TV.")
                        if (connection.devices.isNotEmpty()) Text("A phone is already approved. Open RC TV Remote on it to reconnect, or continue without adding another.")
                    }
                    4 -> {
                        Text(if (phoneConnected) "Phone connected. You're ready to explore." else "You can start using RC TV now.")
                        Text("Remote in the navigation bar connects phones. Keyboard explains phone typing. Settings > Easy setup lets you revisit any skipped step.")
                        if (!access.optedIn || !access.connected) Text("Phone control in other apps is not enabled yet. You can finish that later.")
                        if (home.resolvedPackage != context.packageName) Text("Your original Home is unchanged. Open RC TV from Apps whenever you want.")
                    }
                }
            }
        }, confirmButton = { FocusButton(if (step == 4) "Start watching" else if (step == 0) "Let's begin" else "Continue", "Minimal") { if (step == 4) finish() else { help = false; step++ } } },
        dismissButton = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (step > 0) FocusButton("Back", "Minimal") { help = false; step-- }
            FocusButton("Finish later", "Minimal", onClick = finish)
        } })
}
