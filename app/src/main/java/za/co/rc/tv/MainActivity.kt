package za.co.rc.tv

import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import za.co.rc.tv.core.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private fun traceBackKey(event: android.view.KeyEvent) {
        if (BuildConfig.PROFILE_DIAGNOSTICS && event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            android.util.Log.i("RcNavigation", "Back keyCode=${event.keyCode} action=${event.action} repeat=${event.repeatCount}")
        }
    }
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        traceBackKey(event)
        return super.onKeyDown(keyCode, event)
    }
    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent): Boolean {
        traceBackKey(event)
        return super.onKeyUp(keyCode, event)
    }
    override fun onPause() {
        RenderDiagnostics.snapshot()
        super.onPause()
    }
    private val repository by lazy { AppRepository(this) }
    private val preferences by lazy { Preferences(this) }
    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    private fun openSettings() {
        runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            .onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }.onFailure { message("Open Android Settings from your original launcher.") } }
    }
    private fun selectHome() {
        val roles = getSystemService(RoleManager::class.java)
        if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
            runCatching { startActivity(roles.createRequestRoleIntent(RoleManager.ROLE_HOME)) }.onFailure { openSettings() }
        } else openSettings()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(typography = za.co.rc.tv.ui.LauncherTypography, colorScheme = darkColorScheme(primary = Color(0xFF9FE9D5), background = Color(0xFF09151F), surface = Color(0xFF182D39))) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                val prefs by preferences.state.collectAsStateWithLifecycle(LauncherPreferences())
                var discovery by remember { mutableStateOf(AppDiscoveryState()) }
                var section by rememberSaveable { mutableStateOf("Home") }
                var query by rememberSaveable { mutableStateOf("") }
                var homeStatus by remember { mutableStateOf("") }
                var refresh by remember { mutableIntStateOf(0) }
                var iconGeneration by remember { mutableIntStateOf(0) }
                var clock by remember { mutableStateOf("") }
                val scope = rememberCoroutineScope()
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                LaunchedEffect(lifecycle, refresh) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        discovery = refreshDiscovery(discovery, { repository.discover() }) {
                            android.util.Log.e("RcDiscovery", "Installed app discovery failed", it)
                        }
                        repository.invalidateIcons()
                        iconGeneration++
                        android.util.Log.d("RcDiscovery", "Refresh complete: ${discovery.apps.size} apps, error=${discovery.error != null}")
                        val current = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                        homeStatus = if (current?.activityInfo?.packageName == packageName) "RC TV Launcher is your default Home." else "Current Home: ${current?.loadLabel(packageManager) ?: "System chooser / unavailable"}"
                    }
                }
                LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()); kotlinx.coroutines.delay(30_000) } } }
                // Home is the navigation root, not an exit to the previous Android task.
                // Keep this active at Home as well as in launcher sections.
                BackHandler { section = "Home" }
                za.co.rc.tv.ui.CinematicLauncher(
                    discovery = discovery, repository = repository, iconGeneration = iconGeneration,
                    prefs = prefs, section = section, onSection = { section = it },
                    query = query, onQuery = { query = it }, clock = clock, homeStatus = homeStatus,
                    onLaunch = { if (!repository.launch(it)) message("This app cannot currently be opened.") },
                    onPin = { app -> scope.launch { runCatching { preferences.toggleFavourite(app.packageName) }.onFailure { message("Could not save favourite.") } } },
                    onApplyWallpaper = { id -> runCatching { preferences.setWallpaper(id) }
                        .onFailure { message("Could not apply wallpaper. Please try again.") }.isSuccess },
                    onSaveOrder = { order -> runCatching { preferences.reorderFavourites(order) }
                        .onFailure { message("Could not save order. Favourites may have changed; try again.") }.isSuccess },
                    onRetry = { refresh++ }, onChooseHome = { selectHome() }, onRestoreHome = { openSettings() },
                    onAndroidSettings = { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }.onFailure { message("Settings unavailable.") } },
                    onTheme = { value -> scope.launch { runCatching { preferences.setTheme(value) }.onFailure { message("Could not save theme.") } } },
                    onMotion = { value -> scope.launch { runCatching { preferences.setMotion(value) }.onFailure { message("Could not save motion preference.") } } },
                    onReducedMotion = { scope.launch { runCatching { preferences.setReducedMotion(!prefs.reducedMotion) }.onFailure { message("Could not save preference.") } } }
                )
                }
            }
        }
        // Android 11's Window getter can throw before the decor view exists.
        // setContent creates it; posting waits for attachment before hiding bars.
        window.decorView.post {
            window.decorView.windowInsetsController?.apply {
                hide(android.view.WindowInsets.Type.systemBars())
                systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}
