package za.co.rc.tv

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import java.io.File
import kotlinx.coroutines.runBlocking
import za.co.rc.tv.core.*

/** Emulator only: fetch real conditions for a named test city, then restore all weather data. */
object WeatherLiveReview {
    fun run(test: Instrumentation) = runBlocking {
        val context = test.targetContext
        val actual = context.getSharedPreferences("weather",Context.MODE_PRIVATE)
        val original = actual.getString("record",null)
        val scratch = context.getSharedPreferences("weather-live-review",Context.MODE_PRIVATE)
        scratch.edit().clear().commit()
        try {
            val weather = WeatherRepository(context,storageName="weather-live-review")
            val place = weather.search("Pretoria").first { it.label.contains("South Africa") }
            weather.select(place); weather.refreshIfDue()
            check(weather.state.value.reading != null) { weather.state.value.message }
            actual.edit().putString("record",scratch.getString("record",null)).commit()
            val activity = test.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(2000)
            val ui = test.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)!!
            val screenshot = ui.takeScreenshot() ?: error("No screenshot")
            File(context.getExternalFilesDir(null),"weather-live-review.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }; screenshot.recycle()
            test.runOnMainSync { activity.finish() }
        } finally {
            val edit = actual.edit(); if(original == null) edit.remove("record") else edit.putString("record",original); edit.commit()
            scratch.edit().clear().commit()
        }
    }
}
