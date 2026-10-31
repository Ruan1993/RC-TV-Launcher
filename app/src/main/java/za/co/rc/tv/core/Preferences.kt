package za.co.rc.tv.core

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.IOException

private val Context.store by preferencesDataStore("launcher", corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() })
data class LauncherPreferences(val favourites: Set<String> = emptySet(), val reducedMotion: Boolean = false, val theme: String = "Cinematic", val motion: String = "Minimal", val favouriteOrder: List<String> = emptyList(), val wallpaper: String = BundledWallpaper.MOUNTAIN.id,
    val rotationEnabled: Boolean = false, val rotationMinutes: Int = 15,
    val rotationPool: Set<String> = setOf("mountain", "coast", "desert"),
    val wallpaperTransition: String = "Instant", val rotationAt: Long = 0L,
    val accent: String = "Original (Default)", val fontStyle: String = "Modern", val textSize: String = "Standard", val wallpaperVisibility: String = "Balanced", val focusAnimation: String = "Standard")
class Preferences(context: Context) {
    private val store = context.applicationContext.store
    private val favourites = stringSetPreferencesKey("favourites")
    private val order = stringPreferencesKey("favourite_order")
    private val motion = booleanPreferencesKey("reduced_motion")
    private val theme = stringPreferencesKey("theme")
    private val wallpaper = stringPreferencesKey("wallpaper")
    private val rotate = booleanPreferencesKey("wallpaper_rotation")
    private val interval = intPreferencesKey("wallpaper_interval")
    private val pool = stringSetPreferencesKey("wallpaper_pool")
    private val transition = stringPreferencesKey("wallpaper_transition")
    private val rotatedAt = longPreferencesKey("wallpaper_rotated_at")
    private val accent = stringPreferencesKey("accent_theme")
    private val fontStyle = stringPreferencesKey("font_style")
    private val textSize = stringPreferencesKey("text_size")
    private val visibility = stringPreferencesKey("wallpaper_visibility")
    private val focusAnimation = stringPreferencesKey("focus_animation")
    private val intensity = stringPreferencesKey("animation_intensity")
    val state = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { LauncherPreferences(it[favourites].orEmpty(), it[motion] ?: false, validTheme(it[theme]), validMotion(it[intensity]), it[order]?.split("\n")?.filter(String::isNotBlank).orEmpty(), validWallpaper(it[wallpaper]),
            it[rotate] ?: false, validRotationInterval(it[interval] ?: 15),
            it[pool]?.filter { id -> validWallpaper(id) == id }?.toSet() ?: setOf("mountain", "coast", "desert"),
            validWallpaperTransition(it[transition]), it[rotatedAt] ?: 0L, validAccent(it[accent]), validFontStyle(it[fontStyle]), validTextSize(it[textSize]), validWallpaperVisibility(it[visibility]), validFocusAnimation(it[focusAnimation] ?: legacyFocusAnimation(it[intensity]))) }
    suspend fun toggleFavourite(packageName: String) { store.edit { p ->
        val current = p[favourites].orEmpty(); p[favourites] = if (packageName in current) current - packageName else current + packageName
    } }
    suspend fun reorderFavourites(packages: List<String>) { store.edit { p ->
        require(packages.distinct().size == packages.size && packages.toSet() == p[favourites].orEmpty())
        p[order] = packages.joinToString("\n")
    } }
    suspend fun setWallpaper(value: String) {
        require(validWallpaper(value) == value)
        store.edit { it[wallpaper] = value; it[rotatedAt] = System.currentTimeMillis() }
    }
    suspend fun setRotation(enabled: Boolean) { store.edit { it[rotate] = enabled; it[rotatedAt] = System.currentTimeMillis() } }
    suspend fun setWallpaperIfCurrent(value: String, expected: String) {
        require(BundledWallpaper.entries.any { it.id == value })
        store.edit { p -> check(validWallpaper(p[wallpaper]) == expected) { "Wallpaper changed; ask again" }; p[wallpaper] = value; p[rotatedAt] = System.currentTimeMillis() }
    }
    suspend fun setRotationIfCurrent(value: Boolean, expected: Boolean) {
        store.edit { p -> check((p[rotate] ?: false) == expected) { "Rotation changed; ask again" }; p[rotate] = value; p[rotatedAt] = System.currentTimeMillis() }
    }
    suspend fun setRotationMinutes(value: Int) { require(value in rotationIntervals); store.edit { it[interval] = value; it[rotatedAt] = System.currentTimeMillis() } }
    suspend fun setWallpaperTransition(value: String) { require(validWallpaperTransition(value) == value); store.edit { it[transition] = value } }
    suspend fun toggleRotationWallpaper(id: String) { require(validWallpaper(id) == id); store.edit { p ->
        val current = p[pool] ?: setOf("mountain", "coast", "desert")
        p[pool] = if (id in current) current - id else current + id
    } }
    suspend fun advanceWallpaper(available: List<String>) { store.edit { p ->
        nextWallpaper(validWallpaper(p[wallpaper]), p[pool] ?: setOf("mountain", "coast", "desert"), available)?.let { p[wallpaper] = it }
        p[rotatedAt] = System.currentTimeMillis()
    } }
    suspend fun removeWallpaper(id: String) { require(isImportedWallpaper(id)); store.edit { p ->
        if (p[wallpaper] == id) p[wallpaper] = "mountain"
        p[pool] = (p[pool] ?: setOf("mountain", "coast", "desert")) - id
    } }
    suspend fun setAccent(value: String) { require(value in accentThemes); store.edit { it[accent] = value } }
    suspend fun setFontStyle(value: String) { require(value in fontStyles); store.edit { it[fontStyle] = value } }
    suspend fun setTextSize(value: String) { require(value in textSizes); store.edit { it[textSize] = value } }
    suspend fun setWallpaperVisibility(value: String) { require(value in wallpaperVisibilities); store.edit { it[visibility] = value } }
    suspend fun setFocusAnimation(value: String) { require(value in focusAnimations); store.edit { it[focusAnimation] = value } }
    suspend fun resetAppearance() { store.edit { it.remove(accent); it.remove(fontStyle); it.remove(textSize); it.remove(visibility); it.remove(focusAnimation) } }
    suspend fun setTheme(value: String) { store.edit { it[theme] = validTheme(value) } }
    suspend fun setMotion(value: String) { store.edit { it[intensity] = validMotion(value) } }
    suspend fun setReducedMotion(enabled: Boolean) { store.edit { it[motion] = enabled } }
}

