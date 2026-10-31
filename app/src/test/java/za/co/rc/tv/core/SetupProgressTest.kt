package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class SetupProgressTest {
    @Test fun upgradesAndDismissedSetupDoNotInterruptExistingUsers() {
        assertTrue(SetupProgress.offerOnFirstOpen(false, false))
        assertFalse(SetupProgress.offerOnFirstOpen(false, true))
        assertFalse(SetupProgress.offerOnFirstOpen(true, false))
    }
    @Test fun openingSettingsOrAllowingOnlyOneGateCannotClaimCompletion() {
        assertEquals(1, SetupProgress.automaticStep(1, false, true, true))
        assertEquals(2, SetupProgress.automaticStep(1, true, false, false))
        assertEquals(2, SetupProgress.automaticStep(2, false, true, false))
        assertEquals(2, SetupProgress.automaticStep(2, false, false, true))
        assertEquals(3, SetupProgress.automaticStep(2, false, true, true))
    }
}
