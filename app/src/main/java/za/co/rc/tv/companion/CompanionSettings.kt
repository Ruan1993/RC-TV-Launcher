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

@Composable fun CompanionSettings(server: CompanionServer, initialPage: Int = 0, close: () -> Unit) {
    val state by server.state.collectAsState()
    val access by RemoteRuntime.access.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var page by remember { mutableIntStateOf(initialPage) }
    var confirmSystem by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(page) { scroll.scrollTo(0) }
    LaunchedEffect(server, state.running, page) {
        if (state.running && page == 0) {
            server.preparePairing()
            while (true) { kotlinx.coroutines.delay(1000); server.refreshPairingDisplay() }
        }
    }
    fun openAccessibility() {
        runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            .onFailure { android.widget.Toast.makeText(context, "On TV: Settings > Device Preferences > Accessibility > RC TV Remote system control.", android.widget.Toast.LENGTH_LONG).show() }
    }
    if (confirmSystem) {
        AlertDialog(onDismissRequest = { confirmSystem = false }, title = { Text("Allow control in other apps?") },
            text = { Text("Your approved phones will be able to control supported apps and send non-password text through RC TV Keyboard. Android Accessibility can inspect the active screen to move focus and select controls. Screen content is not stored or sent to your phone. Some apps do not support every control. Your physical remote keeps working. Only allow phones you trust. Next, enable RC TV Remote system control in Android and approve its warning using the physical remote.") },
            confirmButton = { FocusButton("Allow and continue", "Minimal") { RemoteRuntime.setOptIn(context, true); confirmSystem = false; openAccessibility() } },
            dismissButton = { FocusButton("Cancel", "Minimal") { confirmSystem = false } })
        return
    }
    AlertDialog(onDismissRequest = close, title = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Phone remote")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Pair phone", "Other apps", "Typing").forEachIndexed { index, title ->
                    FocusButton(title, "Minimal", selected = page == index) { page = index }
                }
            }
            if (page == 0 && state.running) {
                Text("Address: ${state.address}", style = MaterialTheme.typography.bodyMedium)
                Text(if (state.code.isNotEmpty()) "Code: ${state.code}" else if (state.pending != null) "Approve your phone on this TV"
                    else if (state.devices.size >= 3) "Three phones approved. Remove one below."
                    else "Choose New pairing code", style = MaterialTheme.typography.headlineSmall)
                FocusButton("New pairing code", "Minimal") { server.start() }
            }
            if (page == 1) {
                Text(if (access.optedIn && access.connected) "Ready for supported apps" else "Setup needed to stay connected outside RC TV", style = MaterialTheme.typography.titleMedium)
                FocusButton(if (!access.optedIn) "Enable other-app control" else if (!access.connected) "Enable Android permission" else "Review Android permission", "Minimal") {
                    if (!access.optedIn) confirmSystem = true else openAccessibility()
                }
            }
        }
    }, text = {
        Column(Modifier.heightIn(max = 185.dp).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (page) {
                0 -> {
                    Text("1. Open RC TV Remote on your phone. Use the same Wi-Fi or local network.\n2. Choose this TV and compare the security fingerprint.\n3. Enter the code, then approve your phone on this TV.")
                    if (state.running) {
                        Text("Security fingerprint\n${LinkWire.displayFingerprint(state.fingerprint)}", style = MaterialTheme.typography.bodySmall)
                        Text("A code lasts two minutes. Once connected, this screen closes automatically. Approved phones reconnect until you remove them.")
                        for (device in state.devices) FocusButton("Remove ${device.name}", "Minimal") { server.revoke(device.id) }
                        FocusButton("Stop and forget all phones", "Minimal") { server.stop() }
                    } else {
                        Text(state.message)
                        FocusButton("Start pairing", "Minimal") { server.start() }
                    }
                }
                1 -> {
                    Text("1. Choose Enable other-app control above.\n2. In Android Accessibility, choose RC TV Remote system control and turn it On. Approve Android's warning using the physical remote.\n3. Return here. When it says Ready, open YouCine or another app.")
                    Text("RC TV permission: ${if (access.optedIn) "allowed" else "not yet allowed"}. Android permission: ${if (access.connected) "connected" else "not enabled"}.")
                    Text("Use your phone and physical remote together. Home and Back use Android controls. Navigation depends on the app; some screens need the physical remote. Media buttons require player support.")
                    if (access.optedIn) FocusButton("Turn other-app control off", "Minimal") { RemoteRuntime.setOptIn(context, false) }
                }
                2 -> {
                    Text("1. Enable and choose RC TV Keyboard below.\n2. In the TV app, select a search or normal text box.\n3. On your phone, type in Text for TV and tap Send text. The TV keyboard can stay hidden in apps such as YouCine.")
                    FocusButton("Enable RC TV Keyboard", "Minimal") {
                        runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)) }
                    }
                    FocusButton("Choose keyboard", "Minimal") { context.getSystemService(android.view.inputmethod.InputMethodManager::class.java).showInputMethodPicker() }
                    Text("For typing in other apps, complete Other apps setup too. Passwords and custom app keyboards are not supported. Your physical remote and TV keyboard still work.")
                }
            }
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
            text = { Text("Only approve the phone you are pairing. It can reconnect until you remove it. If you enable system control separately, approved phones can also control supported apps and send non-password text through RC TV Keyboard. Revoke it at any time in Phone companion settings.") },
            confirmButton = { FocusButton("Approve phone", "Minimal") { server.approve(request.id) } },
            dismissButton = { FocusButton("Decline", "Minimal", Modifier.focusRequester(decline)) { server.reject(request.id) } })
    }
}

