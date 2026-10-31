package za.co.rc.tv

import android.app.Instrumentation
import android.content.Context
import kotlinx.coroutines.runBlocking
import za.co.rc.tv.core.*

/** Dedicated isolated preferences. Simulated weather is never written to the product's weather store. */
object WeatherReviewCheck {
    fun run(test: Instrumentation) = runBlocking {
        val context = test.targetContext
        val name = "weather-isolated-review"
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        var time = 1_790_000_000_000L; var requests = 0; var offline = false
        val transport: (String) -> String = {
            requests++; if (offline) throw java.io.IOException("offline fixture")
            """{"current_units":{"temperature_2m":"\u00b0C"},"current":{"temperature_2m":22.0,"weather_code":2,"time":${time / 1000}}}"""
        }
        try {
            val weather = WeatherRepository(context,{ time },transport,name)
            check(weather.state.value.place == null); weather.refreshIfDue(); check(requests == 0)
            weather.select(WeatherPlace("Isolated test fixture",-25.7,28.2)); weather.refreshIfDue()
            check(requests == 1 && weather.state.value.reading?.celsius == 22.0)
            weather.refreshIfDue(); check(requests == 1)
            val recreated = WeatherRepository(context,{ time },transport,name)
            recreated.refreshIfDue(); check(requests == 1 && recreated.state.value.reading != null)
            time += WeatherPolicy.HOUR; offline = true; recreated.refreshIfDue()
            check(requests == 2 && recreated.state.value.message.contains("cached") && recreated.state.value.reading != null)
            recreated.refreshIfDue(); check(requests == 2)
            time += WeatherPolicy.DAY
            check(!WeatherPolicy.usable(time,recreated.state.value.reading!!.observed))
            val media = context.getSystemService(android.media.session.MediaSessionManager::class.java)
            check(context.checkSelfPermission(android.Manifest.permission.MEDIA_CONTENT_CONTROL) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            check(runCatching { media.getActiveSessions(null) }.exceptionOrNull() is SecurityException)
            recreated.disable(); check(recreated.state.value.place == null && recreated.state.value.reading == null)
            recreated.refreshIfDue(); check(requests == 2)
        } finally { context.getSharedPreferences(name,Context.MODE_PRIVATE).edit().clear().commit() }
    }
}
