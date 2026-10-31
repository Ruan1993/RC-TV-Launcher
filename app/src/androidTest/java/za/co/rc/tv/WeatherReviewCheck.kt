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
            val legacyStorage=context.getSharedPreferences(name,Context.MODE_PRIVATE)
            val legacyRecord=org.json.JSONObject(legacyStorage.getString("record",null)!!).apply { remove("enabled") }
            legacyStorage.edit().putString("record",legacyRecord.toString()).commit()
            check(WeatherRepository(context,{ time },transport,name).state.value.enabled)
            weather.setEnabled(false); check(!weather.state.value.enabled && weather.state.value.place!=null)
            val offReload=WeatherRepository(context,{ time },transport,name); check(!offReload.state.value.enabled)
            offReload.refreshIfDue(); check(requests==1)
            weather.setEnabled(true)
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
            val legacy=org.json.JSONObject("""{"current_units":{"temperature_2m":"\u00b0C"},"current":{"temperature_2m":22,"weather_code":2,"time":${time/1000}}}""")
            check(weather.parseForecast(legacy).feelsLike==null)
            val expanded=org.json.JSONObject(legacy.toString())
            expanded.getJSONObject("current_units").put("apparent_temperature","\u00B0C").put("relative_humidity_2m","%").put("wind_speed_10m","km/h")
            expanded.getJSONObject("current").put("apparent_temperature",24.5).put("relative_humidity_2m",60).put("wind_speed_10m",12.4)
            val details=weather.parseForecast(expanded); check(details.feelsLike==24.5 && details.humidity==60.0 && details.windKmh==12.4)
            expanded.getJSONObject("current_units").put("wind_speed_10m","mph"); expanded.getJSONObject("current").put("relative_humidity_2m",101)
            check(weather.parseForecast(expanded).humidity==null && weather.parseForecast(expanded).windKmh==null)
            recreated.disable(); check(recreated.state.value.place == null && recreated.state.value.reading == null)
            recreated.refreshIfDue(); check(requests == 2)
        } finally { context.getSharedPreferences(name,Context.MODE_PRIVATE).edit().clear().commit() }
    }
}
