package za.co.rc.tv.core

import android.content.Context
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
data class LauncherPreferences(val favourites: Set<String> = emptySet(), val reducedMotion: Boolean = false, val theme: String = "Cinematic", val motion: String = "Minimal")
class Preferences(context: Context) {
    private val store = context.applicationContext.store
    private val favourites = stringSetPreferencesKey("favourites")
    private val motion = booleanPreferencesKey("reduced_motion")
    private val theme = stringPreferencesKey("theme")
    private val intensity = stringPreferencesKey("animation_intensity")
    val state = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { LauncherPreferences(it[favourites].orEmpty(), it[motion] ?: false, validTheme(it[theme]), validMotion(it[intensity])) }
    suspend fun toggleFavourite(packageName: String) { store.edit { p ->
        val current = p[favourites].orEmpty(); p[favourites] = if (packageName in current) current - packageName else current + packageName
    } }
    suspend fun setTheme(value: String) { store.edit { it[theme] = validTheme(value) } }
    suspend fun setMotion(value: String) { store.edit { it[intensity] = validMotion(value) } }
    suspend fun setReducedMotion(enabled: Boolean) { store.edit { it[motion] = enabled } }
}

fun validTheme(value: String?) = value?.takeIf { it in listOf("Cinematic", "Minimalist") } ?: "Cinematic"
fun validMotion(value: String?) = value?.takeIf { it in listOf("Minimal", "Balanced", "Cinematic") } ?: "Minimal"
