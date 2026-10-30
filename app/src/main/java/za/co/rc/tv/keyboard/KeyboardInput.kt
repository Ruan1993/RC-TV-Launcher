package za.co.rc.tv.keyboard

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

object KeyboardInput {
    fun isNumber(info: EditorInfo) = (info.inputType and InputType.TYPE_MASK_CLASS) in listOf(InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME)
    fun isPassword(info: EditorInfo): Boolean {
        val type = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        return (type == InputType.TYPE_CLASS_TEXT && variation in listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (type == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }
    fun enterLabel(info: EditorInfo) = when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
        EditorInfo.IME_ACTION_SEARCH -> "Search"; EditorInfo.IME_ACTION_NEXT -> "Next"; EditorInfo.IME_ACTION_GO -> "Go"
        EditorInfo.IME_ACTION_DONE -> "Done"; EditorInfo.IME_ACTION_SEND -> "Send"; else -> "Enter"
    }
    /** No composing buffer, surrounding-text read, suggestions, clipboard, logging or persistence. */
    fun send(key: TvKey, connection: InputConnection?, info: EditorInfo) {
        if (connection == null) return
        when (key.action) {
            KeyAction.TEXT -> connection.commitText(key.text, 1)
            KeyAction.DELETE -> if (!connection.deleteSurroundingTextInCodePoints(1, 0)) connection.deleteSurroundingText(1, 0)
            KeyAction.ENTER -> {
                val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
                if (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0 || action in listOf(EditorInfo.IME_ACTION_NONE, EditorInfo.IME_ACTION_UNSPECIFIED)) connection.commitText("\n", 1)
                else if (!connection.performEditorAction(action)) {
                    connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
            }
            else -> Unit
        }
    }
}