fun validTheme(value: String?) = value?.takeIf { it in listOf("Cinematic", "Minimalist") } ?: "Cinematic"
fun validMotion(value: String?) = value?.takeIf { it in listOf("Minimal", "Balanced", "Cinematic") } ?: "Minimal"

val accentThemes = listOf("Original (Default)", "Emerald Green", "Ocean Blue", "Rose Pink", "Ruby Red", "Midnight Black", "Slate Grey")
val fontStyles = listOf("Modern", "Classic", "Bold / High Readability")
val textSizes = listOf("Standard", "Comfortable", "Large", "Extra Large")
fun validAccent(value: String?) = value?.takeIf { it in accentThemes } ?: accentThemes.first()
fun validFontStyle(value: String?) = value?.takeIf { it in fontStyles } ?: fontStyles.first()
fun validTextSize(value: String?) = value?.takeIf { it in textSizes } ?: textSizes.first()
fun textScale(value: String) = when (value) { "Comfortable" -> 1.15f; "Large" -> 1.3f; "Extra Large" -> 1.5f; else -> 1f }

val wallpaperVisibilities = listOf("Subtle", "Balanced", "Vivid")
fun validWallpaperVisibility(value: String?) = value?.takeIf { it in wallpaperVisibilities } ?: "Balanced"
fun wallpaperScrimAlpha(base: Float, visibility: String): Float = (base * when (visibility) { "Subtle" -> 1.12f; "Vivid" -> .18f; else -> 1f }).coerceIn(0f, .99f)
