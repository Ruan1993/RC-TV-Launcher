package za.co.rc.tv.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.co.rc.tv.core.*

/** Uses the same local images, renderer thumbnails, preference callbacks and fullscreen preview. */
@Composable internal fun MyWallpaperGallery(options: List<WallpaperOption>, prefs: LauncherPreferences, motion: String,
    visible: Boolean, onPreview: (WallpaperOption) -> Unit, onApply: suspend (String) -> Boolean,
    onRemove: suspend (String) -> Boolean, onAddRotation: suspend (Set<String>) -> Unit, onClose: () -> Unit) {
    var selection by remember { mutableStateOf(emptySet<String>()) }
    var activeId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<WallpaperOption?>(null) }
    val scope = rememberCoroutineScope()
    val grid = rememberLazyGridState()
    val focuses = remember { mutableMapOf<String, FocusRequester>() }
    val backFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }
    val active = options.firstOrNull { it.id == activeId } ?: options.firstOrNull()
    LaunchedEffect(options, visible, confirm) {
        selection = selection.intersect(options.map { it.id }.toSet())
        focuses.keys.retainAll(options.map { it.id }.toSet())
        if (visible && confirm == null) {
            activeId = active?.id
            val index = options.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
            grid.scrollToItem(index); withFrameNanos { }; withFrameNanos { }
            runCatching { (activeId?.let { focuses[it] } ?: backFocus).requestFocus() }
        }
    }
    LaunchedEffect(confirm) { if (confirm != null) { withFrameNanos { }; cancelFocus.requestFocus() } }
    if (!visible) return
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My Wallpaper Gallery", style = MaterialTheme.typography.headlineLarge)
        Text("OK selects images. Preview, Apply and Remove act on the highlighted image. ${selection.size} selected.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FocusButton("Back", motion, Modifier.focusRequester(backFocus)) { if (!busy) onClose() }
            FocusButton(if (busy) "Working…" else "Add Selected to Rotation", motion) {
                if (!busy && selection.isNotEmpty()) { busy = true; scope.launch {
                    try { onAddRotation(selection); message = "Added ${selection.size} to rotation. Existing choices and rotation On/Off are unchanged."; selection = emptySet() }
                    catch (failure: Exception) { message = failure.message ?: "Could not update rotation." }
                    finally { busy = false }
                } }
            }
            FocusButton("Clear selection", motion) { if (!busy) selection = emptySet() }
        }
        active?.let { image ->
            Text("Highlighted: ${image.title}${if (image.id == prefs.wallpaper) " · Home" else ""}${if (image.id in prefs.rotationPool) " · In rotation" else ""}", maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusButton("Fullscreen preview", motion) { if (!busy) onPreview(image) }
                FocusButton("Apply to Home", motion) { if (!busy) { busy = true; scope.launch {
                    try { message = if (onApply(image.id)) "Applied to Home." else "Could not apply wallpaper." }
                    finally { busy = false }
                } } }
                FocusButton("Remove image…", motion) {
                    if (!busy) {
                        if (canDeleteGalleryWallpaper(image.id, prefs.wallpaper, prefs.rotationPool)) confirm = image
                        else message = "This image is used by Home or rotation. Apply another wallpaper and remove it from rotation first."
                    }
                }
            }
        }
        message?.let { Text(it, color = Color(0xFFC7E9E2)) }
        if (options.isEmpty()) Text("No saved online wallpapers. Preview an online image and choose Add to Gallery.")
        LazyVerticalGrid(GridCells.Fixed(3), state = grid, modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(options, key = { it.id }) { image ->
                val context = LocalContext.current
                val thumbnail by produceState<Bitmap?>(null, image.id) { value = withContext(Dispatchers.IO) { wallpaperThumbnail(context, image.id) } }
                var focused by remember { mutableStateOf(false) }
                val chosen = image.id in selection
                val focus = focuses.getOrPut(image.id) { FocusRequester() }
                OutlinedButton({ if (!busy) selection = if (chosen) selection - image.id else selection + image.id },
                    Modifier.fillMaxWidth().height(180.dp).focusRequester(focus)
                        .onFocusChanged { focused = it.isFocused; if (focused) activeId = image.id }
                        .semantics { selected = chosen }
                        .border(if (focused) 2.dp else 1.dp, if (focused || chosen || image.id == activeId) Color(0xFFC7E9E2) else Color(0x33FFFFFF), RoundedCornerShape(20.dp)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xAA15232D), contentColor = Color.White),
                    contentPadding = PaddingValues(10.dp)) {
                    Column {
                        thumbnail?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().height(105.dp), contentScale = ContentScale.Crop) }
                        Text("${if (chosen) "✓" else "+"} ${image.title}", maxLines = 2)
                    }
                }
            }
        }
    }
    confirm?.let { image -> AlertDialog(onDismissRequest = { if (!busy) confirm = null },
        title = { Text("Remove ${image.title}?") }, text = { Text("This deletes the saved offline image and its credit. Other images are kept.") },
        confirmButton = { FocusButton("Remove", motion) { if (!busy) { busy = true; scope.launch {
            try { message = if (onRemove(image.id)) "Image removed." else "Image could not be removed; it may be in use." }
            finally { busy = false; confirm = null }
        } } } }, dismissButton = { FocusButton("Cancel", motion, Modifier.focusRequester(cancelFocus)) { if (!busy) confirm = null } }) }
}
