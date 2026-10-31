package za.co.rc.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Inactive by design: no verified, user-approved public HTTPS APK destination exists. */
@Composable fun RemoteDownload(returnToPairing: () -> Unit, close: () -> Unit) {
    AlertDialog(onDismissRequest=close, modifier=Modifier.widthIn(max=680.dp), title={ Text("Download RC TV Remote") }, text={
        Column(Modifier.heightIn(max=230.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().heightIn(min=96.dp).background(LocalAppearance.current.glass).border(1.dp,LocalAppearance.current.border).padding(16.dp),contentAlignment=Alignment.Center) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("QR download unavailable",style=MaterialTheme.typography.titleMedium)
                    Text("Public download link not published yet",style=MaterialTheme.typography.bodyMedium)
                }
            }
            DownloadNote("RC TV Remote 0.5.1-rc1 · Android 8.0 or later · Android phones only")
            DownloadNote("1. Transfer the supplied phone APK to your Android phone and open it. Existing paired phones can keep using 0.5.0.")
            DownloadNote("2. Follow Android's installation prompts. Allow installation only from the file app you trust, then turn that allowance off again.")
            DownloadNote("3. Open RC TV Remote. Use the same Wi-Fi or local network as the TV, then choose Return to pairing below.")
            DownloadNote("Online download is not available in this preview. Use the phone APK supplied with this TV build.")
        }
    },confirmButton={ FocusButton("Return to pairing","Minimal",onClick=returnToPairing) },dismissButton={ FocusButton("Back","Minimal",onClick=close) })
}

@Composable private fun DownloadNote(text: String) {
    var focused by remember { mutableStateOf(false) }
    Text(text, modifier=Modifier.fillMaxWidth().onFocusChanged { focused=it.isFocused }
        .border(1.dp,if(focused) LocalAppearance.current.focus else Color.Transparent).focusable().padding(6.dp))
}
