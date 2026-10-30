package za.co.rc.tv.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import za.co.rc.tv.core.*
import kotlinx.coroutines.launch

@Composable
internal fun WallpaperSettings(options: List<WallpaperOption>, prefs: LauncherPreferences,
    onImport: () -> Unit, onRemove: suspend (String) -> Boolean,
    onRotation: (Boolean) -> Unit, onInterval: (Int) -> Unit, onPool: (String) -> Unit,
    onTransition: (String) -> Unit, onNext: () -> Unit, selectedId: String, appliedId: String, motion: String, saving: Boolean,
    minimalist: Boolean, onSelect: (String) -> Unit, onPreview: () -> Unit,
    onApply: () -> Unit, onClose: () -> Unit) {
    var removeConfirmation by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val removeCancelFocus = remember { FocusRequester() }
    LaunchedEffect(removeConfirmation) { if (removeConfirmation) { withFrameNanos { }; removeCancelFocus.requestFocus() } }
    val galleryState = rememberLazyListState()
    val safeSelection = selectedId.takeIf { id -> options.any { it.id == id } } ?: "mountain"
    val selectedFocus = remember { FocusRequester() }
    val previewFocus = remember { FocusRequester() }
    val inputMode = LocalInputModeManager.current
    LaunchedEffect(Unit) {
        inputMode.requestInputMode(InputMode.Keyboard)
        galleryState.scrollToItem(options.indexOfFirst { it.id == safeSelection }.coerceAtLeast(0))
        withFrameNanos { }; selectedFocus.requestFocus()
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Wallpaper", style = MaterialTheme.typography.headlineLarge)
        Text("Choose a scene. Preview before applying.", style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = .8f))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
          val cardWidth = ((maxWidth - 28.dp) / 3).coerceAtLeast(200.dp)
          LazyRow(state = galleryState, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(options, key = { it.id }) { wallpaper ->
                val context = LocalContext.current
                val bitmap by produceState<Bitmap?>(null, wallpaper) {
                    value = withContext(Dispatchers.IO) {
                        wallpaperThumbnail(context, wallpaper.id)
                    }
                }
                var focused by remember { mutableStateOf(false) }
                val selected = safeSelection == wallpaper.id
                val shape = RoundedCornerShape(18.dp)
                OutlinedButton({ if (!saving) onSelect(wallpaper.id) },
                    Modifier.width(cardWidth).height(196.dp)
                        .then(if (selected) Modifier.focusRequester(selectedFocus) else Modifier)
                        .focusProperties { down = previewFocus }
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { this.selected = selected }
                        .border(if (focused) 2.dp else 1.dp,
                            if (focused) Color(0xFFD5F5EB) else if (selected) Color(0xFF8FBDB5) else Color(0x44FFFFFF), shape),
                    shape = shape, border = null,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xAA15232D), contentColor = Color.White),
                    contentPadding = PaddingValues(10.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().height(100.dp), contentScale = ContentScale.Crop) }
                        Text(wallpaper.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(if (appliedId == wallpaper.id) "Current" else if (selected) "Selected" else "",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFFC7E9E2))
                    }
                }
            }
        }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FocusButton("Preview", motion, Modifier.focusRequester(previewFocus).focusProperties { up = selectedFocus }) { if (!saving) onPreview() }
            FocusButton(if (saving) "Applying…" else "Apply wallpaper", motion, Modifier.focusProperties { up = selectedFocus }, onClick = onApply)
            FocusButton("Back", motion, Modifier.focusProperties { up = selectedFocus }) { if (!saving) onClose() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FocusButton(if (saving) "Working…" else "Import image…", motion) { if (!saving) onImport() }
            if (isImportedWallpaper(safeSelection)) FocusButton("Remove selected…", motion) { if (!saving) removeConfirmation = true }
        }
        HorizontalDivider(color = Color.White.copy(alpha = .12f))
        Text("Automatic rotation", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FocusButton("Rotation: ${if (prefs.rotationEnabled) "on" else "off"}", motion, selected = prefs.rotationEnabled) { onRotation(!prefs.rotationEnabled) }
            val intervalLabel = when (prefs.rotationMinutes) { 60 -> "1 hour"; 1440 -> "24 hours"; else -> "${prefs.rotationMinutes} minutes" }
            FocusButton("Interval: $intervalLabel", motion) { onInterval(rotationIntervals[(rotationIntervals.indexOf(prefs.rotationMinutes) + 1).mod(rotationIntervals.size)]) }
            FocusButton("Next wallpaper", motion, onClick = onNext)
        }
        Text("Select Interval to change it. Rotation runs while Home is active; at least two scenes must be included.", style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(options, key = { it.id }) { wallpaper ->
                val included = wallpaper.id in prefs.rotationPool
                FocusButton("${if (included) "✓" else "+"} ${wallpaper.title}", motion, selected = included) { onPool(wallpaper.id) }
            }
        }
        Text("Transition", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Instant", "Crossfade", "Gentle Slide").forEach { value ->
                FocusButton(value, motion, selected = prefs.wallpaperTransition == value) { onTransition(value) }
            }
        }
        Text("Minimal and reduced motion switch instantly.", style = MaterialTheme.typography.bodySmall)
        if (minimalist) Text("Minimalist keeps a plain background. Your wallpaper is saved for the Cinematic theme.",
            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .78f))
    }
    if (removeConfirmation) AlertDialog(onDismissRequest = { if (!removing) removeConfirmation = false },
        title = { Text("Remove imported wallpaper?") },
        text = { Text("The private copy will be deleted. If it is current, the mountain will be restored. Your original image is unchanged.") },
        confirmButton = { FocusButton(if (removing) "Removing…" else "Remove", motion) {
            if (!removing) { removing = true; scope.launch {
                if (onRemove(safeSelection)) { onSelect("mountain"); removeConfirmation = false }
                removing = false
            } }
        } },
        dismissButton = { FocusButton("Cancel", motion, Modifier.focusProperties { canFocus = true }.focusRequester(removeCancelFocus)) { if (!removing) removeConfirmation = false } })
}
