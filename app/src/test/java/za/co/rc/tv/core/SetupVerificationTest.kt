package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class SetupVerificationTest {
    @Test fun roleOrExplicitLaunchCannotPassPhysicalHome() {
        assertFalse(SetupVerification.homeVerified(true, false, false))
        assertFalse(SetupVerification.homeVerified(false, true, false))
        assertFalse(SetupVerification.homeVerified(true, true, true))
        assertTrue(SetupVerification.homeVerified(true, true, false))
    }
    @Test fun interceptAndAppIntentsAreNotNativeHomeEvidence() {
        assertFalse(SetupVerification.nativeHome("za.co.rc.tv.EXPERIMENT_HOME", setOf("android.intent.category.HOME")))
        assertFalse(SetupVerification.nativeHome("android.intent.action.MAIN", setOf("android.intent.category.LAUNCHER")))
        assertFalse(SetupVerification.nativeHome(null, null))
        assertTrue(SetupVerification.nativeHome("android.intent.action.MAIN", setOf("android.intent.category.HOME")))
    }
    @Test fun restoreNeedsEveryIndependentGate() {
        for (missing in 0..3) {
            val gates = MutableList(4) { true }; gates[missing] = false
            assertFalse(SetupVerification.restored(gates[0], gates[1], gates[2], !gates[3]))
        }
        assertTrue(SetupVerification.restored(true, true, true, false))
    }
    @Test fun fallbackAndRecoveryScreensCannotBeOriginalHomes() {
        assertFalse(reliableHomeTarget("com.android.tv.settings", "com.android.tv.settings.system.FallbackHome"))
        assertFalse(reliableHomeTarget("com.google.android.tungsten.setupwraith", "RecoveryActivity"))
        assertTrue(reliableHomeTarget("com.google.android.apps.tv.launcherx", "home.HomeActivity"))
    }
}
