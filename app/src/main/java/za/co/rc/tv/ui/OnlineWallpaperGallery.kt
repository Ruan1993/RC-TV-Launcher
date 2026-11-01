package za.co.rc.tv.ui

import android.graphics.ImageDecoder
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import za.co.rc.tv.core.*

private data class GalleryPreview(val online: OnlineWallpaper? = null, val saved: WallpaperOption? = null) {
    val id get() = online?.id ?: saved!!.id
    val title get() = online?.title ?: saved!!.title
    val attribution get() = online?.attribution ?: saved!!.attribution.orEmpty()
}

@Composable
internal fun OnlineWallpaperGallery(motion: String, onApply: suspend (OnlineWallpaper, Bitmap) -> String,
    onApplied: (String) -> Unit, onClose: () -> Unit,
    options: List<WallpaperOption>, prefs: LauncherPreferences, history: WallpaperPageHistory,
    startSaved: Boolean, onSave: suspend (OnlineWallpaper, Bitmap) -> String,
    onSavedApply: suspend (String) -> Boolean, onRemove: suspend (String) -> Boolean,
    onAddRotation: suspend (Set<String>) -> Unit) {
    val context = LocalContext.current
    val repository = remember { OnlineWallpapers(context) }
    val scope = rememberCoroutineScope()
    var pageIndex by remember { mutableIntStateOf(history.current) }
    var myGallery by remember { mutableStateOf(startSaved) }
    val saved = options.filter { it.attribution != null }
    val myGalleryFocus = remember { FocusRequester() }
    var retry by remember { mutableIntStateOf(0) }
    var page by remember { mutableStateOf<OnlineWallpaperPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<GalleryPreview?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }
    var previewRetry by remember { mutableIntStateOf(0) }
    var applying by remember { mutableStateOf(false) }
    var applied by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val firstFocus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }
    val applyFocus = remember { FocusRequester() }
    val grid = rememberLazyGridState()
    val cardFocus = remember { mutableMapOf<Long, FocusRequester>() }
    fun closePreview() { if (!applying) { selected = null; preview = null; previewError = null; applied = false } }
    LaunchedEffect(pageIndex, retry, myGallery) {
        if (myGallery) return@LaunchedEffect
        error = null; cardFocus.clear()
        try {
            if (history.entry().page == null) history.store(repository.page(history.entry().cursor))
            page = history.entry().page
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { error = failure.message ?: "Cannot connect. Check Wi-Fi and try again."; page = null }
        val restoreId = history.entry().focused
        val index = page?.images?.indexOfFirst { it.page == restoreId }?.coerceAtLeast(0) ?: 0
        grid.scrollToItem(index)
        withFrameNanos { }; withFrameNanos { }
        runCatching { (restoreId?.let { cardFocus[it] } ?: if (page?.images?.isNotEmpty() == true) firstFocus else backFocus).requestFocus() }
    }
    LaunchedEffect(selected, previewRetry) {
        preview = null; previewError = null; applied = false; status = null
        val image = selected ?: return@LaunchedEffect
        try {
            val metrics = context.resources.displayMetrics
            val file = WallpaperStore.image(context, image.id)
            preview = if (file.isFile) withContext(Dispatchers.IO) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                    val size = OnlineWallpaperPolicy.fit(info.size.width, info.size.height, metrics.widthPixels, metrics.heightPixels)
                    decoder.setTargetSize(size.first, size.second); decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else repository.preview(image.online ?: error("Saved image unavailable."), metrics.widthPixels, metrics.heightPixels)
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { previewError = failure.message ?: "Preview unavailable. Check Wi-Fi and try again." }
        withFrameNanos { }; runCatching { applyFocus.requestFocus() }
    }
    var returnCard by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(selected) { if (selected == null) { withFrameNanos { }; returnCard?.let { runCatching { cardFocus[it]?.requestFocus() } } } }
    Dialog(onDismissRequest = { if (selected != null) closePreview() else if (myGallery && !startSaved) myGallery = false else if (!applying) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !applying, dismissOnClickOutside = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0B1822))) {
            if (myGallery) MyWallpaperGallery(saved, prefs, motion, visible = selected == null, onPreview = { selected = GalleryPreview(saved = it) },
                onApply = { id -> if (onSavedApply(id)) { onApplied(id); true } else false }, onRemove = onRemove,
                onAddRotation = onAddRotation, onClose = { if (startSaved) onClose() else myGallery = false })
            if (selected == null && !myGallery) Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Online Wallpapers", style = MaterialTheme.typography.headlineLarge)
                Text("Wikimedia Commons · Featured landscapes · Select to preview", style = MaterialTheme.typography.bodyLarge)
                Text("Thumbnails: up to 10 MB. Add images to your offline gallery without changing Home.", style = MaterialTheme.typography.bodySmall)
                Text("Gallery limit: 16 MB. Saved images are kept until you explicitly remove them.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FocusButton("Back", motion, Modifier.focusRequester(backFocus), onClick = onClose)
                    FocusButton("My Wallpaper Gallery", motion, Modifier.focusRequester(myGalleryFocus)) { myGallery = true }
                    if (page?.next != null) FocusButton("Next page", motion) {
                        if (history.next()) { page = null; pageIndex = history.current } else error = "No new page is available."
                    }
                    FocusButton("Reload", motion) { history.entry().page = null; retry++ }
                }
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history.numbers().size) { index ->
                        val number = history.numbers()[index]
                        FocusButton("$number", motion, selected = number == pageIndex) {
                            if (number != pageIndex) { history.visit(number); page = null; pageIndex = number }
                        }
                    }
                }
                Text("Page $pageIndex · Last 10 pages kept for this app session", style = MaterialTheme.typography.bodySmall)
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
                            try { value = if (saved.any { it.id == image.id }) withContext(Dispatchers.IO) { wallpaperThumbnail(context, image.id) } else repository.thumbnail(image) }
                            catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) { thumbnailFailed = true; value = null }
                        }
                        OutlinedButton(onClick = { returnCard = image.page; selected = GalleryPreview(online = image) },
                            modifier = Modifier.fillMaxWidth().height(180.dp).focusRequester(focus)
                                .onFocusChanged { focused = it.isFocused; if (focused) history.entry().focused = image.page }
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
                            if (bitmap != null && !applying && !applied) { applying = true; previewError = null; status = null; scope.launch {
                                try {
                                    val id = if (image.online != null) onApply(image.online, bitmap)
                                        else image.id.also { check(onSavedApply(it)) { "Could not apply saved wallpaper." } }
                                    onApplied(id); applied = true
                                }
                                catch (failure: Exception) { previewError = failure.message ?: "Could not save. Existing wallpaper unchanged." }
                                finally { applying = false }
                            } }
                        }
                        if (image.online != null) FocusButton(if (saved.any { it.id == image.id }) "In My Gallery" else "Add to Gallery", motion) {
                            val bitmap = preview
                            if (!applying && bitmap != null && saved.none { it.id == image.id }) { applying = true; previewError = null; scope.launch {
                                try { onSave(image.online, bitmap); status = "Saved offline. Home and rotation unchanged." }
                                catch (failure: Exception) { previewError = failure.message ?: "Could not save wallpaper." }
                                finally { applying = false }
                            } }
                        }
                        if (preview == null && previewError != null) FocusButton("Retry preview", motion) { previewRetry++ }
                        FocusButton("Back to gallery", motion) { closePreview() }
                    }
                    status?.let { Text(it, color = Color(0xFFC7E9E2)) }
                    if (preview != null && previewError != null) Text(previewError!!, color = Color(0xFFFFD6AA))
                }
            }
        }
    }
}
