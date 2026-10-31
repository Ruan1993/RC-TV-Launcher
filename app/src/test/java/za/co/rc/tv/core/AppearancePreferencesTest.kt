package za.co.rc.tv.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearancePreferencesTest {
    @Test fun missingOrUnknownValuesUseSafeDefaults() {
        assertEquals("Cinematic", validTheme(null))
        assertEquals("Cinematic", validTheme("unknown"))
        assertEquals("Minimal", validMotion(null))
        assertEquals("Minimal", validMotion("unknown"))
    }
    @Test fun supportedPreferencesSurviveValidation() {
        listOf("Cinematic", "Minimalist").forEach { assertEquals(it, validTheme(it)) }
        listOf("Minimal", "Balanced", "Cinematic").forEach { assertEquals(it, validMotion(it)) }
        accentThemes.forEach { assertEquals(it, validAccent(it)) }
        fontStyles.forEach { assertEquals(it, validFontStyle(it)) }
        textSizes.forEach { assertEquals(it, validTextSize(it)) }
        assertEquals("Original (Default)", validAccent("invalid"))
        assertEquals("Modern", validFontStyle(null))
        assertEquals("Standard", validTextSize("invalid"))
        assertEquals(1f, textScale("Standard"))
        assertEquals(1.5f, textScale("Extra Large"))
    }
}
