package za.co.rc.tv.core

import androidx.datastore.preferences.core.*
import org.junit.Assert.*
import org.junit.Test

class LauncherAppearanceResetTest {
    @Test fun visualResetLeavesFavouriteOrderAccessibilityScreensaverAndOtherData() {
        val favourites=(1..8).map { "app.$it" }.toSet()
        val order=favourites.reversed().joinToString("\n")
        val p=mutablePreferencesOf(
            stringPreferencesKey("theme") to "Minimalist", stringPreferencesKey("wallpaper") to "imported-original",
            booleanPreferencesKey("wallpaper_rotation") to true, stringPreferencesKey("accent_theme") to "Ruby Red",
            stringPreferencesKey("font_style") to "Classic", stringPreferencesKey("text_size") to "Extra Large",
            stringPreferencesKey("focus_animation") to "Enhanced", stringPreferencesKey("wallpaper_visibility") to "Vivid",
            stringSetPreferencesKey("favourites") to favourites, stringPreferencesKey("favourite_order") to order,
            booleanPreferencesKey("reduced_motion") to true, stringPreferencesKey("screensaver_provider") to RC_SCREENSAVER,
            stringPreferencesKey("screensaver_style") to "Black Screen", intPreferencesKey("screensaver_minutes") to 120,
            stringPreferencesKey("unrelated_future_preference") to "keep")
        resetLauncherVisualPreferences(p)
        assertNull(p[stringPreferencesKey("theme")]); assertNull(p[stringPreferencesKey("wallpaper")])
        assertNull(p[booleanPreferencesKey("wallpaper_rotation")]); assertNull(p[stringPreferencesKey("accent_theme")])
        assertNull(p[stringPreferencesKey("text_size")]); assertNull(p[stringPreferencesKey("focus_animation")])
        assertEquals(favourites,p[stringSetPreferencesKey("favourites")]); assertEquals(order,p[stringPreferencesKey("favourite_order")])
        assertEquals(true,p[booleanPreferencesKey("reduced_motion")])
        assertEquals(RC_SCREENSAVER,p[stringPreferencesKey("screensaver_provider")])
        assertEquals("Black Screen",p[stringPreferencesKey("screensaver_style")]); assertEquals(120,p[intPreferencesKey("screensaver_minutes")])
        assertEquals("keep",p[stringPreferencesKey("unrelated_future_preference")])
    }
}
