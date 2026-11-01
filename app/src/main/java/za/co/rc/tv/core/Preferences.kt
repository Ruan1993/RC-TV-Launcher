package za.co.rc.tv.core

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.IOException
import kotlinx.coroutines.flow.first

private val providerKey = stringPreferencesKey("screensaver_provider")
internal val weatherConfigurationKey = stringPreferencesKey("weather_configuration")
private val Context.store by preferencesDataStore("launcher", corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    produceMigrations = { context ->
        // Capture before the first read creates a new datastore file. Persist the decision
        // once, so a new installation cannot turn into a legacy installation on restart.
        val existing = context.preferencesDataStoreFile("launcher").exists()
        listOf(object : androidx.datastore.core.DataMigration<androidx.datastore.preferences.core.Preferences> {
            override suspend fun shouldMigrate(currentData: androidx.datastore.preferences.core.Preferences) = currentData[providerKey] !in screensaverProviders
            override suspend fun migrate(currentData: androidx.datastore.preferences.core.Preferences) = currentData.toMutablePreferences().apply {
                this[providerKey] = initialScreensaverProvider(currentData[providerKey], existing)
            }
            override suspend fun cleanUp() = Unit
        }, object : androidx.datastore.core.DataMigration<androidx.datastore.preferences.core.Preferences> {
            override suspend fun shouldMigrate(currentData: androidx.datastore.preferences.core.Preferences) = currentData[weatherConfigurationKey] == null
            override suspend fun migrate(currentData: androidx.datastore.preferences.core.Preferences) = currentData.toMutablePreferences().apply {
                this[weatherConfigurationKey] = WeatherConfiguration.fromLegacy(context.getSharedPreferences("weather", Context.MODE_PRIVATE).getString("record", null)).encode()
            }
            override suspend fun cleanUp() = Unit
        })
    })
data class LauncherPreferences(val favourites: Set<String> = emptySet(), val reducedMotion: Boolean = false, val theme: String = "Cinematic", val motion: String = "Minimal", val favouriteOrder: List<String> = emptyList(), val wallpaper: String = BundledWallpaper.MOUNTAIN.id,
    val rotationEnabled: Boolean = false, val rotationMinutes: Int = 15,
    val rotationPool: Set<String> = setOf("mountain", "coast", "desert"),
    val wallpaperTransition: String = "Instant", val rotationAt: Long = 0L,
    val accent: String = "Original (Default)", val fontStyle: String = "Modern", val textSize: String = "Standard", val wallpaperVisibility: String = "Balanced", val focusAnimation: String = "Standard",
    val screensaverMinutes: Int = 30, val screensaverStyle: String = "Cinematic Wallpapers", val screensaverProvider: String = ANDROID_SCREENSAVER)
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
    private val saverMinutes = intPreferencesKey("screensaver_minutes")
    private val saverStyle = stringPreferencesKey("screensaver_style")
    val weatherConfiguration = store.data.map { WeatherConfiguration.decode(it[weatherConfigurationKey] ?: WeatherConfiguration().encode()) }
    suspend fun setWeatherConfiguration(value: WeatherConfiguration) { store.edit { it[weatherConfigurationKey] = value.encode() } }
    internal suspend fun backupValues(): Map<String, Any> = store.data.first().asMap().entries.associate { it.key.name to it.value }
    internal suspend fun restoreBackup(values: Map<String, Any>) {
        BackupSchema.validate(values)
        store.edit { target ->
            BackupSchema.rules.keys.forEach { name -> target.asMap().keys.firstOrNull { it.name == name }?.let { target.remove(it) } }
            values.forEach { (name, value) -> when (value) {
                is Boolean -> target[booleanPreferencesKey(name)] = value
                is Int -> target[intPreferencesKey(name)] = value
                is String -> target[stringPreferencesKey(name)] = value
                is Set<*> -> target[stringSetPreferencesKey(name)] = value.filterIsInstance<String>().toSet()
                else -> error("Unsupported setting")
            } }
            // Runtime scheduling is local to this device, not part of the portable backup.
            target[rotatedAt] = System.currentTimeMillis()
        }
    }
    val state = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { LauncherPreferences(it[favourites].orEmpty(), it[motion] ?: false, validTheme(it[theme]), validMotion(it[intensity]), it[order]?.split("\n")?.filter(String::isNotBlank).orEmpty(), validWallpaper(it[wallpaper]),
            it[rotate] ?: false, validRotationInterval(it[interval] ?: 15),
            it[pool]?.filter { id -> validWallpaper(id) == id }?.toSet() ?: setOf("mountain", "coast", "desert"),
            validWallpaperTransition(it[transition]), it[rotatedAt] ?: 0L, validAccent(it[accent]), validFontStyle(it[fontStyle]), validTextSize(it[textSize]), validWallpaperVisibility(it[visibility]), validFocusAnimation(it[focusAnimation] ?: legacyFocusAnimation(it[intensity])),
            validScreensaverMinutes(it[saverMinutes]), validScreensaverStyle(it[saverStyle]), initialScreensaverProvider(it[providerKey], false)) }
    suspend fun setScreensaverProvider(value: String) { require(value in screensaverProviders); store.edit { it[providerKey] = value } }
    suspend fun setScreensaverMinutes(value: Int) { require(value in screensaverIntervals); store.edit { it[saverMinutes] = value } }
    suspend fun setScreensaverStyle(value: String) { require(value in screensaverStyles); store.edit { it[saverStyle] = value } }
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
    suspend fun resetLauncherAppearance() { store.edit { resetLauncherVisualPreferences(it) } }
    suspend fun setTheme(value: String) { store.edit { it[theme] = validTheme(value) } }
    suspend fun setMotion(value: String) { store.edit { it[intensity] = validMotion(value) } }
    suspend fun setReducedMotion(enabled: Boolean) { store.edit { it[motion] = enabled } }
}

/** Explicit visual allowlist. Imported files, accessibility motion, screensaver and nonvisual data stay intact. */
fun resetLauncherVisualPreferences(prefs: androidx.datastore.preferences.core.MutablePreferences) {
    val visual = setOf("theme", "wallpaper", "wallpaper_rotation", "wallpaper_interval", "wallpaper_pool", "wallpaper_transition", "wallpaper_rotated_at",
        "accent_theme", "font_style", "text_size", "wallpaper_visibility", "focus_animation", "animation_intensity")
    prefs.asMap().keys.filter { it.name in visual }.forEach { prefs.remove(it) }
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
