package za.co.rc.tv.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import za.co.rc.tv.core.*

/** Internal, permission-gated image chooser; import still uses WallpaperStore in MainActivity. */
class MediaImageBrowserActivity : ComponentActivity() {
    private val library by lazy { MediaImages(this) }
    private fun choose(uri: android.net.Uri) {
        setResult(RESULT_OK, Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)); finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(typography = LauncherTypography, colorScheme = darkColorScheme(primary = Color(0xFFC7E9E2))) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    var allowed by remember { mutableStateOf(library.canRead()) }
                    var generation by remember { mutableIntStateOf(0) }
                    var page by remember { mutableIntStateOf(0) }
                    var data by remember { mutableStateOf(MediaImagePage(emptyList(), false)) }
                    var loading by remember { mutableStateOf(false) }
                    var error by remember { mutableStateOf<String?>(null) }
                    var selected by remember { mutableStateOf<MediaImage?>(null) }
                    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                        allowed = library.canRead(); page = 0; generation++
                        if (!allowed) error = "Image access was not granted. You can retry or use a working document picker."
                    }
                    val document = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { choose(it) } }
                    DisposableEffect(Unit) {
                        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { allowed = library.canRead(); generation++ } }
                        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
                    }
                    LaunchedEffect(allowed, page, generation) {
                        if (allowed) {
                            loading = true; error = null
                            try { data = library.page(page) }
                            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (failure: Exception) { error = "Images are unavailable. Check image permissions or try Refresh."; data = MediaImagePage(emptyList(), false) }
                            finally { loading = false }
                        } else data = MediaImagePage(emptyList(), false)
                    }
                    BackHandler(selected != null) { selected = null }
                    Column(Modifier.fillMaxSize().background(Color(0xFF0B1720)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Local images", style = MaterialTheme.typography.headlineLarge)
                        Text("Images Android makes available. USB images appear only if the device indexes them; other USB folders need a supported document provider.", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FocusButton(if (allowed) "Image permissions…" else "Allow image access", "Minimal") {
                                if (allowed) startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName")))
                                else permissions.launch(imageReadPermissions(Build.VERSION.SDK_INT))
                            }
                            FocusButton("Refresh", "Minimal") { generation++ }
                            FocusButton("System picker…", "Minimal") {
                                val handler = packageManager.resolveActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                                if (handler == null || handler.activityInfo.packageName == "com.google.android.tv.frameworkpackagestubs") error = "No working system picker on this firmware. USB folders not indexed by Android are unavailable here."
                                else runCatching { document.launch(arrayOf("image/jpeg", "image/png", "image/webp")) }.onFailure { error = "System picker unavailable." }
                            }
                            FocusButton("Back", "Minimal") { finish() }
                        }
                        if (!allowed) Text("Image access is requested only for browsing images. RC TV does not request all-files access.", style = MaterialTheme.typography.bodyMedium)
                        error?.let { Text(it, color = Color(0xFFFFD3B0), style = MaterialTheme.typography.bodyMedium) }
                        if (loading) Text("Loading images…")
                        else if (allowed && data.images.isEmpty()) Text("No accessible JPEG, PNG or WebP images on this page. Add images to shared Pictures/Download storage and refresh.")
                        LazyVerticalGrid(GridCells.Adaptive(210.dp), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(data.images, key = { it.uri.toString() }) { image ->
                                val bitmap by produceState<android.graphics.Bitmap?>(null, image.uri) { value = library.thumbnail(image.uri) }
                                var focused by remember { mutableStateOf(false) }
                                val shape = RoundedCornerShape(18.dp)
                                OutlinedButton({ selected = image }, Modifier.fillMaxWidth().height(170.dp).onFocusChanged { focused = it.isFocused }
                                    .border(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFD5F5EB) else Color(0x447DA99F), shape), shape = shape,
                                    border = null, colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF192D39), contentColor = Color.White)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().height(95.dp), contentScale = ContentScale.Crop) }
                                        Text(image.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                        Text(image.bucket, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (page > 0) FocusButton("Previous page", "Minimal") { page-- }
                            Text("Page ${page + 1} · ${data.images.size} images", style = MaterialTheme.typography.bodySmall)
                            if (data.hasNext) FocusButton("Next page", "Minimal") { page++ }
                        }
                    }
                    selected?.let { image ->
                        val preview by produceState<android.graphics.Bitmap?>(null, image.uri) { value = library.thumbnail(image.uri, true) }
                        AlertDialog(onDismissRequest = { selected = null }, title = { Text(image.name, maxLines = 1) },
                            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                preview?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Fit) }
                                Text(if (image.bytes > 32L * 1024 * 1024) "This image exceeds the 32 MB import limit." else "Import a private, TV-sized copy. The original image is unchanged.")
                            } },
                            confirmButton = { if (image.bytes <= 32L * 1024 * 1024) FocusButton("Import image", "Minimal") { choose(image.uri) } },
                            dismissButton = { FocusButton("Cancel", "Minimal") { selected = null } })
                    }
                }
            }
        }
    }
}
