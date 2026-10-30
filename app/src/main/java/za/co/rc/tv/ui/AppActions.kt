package za.co.rc.tv.ui

import android.view.KeyEvent as AndroidKey
import android.view.ViewConfiguration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import za.co.rc.tv.core.*

private fun isSelect(code: Int) = code == AndroidKey.KEYCODE_DPAD_CENTER ||
    code == AndroidKey.KEYCODE_ENTER || code == AndroidKey.KEYCODE_NUMPAD_ENTER

private fun Modifier.remotePress(launchApp: () -> Unit, openMenu: (Long?) -> Unit): Modifier = composed {
    val press = remember { RemotePress() }
    val scope = rememberCoroutineScope()
    val launch by rememberUpdatedState(launchApp)
    val menu by rememberUpdatedState(openMenu)
    var timer by remember { mutableStateOf<Job?>(null) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) { timer?.cancel(); press.cancel() }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); timer?.cancel(); press.cancel() }
    }
    this.onFocusChanged { if (!it.isFocused) { timer?.cancel(); press.cancel() } }
        .onPreviewKeyEvent { event ->
            val key = event.nativeKeyEvent
            if (!isSelect(key.keyCode)) false else {
                if (BuildConfigDiagnostics.enabled) android.util.Log.i("RcAppPress",
                    "code=${key.keyCode} action=${key.action} repeat=${key.repeatCount} cancelled=${key.isCanceled}")
                when (key.action) {
                    AndroidKey.ACTION_DOWN -> if (press.down(key.repeatCount)) {
                        val pressTime = key.downTime
                        timer = scope.launch {
                            delay(ViewConfiguration.getLongPressTimeout().toLong())
                            if (press.hold()) menu(pressTime)
                        }
                    }
                    AndroidKey.ACTION_UP -> {
                        timer?.cancel()
                        if (!key.isCanceled && key.eventTime - key.downTime >= ViewConfiguration.getLongPressTimeout() && press.hold()) menu(null)
                        if (press.up(key.isCanceled)) launch()
                    }
                }
                true
            }
        }.semantics { onLongClick("Application options") { menu(null); true } }
}

private object BuildConfigDiagnostics { val enabled = za.co.rc.tv.BuildConfig.DEBUG || za.co.rc.tv.BuildConfig.PROFILE_DIAGNOSTICS }

/** Popup position is beside the actual lazy-list tile and clamps to the TV viewport. */
private object BesideTile : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
        layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val right = anchorBounds.right + 16
        val x = if (right + popupContentSize.width <= windowSize.width - 16) right
            else anchorBounds.left - popupContentSize.width - 16
        return IntOffset(x.coerceIn(16, (windowSize.width - popupContentSize.width - 16).coerceAtLeast(16)),
            anchorBounds.center.y.minus(popupContentSize.height / 2)
                .coerceIn(16, (windowSize.height - popupContentSize.height - 16).coerceAtLeast(16)))
    }
}

@Composable
internal fun AppActionHost(app: LauncherApp, repository: AppRepository, prefs: LauncherPreferences,
    apps: List<LauncherApp>, motion: String, home: Boolean, onLaunch: () -> Unit, onPin: () -> Unit,
    onRearrange: (LauncherApp) -> Unit, content: @Composable (Modifier) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var openingPressTime by remember { mutableStateOf<Long?>(null) }
    var restore by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    val first = remember { FocusRequester() }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) { menu = false; restore = false }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val focus = LocalFocusManager.current
    val pinned = app.packageName in prefs.favourites
    val dismiss = { menu = false; restore = true }
    val inputMode = LocalInputModeManager.current
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(restore, windowInfo.isWindowFocused) {
        if (restore && windowInfo.isWindowFocused) {
            // Android Dialog/Popup teardown can finish after its Compose content is removed.
            delay(120)
            inputMode.requestInputMode(InputMode.Keyboard)
            runCatching { requester.requestFocus() }.onFailure { android.util.Log.w("RcAppPress", "Focus restore unavailable", it) }
            restore = false
        }
    }
    Box {
        content(Modifier.focusRequester(requester).remotePress(onLaunch, { pressTime -> openingPressTime = pressTime; menu = true }))
        if (menu) Popup(BesideTile, onDismissRequest = dismiss, properties = PopupProperties(focusable = true)) {
            LaunchedEffect(Unit) { first.requestFocus() }
            GlassActionSurface(motion, Modifier.width(290.dp).onPreviewKeyEvent { event ->
                val key = event.nativeKeyEvent
                if (openingPressTime == key.downTime && isSelect(key.keyCode)) {
                    if (key.action == AndroidKey.ACTION_UP) openingPressTime = null
                    true
                } else {
                    openingPressTime = null
                    false
                }
            }) {
                Text(app.label, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                FocusButton(if (pinned) "Remove from Favourites" else "Add to Favourites", motion,
                    Modifier.fillMaxWidth().focusRequester(first)) {
                    if (home && pinned) focus.moveFocus(FocusDirection.Right)
                    dismiss(); onPin()
                }
                if (pinned && prefs.favourites.size > 1) FocusButton("Rearrange favourites", motion, Modifier.fillMaxWidth()) {
                    menu = false; onRearrange(app)
                }
                val context = androidx.compose.ui.platform.LocalContext.current
                FocusButton("App information", motion, Modifier.fillMaxWidth()) {
                    dismiss(); if (!repository.appInformation(app)) android.widget.Toast.makeText(context, "App information unavailable.", android.widget.Toast.LENGTH_LONG).show()
                }
                if (remember(app.packageName) { repository.canUninstall(app) }) FocusButton("Uninstall…", motion, Modifier.fillMaxWidth()) {
                    dismiss(); if (!repository.requestUninstall(app)) android.widget.Toast.makeText(context, "Uninstall unavailable.", android.widget.Toast.LENGTH_LONG).show()
                }
                FocusButton("Close", motion, Modifier.fillMaxWidth(), onClick = dismiss)
            }
        }
    }
}

@Composable
private fun GlassActionSurface(motion: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(if (entered || motion == "Minimal") 1f else .97f,
        if (motion == "Minimal") snap() else spring(dampingRatio = if (motion == "Cinematic") .82f else 1f, stiffness = 650f), label = "actionPanel")
    val shape = RoundedCornerShape(22.dp)
    Column(modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        .background(Brush.verticalGradient(listOf(Color(0xF52B414B), Color(0xFA101D28))), shape)
        .border(1.dp, Color(0x99C7E9E2), shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

internal fun orderedFavouritePackages(prefs: LauncherPreferences, apps: List<LauncherApp>): List<String> =
    (prefs.favouriteOrder + apps.map { it.packageName } + prefs.favourites.sorted())
        .filter { it in prefs.favourites }.distinct()
