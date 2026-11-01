package za.co.rc.tv.core

import org.json.JSONObject

data class WeatherConfiguration(val place: WeatherPlace? = null, val enabled: Boolean = false) {
    fun encode(): String = JSONObject().put("enabled", enabled).put("place", place?.let {
        JSONObject().put("label", it.label).put("latitude", it.latitude).put("longitude", it.longitude)
    } ?: JSONObject.NULL).toString()
    companion object {
        fun decode(raw: String): WeatherConfiguration {
            require(raw.length <= 2048)
            val json = parseBackupObject(raw)
            require(json.keys().asSequence().toSet() == setOf("enabled", "place") && json.get("enabled") is Boolean)
            val place = if (json.isNull("place")) null else json.getJSONObject("place").let {
                require(it.keys().asSequence().toSet() == setOf("label", "latitude", "longitude"))
                require(it.get("label") is String && it.get("latitude") is Number && it.get("longitude") is Number)
                WeatherPlace(it.getString("label"), it.getDouble("latitude"), it.getDouble("longitude")).also { p -> require(p.label.none(Char::isISOControl)) }
            }
            return WeatherConfiguration(place, json.getBoolean("enabled")).also { require(!it.enabled || it.place != null) }
        }
        fun fromLegacy(raw: String?): WeatherConfiguration = runCatching {
            if (raw == null) return WeatherConfiguration()
            val json = JSONObject(raw)
            WeatherConfiguration(WeatherPlace(json.getString("label"), json.getDouble("lat"), json.getDouble("lon")), json.optBoolean("enabled", true))
        }.getOrDefault(WeatherConfiguration())
    }
}
