package za.co.rc.tv.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import za.co.rc.tv.core.*

@Composable internal fun BackupRestore(motion: String) {
    val context = LocalContext.current
    val backup = remember(context) { SettingsBackup(context) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    var localCopies by remember { mutableStateOf<List<LocalBackup>?>(null) }
    val exportFocus = remember { FocusRequester() }
    val importFocus = remember { FocusRequester() }
    var returnToImport by remember { mutableStateOf(false) }
    var returning by remember { mutableStateOf(false) }
    val input = LocalInputModeManager.current
    val statusPosition = remember { BringIntoViewRequester() }
    LaunchedEffect(message) { if (message.isNotEmpty()) { withFrameNanos { }; statusPosition.bringIntoView() } }
    val savePicker = remember { backup.pickerAvailable(android.content.Intent.ACTION_CREATE_DOCUMENT) }
    val openPicker = remember { backup.pickerAvailable(android.content.Intent.ACTION_OPEN_DOCUMENT) }
    fun resultFocus(importing: Boolean) { returnToImport = importing; returning = true }
    LaunchedEffect(returning, busy, preview) {
        if (returning && !busy && preview == null) {
            withFrameNanos { }; input.requestInputMode(InputMode.Keyboard)
            (if (returnToImport) importFocus else exportFocus).requestFocus(); returning = false
        }
    }
    fun fail(error: Exception) {
        if (error is CancellationException) throw error
        message = "Could not complete this operation. Your settings were not changed. ${error.message?.take(180).orEmpty()}"
    }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) { message = "Backup cancelled. Your settings were not changed."; resultFocus(false) }
        else { busy = true; scope.launch { try { val bytes = backup.export(uri); message = "Backup saved and read back successfully ($bytes bytes). Keep it somewhere private: it includes your weather location." }
            catch (e: Exception) { fail(e); message += " An incomplete file may remain at the selected destination." }
            finally { busy = false; resultFocus(false) } } }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) { message = "Restore cancelled. Your settings were not changed."; resultFocus(true) }
        else { busy = true; scope.launch { try { preview = backup.inspect(uri); message = "Backup validated. Review it before restoring." }
            catch (e: Exception) { fail(e) } finally { busy = false; resultFocus(true) } } }
    }
    Text("Protect your RC TV settings", style = MaterialTheme.typography.titleMedium, color = LocalAppearance.current.accent)
    Text("Save favourites and their order, appearance and animation choices, screensaver settings, and weather location. " +
        if (savePicker) "Choose a USB/HDD or another writable location in Android’s file picker." else "Use TV Downloads below for a local copy; this TV’s storage picker is unavailable.")
    if (message.isNotEmpty()) Text(message, Modifier.bringIntoViewRequester(statusPosition), color = LocalAppearance.current.accent)
    FocusButton(if (busy) "Working…" else "Create backup", motion, Modifier.focusRequester(exportFocus)) {
        if (!busy && !savePicker) message = "This TV firmware does not provide a working Android save picker. USB/HDD export is unavailable here. You can save a local copy to TV Downloads instead."
        else if (!busy) { message = ""; runCatching { create.launch("RC-TV-settings-${java.text.SimpleDateFormat("yyyy-MM-dd-HHmmss", java.util.Locale.ROOT).format(java.util.Date())}.json") }
            .onFailure { message = "Android's save picker is unavailable. No settings changed." } }
    }
    FocusButton("Restore from backup", motion, Modifier.focusRequester(importFocus)) {
        if (!busy && !openPicker) message = "This TV firmware does not provide a working Android file picker. Use Restore a TV Downloads copy for files RC TV saved on this TV. Import from other storage needs a working system picker."
        else if (!busy) { message = ""; runCatching { open.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
            .onFailure { message = "Android's file picker is unavailable. No settings changed." } }
    }
    if (!savePicker || !openPicker) Text("Android’s file picker is unavailable on this TV. The local options below use TV Downloads, not your USB/HDD. A local copy does not protect against a TV reset or device failure.", color = LocalAppearance.current.accent)
    FocusButton("Save a copy to TV Downloads", motion) {
        if (!busy) { busy = true; scope.launch { try {
            val saved = backup.saveToDownloads(); message = "Saved and verified: TV Downloads / RC TV / ${saved.name}. This is on the TV, not your USB/HDD. Copy it off this TV using a trusted file app or PC for independent protection."
        } catch (e: Exception) { fail(e) } finally { busy = false } } }
    }
    FocusButton("Restore a TV Downloads copy", motion) {
        if (!busy) { busy = true; scope.launch { try { val copies = backup.downloads(); if (copies.isEmpty()) message = "No RC TV backups found in this TV’s Downloads." else localCopies = copies }
            catch (e: Exception) { fail(e) } finally { busy = false } } }
    }
    Text("Backups include your chosen weather location and coordinates. They are readable settings files, not encrypted archives. Store them privately.")
    Text("Phone credentials, passwords, keys, Android Home selection and permissions are never included. Existing pairing on this TV stays untouched; securely pair your phone again on another TV. Keyboard activation and remote permissions must be set up on that TV.")
    Text("Imported wallpaper image files and apps are not included. Keep original images separately. Missing images are shown in the restore preview and use a bundled fallback.")
    BackHandler(busy && preview == null) { /* Keep a running file operation visible. */ }
    localCopies?.let { copies ->
        val cancelFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { withFrameNanos { }; input.requestInputMode(InputMode.Keyboard); cancelFocus.requestFocus() }
        AlertDialog(onDismissRequest = { if (!busy) localCopies = null }, title = { Text("TV Downloads backups") }, text = {
            Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                copies.forEach { copy -> FocusButton(copy.name, motion) {
                    if (!busy) { busy = true; scope.launch { try { preview = backup.inspect(copy.uri); message = "Backup validated. Review it before restoring." }
                        catch (e: Exception) { fail(e) } finally { localCopies = null; busy = false; resultFocus(true) } } }
                } }
            }
        }, confirmButton = { FocusButton("Cancel", motion, Modifier.focusRequester(cancelFocus)) { if (!busy) localCopies = null } })
    }
    preview?.let { candidate ->
        val cancelFocus = remember { FocusRequester() }
        val detailsFocus = remember { FocusRequester() }
        val detailsScroll = rememberScrollState()
        var detailsFocused by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { withFrameNanos { }; input.requestInputMode(InputMode.Keyboard); cancelFocus.requestFocus() }
        AlertDialog(onDismissRequest = { if (!busy) { preview = null; message = "Restore cancelled. Your settings were not changed." } },
            title = { Text("Restore this backup?") },
            text = {
                Column(Modifier.heightIn(max = 280.dp).border(1.dp, if (detailsFocused) LocalAppearance.current.accent else androidx.compose.ui.graphics.Color.Transparent)
                    .verticalScroll(detailsScroll).focusRequester(detailsFocus).onFocusChanged { detailsFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        val key = event.nativeKeyEvent
                        val delta = when (key.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> if (detailsScroll.canScrollForward) 140f else null
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> if (detailsScroll.canScrollBackward) -140f else null
                            else -> null
                        }
                        if (delta != null) { if (key.action == android.view.KeyEvent.ACTION_DOWN) scope.launch { detailsScroll.scrollBy(delta) }; true } else false
                    }.focusable(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Saved ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(candidate.createdAt))} · RC TV ${candidate.appVersion}")
                    Text("${candidate.favourites.size} favourites in saved order:\n${candidate.favouriteLabels.ifEmpty { candidate.favourites }.joinToString(" → ").ifEmpty { "None" }}")
                    Text("Appearance: ${candidate.values["theme"]}, ${candidate.values["accent_theme"]}, ${candidate.values["font_style"]}, ${candidate.values["text_size"]}. Wallpaper: ${candidate.values["wallpaper"]}. Rotation: ${candidate.values["wallpaper_rotation"]} / ${candidate.values["wallpaper_interval"]} minutes.")
                    Text("Motion: ${candidate.values["animation_intensity"]}; focus ${candidate.values["focus_animation"]}; Reduced Motion ${candidate.values["reduced_motion"]}.")
                    Text("Wallpaper visibility: ${candidate.values["wallpaper_visibility"]}; transition: ${candidate.values["wallpaper_transition"]}; rotation pool: ${(candidate.values["wallpaper_pool"] as Set<*>).size} images.")
                    Text("Screensaver: ${candidate.values["screensaver_provider"]}, ${candidate.values["screensaver_style"]}, ${candidate.values["screensaver_minutes"]} minutes (0 = Never).")
                    val weather = WeatherConfiguration.decode(candidate.values.getValue("weather_configuration") as String)
                    Text("Weather: ${weather.place?.label ?: "No saved location"} · ${if (weather.enabled) "On" else "Off"}")
                    candidate.notes.forEach { Text(it) }
                    Text("This replaces supported settings and favourites together. Phone pairing, Android Home, permissions and other app data stay unchanged. Cancel to keep your current settings.")
                }
            },
            confirmButton = { FocusButton(if (busy) "Restoring…" else "Restore settings", motion, Modifier.focusProperties { up = detailsFocus }) {
                if (!busy) { busy = true; scope.launch { try {
                    backup.restore(candidate); preview = null; message = "Settings restored successfully. Existing phone pairing and Android settings were kept."
                } catch (e: Exception) { fail(e); preview = null } finally { busy = false; resultFocus(true) } } }
            } },
            dismissButton = { FocusButton("Cancel", motion, Modifier.focusRequester(cancelFocus).focusProperties { up = detailsFocus }) { if (!busy) { preview = null; message = "Restore cancelled. Your settings were not changed." } } })
    }
}
