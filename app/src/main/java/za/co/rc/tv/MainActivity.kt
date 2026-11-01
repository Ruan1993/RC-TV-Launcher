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
    private var experimentHomeRequest by mutableIntStateOf(0)
    private var homeKeySettings by mutableStateOf(false)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == HomeKeyExperimentService.ACTION_HOME || intent.action == HomeKeyExperimentService.ACTION_SETTINGS) {
            homeKeySettings = intent.action == HomeKeyExperimentService.ACTION_SETTINGS
            screensaverVisible = false
            lastInteraction = android.os.SystemClock.elapsedRealtime()
            experimentHomeRequest++
        }
    }
    private var lastInteraction by mutableLongStateOf(android.os.SystemClock.elapsedRealtime())
    private var activityResumed by mutableStateOf(false)
    private var activityFocused by mutableStateOf(false)
    private var screensaverVisible by mutableStateOf(false)
    override fun onUserInteraction() {
        super.onUserInteraction()
        lastInteraction = android.os.SystemClock.elapsedRealtime()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        activityFocused = hasFocus
        if (hasFocus) lastInteraction = android.os.SystemClock.elapsedRealtime()
    }
    private var remoteAction: ((String, String) -> Boolean)? = null
    private val companion by lazy { za.co.rc.tv.companion.RemoteRuntime.server(this) }
    override fun onResume() {
        super.onResume()
        activityResumed = true
        lastInteraction = android.os.SystemClock.elapsedRealtime()
        za.co.rc.tv.companion.RemoteRuntime.foreground = { key, text ->
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && (window.decorView.hasWindowFocus() || screensaverVisible || key == "CLOSE_SETUP") && remoteAction?.invoke(key, text) == true
        }
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
        activityResumed = false
        screensaverVisible = false
        za.co.rc.tv.companion.RemoteRuntime.pause(this)
        RenderDiagnostics.snapshot()
        super.onPause()
    }
    private val repository by lazy { AppRepository(this) }
    private val wallpaperStore by lazy { WallpaperStore(this) }
    private val preferences by lazy { Preferences(this) }
    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    private fun openSettings() {
        val intent = HomeIntegration.homeSettings(this)
        if (intent == null) {
            message("Home selection is unavailable here. Check Android Settings → Apps → Default apps. The original launcher remains installed.")
            openSystemDestination(this, androidSettingsDestination)
        } else runCatching { homeSelection.launch(intent) }.onFailure {
            message("Home selection could not open. Choose the original launcher in Android Settings if available.")
            openSystemDestination(this, androidSettingsDestination)
        }
    }
    private val homeSelection = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        message(HomeIntegration.inspect(this).description(packageName))
    }
    private fun selectHome() {
        val state = HomeIntegration.inspect(this)
        val roles = getSystemService(RoleManager::class.java)
        val intent = runCatching { if (state.roleAvailable && !state.roleHeld) roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
            else HomeIntegration.homeSettings(this) }.getOrNull()
        if (intent == null) message("This firmware does not expose supported Home selection. Continue launching RC TV as an app; Xiaomi Home is unchanged.")
        else runCatching { homeSelection.launch(intent) }.onFailure { message("Home selection is unavailable on this firmware. Xiaomi's launcher remains installed.") }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == HomeKeyExperimentService.ACTION_HOME || intent.action == HomeKeyExperimentService.ACTION_SETTINGS) {
            homeKeySettings = intent.action == HomeKeyExperimentService.ACTION_SETTINGS
            experimentHomeRequest++
        }
        val suggestSetup = SetupProgress.offerOnFirstOpen(getSharedPreferences("easy-setup", MODE_PRIVATE).getBoolean("seen", false),
            java.io.File(filesDir, "datastore/launcher.preferences_pb").exists())
        setContent {
            val prefs by preferences.state.collectAsStateWithLifecycle(LauncherPreferences())
            za.co.rc.tv.ui.LauncherAppearance(prefs) {
                var discovery by remember { mutableStateOf(AppDiscoveryState()) }
                val homeContentReady = za.co.rc.tv.ui.rememberHomeContentDrawn(discovery.loaded)
                var section by rememberSaveable { mutableStateOf("Home") }
                var query by rememberSaveable { mutableStateOf("") }
                val setupPrefs = remember { getSharedPreferences("easy-setup", MODE_PRIVATE) }
                var setupOpen by rememberSaveable { mutableStateOf(suggestSetup) }
                var companionOpen by remember { mutableStateOf(false) }
                var companionPage by remember { mutableIntStateOf(0) }
                var offerOtherApps by remember { mutableStateOf(false) }
                LaunchedEffect(experimentHomeRequest) {
                    if (experimentHomeRequest > 0) {
                        section = if (homeKeySettings) "Settings" else "Home"
                        setupOpen = false; companionOpen = false; offerOtherApps = false
                    }
                }
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
                        lastInteraction = android.os.SystemClock.elapsedRealtime()
                        remoteInputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
                        if (screensaverVisible) { screensaverVisible = false; true } else when (key) {
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
                DisposableEffect(repository) {
                    val receiver = object : android.content.BroadcastReceiver() {
                        override fun onReceive(context: android.content.Context?, intent: Intent?) {
                            val packages = intent?.data?.schemeSpecificPart?.let { arrayOf(it) }
                                ?: intent?.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST).orEmpty()
                            if (packages.isNotEmpty()) {
                                packages.forEach(repository::invalidateIcons)
                                iconGeneration++
                                refresh++
                            }
                        }
                    }
                    val packages = android.content.IntentFilter().apply {
                        addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
                        addAction(Intent.ACTION_PACKAGE_CHANGED); addAction(Intent.ACTION_PACKAGE_REPLACED)
                        addDataScheme("package")
                    }
                    val availability = android.content.IntentFilter().apply {
                        addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE); addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)
                        addAction(Intent.ACTION_PACKAGES_SUSPENDED); addAction(Intent.ACTION_PACKAGES_UNSUSPENDED)
                    }
                    // Package broadcasts are protected system broadcasts. Keep observing while paused,
                    // so returning from an installer or Android Settings refreshes just the affected icons.
                    androidx.core.content.ContextCompat.registerReceiver(this@MainActivity, receiver, packages, androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
                    androidx.core.content.ContextCompat.registerReceiver(this@MainActivity, receiver, availability, androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
                    onDispose { unregisterReceiver(receiver) }
                }
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
                val saverBlocked = section != "Home" || setupOpen || companionOpen || offerOtherApps || companionState.pending != null
                val dismissSaver = { screensaverVisible = false; lastInteraction = android.os.SystemClock.elapsedRealtime() }
                LaunchedEffect(companionState.pending) {
                    if (companionState.pending != null) dismissSaver()
                }
                LaunchedEffect(prefs.screensaverProvider) {
                    if (prefs.screensaverProvider == ANDROID_SCREENSAVER) dismissSaver()
                }
                LaunchedEffect(lastInteraction, activityResumed, activityFocused, prefs.screensaverMinutes, prefs.screensaverProvider, saverBlocked, screensaverVisible) {
                    if (prefs.screensaverProvider == RC_SCREENSAVER && !screensaverVisible && !saverBlocked && activityResumed && activityFocused) {
                        val wait = screensaverDelay(prefs.screensaverMinutes, lastInteraction, android.os.SystemClock.elapsedRealtime())
                        if (wait != null) {
                            kotlinx.coroutines.delay(wait)
                            // One check at the deadline. Active audio suppresses this idle session;
                            // the next interaction or resume starts a fresh one. No polling service.
                            val audio = getSystemService(android.media.AudioManager::class.java)
                            val power = getSystemService(android.os.PowerManager::class.java)
                            if (mayShowScreensaver(activityResumed, activityFocused, power.isInteractive,
                                    audio.isMusicActive, saverBlocked)) screensaverVisible = true
                        }
                    }
                }
                LaunchedEffect(lifecycle, refresh) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        discovery = refreshDiscovery(discovery, { repository.discover() }) {
                            android.util.Log.e("RcDiscovery", "Installed app discovery failed", it)
                        }
                        android.util.Log.d("RcDiscovery", "Refresh complete: ${discovery.apps.size} apps, error=${discovery.error != null}")
                    }
                }
                LaunchedEffect(lifecycle, homeContentReady) {
                    if (homeContentReady) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        android.util.Log.i("RcStartup", "Optional resume work started uptime=${android.os.SystemClock.uptimeMillis()}")
                        companion.resume()
                        wallpapers = runCatching { wallpaperStore.list() }.getOrElse { message("Wallpaper library unavailable."); bundledWallpapers }
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
                CompositionLocalProvider(za.co.rc.tv.ui.LocalHomeContentReady provides homeContentReady) {
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
                        runCatching { preferences.removeWallpaper(id, OnlineWallpaperStorage.isOnline(this@MainActivity, id)); wallpaperStore.remove(id); wallpapers = wallpaperStore.list() }
                            .onFailure { message(it.message ?: "Could not remove wallpaper.") }.isSuccess
                    },
                    onRotation = { value -> saveWallpaperSetting { preferences.setRotation(value) } },
                    onRotationInterval = { value -> saveWallpaperSetting { preferences.setRotationMinutes(value) } },
                    onRotationPool = { id -> saveWallpaperSetting { preferences.toggleRotationWallpaper(id) } },
                    onTransition = { value -> saveWallpaperSetting { preferences.setWallpaperTransition(value) } },
                    onRotateWallpaper = { runCatching { preferences.advanceWallpaper(wallpapers.map { it.id }) }.onFailure { message("Could not rotate wallpaper.") }; Unit },
                    onApplyWallpaper = { id -> runCatching { preferences.setWallpaper(id) }
                        .onFailure { message("Could not apply wallpaper. Please try again.") }.isSuccess },
                    onOnlineApply = { image, bitmap ->
                        val id = OnlineWallpaperStorage(this@MainActivity).save(image, bitmap)
                        preferences.setWallpaper(id)
                        wallpapers = runCatching { wallpaperStore.list() }.getOrElse {
                            wallpapers.filterNot { it.id == id } + WallpaperOption(id, image.title.take(40), image.attribution)
                        }
                        id
                    },
                    onOnlineSave = { image, bitmap ->
                        val id = OnlineWallpaperStorage(this@MainActivity).save(image, bitmap)
                        wallpapers = runCatching { wallpaperStore.list() }.getOrElse {
                            wallpapers.filterNot { it.id == id } + WallpaperOption(id, image.title.take(40), image.attribution)
                        }
                        id
                    },
                    onAddRotation = { ids ->
                        check(ids.all { id -> wallpapers.any { it.id == id && it.attribution != null } }) { "A selected image is no longer available." }
                        preferences.addRotationWallpapers(ids)
                    },
                    onSaveOrder = { order -> runCatching { preferences.reorderFavourites(order) }
                        .onFailure { message("Could not save order. Favourites may have changed; try again.") }.isSuccess },
                    onRetry = { refresh++ }, onChooseHome = { selectHome() }, onRestoreHome = { openSettings() },
                    onWifiSettings = { za.co.rc.tv.core.openConnectionSettings(this@MainActivity,false) },
                    onBluetoothSettings = { za.co.rc.tv.core.openConnectionSettings(this@MainActivity,true) },
                    onAndroidSettings = { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }.onFailure { message("Settings unavailable.") } },
                    onAccent = { value -> scope.launch { preferences.setAccent(value) } },
                    onFontStyle = { value -> scope.launch { preferences.setFontStyle(value) } },
                    onTextSize = { value -> scope.launch { preferences.setTextSize(value) } },
                    onWallpaperVisibility = { value -> scope.launch { preferences.setWallpaperVisibility(value) } },
                    onFocusAnimation = { value -> scope.launch { preferences.setFocusAnimation(value) } },
                    onScreensaverMinutes = { value -> scope.launch { preferences.setScreensaverMinutes(value) } },
                    onScreensaverProvider = { value -> scope.launch { preferences.setScreensaverProvider(value) } },
                    onScreensaverStyle = { value -> scope.launch { preferences.setScreensaverStyle(value) } },
                    onPreviewScreensaver = { screensaverVisible = true },
                    onResetAppearance = { scope.launch { preferences.resetAppearance() } },
                    onResetLauncherAppearance = { runCatching { preferences.resetLauncherAppearance() }
                        .onFailure { message("Could not reset appearance. Your other settings are unchanged.") }.isSuccess },
                    onTheme = { value -> scope.launch { runCatching { preferences.setTheme(value) }.onFailure { message("Could not save theme.") } } },
                    onMotion = { value -> scope.launch { runCatching { preferences.setMotion(value) }.onFailure { message("Could not save motion preference.") } } },
                    onReducedMotion = { scope.launch { runCatching { preferences.setReducedMotion(!prefs.reducedMotion) }.onFailure { message("Could not save preference.") } } }
                )
                }
                if (setupOpen) za.co.rc.tv.ui.SetupWizard(companion, { selectHome() }, { openSettings() }) {
                    setupPrefs.edit().putBoolean("seen", true).apply(); setupOpen = false
                }
                if (companionOpen) za.co.rc.tv.companion.CompanionSettings(companion, companionPage) { companionOpen = false }
                if (offerOtherApps) AlertDialog(onDismissRequest = { offerOtherApps = false }, title = { Text("Phone connected successfully") },
                    text = { Text("Use your phone in YouCine and other supported apps? Complete the Android permission setup once. Your physical remote keeps working.") },
                    confirmButton = { za.co.rc.tv.ui.FocusButton("Set up other apps", "Minimal") { offerOtherApps = false; companionPage = 1; companionOpen = true } },
                    dismissButton = { za.co.rc.tv.ui.FocusButton("Later", "Minimal") { offerOtherApps = false } })
                za.co.rc.tv.companion.CompanionApproval(companion)
                if (screensaverVisible && activityResumed) za.co.rc.tv.ui.Screensaver(prefs, clock, dismissSaver)
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
