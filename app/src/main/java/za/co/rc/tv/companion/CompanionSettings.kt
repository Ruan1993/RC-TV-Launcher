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
    AlertDialog(onDismissRequest = close, title = { Text("Phone companion") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.message)
            Text("Install RC TV Remote on your Android phone. Use the same trusted local network. Compare the security fingerprint, enter the code, then approve on this TV.")
            if (state.running) {
                Text("Address: ${state.address}")
                Text("Pairing code: ${state.code.ifEmpty { "expired or used" }}", style = MaterialTheme.typography.headlineSmall)
                Text("Code expires after two minutes. Approved pairing lasts up to seven days. Opening another app pauses control; phones reconnect when RC TV returns.")
                FocusButton("New pairing code", "Minimal") { server.start() }
                Text("Security fingerprint\n${LinkWire.displayFingerprint(state.fingerprint)}", style = MaterialTheme.typography.bodySmall)
                for (device in state.devices) FocusButton("Revoke ${device.name}", "Minimal") { server.revoke(device.id) }
                FocusButton("Stop & disconnect all", "Minimal") { server.stop() }
            } else FocusButton("Start pairing", "Minimal") { server.start() }
            Text("Controls are limited to RC TV. Close this dialog after pairing to navigate. No system-wide control, password entry or cloud account.")
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
            text = { Text("Only approve the phone you are pairing. It can reconnect to RC TV navigation and app search for up to seven days. Revoke it at any time in Phone companion settings.") },
            confirmButton = { FocusButton("Approve phone", "Minimal") { server.approve(request.id) } },
            dismissButton = { FocusButton("Decline", "Minimal", Modifier.focusRequester(decline)) { server.reject(request.id) } })
    }
}

