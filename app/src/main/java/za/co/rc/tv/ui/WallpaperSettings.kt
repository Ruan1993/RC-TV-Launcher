package za.co.rc.tv.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import za.co.rc.tv.core.BundledWallpaper

@Composable
internal fun WallpaperSettings(selectedId: String, appliedId: String, motion: String, saving: Boolean,
    minimalist: Boolean, onSelect: (String) -> Unit, onPreview: () -> Unit,
    onApply: () -> Unit, onClose: () -> Unit) {
    val selectedFocus = remember { FocusRequester() }
    val previewFocus = remember { FocusRequester() }
    val inputMode = LocalInputModeManager.current
    LaunchedEffect(Unit) { inputMode.requestInputMode(InputMode.Keyboard); withFrameNanos { }; selectedFocus.requestFocus() }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Wallpaper", style = MaterialTheme.typography.headlineLarge)
        Text("Choose a scene. Preview before applying.", style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = .8f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BundledWallpaper.entries.forEach { wallpaper ->
                val context = LocalContext.current
                val bitmap by produceState<Bitmap?>(null, wallpaper) {
                    value = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeResource(context.resources, wallpaper.thumbnailResource())
                    }
                }
                var focused by remember { mutableStateOf(false) }
                val selected = selectedId == wallpaper.id
                val shape = RoundedCornerShape(18.dp)
                OutlinedButton({ if (!saving) onSelect(wallpaper.id) },
                    Modifier.weight(1f).height(196.dp)
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FocusButton("Preview", motion, Modifier.focusRequester(previewFocus).focusProperties { up = selectedFocus }) { if (!saving) onPreview() }
            FocusButton(if (saving) "Applying…" else "Apply wallpaper", motion, Modifier.focusProperties { up = selectedFocus }, onClick = onApply)
            FocusButton("Back", motion, Modifier.focusProperties { up = selectedFocus }) { if (!saving) onClose() }
        }
        if (minimalist) Text("Minimalist keeps a plain background. Your wallpaper is saved for the Cinematic theme.",
            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .78f))
    }
}
