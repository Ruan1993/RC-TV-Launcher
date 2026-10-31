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
    private var remoteAction: ((String, String) -> Boolean)? = null
    private val companion by lazy { za.co.rc.tv.companion.RemoteRuntime.server(this) }
    override fun onResume() {
        super.onResume()
        za.co.rc.tv.companion.RemoteRuntime.foreground = { key, text ->
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && (window.decorView.hasWindowFocus() || key == "CLOSE_SETUP") && remoteAction?.invoke(key, text) == true
        }
        companion.resume()
    }
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
        za.co.rc.tv.companion.RemoteRuntime.pause(this)
        RenderDiagnostics.snapshot()
        super.onPause()
    }
    private val repository by lazy { AppRepository(this) }
    private val wallpaperStore by lazy { WallpaperStore(this) }
    private val preferences by lazy { Preferences(this) }
    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    private fun openSettings() {
        runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            .onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }.onFailure { message("Open Android Settings from your original launcher.") } }
    }
    private val homeSelection = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        message(HomeIntegration.inspect(this).description(packageName))
    }
    private fun selectHome() {
        val state = HomeIntegration.inspect(this)
        val roles = getSystemService(RoleManager::class.java)
        val intent = if (state.roleAvailable && !state.roleHeld) roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
            else HomeIntegration.homeSettings(this)
        if (intent == null) message("This firmware does not expose supported Home selection. Continue launching RC TV as an app; Xiaomi Home is unchanged.")
        else runCatching { homeSelection.launch(intent) }.onFailure { message("Home selection is unavailable on this firmware. Xiaomi's launcher remains installed.") }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val suggestSetup = SetupProgress.offerOnFirstOpen(getSharedPreferences("easy-setup", MODE_PRIVATE).getBoolean("seen", false),
            java.io.File(filesDir, "datastore/launcher.preferences_pb").exists())
        setContent {
            val prefs by preferences.state.collectAsStateWithLifecycle(LauncherPreferences())
            za.co.rc.tv.ui.LauncherAppearance(prefs) {
                var discovery by remember { mutableStateOf(AppDiscoveryState()) }
                var section by rememberSaveable { mutableStateOf("Home") }
                var query by rememberSaveable { mutableStateOf("") }
                val setupPrefs = remember { getSharedPreferences("easy-setup", MODE_PRIVATE) }
                var setupOpen by rememberSaveable { mutableStateOf(suggestSetup) }
                var companionOpen by remember { mutableStateOf(false) }
                var companionPage by remember { mutableIntStateOf(0) }
                var offerOtherApps by remember { mutableStateOf(false) }
                val companionState by companion.state.collectAsStateWithLifecycle()
                var seenConnection by remember { mutableLongStateOf(companionState.connectionEvent) }
                LaunchedEffect(companionState.connectionEvent) {
                    if (companionState.connectionEvent > seenConnection) {
                        seenConnection = companionState.connectionEvent
                        companionOpen = false
                        message("Phone connected successfully")
                        val config = getSharedPreferences("companion-connection", MODE_PRIVATE)
                        if ((!za.co.rc.tv.companion.RemoteRuntime.access.value.connected || !za.co.rc.tv.companion.RemoteRuntime.optedIn(this@MainActivity)) && !config.getBoolean("setup-offered", false)) {
                            config.edit().putBoolean("setup-offered", true).apply()
                            if (!setupOpen) offerOtherApps = true
                        }
                    }
                }
                val remoteInputMode = androidx.compose.ui.platform.LocalInputModeManager.current
                SideEffect {
                    remoteAction = { key, text ->
                        remoteInputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
                        when (key) {
                            "CLOSE_SETUP" -> if (companionOpen && companionState.pending == null && !offerOtherApps) { companionOpen = false; true } else false
                            "HOME" -> { section = "Home"; true }
                            "SEARCH_TEXT" -> { section = "Search"; query = text; true }
                            "BACK" -> { onBackPressedDispatcher.onBackPressed(); true }
                            else -> {
                                val code = mapOf("UP" to 19, "DOWN" to 20, "LEFT" to 21, "RIGHT" to 22, "OK" to 23, "BACK" to 4)[key]
                                if (code == null) false else {
                                    window.decorView.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, code))
                                    window.decorView.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, code)); true
                                }
                            }
                        }
                    }
                }
                var keyboardStatus by remember { mutableStateOf("") }
                var homeStatus by remember { mutableStateOf("") }
                var refresh by remember { mutableIntStateOf(0) }
                var iconGeneration by remember { mutableIntStateOf(0) }
                var clock by remember { mutableStateOf("") }
                val scope = rememberCoroutineScope()
                val saveWallpaperSetting: (suspend () -> Unit) -> Unit = { action ->
                    scope.launch { runCatching { action() }.onFailure { message("Could not save wallpaper settings.") } }; Unit
                }
                var wallpapers by remember { mutableStateOf(bundledWallpapers) }
                var wallpaperBusy by remember { mutableStateOf(false) }
                val importImage: (android.net.Uri?) -> Unit = { uri ->
                    if (uri != null) scope.launch {
                        wallpaperBusy = true
                        try { wallpaperStore.import(uri); wallpapers = wallpaperStore.list(); message("Wallpaper imported. Select it in the gallery.") }
                        catch (failure: Exception) { message(failure.message ?: "Could not import this image.") }
                        finally { wallpaperBusy = false }
                    }
                }
                val picker = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == RESULT_OK) importImage(result.data?.data)
                }
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                LaunchedEffect(lifecycle, refresh) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        wallpapers = runCatching { wallpaperStore.list() }.getOrElse { message("Wallpaper library unavailable."); bundledWallpapers }
                        discovery = refreshDiscovery(discovery, { repository.discover() }) {
                            android.util.Log.e("RcDiscovery", "Installed app discovery failed", it)
                        }
                        repository.invalidateIcons()
                        iconGeneration++
                        android.util.Log.d("RcDiscovery", "Refresh complete: ${discovery.apps.size} apps, error=${discovery.error != null}")
                        val input = getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                        val ownKeyboard = android.content.ComponentName(this@MainActivity, za.co.rc.tv.keyboard.RcInputMethodService::class.java)
                        val enabled = input.enabledInputMethodList.any { android.content.ComponentName(it.packageName, it.serviceName) == ownKeyboard }
                        val selected = android.content.ComponentName.unflattenFromString(Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()) == ownKeyboard
                        keyboardStatus = if (selected) "RC TV Keyboard is selected." else if (enabled) "Enabled; choose it in Android when you want to use it." else "Optional keyboard is disabled. Your existing keyboard is unchanged."
                        homeStatus = HomeIntegration.inspect(this@MainActivity).description(packageName)
                    }
                }
                LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()); kotlinx.coroutines.delay(30_000) } } }
                // Home is the navigation root, not an exit to the previous Android task.
                // Keep this active at Home as well as in launcher sections.
                BackHandler { section = "Home" }
                za.co.rc.tv.ui.CinematicLauncher(
                    discovery = discovery, repository = repository, iconGeneration = iconGeneration,
                    prefs = prefs, section = section, onSection = { section = it },
                    query = query, onQuery = { query = it }, clock = clock, homeStatus = homeStatus, keyboardStatus = keyboardStatus,
                    onKeyboardPreview = { startActivity(Intent(this@MainActivity, za.co.rc.tv.keyboard.KeyboardPreviewActivity::class.java)) },
                    onKeyboardEnable = { runCatching { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }.onFailure { message("Keyboard settings unavailable.") } },
                    onKeyboardChoose = { getSystemService(android.view.inputmethod.InputMethodManager::class.java).showInputMethodPicker() },
                    onCompanion = { companionPage = 0; companionOpen = true },
                    onSystemControl = { companionPage = 1; companionOpen = true },
                    onPhoneTyping = { companionPage = 2; companionOpen = true },
                    onEasySetup = { setupOpen = true },
                    onLaunch = { if (!repository.launch(it)) message("This app cannot currently be opened.") },
                    onPin = { app -> scope.launch { runCatching { preferences.toggleFavourite(app.packageName) }.onFailure { message("Could not save favourite.") } } },
                    wallpapers = wallpapers, wallpaperBusy = wallpaperBusy,
                    onImportWallpaper = {
                        runCatching { picker.launch(Intent(this@MainActivity, za.co.rc.tv.ui.MediaImageBrowserActivity::class.java)) }
                            .onFailure { message("Image browser could not be opened.") }
                    },
                    onRemoveWallpaper = { id ->
                        runCatching { preferences.removeWallpaper(id); wallpaperStore.remove(id); wallpapers = wallpaperStore.list() }
                            .onFailure { message("Could not remove wallpaper.") }.isSuccess
                    },
                    onRotation = { value -> saveWallpaperSetting { preferences.setRotation(value) } },
                    onRotationInterval = { value -> saveWallpaperSetting { preferences.setRotationMinutes(value) } },
                    onRotationPool = { id -> saveWallpaperSetting { preferences.toggleRotationWallpaper(id) } },
                    onTransition = { value -> saveWallpaperSetting { preferences.setWallpaperTransition(value) } },
                    onRotateWallpaper = { runCatching { preferences.advanceWallpaper(wallpapers.map { it.id }) }.onFailure { message("Could not rotate wallpaper.") }; Unit },
                    onApplyWallpaper = { id -> runCatching { preferences.setWallpaper(id) }
                        .onFailure { message("Could not apply wallpaper. Please try again.") }.isSuccess },
                    onSaveOrder = { order -> runCatching { preferences.reorderFavourites(order) }
                        .onFailure { message("Could not save order. Favourites may have changed; try again.") }.isSuccess },
                    onRetry = { refresh++ }, onChooseHome = { selectHome() }, onRestoreHome = { openSettings() },
                    onAndroidSettings = { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }.onFailure { message("Settings unavailable.") } },
                    onAccent = { value -> scope.launch { preferences.setAccent(value) } },
                    onFontStyle = { value -> scope.launch { preferences.setFontStyle(value) } },
                    onTextSize = { value -> scope.launch { preferences.setTextSize(value) } },
                    onWallpaperVisibility = { value -> scope.launch { preferences.setWallpaperVisibility(value) } },
                    onResetAppearance = { scope.launch { preferences.resetAppearance() } },
                    onTheme = { value -> scope.launch { runCatching { preferences.setTheme(value) }.onFailure { message("Could not save theme.") } } },
                    onMotion = { value -> scope.launch { runCatching { preferences.setMotion(value) }.onFailure { message("Could not save motion preference.") } } },
                    onReducedMotion = { scope.launch { runCatching { preferences.setReducedMotion(!prefs.reducedMotion) }.onFailure { message("Could not save preference.") } } }
                )
                if (setupOpen) za.co.rc.tv.ui.SetupWizard(companion, { selectHome() }, { openSettings() }) {
                    setupPrefs.edit().putBoolean("seen", true).apply(); setupOpen = false
                }
                if (companionOpen) za.co.rc.tv.companion.CompanionSettings(companion, companionPage) { companionOpen = false }
                if (offerOtherApps) AlertDialog(onDismissRequest = { offerOtherApps = false }, title = { Text("Phone connected successfully") },
                    text = { Text("Use your phone in YouCine and other supported apps? Complete the Android permission setup once. Your physical remote keeps working.") },
                    confirmButton = { za.co.rc.tv.ui.FocusButton("Set up other apps", "Minimal") { offerOtherApps = false; companionPage = 1; companionOpen = true } },
                    dismissButton = { za.co.rc.tv.ui.FocusButton("Later", "Minimal") { offerOtherApps = false } })
                za.co.rc.tv.companion.CompanionApproval(companion)
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
