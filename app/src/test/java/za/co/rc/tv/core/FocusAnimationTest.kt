package za.co.rc.tv.core
import org.junit.Assert.*
import org.junit.Test
class FocusAnimationTest {
    @Test fun legacyChoicesAndFreshDefaultsArePreserved() {
        assertEquals("Standard",legacyFocusAnimation(null)); assertEquals("Off",legacyFocusAnimation("Minimal"))
        assertEquals("Subtle",legacyFocusAnimation("Balanced")); assertEquals("Standard",legacyFocusAnimation("Cinematic"))
        assertEquals("Standard",validFocusAnimation("invalid"))
    }
    @Test fun effectsAreBoundedAndReducedMotionWins() {
        assertEquals(1f,focusScale("Off")); assertEquals(1.025f,focusScale("Subtle"))
        assertEquals(1.05f,focusScale("Standard")); assertEquals(1.10f,focusScale("Enhanced"))
        focusAnimations.forEach { assertEquals(1f,focusScale(activeFocusAnimation(LauncherPreferences(reducedMotion=true,focusAnimation=it)))) }
    }
}
