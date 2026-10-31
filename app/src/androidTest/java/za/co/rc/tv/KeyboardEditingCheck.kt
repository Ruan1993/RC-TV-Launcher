package za.co.rc.tv

import android.app.Instrumentation
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import za.co.rc.tv.keyboard.*

/** Only synthetic text in a real native editor; never enables an IME or touches preferences. */
object KeyboardEditingCheck {
    fun run(test: Instrumentation) {
        val activity = test.startActivitySync(Intent(test.targetContext, KeyboardPreviewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        fun find(view: View): EditText? = if (view is EditText) view else if (view is ViewGroup) (0 until view.childCount).firstNotNullOfOrNull { find(view.getChildAt(it)) } else null
        lateinit var field: EditText
        test.runOnMainSync { field = find(activity.window.decorView)!!; field.requestFocusFromTouch() }
        fun send(action: KeyAction, text: String = "") {
            test.runOnMainSync { val info = EditorInfo(); val connection = field.onCreateInputConnection(info); KeyboardInput.send(TvKey(text, action = action), connection, info) }
            test.waitForIdleSync()
        }
        fun checkText(value: String) { var actual = ""; test.runOnMainSync { actual = field.text.toString() }; check(actual == value) { "Synthetic editor result mismatch" } }
        try {
            test.runOnMainSync {
                field.setText("ab😀"); field.setSelection(field.length())
                val connection = field.onCreateInputConnection(EditorInfo())!!
                check(KeyboardInput.deleteForRemote(connection, field.selectionStart, field.selectionEnd))
                check(field.text.toString() == "ab")
                field.setSelection(0, 2)
                check(KeyboardInput.deleteForRemote(connection, 0, 2)); check(field.text.isEmpty())
                check(!KeyboardInput.deleteForRemote(connection, -1, -1))
            }
            test.runOnMainSync { field.setText("ab😀cd"); field.setSelection(field.length()) }
            send(KeyAction.CURSOR_LEFT); send(KeyAction.TEXT, "X"); checkText("ab😀cXd")
            send(KeyAction.DELETE); checkText("ab😀cd")
            test.runOnMainSync { field.setSelection(1, 4) }
            send(KeyAction.DELETE); checkText("acd")
            test.runOnMainSync { field.setText(""); field.setSelection(0) }
            send(KeyAction.CURSOR_LEFT); send(KeyAction.CURSOR_RIGHT); send(KeyAction.DELETE); checkText("")
            test.runOnMainSync { field.setText("abcd"); field.setSelection(4) }
            send(KeyAction.CURSOR_LEFT); send(KeyAction.CURSOR_LEFT); send(KeyAction.TEXT, "X"); checkText("abXcd")
            test.runOnMainSync {
                var moves = 0
                val keys = TvKeyboardView(activity) { if (it.action == KeyAction.CURSOR_LEFT) moves++ }
                val left = KeyboardModel.rows(KeyboardMode.LETTERS, false).last().indexOfFirst { it.action == KeyAction.CURSOR_LEFT }
                repeat(3) { keys.remoteDown(KeyEvent.KEYCODE_DPAD_DOWN) }
                // Move to the known cursor-left key from whichever weighted column was reached.
                val row = keys.getChildAt(5) as ViewGroup
                row.getChildAt(left).requestFocusFromTouch(); row.getChildAt(left).performClick(); moves = 0
                repeat(3) { keys.remoteDown(KeyEvent.KEYCODE_DPAD_CENTER) }
                keys.remoteUp(KeyEvent.KEYCODE_DPAD_CENTER, false)
                check(moves == 3)
            }
        } finally { test.runOnMainSync { field.text.clear(); activity.finish() } }
    }
}
