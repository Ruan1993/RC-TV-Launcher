package za.co.rc.tv.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Shared by the real IME and its isolated preview; contains keys, never typed text. */
class TvKeyboardView(context: Context, private val onKey: (TvKey) -> Unit) : LinearLayout(context) {
    private var mode = KeyboardMode.LETTERS
    private var shift = false
    private var enter = "Enter"
    private var password = false
    private var rows = KeyboardModel.rows(mode, shift)
    private var cursor = KeyPosition(1, 4)
    private val buttons = mutableListOf<List<Button>>()
    init { orientation = VERTICAL; setPadding(dp(12), dp(6), dp(12), dp(6)); background = surface(false, true); rebuild() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun surface(selected: Boolean, panel: Boolean = false) = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
        if (selected) intArrayOf(0xFFD1F0E8.toInt(), 0xFF97C5BB.toInt()) else if (panel) intArrayOf(0xF5253B47.toInt(), 0xFA101D27.toInt()) else intArrayOf(0xFF273C48.toInt(), 0xFF1A2B36.toInt())).apply {
        cornerRadius = dp(if (panel) 18 else 10).toFloat(); setStroke(dp(1), if (selected) 0xFFF0FFF9.toInt() else 0x557DA99F)
    }
    fun configure(number: Boolean, actionLabel: String, isPassword: Boolean) {
        mode = if (number) KeyboardMode.NUMBERS else KeyboardMode.LETTERS; shift = false; enter = actionLabel; password = isPassword
        cursor = if (number) KeyPosition(0, 0) else KeyPosition(1, 4); rebuild()
    }
    private fun rebuild() {
        val restoreFocus = hasFocus()
        removeAllViews(); buttons.clear(); rows = KeyboardModel.rows(mode, shift, enter)
        addView(TextView(context).apply { text = if (password) "RC TV Keyboard · Password field" else "RC TV Keyboard"; setTextColor(0xFFC7E9E2.toInt()); textSize = 13f; setPadding(dp(4), 0, 0, dp(4)) })
        rows.forEachIndexed { rowIndex, keys ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            val controls = keys.mapIndexed { columnIndex, key ->
                Button(context).apply {
                    text = key.label; contentDescription = if (key.action == KeyAction.DELETE) "Backspace" else key.label
                    isAllCaps = false; textSize = if (key.label.length > 4) 16f else 19f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    background = android.graphics.drawable.StateListDrawable().apply {
                        addState(intArrayOf(android.R.attr.state_selected), surface(true)); addState(intArrayOf(), surface(false))
                    }
                    setTextColor(android.content.res.ColorStateList(arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()), intArrayOf(0xFF0B2025.toInt(), Color.WHITE)))
                    minimumWidth = 0; minWidth = 0; minimumHeight = 0; minHeight = 0; setPadding(dp(3), 0, dp(3), 0)
                    setOnFocusChangeListener { _, focused -> if (focused) select(KeyPosition(rowIndex, columnIndex), false) }
                    setOnClickListener { requestFocusFromTouch(); select(KeyPosition(rowIndex, columnIndex), false); activate() }
                    row.addView(this, LayoutParams(0, dp(42), key.weight).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
                }
            }
            buttons += controls; addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        cursor = KeyPosition(cursor.row.coerceIn(0, rows.lastIndex), cursor.column.coerceIn(0, rows[cursor.row.coerceIn(0, rows.lastIndex)].lastIndex))
        select(cursor, restoreFocus)
    }
    private fun select(position: KeyPosition, focus: Boolean = true) {
        buttons.getOrNull(cursor.row)?.getOrNull(cursor.column)?.isSelected = false
        cursor = position
        buttons[cursor.row][cursor.column].isSelected = true
        if (focus) buttons[cursor.row][cursor.column].requestFocus()
    }
    private fun activate() {
        val key = rows[cursor.row][cursor.column]
        when (key.action) {
            KeyAction.SHIFT -> { shift = !shift; rebuild() }
            KeyAction.MODE -> { mode = KeyboardMode.entries[(mode.ordinal + 1) % KeyboardMode.entries.size]; shift = false; cursor = KeyPosition(4, 0); rebuild() }
            else -> onKey(key)
        }
    }
    fun remoteDown(code: Int): Boolean {
        val position = when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> KeyboardModel.move(rows, cursor, horizontal = -1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> KeyboardModel.move(rows, cursor, horizontal = 1)
            KeyEvent.KEYCODE_DPAD_UP -> KeyboardModel.move(rows, cursor, vertical = -1)
            KeyEvent.KEYCODE_DPAD_DOWN -> KeyboardModel.move(rows, cursor, vertical = 1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> { buttons[cursor.row][cursor.column].isPressed = true; return true }
            else -> return false
        }
        select(position); return true
    }
    fun remoteUp(code: Int, cancelled: Boolean): Boolean {
        if (code in listOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)) {
            buttons[cursor.row][cursor.column].isPressed = false; if (!cancelled) activate(); return true
        }
        return code in listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN)
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        (if (event.action == KeyEvent.ACTION_DOWN) remoteDown(event.keyCode) else if (event.action == KeyEvent.ACTION_UP) remoteUp(event.keyCode, event.isCanceled) else false) || super.dispatchKeyEvent(event)
}
