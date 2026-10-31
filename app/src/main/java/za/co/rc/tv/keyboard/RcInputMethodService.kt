package za.co.rc.tv.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

/** Optional system-bound IME. Only Android can bind it; no typed content leaves the connection. */
class RcInputMethodService : InputMethodService() {
    private var keyboard: TvKeyboardView? = null
    override fun onCreateInputView(): View = TvKeyboardView(this) { key ->
        when (key.action) {
            KeyAction.HIDE -> requestHideSelf(0)
            KeyAction.SWITCH -> getSystemService(InputMethodManager::class.java).showInputMethodPicker()
            else -> currentInputEditorInfo?.let { KeyboardInput.send(key, currentInputConnection, it) }
        }
    }.also { keyboard = it }
    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
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
        keyboard?.configure(false, "Enter", false)
        super.onFinishInput()
    }
    override fun onDestroy() { keyboard = null; super.onDestroy() }
}
