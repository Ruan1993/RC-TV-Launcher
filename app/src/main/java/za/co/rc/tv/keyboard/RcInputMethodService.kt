package za.co.rc.tv.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

/** Optional system-bound IME. Only Android can bind it; no typed content leaves the connection. */
class RcInputMethodService : InputMethodService() {
    companion object {
        private var active: RcInputMethodService? = null
        fun remoteText(text: String, expectedPackage: String): za.co.rc.tv.companion.CommandResult {
            val service = active ?: return za.co.rc.tv.companion.CommandResult(false, "Choose RC TV Keyboard and focus a text field first.")
            val info = service.currentInputEditorInfo
            val connection = service.currentInputConnection
            if (info == null || info.packageName != expectedPackage || connection == null ||
                info.inputType == 0 || KeyboardInput.isPassword(info) || text.isEmpty() || text.length > 160) {
                return za.co.rc.tv.companion.CommandResult(false, "Select a normal TV text field and choose RC TV Keyboard. Password fields are not supported.")
            }
            return za.co.rc.tv.companion.CommandResult(connection.commitText(text, 1), "The app did not accept text. Use its keyboard or physical remote.")
        }
    }
    private var keyboard: TvKeyboardView? = null
    private var selectionStart = -1
    private var selectionEnd = -1
    override fun onStartInput(info: EditorInfo, restarting: Boolean) {
        super.onStartInput(info, restarting)
        active = this
    }
    override fun onCreateInputView(): View = TvKeyboardView(this) { key ->
        when (key.action) {
            KeyAction.HIDE -> requestHideSelf(0)
            KeyAction.SWITCH -> getSystemService(InputMethodManager::class.java).showInputMethodPicker()
            else -> currentInputEditorInfo?.let { KeyboardInput.send(key, currentInputConnection, it, selectionStart, selectionEnd) }
        }
    }.also { keyboard = it }
    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        active = this
        selectionStart = info.initialSelStart; selectionEnd = info.initialSelEnd
        keyboard?.configure(KeyboardInput.isNumber(info), KeyboardInput.enterLabel(info), KeyboardInput.isPassword(info))
    }
    override fun onEvaluateFullscreenMode() = false
    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        // A TV remote counts as hardware input; still offer the optional on-screen keys.
        return true
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        (isInputViewShown && keyboard?.remoteDown(keyCode) == true) || super.onKeyDown(keyCode, event)
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        (isInputViewShown && keyboard?.remoteUp(keyCode, event.isCanceled) == true) || super.onKeyUp(keyCode, event)
    override fun onFinishInput() {
        if (active === this) active = null
        selectionStart = -1; selectionEnd = -1
        keyboard?.configure(false, "Enter", false)
        super.onFinishInput()
    }
    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        selectionStart = newSelStart; selectionEnd = newSelEnd
    }
    override fun onDestroy() { if (active === this) active = null; keyboard = null; super.onDestroy() }
}
