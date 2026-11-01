package za.co.rc.tv.ui

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import za.co.rc.tv.core.*

@Composable
internal fun OnlineWallpaperGallery(motion: String, onApply: suspend (OnlineWallpaper, Bitmap) -> String,
    onApplied: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { OnlineWallpapers(context) }
    val scope = rememberCoroutineScope()
    var cursors by remember { mutableStateOf(listOf<OnlineWallpaperCursor?>(null)) }
    var pageIndex by remember { mutableIntStateOf(0) }
    var retry by remember { mutableIntStateOf(0) }
    var page by remember { mutableStateOf<OnlineWallpaperPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<OnlineWallpaper?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }
    var previewRetry by remember { mutableIntStateOf(0) }
    var applying by remember { mutableStateOf(false) }
    var applied by remember { mutableStateOf(false) }
    val firstFocus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }
    val applyFocus = remember { FocusRequester() }
    val grid = rememberLazyGridState()
    val cardFocus = remember { mutableMapOf<Long, FocusRequester>() }
    fun closePreview() { if (!applying) { selected = null; preview = null; previewError = null; applied = false } }
    LaunchedEffect(pageIndex, retry) {
        page = null; error = null; cardFocus.clear()
        try { page = repository.page(cursors[pageIndex]) }
        catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { error = failure.message ?: "Cannot connect. Check Wi-Fi and try again." }
        grid.scrollToItem(0)
        withFrameNanos { }
        runCatching { if (page?.images?.isNotEmpty() == true) firstFocus.requestFocus() else backFocus.requestFocus() }
    }
    LaunchedEffect(selected, previewRetry) {
        preview = null; previewError = null; applied = false
        val image = selected ?: return@LaunchedEffect
        try {
            val metrics = context.resources.displayMetrics
            preview = repository.preview(image, metrics.widthPixels, metrics.heightPixels)
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { previewError = failure.message ?: "Preview unavailable. Check Wi-Fi and try again." }
        withFrameNanos { }; runCatching { applyFocus.requestFocus() }
    }
    var returnCard by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(selected) { if (selected == null) { withFrameNanos { }; returnCard?.let { runCatching { cardFocus[it]?.requestFocus() } } } }
    Dialog(onDismissRequest = { if (selected != null) closePreview() else if (!applying) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !applying, dismissOnClickOutside = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0B1822))) {
            if (selected == null) Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Online Wallpapers", style = MaterialTheme.typography.headlineLarge)
                Text("Wikimedia Commons · Featured landscapes · Select to preview", style = MaterialTheme.typography.bodyLarge)
                Text("Thumbnails: up to 10 MB. Previews are temporary. Only applied images are saved offline.", style = MaterialTheme.typography.bodySmall)
                Text("A new online image replaces the previous one unless included in rotation. Your imports are kept.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FocusButton("Back", motion, Modifier.focusRequester(backFocus), onClick = onClose)
                    if (pageIndex > 0) FocusButton("Previous", motion) { pageIndex-- }
                    if (page?.next != null) FocusButton("Next page", motion) {
                        cursors = cursors.take(pageIndex + 1) + page!!.next; pageIndex++
                    }
                    FocusButton("Reload", motion) { retry++ }
                    Text("Page ${pageIndex + 1}", Modifier.align(Alignment.CenterVertically))
                }
                error?.let { Text(it, color = Color(0xFFFFD6AA)) }
                if (page == null && error == null) Text("Loading landscapes…")
                if (page?.images?.isEmpty() == true) Text("No compatible licensed images on this page. Try the next page.")
                LazyVerticalGrid(GridCells.Fixed(3), state = grid, modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(page?.images.orEmpty(), key = { it.page }) { image ->
                        var focused by remember { mutableStateOf(false) }
                        val focus = cardFocus.getOrPut(image.page) { FocusRequester() }
                        var thumbnailFailed by remember(image, retry) { mutableStateOf(false) }
                        val thumbnail by produceState<Bitmap?>(null, image, retry) {
                            try { value = repository.thumbnail(image) }
                            catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) { thumbnailFailed = true; value = null }
                        }
                        OutlinedButton(onClick = { returnCard = image.page; selected = image },
                            modifier = Modifier.fillMaxWidth().height(180.dp).focusRequester(focus)
                                .onFocusChanged { focused = it.isFocused }
                                .border(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFC7E9E2) else Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                                .then(if (page?.images?.firstOrNull()?.page == image.page) Modifier.focusRequester(firstFocus) else Modifier),
                            contentPadding = PaddingValues(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xAA15232D), contentColor = Color.White)) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                thumbnail?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().height(105.dp), contentScale = ContentScale.Crop) }
                                    ?: Text(if (thumbnailFailed) "Thumbnail unavailable · Select to preview" else "Loading thumbnail…", Modifier.height(105.dp))
                                Text(image.title, maxLines = 2, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
            selected?.let { image ->
                val creditScroll = rememberScrollState()
                var creditFocused by remember { mutableStateOf(false) }
                val creditStep = with(LocalDensity.current) { 48.dp.toPx() }
                Column(Modifier.fillMaxSize().background(Color(0xFF09131C)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        preview?.let { Image(it.asImageBitmap(), image.title, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                            ?: Text(previewError ?: "Loading fullscreen preview…", color = Color.White)
                    }
                    // Credit is deliberately retained in full, including source, licence and modification notice.
                    Column(Modifier.fillMaxWidth().heightIn(max = 110.dp)
                        .border(1.dp, if (creditFocused) Color(0xFFC7E9E2) else Color.Transparent, RoundedCornerShape(8.dp))
                        .verticalScroll(creditScroll).onFocusChanged { creditFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            val key = event.nativeKeyEvent
                            val down = key.keyCode == KeyEvent.KEYCODE_DPAD_DOWN && creditScroll.canScrollForward
                            val up = key.keyCode == KeyEvent.KEYCODE_DPAD_UP && creditScroll.canScrollBackward
                            if (key.action == KeyEvent.ACTION_DOWN && (down || up)) {
                                scope.launch { creditScroll.scrollBy(if (down) creditStep else -creditStep) }; true
                            } else false
                        }.focusable()) {
                        Text(image.attribution, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FocusButton(if (applying) "Saving…" else if (applied) "Applied offline" else "Apply wallpaper", motion, Modifier.focusRequester(applyFocus)) {
                            val bitmap = preview
                            if (bitmap != null && !applying && !applied) { applying = true; previewError = null; scope.launch {
                                try { val id = onApply(image, bitmap); onApplied(id); applied = true }
                                catch (failure: Exception) { previewError = failure.message ?: "Could not save. Existing wallpaper unchanged." }
                                finally { applying = false }
                            } }
                        }
                        if (preview == null && previewError != null) FocusButton("Retry preview", motion) { previewRetry++ }
                        FocusButton("Back to gallery", motion) { closePreview() }
                    }
                    if (preview != null && previewError != null) Text(previewError!!, color = Color(0xFFFFD6AA))
                }
            }
        }
    }
}
