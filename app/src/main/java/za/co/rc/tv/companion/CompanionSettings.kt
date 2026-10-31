package za.co.rc.tv.companion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import za.co.rc.tv.link.LinkWire
import za.co.rc.tv.ui.FocusButton

@Composable fun CompanionSettings(server: CompanionServer, close: () -> Unit) {
    val state by server.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var systemWide by remember { mutableStateOf(RemoteRuntime.optedIn(context)) }
    var confirmSystem by remember { mutableStateOf(false) }
    LaunchedEffect(server, state.running) {
        if (state.running) {
            server.preparePairing()
            while (true) { kotlinx.coroutines.delay(1000); server.refreshPairingDisplay() }
        }
    }
    if (confirmSystem) AlertDialog(onDismissRequest = { confirmSystem = false }, title = { Text("Allow approved phones to control other apps?") },
        text = { Text("All currently approved phones can request Home, Back, app focus/click, media buttons and non-password text through RC TV Keyboard. The accessibility service can inspect the active window. No screen history is stored or sent. Android 11 navigation depends on each app; it cannot inject arbitrary D-pad keys. Only enable this on a trusted network. You must also enable RC TV Remote system control in Android Accessibility. Use the physical remote for that consent. Revoke any phone you no longer trust before enabling.") },
        confirmButton = { FocusButton("Allow system control", "Minimal") { RemoteRuntime.setOptIn(context, true); systemWide = true; confirmSystem = false } },
        dismissButton = { FocusButton("Cancel", "Minimal") { confirmSystem = false } })
    AlertDialog(onDismissRequest = close, title = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Phone companion")
            if (state.running) {
                Text("Address: ${state.address}", style = MaterialTheme.typography.bodyMedium)
                Text(if (state.code.isNotEmpty()) "Pairing code: ${state.code}" else if (state.pending != null) "Approve your phone on this TV"
                    else if (state.devices.size >= 3) "Three phones approved. Revoke one to pair another."
                    else "No active code. Choose New pairing code.", style = MaterialTheme.typography.headlineSmall)
                FocusButton("New pairing code", "Minimal") { server.start() }
            }
        }
    }, text = {
        Column(Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.message)
            Text("Install RC TV Remote on your Android phone. Use the same trusted local network. Compare the security fingerprint, enter the code, then approve on this TV.")
            if (state.running) {
                Text("Code expires after two minutes. Approved pairing lasts up to seven days. Outside RC TV, control requires both system-control opt-in and Android Accessibility consent.")
                Text("Security fingerprint\n${LinkWire.displayFingerprint(state.fingerprint)}", style = MaterialTheme.typography.bodySmall)
                for (device in state.devices) FocusButton("Revoke ${device.name}", "Minimal") { server.revoke(device.id) }
                FocusButton("Stop & disconnect all", "Minimal") { server.stop() }
            } else FocusButton("Start pairing", "Minimal") { server.start() }
            FocusButton(if (systemWide) "Turn system control off" else "Set up system control…", "Minimal") {
                if (systemWide) { RemoteRuntime.setOptIn(context, false); systemWide = false } else confirmSystem = true
            }
            Text("System control: ${if (systemWide) "allowed" else "off"}. Accessibility connection: ${if (RemoteRuntime.accessibility != null) "connected" else "not connected"}. Enable RC TV Remote system control in Android Accessibility using the physical remote. Android shows its own warning. Disable it there at any time.")
            FocusButton("Open Android Accessibility…", "Minimal") {
                runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    .onFailure { android.widget.Toast.makeText(context, "Open Android Settings > Device Preferences > Accessibility with the physical remote.", android.widget.Toast.LENGTH_LONG).show() }
            }
            Text("Close this dialog to navigate RC TV. Android 11 focus/click works only in apps exposing accessible controls. Home returns to your selected Android Home. Media requests cannot confirm playback. For text, enable and choose RC TV Keyboard in RC TV Settings, focus a non-password field, then use Send text on the phone. Other keyboards and protected interfaces may not work. Physical remote and stock Home remain available.")
        }
    }, confirmButton = { FocusButton("Done", "Minimal", onClick = close) })
}

@Composable fun CompanionApproval(server: CompanionServer) {
    val state by server.state.collectAsState()
    state.pending?.let { request ->
        val decline = remember { androidx.compose.ui.focus.FocusRequester() }
        val inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
        LaunchedEffect(request.id) {
            inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
            kotlinx.coroutines.delay(100)
            decline.requestFocus()
        }
        AlertDialog(onDismissRequest = { server.reject(request.id) }, title = { Text("Allow ${request.name}?") },
            text = { Text("Only approve the phone you are pairing. It can reconnect for up to seven days. If you enable system control separately, approved phones can also control supported apps and send non-password text through RC TV Keyboard. Revoke it at any time in Phone companion settings.") },
            confirmButton = { FocusButton("Approve phone", "Minimal") { server.approve(request.id) } },
            dismissButton = { FocusButton("Decline", "Minimal", Modifier.focusRequester(decline)) { server.reject(request.id) } })
    }
}

