package za.co.rc.tv

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import za.co.rc.tv.core.*

/** Emulator only: fetch real conditions for a named test city, then restore all weather data. */
object WeatherLiveReview {
    fun run(test: Instrumentation, large: Boolean = false) = runBlocking {
        val context = test.targetContext
        val prefs = Preferences(context); val originalPrefs=prefs.state.first()
        val actual = context.getSharedPreferences("weather",Context.MODE_PRIVATE)
        val original = actual.getString("record",null)
        val scratch = context.getSharedPreferences("weather-live-review",Context.MODE_PRIVATE)
        scratch.edit().clear().commit()
        try {
            if(large) { prefs.setTextSize("Extra Large"); prefs.setFontStyle("Bold / High Readability") }
            val weather = WeatherRepository(context,storageName="weather-live-review")
            val place = weather.search("Pretoria").first { it.label.contains("South Africa") }
            weather.select(place); weather.refreshIfDue()
            check(weather.state.value.reading != null) { weather.state.value.message }
            actual.edit().putString("record",scratch.getString("record",null)).commit()
            val activity = test.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(2000)
            val ui = test.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)!!
            test.runOnMainSync { activity.window.decorView.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) }
            var root: android.view.accessibility.AccessibilityNodeInfo? = null
            for(attempt in 0 until 30) { root=ui.rootInActiveWindow; if(root != null) break; Thread.sleep(100) }
            var cards=0
            fun visit(node: android.view.accessibility.AccessibilityNodeInfo) {
                val rect=android.graphics.Rect(); node.getBoundsInScreen(rect)
                if(node.className?.toString() == "android.widget.Button" && rect.width()==432 && rect.height()==380) cards++
                for(index in 0 until node.childCount) node.getChild(index)?.let { visit(it); it.recycle() }
            }
            root?.let { visit(it); it.recycle() }; check(cards>0) { "Weather pushed full-size cards out of view" }
            val screenshot = ui.takeScreenshot() ?: error("No screenshot")
            File(context.getExternalFilesDir(null),"weather-live-review.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }; screenshot.recycle()
            test.runOnMainSync { activity.finish() }
        } finally {
            val edit = actual.edit(); if(original == null) edit.remove("record") else edit.putString("record",original); edit.commit()
            prefs.setTextSize(originalPrefs.textSize); prefs.setFontStyle(originalPrefs.fontStyle)
            scratch.edit().clear().commit()
        }
    }
}
