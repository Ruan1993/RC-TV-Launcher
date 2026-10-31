package za.co.rc.tv.core
import org.junit.Assert.*
import org.junit.Test

class WeatherPolicyTest {
    @Test fun hourlyRefreshAndClockRollbackAreBounded() {
        assertFalse(WeatherPolicy.refreshDue(10_000_000, 9_000_000))
        assertTrue(WeatherPolicy.refreshDue(9_000_000 + WeatherPolicy.HOUR, 9_000_000))
        assertTrue(WeatherPolicy.refreshDue(1_000, 9_000_000))
        assertTrue(WeatherPolicy.refreshDue(1_000, 0))
    }
    @Test fun oldAndInvalidWeatherIsNeverCurrent() {
        assertFalse(WeatherPolicy.usable(10_000_000, 0))
        assertFalse(WeatherPolicy.usable(10_000_000, 11_000_000))
        assertFalse(WeatherPolicy.usable(1_000 + WeatherPolicy.DAY, 1_000))
        assertTrue(WeatherPolicy.usable(1_000 + WeatherPolicy.HOUR, 1_000))
        assertEquals("Conditions unavailable", WeatherPolicy.condition(1000))
        assertEquals("Thunderstorms", WeatherPolicy.condition(95))
    }
}
