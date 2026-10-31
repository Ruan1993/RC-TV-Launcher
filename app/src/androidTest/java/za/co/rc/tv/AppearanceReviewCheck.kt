package za.co.rc.tv

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import za.co.rc.tv.core.*
import java.io.File

/** Emulator-only review via actual saved preferences; never used on the user's Xiaomi. */
object AppearanceReviewCheck {
    fun run(test: Instrumentation, args: Bundle) {
        val prefs = Preferences(test.targetContext)
        val before = runBlocking { prefs.state.first() }
        try {
            runBlocking {
                prefs.setAccent(accentThemes[args.getString("accent", "0").toInt()])
                prefs.setFontStyle(fontStyles[args.getString("font", "0").toInt()])
                prefs.setTextSize(textSizes[args.getString("size", "0").toInt()])
            }
            val activity = test.startActivitySync(Intent(test.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(1800)
            val ui = test.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)!!
            test.runOnMainSync { activity.window.decorView.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) }
            var candidate: AccessibilityNodeInfo? = null
            for (attempt in 0 until 30) { candidate = ui.rootInActiveWindow; if (candidate != null) break; Thread.sleep(100) }
            val root = candidate ?: error("No review window")
            var cards = 0
            val density = activity.resources.displayMetrics.density
            fun visit(node: AccessibilityNodeInfo) {
                val rect = Rect(); node.getBoundsInScreen(rect)
                if (node.className?.toString() == "android.widget.Button" && rect.height() == (190 * density).toInt() && rect.width() == (216 * density).toInt()) cards++
                for (i in 0 until node.childCount) node.getChild(i)?.let { visit(it); it.recycle() }
            }
            try { visit(root) } finally { root.recycle() }
            check(cards > 0) { "Unchanged full-size card was not fully visible" }
            val bitmap = ui.takeScreenshot() ?: error("No screenshot")
            File(test.targetContext.getExternalFilesDir(null), "appearance-review.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
            val saved = runBlocking { prefs.state.first() }
            check(saved.accent == accentThemes[args.getString("accent", "0").toInt()])
            test.runOnMainSync { activity.finish() }
        } finally {
            runBlocking { prefs.setAccent(before.accent); prefs.setFontStyle(before.fontStyle); prefs.setTextSize(before.textSize) }
        }
    }
}
