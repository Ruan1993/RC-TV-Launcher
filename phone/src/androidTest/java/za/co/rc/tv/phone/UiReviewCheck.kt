package za.co.rc.tv.phone

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import org.json.JSONObject
import java.io.File

/** Emulator visual fixture only. Never pairs or sends a remote command. */
object UiReviewCheck {
    fun run(test: Instrumentation, args: Bundle) {
        val activity = test.startActivitySync(Intent(test.targetContext, PhoneActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Thread.sleep(1200)
        val screen = args.getString("screen") ?: "disconnected"
        fun invoke(name: String, vararg values: Any) {
            val method = PhoneActivity::class.java.declaredMethods.first { it.name == name && it.parameterTypes.size == values.size }
            method.isAccessible = true; method.invoke(activity, *values)
        }
        test.runOnMainSync {
            if (screen in listOf("remote", "typing", "reconnecting")) invoke("remote", JSONObject().put("systemControl", true).put("accessibilityConnected", true))
            if (screen == "pairing") {
                val field = PhoneActivity::class.java.getDeclaredField("generation").apply { isAccessible = true }
                invoke("pair", LinkClient("192.168.1.39", 1), field.getInt(activity))
            }
            if (screen == "typing") {
                val open = PhoneActivity::class.java.declaredMethods.firstOrNull { it.name == "showTyping" }
                if (open != null) { open.isAccessible = true; open.invoke(activity) }
                else {
                    fun find(v: View): EditText? = if (v is EditText && v.hint?.toString()?.startsWith("Text for TV") == true) v else if (v is ViewGroup) (0 until v.childCount).firstNotNullOfOrNull { find(v.getChildAt(it)) } else null
                    find(activity.window.decorView)?.let { it.requestFocus(); activity.getSystemService(InputMethodManager::class.java).showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) }
                }
            }
            if (screen == "reconnecting") {
                PhoneActivity::class.java.getDeclaredField("connected").apply { isAccessible = true }.setBoolean(activity, false)
                invoke("message", "Reconnecting… Keep your TV on.")
            }
        }
        Thread.sleep(3000)
        test.runOnMainSync {
            fun views(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
            if (screen == "remote") {
                val all = views(activity.window.decorView)
                for (label in listOf("Up", "Down", "Left", "Right", "OK, select", "Back", "Home", "Keyboard")) {
                    val control = all.first { it.contentDescription?.toString() == label }
                    check(control.width >= 48 * activity.resources.displayMetrics.density && control.height >= 48 * activity.resources.displayMetrics.density) { "Small touch target: $label" }
                    val visible = android.graphics.Rect()
                    check(control.getGlobalVisibleRect(visible) && visible.height() >= control.height - 1) { "Clipped main control: $label" }
                }
            }
            if (screen == "typing") {
                val dialog = PhoneActivity::class.java.getDeclaredField("typingDialog").apply { isAccessible = true }.get(activity) as? android.app.Dialog
                if (dialog != null) {
                    val all = views(dialog.window!!.decorView)
                    val editor = all.filterIsInstance<EditText>().first()
                    check(!editor.isSaveEnabled)
                    check(editor.imeOptions and android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0)
                    val send = all.first { it.contentDescription?.toString() == "Send to TV" }
                    val visible = android.graphics.Rect(); check(send.getGlobalVisibleRect(visible) && visible.height() >= send.height - 1) { "Send button hidden by phone keyboard" }
                }
            }
        }
        val bitmap = test.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)?.takeScreenshot() ?: error("Screenshot unavailable")
        val file = File(test.targetContext.getExternalFilesDir(null), "ui-review.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        test.finish(Activity.RESULT_OK, Bundle().apply { putString("review", "Emulator UI fixture: $screen; no live pairing or control") })
    }
}
