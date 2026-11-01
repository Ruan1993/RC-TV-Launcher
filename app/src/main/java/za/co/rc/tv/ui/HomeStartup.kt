package za.co.rc.tv.ui

import android.view.ViewTreeObserver
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView

/** Standalone previews/fixtures are ready by default; MainActivity supplies the real draw gate. */
internal val LocalHomeContentReady = staticCompositionLocalOf { true }

/** Release optional work after the first app-content draw, never hold back a frame or input. */
@Composable internal fun rememberHomeContentDrawn(contentAvailable: Boolean): Boolean {
    val view = LocalView.current
    var ready by remember(view) { mutableStateOf(false) }
    DisposableEffect(view, contentAvailable) {
        if (!contentAvailable || ready) onDispose {} else {
            val observer = view.viewTreeObserver
            var posted = false
            var disposed = false
            lateinit var listener: ViewTreeObserver.OnDrawListener
            val afterDraw = Runnable {
                if (observer.isAlive) observer.removeOnDrawListener(listener)
                if (!disposed) {
                    android.util.Log.i("RcStartup", "Home content drawn; optional work released uptime=${android.os.SystemClock.uptimeMillis()}")
                    ready = true
                }
            }
            listener = ViewTreeObserver.OnDrawListener {
                if (!posted) { posted = true; view.post(afterDraw) }
            }
            observer.addOnDrawListener(listener)
            onDispose {
                disposed = true
                view.removeCallbacks(afterDraw)
                if (observer.isAlive) observer.removeOnDrawListener(listener)
            }
        }
    }
    return ready
}
