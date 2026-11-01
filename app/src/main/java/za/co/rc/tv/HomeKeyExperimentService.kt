package za.co.rc.tv

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/** Temporary opt-in SDK experiment; no third-party implementation is incorporated. */
class HomeKeyExperimentService : AccessibilityService() {
    private var homeDownTime: Long? = null
    private val mode get() = getSharedPreferences("home-key-experiment", MODE_PRIVATE).getString("mode", "keys")
    override fun onServiceConnected() { Log.i("RcHomeExperiment", "connected mode=$mode") }
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (mode == "off" || event.keyCode != KeyEvent.KEYCODE_HOME) return false
        Log.i("RcHomeExperiment", "HOME action=${event.action} repeat=${event.repeatCount}")
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) homeDownTime = event.downTime
        if (event.action == KeyEvent.ACTION_UP) {
            val matchingPress = homeDownTime == event.downTime
            homeDownTime = null
            if (matchingPress && !event.isCanceled) {
                val held = event.eventTime - event.downTime
                val longPress = held >= android.view.ViewConfiguration.getLongPressTimeout()
                Log.i("RcHomeExperiment", "release heldMs=$held long=$longPress")
                openHome(if (longPress) "long-key" else "key", longPress)
            }
        }
        return true
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (mode != "fallback" || event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() == "com.google.android.apps.tv.launcherx" &&
            event.className?.toString() == "com.google.android.apps.tv.launcherx.home.HomeActivity") openHome("stock-home-window")
    }
    private fun openHome(reason: String, settings: Boolean = false) {
        runCatching {
            startActivity(Intent(this, MainActivity::class.java).setAction(if (settings) ACTION_SETTINGS else ACTION_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }.onSuccess { Log.i("RcHomeExperiment", "open RC TV reason=$reason") }
            .onFailure { Log.e("RcHomeExperiment", "Home launch failed", it) }
    }
    override fun onInterrupt() = Unit
    companion object {
        const val ACTION_HOME = "za.co.rc.tv.EXPERIMENT_HOME"
        const val ACTION_SETTINGS = "za.co.rc.tv.HOME_KEY_SETTINGS"
    }
}
