package za.co.rc.tv

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/** Temporary opt-in SDK experiment; no third-party implementation is incorporated. */
class HomeKeyExperimentService : AccessibilityService() {
    private var homeDownTime: Long? = null
    private var startupRedirectAttempted = false
    private val mode get() = getSharedPreferences("home-key-experiment", MODE_PRIVATE).getString("mode", "keys")
    override fun onServiceConnected() { Log.i("RcHomeExperiment", "connected mode=$mode") }
    // The service process owns this preference. Foreground test controls send explicit commands,
    // rather than relying on unsupported cross-process SharedPreferences cache invalidation.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val requested = intent?.getStringExtra("mode")
        if (requested == "keys" || requested == "off") {
            val saved = getSharedPreferences("home-key-experiment", MODE_PRIVATE).edit().putString("mode", requested).commit()
            if (saved) homeDownTime = null
            Log.i("RcHomeExperiment", "control mode=$requested saved=$saved")
        }
        stopSelf(startId) // Accessibility binding keeps the service alive; no started background service.
        return START_NOT_STICKY
    }
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
        if (mode == "off" || startupRedirectAttempted ||
            event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        // Exact activity match excludes Apps, Dashboard, Settings, setup and recovery.
        if (event.packageName?.toString() != "com.google.android.apps.tv.launcherx" ||
            event.className?.toString() != "com.google.android.apps.tv.launcherx.home.HomeActivity") return
        // One attempt per service connection/lifetime: repeated window events cannot form a loop.
        // Set before launching so synchronous/re-entrant events cannot schedule another launch.
        startupRedirectAttempted = true
        openHome("startup-stock-home")
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
