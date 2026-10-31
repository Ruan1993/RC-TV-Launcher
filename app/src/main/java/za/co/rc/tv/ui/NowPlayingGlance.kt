package za.co.rc.tv.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Ordinary installs have no media privilege: hide without requesting notification access. */
@Composable fun NowPlayingGlance(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    if (context.checkSelfPermission(Manifest.permission.MEDIA_CONTENT_CONTROL) != PackageManager.PERMISSION_GRANTED) return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var title by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    var artist by remember { mutableStateOf("") }
    DisposableEffect(lifecycle,context) {
        val manager = context.getSystemService(MediaSessionManager::class.java)
        var controller: MediaController? = null
        fun updateMetadata() {
            playing = controller?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
            val current = controller?.metadata
            title = current?.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.toString()?.take(100)?.takeIf { it.isNotBlank() }
                ?: current?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()?.take(100).orEmpty()
            artist = current?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()?.take(100).orEmpty()
        }
        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) { updateMetadata() }
            override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) { updateMetadata() }
            override fun onSessionDestroyed() { title=""; artist="" }
        }
        fun sessions(list: List<MediaController>?) {
            controller?.unregisterCallback(callback)
            controller = list?.firstOrNull { it.metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.isNotBlank() == true || it.metadata?.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.isNotBlank() == true }
            controller?.registerCallback(callback,Handler(Looper.getMainLooper())); updateMetadata()
        }
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { sessions(it) }
        fun stop() { runCatching { manager.removeOnActiveSessionsChangedListener(listener) }; controller?.unregisterCallback(callback); controller=null; title=""; artist="" }
        fun start() { stop(); if (context.checkSelfPermission(Manifest.permission.MEDIA_CONTENT_CONTROL) != PackageManager.PERMISSION_GRANTED) return; runCatching { manager.addOnActiveSessionsChangedListener(listener,null,Handler(Looper.getMainLooper())); sessions(manager.getActiveSessions(null)) }.onFailure { stop() } }
        val observer = LifecycleEventObserver { _, event -> when(event) { Lifecycle.Event.ON_RESUME -> start(); Lifecycle.Event.ON_PAUSE -> stop(); else -> Unit } }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose { lifecycle.removeObserver(observer); stop() }
    }
    if (title.isBlank()) return
    Column(modifier.background(LocalAppearance.current.glass,RoundedCornerShape(12.dp)).padding(horizontal=12.dp,vertical=4.dp)) {
        Text("${if (playing) "Now Playing" else "Media"} · $title",fontSize=15.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        if (artist.isNotBlank()) Text(artist,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
