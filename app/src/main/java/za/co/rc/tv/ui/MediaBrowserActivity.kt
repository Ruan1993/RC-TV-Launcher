package za.co.rc.tv.ui

import android.content.*
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import za.co.rc.tv.core.*

/** Separate media activity: approved Home layout and launcher/task routing are unchanged. */
class MediaBrowserActivity : ComponentActivity() {
    companion object { const val OPERATION = "operation"; const val IMPORT = "import-backup"; const val EXPORT = "export-backup" }
    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val browser = MediaBrowser(this)
        val preferences = Preferences(this)
        val operation = intent.getStringExtra(OPERATION).orEmpty()
        setContent {
            val prefs by preferences.state.collectAsStateWithLifecycle(LauncherPreferences())
            LauncherAppearance(prefs) {
                val motion = if (prefs.reducedMotion) "Minimal" else prefs.motion
                val scope = rememberCoroutineScope()
                var sources by remember { mutableStateOf<List<MediaSource>>(emptyList()) }
                var sourceGeneration by remember { mutableIntStateOf(0) }
                var trail by rememberSaveable { mutableStateOf("[]") }
                var message by remember { mutableStateOf("") }
                var generation by remember { mutableIntStateOf(0) }
                var allowed by remember { mutableStateOf(browser.canReadIndexed()) }
                var sourceLoading by remember { mutableStateOf(true) }
                var sort by rememberSaveable { mutableStateOf(MediaSort.NAME.name) }
                var returnId by rememberSaveable { mutableStateOf("") }
                var needsFocus by remember { mutableStateOf(true) }
                var image by remember { mutableStateOf<MediaEntry?>(null) }
                var saving by remember { mutableStateOf(false) }
                val pickerAvailable = remember { browser.folderPickerAvailable() }
                val records = remember(trail) { JSONArray(trail) }
                val record = if (records.length() > 0) records.getJSONObject(records.length() - 1) else null
                val source = sources.firstOrNull { it.id == record?.optString("source") }
                val location = source?.let { MediaLocation(it, record!!.getString("directory"), record.getString("label")) }
                val key = location?.let { "${it.source.id}/${it.directory}" }.orEmpty()
                var page by remember(key) { mutableIntStateOf(0) }
                var data by remember(key) { mutableStateOf<List<MediaEntry>>(emptyList()) }
                var hasNext by remember(key) { mutableStateOf(false) }
                var loading by remember(key) { mutableStateOf(false) }
                val listState = rememberLazyListState()
                val backFocus = remember { FocusRequester() }
                val entryFocus = remember { FocusRequester() }
                fun refresh() { page = 0; generation++; sourceGeneration++; message = ""; image = null }
                fun enter(next: MediaLocation, entry: String) {
                    trail = JSONArray(trail).put(JSONObject().put("source", next.source.id).put("directory", next.directory).put("label", next.label).put("entry", entry)).toString()
                    returnId = ""; needsFocus = true; message = ""
                }
                fun back() {
                    if (saving) return
                    if (image != null) { image = null; needsFocus = true }
                    else if (records.length() > 0) {
                        returnId = records.getJSONObject(records.length() - 1).getString("entry")
                        needsFocus = true
                        trail = JSONArray().apply { for (i in 0 until records.length() - 1) put(records.get(i)) }.toString(); message = ""
                    } else finish()
                }
                val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    allowed = browser.canReadIndexed(); refresh()
                    if (!allowed) message = "Media permission was not granted. Connected folders with explicit access can still be browsed."
                }
                val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                    if (uri != null) scope.launch { try { browser.rememberTree(uri); sourceGeneration++; message = "Folder connected. Select it under Storage." }
                        catch (e: Exception) { message = "Folder access could not be saved. Choose it again and allow access." } }
                }
                val player = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { needsFocus = true /* A result is not playback proof. */ }
                LaunchedEffect(sourceGeneration) {
                    sourceLoading = true
                    try { sources = browser.sources(operation.isEmpty()); allowed = browser.canReadIndexed() }
                    catch (e: Exception) { if (e is CancellationException) throw e; message = "Storage information is unavailable. Try Refresh." }
                    finally { sourceLoading = false }
                }
                LaunchedEffect(sourceLoading, sources, trail) {
                    if (!sourceLoading && records.length() > 0 && source == null) {
                        trail = "[]"; returnId = ""; image = null; needsFocus = true
                        message = "This storage is no longer available. Reconnect it and Refresh."
                    }
                }
                LaunchedEffect(key, page, generation, allowed) {
                    val current = location ?: return@LaunchedEffect
                    loading = true
                    if (page == 0) data = emptyList()
                    try {
                        val result = browser.page(current, page)
                        if (page > 0) {
                            val existing = data.map { it.id }.toSet()
                            result.entries.firstOrNull { it.id !in existing }?.let { returnId = it.id; needsFocus = true }
                        }
                        data = if (page == 0) result.entries else (data + result.entries).distinctBy { it.id }; hasNext = result.hasNext
                    }
                    catch (e: Exception) {
                        if (e is CancellationException) throw e
                        data = emptyList(); hasNext = false
                        message = if (e is SecurityException || (!allowed && current.source.tree == null)) "Allow media access or select the folder again. No files were changed."
                            else "Media is unavailable. The drive may be disconnected or its folder access may have changed. Return to Storage or Refresh."
                    } finally { loading = false }
                }
                DisposableEffect(key) {
                    val receiver = object : BroadcastReceiver() { override fun onReceive(context: Context?, intent: Intent?) { refresh(); message = "Storage changed. The media list has been refreshed." } }
                    val filter = IntentFilter().apply { addAction(Intent.ACTION_MEDIA_MOUNTED); addAction(Intent.ACTION_MEDIA_UNMOUNTED); addAction(Intent.ACTION_MEDIA_EJECT); addAction(Intent.ACTION_MEDIA_REMOVED); addAction(Intent.ACTION_MEDIA_BAD_REMOVAL); addDataScheme("file") }
                    if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(receiver, filter)
                    onDispose { unregisterReceiver(receiver) }
                }
                DisposableEffect(Unit) {
                    val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { sourceGeneration++; needsFocus = true } }
                    lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
                }
                val visible = remember(data, sort, operation) { sortedMedia(data.filter { it.kind == MediaKind.FOLDER || when (operation) { IMPORT -> it.kind == MediaKind.BACKUP; EXPORT -> false; else -> it.kind != MediaKind.BACKUP } }, MediaSort.valueOf(sort)) }
                LaunchedEffect(key, sourceLoading, loading, returnId, image, needsFocus, visible) {
                    if (needsFocus && !sourceLoading && !loading && image == null) {
                        withFrameNanos { }
                        val index = if (location == null) sources.indexOfFirst { it.id == returnId } else visible.indexOfFirst { it.id == returnId }
                        if (index >= 0) { listState.scrollToItem(index); withFrameNanos { }; runCatching { entryFocus.requestFocus() } }
                        else { if (returnId.isEmpty()) listState.scrollToItem(0); runCatching { backFocus.requestFocus() } }
                        needsFocus = false
                    }
                }
                BackHandler { back() }
                Box(Modifier.fillMaxSize().background(Color(0xFF0B131B))) {
                    if (image == null) Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(when (operation) { IMPORT -> "Choose an RC TV backup"; EXPORT -> "Choose a backup folder"; else -> "Media Browser" }, style = MaterialTheme.typography.headlineLarge)
                        Text(if (location == null) "Storage" else records.let { (0 until it.length()).joinToString(" › ") { i -> it.getJSONObject(i).getString("label") } }, maxLines = 2)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FocusButton(if (location == null) "Back to RC TV" else "Up", motion, Modifier.focusRequester(backFocus)) { back() }
                            FocusButton("Refresh", motion) { refresh() }
                            if (operation.isEmpty()) FocusButton(if (allowed) "Media permissions…" else "Allow media access", motion) { permission.launch(mediaReadPermissions(Build.VERSION.SDK_INT)) }
                            FocusButton("Connect folder…", motion) {
                                if (pickerAvailable) runCatching { folder.launch(null) }.onFailure { message = "Android’s folder picker could not be opened." }
                                else message = "This firmware has no working Android folder picker. Indexed media and Xiaomi Media Player remain available; private USB folders cannot be opened here."
                            }
                            if (operation.isEmpty()) FocusButton("Xiaomi Media Player", motion) {
                                val target = packageManager.getLeanbackLaunchIntentForPackage("com.xiaomi.mitv.mediaexplorer") ?: packageManager.getLaunchIntentForPackage("com.xiaomi.mitv.mediaexplorer")
                                if (target == null || runCatching { player.launch(target) }.isFailure) message = "Xiaomi Media Player is not installed. No alternative app was opened."
                            }
                        }
                        if (operation.isEmpty() && location == null) Text("Browse media Android shares with RC TV. USB folders may be available only through Xiaomi Media Player on this firmware.", style = MaterialTheme.typography.bodyMedium)
                        if (location?.source?.volume != null) Text("Indexed media only. Scan more to discover additional folders/files; sorting applies to the loaded results.", style = MaterialTheme.typography.bodySmall)
                        if (message.isNotEmpty()) Text(message, color = LocalAppearance.current.accent, maxLines = 3)
                        if (operation == EXPORT && location?.source?.tree != null) FocusButton(if (saving) "Saving…" else "Save backup here", motion) {
                            if (!saving) { saving = true; scope.launch { try {
                                val directory = android.provider.DocumentsContract.buildDocumentUriUsingTree(location.source.tree, location.directory)
                                val saved = SettingsBackup(this@MediaBrowserActivity).saveInFolder(directory)
                                setResult(RESULT_OK, Intent().setData(saved.uri).putExtra("name", saved.name)); finish()
                            } catch (e: Exception) { if (e is CancellationException) throw e; message = "Could not save a verified backup here. Check write access or choose another folder. Settings were not changed." }
                            finally { saving = false } } }
                        }
                        if (sourceLoading || loading) Text("Loading media…")
                        if (location == null && !sourceLoading && sources.isEmpty()) Text("No accessible storage folders. Connect a drive and Refresh, or choose a folder if Android provides a picker.")
                        if (location != null && !loading && visible.isEmpty()) Text(if (operation == IMPORT) "No RC TV JSON backups on this page." else "No accessible media on this page. Try Scan more, check permissions, or use Xiaomi Media Player for private USB folders.")
                        if (location != null && operation.isEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MediaSort.entries.forEach { item -> FocusButton(item.label, motion, selected = sort == item.name) { sort = item.name } }
                        }
                        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (location == null) items(sources, key = { it.id }) { item ->
                                FocusButton(item.label + if (item.tree != null) " · Connected folder" else " · Indexed media", motion,
                                    Modifier.fillMaxWidth().then(if (returnId == item.id) Modifier.focusRequester(entryFocus) else Modifier)) { enter(browser.root(item), item.id) }
                            } else items(visible, key = { it.id }) { entry ->
                                val metadata = if (entry.kind == MediaKind.FOLDER) "Folder" else listOf(entry.kind.name.lowercase().replaceFirstChar(Char::uppercase),
                                    entry.name.substringAfterLast('.', "").uppercase(), android.text.format.Formatter.formatShortFileSize(this@MediaBrowserActivity, entry.bytes),
                                    if (entry.modified > 0) java.text.DateFormat.getDateInstance().format(java.util.Date(entry.modified)) else "").filter(String::isNotBlank).joinToString(" · ")
                                FocusButton("${entry.name}\n$metadata", motion, Modifier.fillMaxWidth().semantics { contentDescription = entry.name }.then(if (returnId == entry.id) Modifier.focusRequester(entryFocus) else Modifier), quiet = true) {
                                    returnId = entry.id
                                    when (entry.kind) {
                                        MediaKind.FOLDER -> enter(MediaLocation(location.source, entry.directory!!, entry.name), entry.id)
                                        MediaKind.IMAGE -> image = entry
                                        MediaKind.BACKUP -> if (operation == IMPORT) { setResult(RESULT_OK, Intent().setData(entry.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)); finish() }
                                        else -> {
                                            val target = browser.vlcIntent(entry)
                                            if (packageManager.resolveActivity(target, 0) == null) message = "VLC is not installed or cannot open this media type. Install VLC, then try again; no other player was selected."
                                            else runCatching { player.launch(target) }.onFailure { message = "VLC could not open this file. Check the drive and its read permission." }
                                        }
                                    }
                                }
                            }
                        }
                        if (location != null && hasNext && !loading) FocusButton("Scan more", motion) { page++ }
                    }
                    image?.let { entry -> FullscreenMediaImage(browser, entry) { image = null; needsFocus = true } }
                }
            }
        }
    }
}

@Composable private fun FullscreenMediaImage(browser: MediaBrowser, entry: MediaEntry, close: () -> Unit) {
    var bitmap by remember(entry.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember(entry.id) { mutableStateOf("") }
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(entry.id) {
        try { bitmap = browser.image(entry.uri!!) }
        catch (e: Exception) { if (e is CancellationException) throw e; error = "Image unavailable or too large. Check the drive and try again." }
        withFrameNanos { }; closeFocus.requestFocus()
    }
    DisposableEffect(entry.id) { onDispose { bitmap?.recycle() } }
    BackHandler(onBack = close)
    Column(Modifier.fillMaxSize().background(Color.Black).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { FocusButton("Close image", "Minimal", Modifier.focusRequester(closeFocus), onClick = close); Text(entry.name, maxLines = 2) }
        if (error.isNotEmpty()) Text(error) else if (bitmap == null) Text("Loading image…")
        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
    }
}
