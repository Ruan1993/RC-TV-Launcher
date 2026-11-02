package za.co.rc.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal val LocalSettingsControls = staticCompositionLocalOf { false }

@Composable internal fun ConfirmActionDialog(title: String, detail: String, action: String,
    confirm: () -> Unit, dismiss: () -> Unit) {
    val cancel = remember { FocusRequester() }
    LaunchedEffect(title) { withFrameNanos { }; cancel.requestFocus() }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Text(detail) },
        confirmButton = { FocusButton(action, "Minimal", onClick = confirm) },
        dismissButton = { FocusButton("Cancel", "Minimal", Modifier.focusRequester(cancel), onClick = dismiss) })
}

@Composable internal fun FeatureStatus(label: String, ready: Boolean, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label · ${if (ready) "Ready" else "Needs attention"}", style = MaterialTheme.typography.titleMedium,
            color = LocalAppearance.current.accent)
        Text(detail, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Same readable, bounded shell for setup and recovery. Back never implies successful completion. */
@Composable internal fun GuidedPage(title: String, index: Int, count: Int, back: () -> Unit,
    close: () -> Unit, nextLabel: String, next: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)) {
        CompositionLocalProvider(LocalSettingsControls provides true) {
        BackHandler(onBack = back)
        val backFocus = remember { FocusRequester() }
        LaunchedEffect(title) { withFrameNanos { }; backFocus.requestFocus() }
        Column(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) back()
                true
            } else false
        }.background(MaterialTheme.colorScheme.background).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${index + 1} OF $count  ·  $title", style = MaterialTheme.typography.headlineMedium)
            LinearProgressIndicator(progress = { (index + 1f) / count }, modifier = Modifier.fillMaxWidth())
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusButton("Back", "Minimal", Modifier.heightIn(min = 64.dp).focusRequester(backFocus), onClick = back)
                FocusButton(nextLabel, "Minimal", Modifier.heightIn(min = 64.dp), onClick = next)
                FocusButton("Finish later", "Minimal", Modifier.heightIn(min = 64.dp), onClick = close)
            }
        }
        }
    }
}
