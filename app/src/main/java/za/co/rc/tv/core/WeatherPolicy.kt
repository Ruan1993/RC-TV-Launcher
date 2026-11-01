package za.co.rc.tv.core

import java.util.Locale

/** Only display measurements actually supplied by the existing provider/cache. */
internal fun weatherDetailMeasurements(reading: WeatherReading?, locale: Locale = Locale.getDefault()): List<Pair<String, String>> = buildList {
    reading?.feelsLike?.takeIf(Double::isFinite)?.let { add("Feels like" to String.format(locale, "%.0f °C", it)) }
    reading?.humidity?.takeIf(Double::isFinite)?.let { add("Humidity" to String.format(locale, "%.0f%%", it)) }
    reading?.windKmh?.takeIf(Double::isFinite)?.let { add("Wind" to String.format(locale, "%.1f km/h", it)) }
}

object WeatherPolicy {
    const val HOUR = 3_600_000L
    const val DAY = 24 * HOUR
    fun refreshDue(now: Long, lastAttempt: Long) = lastAttempt <= 0 || now < lastAttempt || now - lastAttempt >= HOUR
    fun usable(now: Long, observed: Long) = observed > 0 && observed <= now + 900_000 && now - observed < DAY
    fun condition(code: Int) = when (code) {
        0 -> "Clear"; 1 -> "Mainly clear"; 2 -> "Partly cloudy"; 3 -> "Overcast"
        45,48 -> "Fog"; 51,53,55 -> "Drizzle"; 56,57 -> "Freezing drizzle"
        61,63,65 -> "Rain"; 66,67 -> "Freezing rain"; 71,73,75,77 -> "Snow"
        80,81,82 -> "Rain showers"; 85,86 -> "Snow showers"; 95,96,99 -> "Thunderstorms"
        else -> "Conditions unavailable"
    }
}
