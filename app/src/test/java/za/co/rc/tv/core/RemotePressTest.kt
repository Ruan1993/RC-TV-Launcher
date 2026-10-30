package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class RemotePressTest {
    @Test fun shortPressLaunchesExactlyOnce() {
        val press = RemotePress()
        assertTrue(press.down(0)); assertTrue(press.up(false)); assertFalse(press.up(false))
    }
    @Test fun holdAndRepeatsNeverLaunchOnRelease() {
        val press = RemotePress()
        assertTrue(press.down(0)); assertFalse(press.down(1)); assertTrue(press.hold())
        assertFalse(press.hold()); assertFalse(press.down(2)); assertFalse(press.up(false))
        assertTrue(press.down(0)); assertTrue(press.up(false))
    }
    @Test fun cancellationOrFocusLossCannotLaunchOrOpenMenu() {
        val press = RemotePress()
        press.down(0); assertFalse(press.up(true)); assertFalse(press.hold())
        press.down(0); press.cancel(); assertFalse(press.hold()); assertFalse(press.up(false))
        assertFalse(press.down(2)); assertFalse(press.up(false))
    }
}
