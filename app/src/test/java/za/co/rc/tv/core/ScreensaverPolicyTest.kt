package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class ScreensaverPolicyTest {
    @Test fun defaultsAndNever() {
        assertEquals(30, validScreensaverMinutes(null))
        assertEquals(30, validScreensaverMinutes(-1))
        assertEquals("Cinematic Wallpapers", validScreensaverStyle("invalid"))
        assertNull(screensaverDelay(0, 10, 100))
    }
    @Test fun deadlinesUseElapsedTimeAndResetOnInput() {
        for (minutes in screensaverIntervals.filter { it > 0 }) {
            assertEquals(minutes * 60_000L, screensaverDelay(minutes, 1000, 1000))
            assertEquals(1L, screensaverDelay(minutes, 1000, 999 + minutes * 60_000L))
            assertEquals(0L, screensaverDelay(minutes, 1000, 2000 + minutes * 60_000L))
        }
        assertEquals(600_000L, screensaverDelay(10, 700_000, 700_000))
        assertEquals(600_000L, screensaverDelay(10, 2000, 1000))
    }
    @Test fun neverCoverOtherAppsAudioOrSystemSleep() {
        assertTrue(mayShowScreensaver(true, true, true, false, false))
        assertFalse(mayShowScreensaver(false, true, true, false, false))
        assertFalse(mayShowScreensaver(true, false, true, false, false))
        assertFalse(mayShowScreensaver(true, true, false, false, false))
        assertFalse(mayShowScreensaver(true, true, true, true, false))
        assertFalse(mayShowScreensaver(true, true, true, false, true))
    }
}
