package za.co.rc.tv.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Read-only guidance that can be reached and scrolled using a TV remote. */
@Composable internal fun ReadableNote(text: String) {
    var focused by remember { mutableStateOf(false) }
    Text(text, style=MaterialTheme.typography.bodySmall,
        modifier=Modifier.fillMaxWidth().onFocusChanged { focused=it.isFocused }
            .border(1.dp,if(focused) LocalAppearance.current.focus else Color.Transparent).focusable().padding(6.dp))
}
