package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class OriginalHomeRecoveryTest {
    @Test fun disabledGoogleCannotUseHomeSelection() {
        val guidance = OriginalHomeRecovery(true, true, true)
        assertFalse(guidance.useHomeSelection)
        assertTrue(guidance.description.contains("Home selection cannot restore"))
        val steps = guidance.recoverySteps
        val ordered = listOf(
            "pm enable --user 0 com.google.android.apps.tv.launcherx",
            "pm default-state --user 0 com.google.android.apps.tv.launcherx",
            "pm enable --user 0 com.google.android.tungsten.setupwraith",
            "pm default-state --user 0 com.google.android.tungsten.setupwraith")
        val positions = ordered.map { steps.indexOf(it) }
        assertTrue(positions.all { it >= 0 })
        assertEquals(positions.sorted(), positions)
        assertTrue(steps.contains("does not disable RC TV’s Accessibility"))
        assertFalse(steps.contains("disable-user"))
    }
    @Test fun enabledGoogleRetainsNormalHomeSelection() {
        val guidance = OriginalHomeRecovery(false, true, false)
        assertTrue(guidance.useHomeSelection)
        assertEquals("Switch to Original Xiaomi Home", guidance.buttonLabel)
        assertTrue(guidance.description.contains("Google TV is enabled"))
    }
    @Test fun missingOrUnreadableGoogleStillAllowsOtherHomeHandlers() {
        val guidance = OriginalHomeRecovery(null, false, false)
        assertTrue(guidance.useHomeSelection)
        assertEquals("Open Android Home selection", guidance.buttonLabel)
        assertTrue(guidance.description.contains("does not enable disabled packages"))
    }
    @Test fun xiaomiCommandsAreNotSuggestedForOtherFirmwareOrDifferentSetupState() {
        for (guidance in listOf(OriginalHomeRecovery(true, false, true), OriginalHomeRecovery(true, true, false))) {
            assertFalse(guidance.useHomeSelection)
            assertFalse(guidance.recoverySteps.contains("pm enable --user 0"))
            assertTrue(guidance.recoverySteps.contains("specific to this device"))
        }
    }
}
