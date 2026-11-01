package za.co.rc.tv.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import za.co.rc.tv.core.*

@Composable internal fun rememberContinueWatching(repository: WatchNextRepository, enabled: Boolean): List<ContinueItem> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val contentReady = LocalHomeContentReady.current
    var entries by remember(repository) { mutableStateOf(emptyList<ContinueItem>()) }
    LaunchedEffect(repository, enabled, contentReady, lifecycle) {
        if (!enabled || !contentReady) { entries=emptyList(); return@LaunchedEffect }
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!repository.permitted()) { entries=emptyList(); return@repeatOnLifecycle }
            try {
                repository.changes().conflate().collectLatest {
                    entries=repository.entries()
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
              catch (_: Exception) { entries=emptyList() }
        }
    }
    return if (enabled && contentReady && repository.permitted()) entries else emptyList()
}

@Composable internal fun ContinueWatchingSettings(enabled: Boolean, motion: String) {
    val context = LocalContext.current
    val preferences = remember(context) { Preferences(context) }
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var permitted by remember { mutableStateOf(context.checkSelfPermission(TV_LISTINGS_PERMISSION)==android.content.pm.PackageManager.PERMISSION_GRANTED) }
    var message by remember { mutableStateOf("") }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permitted=granted
        message=if(granted) "Compatible apps can now share their Continue Watching entries." else "Permission was not granted. The row stays hidden; you can review TV-listings permission in RC TV’s Android app settings."
    }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            permitted=context.checkSelfPermission(TV_LISTINGS_PERMISSION)==android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    Text("Continue Watching",style=MaterialTheme.typography.titleMedium,color=LocalAppearance.current.accent)
    FocusButton("Continue Watching: ${if(enabled) "On" else "Off"}",motion,selected=enabled) {
        scope.launch { runCatching { preferences.setContinueWatching(!enabled) }.onFailure { message="Could not save this setting." } }
    }
    Text("Shows unfinished content shared by compatible apps. Hidden when no eligible content is available. Playback progress appears only when supplied by the app.")
    if (!permitted) FocusButton("Allow TV-listings access…",motion) { request.launch(TV_LISTINGS_PERMISSION) }
    else Text("Android TV-listings permission is granted.",style=MaterialTheme.typography.bodySmall)
    FocusButton("RC TV app permissions",motion) {
        runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,"package:${context.packageName}".toUri())) }
            .onFailure { message="Open Android Settings → Apps → RC TV Launcher → Permissions." }
    }
    if(message.isNotEmpty()) Text(message,style=MaterialTheme.typography.bodySmall)
}

@Composable internal fun ContinueWatchingRow(entries: List<ContinueItem>, source: WatchNextRepository, apps: AppRepository, iconGeneration: Int,
    motion: String, focus: FocusRequester, favourites: FocusRequester) {
    val context=LocalContext.current
    val palette=LocalAppearance.current
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("Continue Watching",style=MaterialTheme.typography.titleMedium,color=palette.accent)
        LazyRow(Modifier.fillMaxWidth().focusRequester(focus).focusGroup(),horizontalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(4.dp)) {
            items(entries,key={ "${it.packageName}:${it.id}" }) { item ->
                val artwork by produceState<android.graphics.Bitmap?>(null,item.artworkUri) { value=source.artwork(item.artworkUri) }
                val icon by produceState<android.graphics.Bitmap?>(null,item.packageName,iconGeneration) { value=apps.icon(item.packageName) }
                val appLabel=remember(item.packageName) { source.appLabel(item) }
                var focused by remember { mutableStateOf(false) }
                val shape=RoundedCornerShape(18.dp)
                Button(onClick={ if(!source.launch(item)) Toast.makeText(context,"This content is unavailable. Open $appLabel from Apps.",Toast.LENGTH_LONG).show() },
                    modifier=Modifier.width(220.dp).onFocusChanged { focused=it.isFocused }
                        .focusProperties { up=favourites }.cinematicFocus(18.dp)
                        .border(if(focused) 2.dp else 1.dp,if(focused) palette.focus else palette.border,shape),
                    shape=shape,colors=ButtonDefaults.buttonColors(containerColor=palette.glass,contentColor=Color.White),
                    contentPadding=PaddingValues(10.dp)) {
                    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.fillMaxWidth().height(92.dp).background(Color.Black.copy(alpha=.25f),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center) {
                            val image=artwork ?: icon
                            if(image != null) Image(image.asImageBitmap(),null,if(artwork != null) Modifier.fillMaxSize() else Modifier.size(48.dp),contentScale=if(artwork != null) ContentScale.Crop else ContentScale.Fit)
                        }
                        Text(item.title,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(appLabel,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                        item.progress?.let { progress -> LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth()) }
                    }
                }
            }
        }
    }
}
