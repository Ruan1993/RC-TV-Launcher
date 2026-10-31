package za.co.rc.tv.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WeatherPlace(val label: String, val latitude: Double, val longitude: Double) {
    init { require(label.isNotBlank() && label.length <= 100 && latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0) }
}
data class WeatherReading(val celsius: Double, val code: Int, val observed: Long, val feelsLike: Double? = null, val humidity: Double? = null, val windKmh: Double? = null)
data class WeatherState(val place: WeatherPlace? = null, val reading: WeatherReading? = null,
    val loading: Boolean = false, val message: String = "Weather is off", val enabled: Boolean = place != null)

/** No jobs, GPS or polling. A Home/RESUMED entry may request one bounded refresh per hour. */
class WeatherRepository(context: Context, private val now: () -> Long = { System.currentTimeMillis() },
    private val fetch: (String) -> String = ::weatherHttp, storageName: String = "weather") {
    private val storage = context.applicationContext.getSharedPreferences(storageName, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    @Volatile private var revision = 0
    private var lastAttempt = 0L
    private val mutable = MutableStateFlow(load())
    val state = mutable.asStateFlow()
    private fun load(): WeatherState = runCatching {
        val raw = storage.getString("record", null) ?: return WeatherState()
        require(raw.length <= 4096)
        val record = JSONObject(raw)
        lastAttempt = record.optLong("attempt")
        val place = WeatherPlace(record.getString("label"), record.getDouble("lat"), record.getDouble("lon"))
        val reading = record.optJSONObject("reading")?.let { parseReading(it) }
        WeatherState(place, reading, message = if (reading != null) "Cached weather" else "Weather unavailable. Open Home to refresh.", enabled = record.optBoolean("enabled",true))
    }.getOrElse { WeatherState(message = "Weather is off. Choose a location to set up again.") }
    @Synchronized private fun save() {
        val current = mutable.value; val place = current.place ?: return
        val record = JSONObject().put("enabled",current.enabled).put("label", place.label).put("lat", place.latitude).put("lon", place.longitude).put("attempt", lastAttempt)
        current.reading?.let { record.put("reading", JSONObject().put("temperature_2m",it.celsius).put("weather_code",it.code).put("time",it.observed / 1000).put("apparent_temperature",it.feelsLike).put("relative_humidity_2m",it.humidity).put("wind_speed_10m",it.windKmh)) }
        storage.edit().putString("record", record.toString()).apply()
    }
    @Synchronized fun select(place: WeatherPlace) { revision++; lastAttempt = 0; mutable.value = WeatherState(place, message = "Location saved. Weather refreshes on Home."); save() }
    @Synchronized fun setEnabled(enabled: Boolean) {
        if(mutable.value.place == null) return
        revision++; mutable.value=mutable.value.copy(enabled=enabled,loading=false,message=if(enabled) "Weather enabled" else "Weather is off"); save()
    }
    @Synchronized fun disable() { revision++; lastAttempt = 0; mutable.value = WeatherState(); storage.edit().clear().apply() }
    suspend fun search(query: String): List<WeatherPlace> = withContext(Dispatchers.IO) {
        require(query.trim().length in 2..80) { "Enter a city name (2–80 characters)." }
        val json = JSONObject(fetch("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(query.trim(), "UTF-8")}&count=3&language=en&format=json"))
        val results = json.optJSONArray("results")
        (0 until minOf(results?.length() ?: 0, 3)).map { index ->
            val result = results!!.getJSONObject(index)
            val label = listOf(result.getString("name"),result.optString("admin1"),result.optString("country")).filter(String::isNotBlank).joinToString(", ").take(100)
            WeatherPlace(label, result.getDouble("latitude"), result.getDouble("longitude"))
        }
    }
    suspend fun refreshIfDue() = withContext(Dispatchers.IO) { mutex.withLock {
        val (place, generation) = synchronized(this@WeatherRepository) {
            val current = mutable.value.place ?: return@withLock
            if (!mutable.value.enabled || !WeatherPolicy.refreshDue(now(), lastAttempt)) return@withLock
            lastAttempt = now(); save()
            mutable.value = mutable.value.copy(loading = true, message = "Updating weather…")
            current to revision
        }
        try {
            val result = JSONObject(fetch("https://api.open-meteo.com/v1/forecast?latitude=${place.latitude}&longitude=${place.longitude}&current=temperature_2m,weather_code,apparent_temperature,relative_humidity_2m,wind_speed_10m&wind_speed_unit=kmh&temperature_unit=celsius&timeformat=unixtime&timezone=UTC&forecast_days=1"))
            val reading = parseForecast(result)
            synchronized(this@WeatherRepository) { if (generation == revision) { mutable.value = WeatherState(place, reading, message = "Updated weather"); save() } }
        } catch (_: Exception) {
            synchronized(this@WeatherRepository) { if (generation == revision) mutable.value = mutable.value.copy(loading = false, message = if (mutable.value.reading != null) "Offline or provider unavailable · cached weather" else "Weather unavailable · check your connection") }
        }
    } }
    internal fun parseForecast(result: JSONObject): WeatherReading {
        require(result.getJSONObject("current_units").getString("temperature_2m") == "\u00B0C") { "Unexpected temperature units" }
        val reading = parseReading(result.getJSONObject("current"))
        require(WeatherPolicy.usable(now(), reading.observed)) { "Provider returned outdated weather" }
        val units=result.getJSONObject("current_units")
        return reading.copy(feelsLike=reading.feelsLike.takeIf { units.optString("apparent_temperature")=="\u00B0C" },
            humidity=reading.humidity.takeIf { units.optString("relative_humidity_2m")=="%" },
            windKmh=reading.windKmh.takeIf { units.optString("wind_speed_10m")=="km/h" })
    }
    private fun parseReading(json: JSONObject): WeatherReading {
        val temperature = json.getDouble("temperature_2m"); require(temperature.isFinite() && temperature in -100.0..70.0)
        val seconds = json.getLong("time"); require(seconds in 1..4_102_444_800L)
        fun optional(key: String, range: ClosedFloatingPointRange<Double>): Double? = json.optDouble(key,Double.NaN).takeIf { it.isFinite() && it in range }
        return WeatherReading(temperature, json.getInt("weather_code"), seconds * 1000,
            optional("apparent_temperature",-100.0..80.0),optional("relative_humidity_2m",0.0..100.0),optional("wind_speed_10m",0.0..500.0))
    }
}

private fun weatherHttp(address: String): String {
    val connection = URL(address).openConnection() as HttpURLConnection
    connection.connectTimeout = 5000; connection.readTimeout = 5000; connection.instanceFollowRedirects = false
    connection.setRequestProperty("User-Agent", "RC-TV/1.0 personal-noncommercial-preview")
    return try {
        check(connection.responseCode == 200) { "Weather provider unavailable" }
        connection.inputStream.use { stream ->
            val output = java.io.ByteArrayOutputStream(); val block = ByteArray(4096)
            val deadline = System.nanoTime() + 10_000_000_000L
            while (true) { check(System.nanoTime() < deadline); val count = stream.read(block); if (count < 0) break; check(output.size()+count <= 65536); output.write(block,0,count) }
            output.toString("UTF-8")
        }
    } finally { connection.disconnect() }
}
