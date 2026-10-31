package za.co.rc.tv.companion

import android.accessibilityservice.AccessibilityService
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Explicitly enabled by the user. No key interception, gestures, screen capture or event history. */
class RemoteAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { RemoteRuntime.attach(this) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Query only on an authenticated command. */ }
    override fun onInterrupt() {}
    override fun onUnbind(intent: android.content.Intent?): Boolean { RemoteRuntime.detach(this); return super.onUnbind(intent) }
    override fun onDestroy() { RemoteRuntime.detach(this); super.onDestroy() }

    fun command(key: String, text: String): CommandResult {
        if (!RemoteRuntime.optedIn(this)) return CommandResult(false, "System control is off on the TV.")
        if (key == "BACK" || key == "HOME") {
            val action = if (key == "HOME") GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK
            return CommandResult(performGlobalAction(action), "Android could not perform $key. Use the physical remote.")
        }
        val root = rootInActiveWindow ?: return CommandResult(false, "No accessible TV window. Use the physical remote.")
        try {
            val pkg = root.packageName?.toString().orEmpty()
            // Permission/approval interfaces remain under local user control.
            if (RemotePolicy.localOnly(pkg, packageName)) {
                return CommandResult(false, "Use the physical remote for TV settings, permissions and RC TV dialogs.")
            }
            if (key == "INPUT_TEXT") return za.co.rc.tv.keyboard.RcInputMethodService.remoteText(text, pkg)
            val media = mapOf("MEDIA_PLAY_PAUSE" to KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                "MEDIA_NEXT" to KeyEvent.KEYCODE_MEDIA_NEXT, "MEDIA_PREVIOUS" to KeyEvent.KEYCODE_MEDIA_PREVIOUS)[key]
            if (media != null) {
                val audio = getSystemService(AudioManager::class.java)
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, media))
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, media))
                return CommandResult(true, "Media request sent to Android. Playback response cannot be confirmed; the player must support media buttons.")
            }
            if (key == "SEARCH_TEXT") return CommandResult(false, "App search works in RC TV. Use Send text with RC TV Keyboard for other apps.")
            if (Build.VERSION.SDK_INT >= 33) {
                val action = mapOf("UP" to GLOBAL_ACTION_DPAD_UP, "DOWN" to GLOBAL_ACTION_DPAD_DOWN,
                    "LEFT" to GLOBAL_ACTION_DPAD_LEFT, "RIGHT" to GLOBAL_ACTION_DPAD_RIGHT, "OK" to GLOBAL_ACTION_DPAD_CENTER)[key]
                if (action != null) return CommandResult(performGlobalAction(action), "Android could not perform $key in this window.")
            }
            val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: return CommandResult(false, "This app exposes no input focus. Android 11 cannot inject D-pad keys. Use the physical remote.")
            try {
                if (key == "OK") return CommandResult(focus.performAction(AccessibilityNodeInfo.ACTION_CLICK),
                    "The focused control does not support accessibility click. Use the physical remote.")
                val direction = mapOf("UP" to View.FOCUS_UP, "DOWN" to View.FOCUS_DOWN,
                    "LEFT" to View.FOCUS_LEFT, "RIGHT" to View.FOCUS_RIGHT)[key]
                    ?: return CommandResult(false, "Unsupported system control.")
                val next = focus.focusSearch(direction)
                    ?: return CommandResult(false, "No accessible focus target in that direction. Use the physical remote.")
                return try { CommandResult(next.performAction(AccessibilityNodeInfo.ACTION_FOCUS),
                    "The app refused focus movement. Use the physical remote.") } finally { next.recycle() }
            } finally { focus.recycle() }
        } catch (_: Exception) {
            return CommandResult(false, "This control is unavailable in the current app. Use the physical remote.")
        } finally { root.recycle() }
    }
}
