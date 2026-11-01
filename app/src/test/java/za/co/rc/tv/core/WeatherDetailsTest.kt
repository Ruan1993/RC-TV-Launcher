package za.co.rc.tv.core

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class WeatherDetailsTest {
    @Test fun missingMeasurementsAreNeverInvented() {
        assertTrue(weatherDetailMeasurements(null).isEmpty())
        assertTrue(weatherDetailMeasurements(WeatherReading(20.0, 0, 1)).isEmpty())
        assertEquals(listOf("Humidity" to "65%"), weatherDetailMeasurements(WeatherReading(20.0, 0, 1, humidity=65.0), Locale.US))
    }
    @Test fun availableMeasurementsHaveTheirProviderUnits() {
        assertEquals(listOf("Feels like" to "18 °C", "Humidity" to "65%", "Wind" to "12.3 km/h"),
            weatherDetailMeasurements(WeatherReading(20.0, 0, 1, 18.0, 65.0, 12.3), Locale.US))
        assertTrue(weatherDetailMeasurements(WeatherReading(20.0, 0, 1, Double.NaN, Double.POSITIVE_INFINITY, Double.NaN)).isEmpty())
    }
}
