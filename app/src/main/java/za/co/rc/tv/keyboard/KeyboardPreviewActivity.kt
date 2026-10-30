package za.co.rc.tv.keyboard

import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.provider.Settings
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.ComponentActivity

/** Real keyboard core against a private editor, without enabling/changing the system IME. */
class KeyboardPreviewActivity : ComponentActivity() {
    private var editor: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fun dp(n: Int) = (resources.displayMetrics.density * n).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)); setBackgroundColor(0xFF0B1720.toInt()); isSaveEnabled = false }
        root.addView(TextView(this).apply { text = "RC TV Keyboard"; textSize = 22f; setTextColor(Color.WHITE) })
        root.addView(TextView(this).apply { text = "Optional and offline. Enable and choose it in Android settings; keep your existing keyboard as fallback. Typed text is never logged or saved."; textSize = 14f; setTextColor(0xFFC7E9E2.toInt()) })
        val controls = LinearLayout(this)
        fun button(label: String, action: () -> Unit) { controls.addView(Button(this).apply { text = label; isAllCaps = false; setOnClickListener { action() } }, LinearLayout.LayoutParams(0, dp(42), 1f)) }
        button("Enable in Android…") { runCatching { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }.onFailure { Toast.makeText(this, "Keyboard settings unavailable on this firmware.", Toast.LENGTH_LONG).show() } }
        button("Choose keyboard…") { getSystemService(InputMethodManager::class.java).showInputMethodPicker() }
        button("Back to RC TV") { finish() }
        root.addView(controls)
        val field = EditText(this).apply { hint = "Try the RC TV keys here"; setHintTextColor(0xFF829B9F.toInt()); setTextColor(Color.WHITE); textSize = 20f; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; showSoftInputOnFocus = false; isSaveEnabled = false }
        editor = field
        root.addView(field, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        lateinit var keyboard: TvKeyboardView
        fun info() = EditorInfo().apply { inputType = field.inputType; imeOptions = EditorInfo.IME_ACTION_NONE }
        val password = CheckBox(this).apply { text = "Password-field preview"; setTextColor(Color.WHITE); isSaveEnabled = false
            setOnCheckedChangeListener { _, checked ->
                field.inputType = if (checked) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                field.showSoftInputOnFocus = false; keyboard.configure(false, "Enter", checked)
            }
        }
        root.addView(password)
        keyboard = TvKeyboardView(this) { key -> when (key.action) {
            KeyAction.HIDE -> finish()
            KeyAction.SWITCH -> getSystemService(InputMethodManager::class.java).showInputMethodPicker()
            else -> KeyboardInput.send(key, field.onCreateInputConnection(info()), info())
        } }
        root.addView(keyboard)
        setContentView(root)
    }
    override fun onDestroy() { editor?.text?.clear(); editor = null; super.onDestroy() }
}
