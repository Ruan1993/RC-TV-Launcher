package za.co.rc.tv.assist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import za.co.rc.tv.ui.FocusButton

data class AssistMessage(val source: String, val text: String)
class AssistConversation {
    val messages = mutableStateListOf(AssistMessage("RC TV · Local", "Try 'Find YouTube', 'Open Settings' or 'Turn off wallpaper rotation'. Local actions work offline. Gemini is not connected. Never enter passwords."))
    fun add(source: String, text: String) { messages.add(AssistMessage(source, text)); while (messages.size > 20) messages.removeAt(0) }
}

@Composable fun AssistPanel(snapshot: AssistSnapshot, conversation: AssistConversation, offline: Boolean,
    execute: suspend (AssistAction) -> String, close: () -> Unit) {
    val current by rememberUpdatedState(snapshot)
    val scope = rememberCoroutineScope()
    val backend = remember { AssistBackend() }
    val keyboard = LocalSoftwareKeyboardController.current
    val inputMode = LocalInputModeManager.current
    val sendFocus = remember { FocusRequester() }
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<AssistAction?>(null) }
    val list = rememberLazyListState()
    LaunchedEffect(Unit) {
        inputMode.requestInputMode(InputMode.Keyboard)
        kotlinx.coroutines.delay(100)
        sendFocus.requestFocus()
        keyboard?.hide()
    }
    LaunchedEffect(conversation.messages.size) { list.scrollToItem((conversation.messages.size - 1).coerceAtLeast(0)) }
    fun runAction(action: AssistAction) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val outcome = if (action.valid(current)) execute(action) else "Action expired or current settings changed. Ask again; nothing was changed."
                conversation.add("RC TV · Verified action", outcome)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { conversation.add("RC TV · Error", "The action could not be completed. No success is claimed; check Settings or try again.") }
            finally { busy = false; pending = null }
        }
    }
    fun send() {
        if (busy || pending != null || prompt.isBlank()) return
        val text = prompt.trim(); prompt = ""; keyboard?.hide()
        inputMode.requestInputMode(InputMode.Keyboard)
        conversation.add("You", text)
        val reply = AssistActions.interpret(text, current)
        if (reply.needsOnline) {
            busy = true
            scope.launch {
                val answer = backend.reply(text)
                conversation.add(if (answer is OnlineAssistResult.Answer) "Online assistant · unverified text" else "RC TV · Service unavailable", when (answer) {
                    is OnlineAssistResult.Answer -> answer.text
                    OnlineAssistResult.NotConfigured -> "Gemini is not connected. This request needs the online AI service. Try a supported local command, such as Find YouTube or Open Settings."
                    OnlineAssistResult.Unavailable -> "The online service is offline or unavailable. Local commands and the launcher still work."
                })
                busy = false
            }
        } else {
            conversation.add("RC TV · Local", reply.text)
            reply.action?.let { if (it.kind == AssistKind.NAVIGATE) runAction(it) else pending = it }
        }
        runCatching { sendFocus.requestFocus() }
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(28.dp)
        Column(Modifier.fillMaxWidth(.86f).fillMaxHeight(.9f).border(1.dp, Color(0x557DA99F), shape)
            .background(Brush.verticalGradient(listOf(Color(0xF5243A47), Color(0xFC0E1D27))), shape).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("RC Assist", style = MaterialTheme.typography.headlineMedium)
            Text(if (offline) "Offline · local commands ready · Gemini not connected" else "Local commands ready · Gemini not connected", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFBCE7DB))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(conversation.messages.toList()) { message ->
                    Column(Modifier.fillMaxWidth().background(Color(0x552E4857), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text(message.source, style = MaterialTheme.typography.labelMedium, color = Color(0xFFBCE7DB))
                        Text(message.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Working…") }
            OutlinedTextField(prompt, { if (it.length <= 500) prompt = it }, Modifier.fillMaxWidth(), enabled = !busy && pending == null,
                label = { Text("Ask RC Assist · no passwords") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusButton("Send", "Minimal", Modifier.focusRequester(sendFocus)) { send() }
                FocusButton("Close", "Minimal", onClick = close)
            }
        }
    }
    pending?.let { action ->
        val cancel = remember { FocusRequester() }
        LaunchedEffect(action) { inputMode.requestInputMode(InputMode.Keyboard); kotlinx.coroutines.delay(100); cancel.requestFocus() }
        AlertDialog(onDismissRequest = { if (!busy) { pending = null; conversation.add("RC TV · Local", "Cancelled. No action taken.") } },
            title = { Text("Confirm RC TV action") }, text = { Text("${action.description}\n\nOnly this supported action will be performed. Cancel keeps the current state.") },
            confirmButton = { FocusButton(if (busy) "Working…" else "Confirm", "Minimal") { if (!busy) runAction(action) } },
            dismissButton = { FocusButton("Cancel", "Minimal", Modifier.focusRequester(cancel)) { if (!busy) { pending = null; conversation.add("RC TV · Local", "Cancelled. No action taken.") } } })
    }
}
