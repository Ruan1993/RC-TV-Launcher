package za.co.rc.tv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Separate temporary test controls; launcher appearance and launcher preferences are untouched. */
class HomeKeyExperimentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 24) }
        val status = TextView(this).apply { text = "Temporary Home experiment. Default Home and system packages are unchanged.\nLong Home may be intercepted. Disable this service in Android Accessibility to undo." }
        layout.addView(status)
        fun button(label: String, action: () -> Unit) { layout.addView(Button(this).apply { text = label; setOnClickListener { action() } }) }
        fun mode(value: String) {
            runCatching { startService(Intent(this, HomeKeyExperimentService::class.java).putExtra("mode", value)) }
                .onSuccess { status.text = "Requested mode: $value" }
                .onFailure { status.text = "Could not change mode. Use Android Accessibility to turn the service off." }
        }
        button("1. Home-key filter only") { mode("keys") }
        button("Stop experiment (pass through all events)") { mode("off") }
        button("Android Accessibility: enable / disable RC TV Home experiment") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        button("Android Settings") { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        button("Open RC TV") { startActivity(Intent(this, MainActivity::class.java).setAction(HomeKeyExperimentService.ACTION_HOME)) }
        setContentView(layout)
    }
}
