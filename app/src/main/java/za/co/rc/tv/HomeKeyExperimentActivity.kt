package za.co.rc.tv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Existing Home-button controls; class name stays stable for Android's service settings link. */
class HomeKeyExperimentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 24) }
        val status = TextView(this).apply { text = "RC TV Home buttons\nShort Home → RC TV Home. Long Home → RC TV Settings.\nRequires RC TV Home buttons to be enabled in Android Accessibility. These controls do not change default Home or system packages." }
        layout.addView(status)
        fun button(label: String, action: () -> Unit) { layout.addView(Button(this).apply { text = label; setOnClickListener { action() } }) }
        fun mode(value: String) {
            runCatching { startService(Intent(this, HomeKeyExperimentService::class.java).putExtra("mode", value)) }
                .onSuccess { status.text = if (value == "keys")
                    "RC TV Home-button control requested. Enable RC TV Home buttons in Android Accessibility if needed."
                    else "Android’s normal Home-button handling requested. The default launcher is unchanged." }
                .onFailure { status.text = "Could not change mode. Use Android Accessibility to turn the service off." }
        }
        button("Use RC TV Home buttons") { mode("keys") }
        button("Use Android’s normal Home buttons") { mode("off") }
        button("Android Accessibility: RC TV Home buttons") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        button("Android Settings") { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        button("Open RC TV") { startActivity(Intent(this, MainActivity::class.java).setAction(HomeKeyExperimentService.ACTION_HOME)) }
        setContentView(layout)
    }
}
