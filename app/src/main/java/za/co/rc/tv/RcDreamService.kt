package za.co.rc.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.dreams.DreamService
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import za.co.rc.tv.core.LauncherPreferences
import za.co.rc.tv.core.Preferences
import za.co.rc.tv.ui.LauncherAppearance
import za.co.rc.tv.ui.ScreensaverContent

/** Android owns scheduling and dismissal; no boot receiver, polling or extra process. */
class RcDreamService : DreamService(), SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry get() = savedState.savedStateRegistry
    override val viewModelStore = ViewModelStore()
    private var view: ComposeView? = null
    private var clock by mutableStateOf("")
    private fun updateClock() { clock = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()) }
    private val timeChanges = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = updateClock()
    }
    override fun onCreate() {
        super.onCreate()
        savedState.performAttach()
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
        updateClock()
        ContextCompat.registerReceiver(this, timeChanges, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false // Android dismisses on user input, including Back and D-pad.
        isFullscreen = true
        isScreenBright = false
        view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@RcDreamService)
            setViewTreeSavedStateRegistryOwner(this@RcDreamService)
            setViewTreeViewModelStoreOwner(this@RcDreamService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val prefs by remember { Preferences(applicationContext).state }.collectAsState(LauncherPreferences())
                LauncherAppearance(prefs) { ScreensaverContent(prefs, clock) { finish() } }
            }
        }
        setContentView(view!!)
        registry.currentState = Lifecycle.State.STARTED
    }
    override fun onDreamingStarted() { super.onDreamingStarted(); registry.currentState = Lifecycle.State.RESUMED }
    override fun onDreamingStopped() { registry.currentState = Lifecycle.State.CREATED; super.onDreamingStopped() }
    override fun onDetachedFromWindow() { view?.disposeComposition(); view = null; super.onDetachedFromWindow() }
    override fun onDestroy() {
        unregisterReceiver(timeChanges)
        registry.currentState = Lifecycle.State.DESTROYED
        view?.disposeComposition()
        viewModelStore.clear()
        super.onDestroy()
    }
}
