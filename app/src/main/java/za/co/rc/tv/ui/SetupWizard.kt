package za.co.rc.tv.ui

import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import za.co.rc.tv.companion.*
import za.co.rc.tv.core.*

/** Optional ten-step setup; Android authority always requires explicit system consent. */
@Composable fun SetupWizard(server: CompanionServer, chooseHome: () -> Unit, restoreHome: () -> Unit, finish: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SetupSession(context) }
    val preferences = remember { Preferences(context) }
    val savedPrefs by preferences.state.collectAsState(initial = null)
    val prefs = savedPrefs ?: LauncherPreferences()
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(session.step) }
    var phone by remember { mutableStateOf(session.phone) }
    var otherApps by remember { mutableStateOf(session.otherApps) }
    var typing by remember { mutableStateOf(session.typing) }
    var message by remember { mutableStateOf("") }
    var home by remember { mutableStateOf(HomeIntegration.inspect(context)) }
    var interception by remember { mutableStateOf(RecoverySnapshot.interceptionEnabled(context)) }
    var observed by remember { mutableStateOf(session.homeObserved) }
    var currentIme by remember { mutableStateOf("") }
    var enabledIme by remember { mutableStateOf(false) }
    var pairing by remember { mutableStateOf<Int?>(null) }
    var recovery by remember { mutableStateOf(false) }
    var consent by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }
    val access by RemoteRuntime.access.collectAsState()
    val connection by server.state.collectAsState()
    var connectionBaseline by remember { mutableLongStateOf(connection.connectionEvent) }
    var phoneVerified by remember { mutableStateOf(false) }
    fun checkpoint(action: () -> Unit): Boolean = runCatching(action).onFailure { message = it.message ?: "Could not save. Try again." }.isSuccess
    fun refresh() {
        home = HomeIntegration.inspect(context); interception = RecoverySnapshot.interceptionEnabled(context); observed = session.homeObserved
        enabledIme = context.getSystemService(InputMethodManager::class.java).enabledInputMethodList.any { it.packageName == context.packageName }
        currentIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycle.addObserver(observer); refresh(); onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { checkpoint { session.begin() }; runCatching { RecoverySnapshot.capture(context) }.onFailure { message = "Recovery observation could not be saved." } }
    LaunchedEffect(connection.connectionEvent) {
        if (connection.connectionEvent > connectionBaseline) { phoneVerified = true; connectionBaseline = connection.connectionEvent }
    }
    fun move(target: SetupStep, skipped: Boolean = false) { if (checkpoint { session.move(target, skipped) }) { step = target; message = "" } }
    fun save(action: suspend () -> Unit) { scope.launch { runCatching { action() }.onSuccess { message = "Saved" }.onFailure { message = "Could not save preference. Try again." } } }
    fun features() { checkpoint { session.features(phone, otherApps, typing) } }
    fun system(action: String) { openSystemDestination(context, SystemDestination("setup", "Android settings", listOf(action), "Open the relevant Android settings page")) }
    val pause = { if (checkpoint { session.pause() }) finish() }
    if (recovery) { RestoreLauncherWizard { recovery = false }; return }
    pairing?.let { page -> CompanionSettings(server, page) { pairing = null; refresh() }; return }
    if (consent) {
        AlertDialog(onDismissRequest = { consent = false }, title = { Text("Allow phone control in other apps?") },
            text = { Text("Approved phones can control supported apps and send non-password text with RC TV Keyboard. Android Accessibility can inspect the active screen. Screen content is not stored or sent to your phone. Confirm using the physical remote. Pairing alone does not grant this access.") },
            confirmButton = { FocusButton("Allow and open Android", "Minimal") { RemoteRuntime.setOptIn(context, true); consent = false; system(Settings.ACTION_ACCESSIBILITY_SETTINGS) } },
            dismissButton = { FocusButton("Cancel", "Minimal") { consent = false } })
        return
    }
    if (savedPrefs == null) {
        GuidedPage("Loading saved preferences", 0, SetupStep.entries.size, pause, pause, "Loading…", {}) {
            Text("Your existing choices are loading. No defaults will be written.")
        }
        return
    }
    val homeReady = SetupVerification.homeVerified(home.resolvedPackage == context.packageName, observed, interception)
    GuidedPage(step.title, step.ordinal, SetupStep.entries.size,
        back = { if (step.ordinal == 0) pause() else move(SetupStep.entries[step.ordinal - 1]) }, close = pause,
        nextLabel = if (step == SetupStep.COMPLETE) "Start watching" else "Continue",
        next = { if (step == SetupStep.COMPLETE) { if (checkpoint { session.complete() }) finish() } else move(SetupStep.entries[step.ordinal + 1]) }) {
        if (message.isNotBlank()) Text(message, color = LocalAppearance.current.accent)
        when (step) {
            SetupStep.WELCOME -> {
                Text("Your favourite apps, Cinematic Glass style and a phone remote. Skip optional steps without changing current choices.")
                Text("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE}")
                FeatureStatus("Home selection", home.roleAvailable || home.homeSettingsAvailable, "A selection screen is not proof that physical Home uses RC TV. App mode remains available.")
                if (context.getSystemService(android.app.ActivityManager::class.java).isLowRamDevice) Text("Low-memory device detected. Minimal motion is recommended.")
            }
            SetupStep.APPEARANCE -> {
                Text("Selections save immediately. Rerunning setup retains your choices.")
                AppearanceSettings(prefs, "Minimal", { save { preferences.setAccent(it) } }, { save { preferences.setFontStyle(it) } },
                    { save { preferences.setTextSize(it) } }, { save { preferences.setWallpaperVisibility(it) } }, { save { preferences.resetAppearance() } })
                Text("Layout", style = MaterialTheme.typography.titleMedium)
                SettingsOptions { listOf("Cinematic", "Minimalist").forEach { value -> FocusButton(value, "Minimal", selected = prefs.theme == value) { save { preferences.setTheme(value) } } } }
                Text("Bundled wallpaper · ${prefs.wallpaper}")
                SettingsOptions { bundledWallpapers.forEach { image -> FocusButton(image.title, "Minimal", selected = prefs.wallpaper == image.id) { save { preferences.setWallpaper(image.id) } } } }
                Text("Imported images, online galleries and rotation remain in Settings → Appearance → Choose wallpaper.")
                SettingsOptions { listOf("Minimal", "Balanced", "Cinematic").forEach { value -> FocusButton("Motion: $value", "Minimal", selected = prefs.motion == value) { save { preferences.setMotion(value) } } } }
                FocusButton("Reduced motion: ${if (prefs.reducedMotion) "On" else "Off"}", "Minimal") { save { preferences.setReducedMotion(!prefs.reducedMotion) } }
            }
            SetupStep.FAVOURITES -> {
                FavouriteSettings(preferences, prefs)
            }
            SetupStep.FEATURES -> {
                Text("Core Home needs none of these permissions. Enable only features you want.")
                FocusButton("Phone remote: ${if (phone) "On" else "Off"}", "Minimal") { phone = !phone; features() }
                FocusButton("Control other apps: ${if (otherApps) "On" else "Off"}", "Minimal") { otherApps = !otherApps; features() }
                FocusButton("Phone typing: ${if (typing) "On" else "Off"}", "Minimal") { typing = !typing; features() }
            }
            SetupStep.PERMISSIONS -> {
                if (otherApps || typing) {
                    FeatureStatus("Other-app control", access.optedIn && access.connected, "RC TV opt-in: ${access.optedIn}. Android service connected: ${access.connected}.")
                    FocusButton("Set up other-app control", "Minimal") { consent = true }
                }
                if (typing) {
                    FeatureStatus("RC TV Keyboard", enabledIme && currentIme.startsWith("${context.packageName}/"), "Enabled: $enabledIme. Selected: ${currentIme.startsWith("${context.packageName}/")}.")
                    FocusButton("Enable keyboard in Android", "Minimal") { system(Settings.ACTION_INPUT_METHOD_SETTINGS) }
                    FocusButton("Choose keyboard", "Minimal") { context.getSystemService(InputMethodManager::class.java).showInputMethodPicker() }
                    FocusButton("Try keyboard", "Minimal") { context.startActivity(Intent(context, za.co.rc.tv.keyboard.KeyboardPreviewActivity::class.java)) }
                }
                if (!otherApps && !typing) Text("No additional permissions needed for your choices.")
                FocusButton("Check again", "Minimal") { refresh() }
            }
            SetupStep.PHONE -> {
                FeatureStatus("Phone connection", phoneVerified, "${connection.devices.size} approved phone(s). Approval and live connection are different.")
                FocusButton("Connect or manage phones", "Minimal") { server.start(); pairing = 0 }
                if (!phone) Text("Phone remote was not selected. You can skip this step.")
            }
            SetupStep.HOME -> {
                Text(home.description(context.packageName))
                Text("This wizard never disables system launcher packages. A role selection does not prove effective Home routing.")
                FocusButton("Choose RC TV in Android", "Minimal", onClick = chooseHome)
                FocusButton("Open Home settings", "Minimal", onClick = restoreHome)
                FocusButton("Check again", "Minimal") { refresh() }
            }
            SetupStep.VERIFY -> {
                FeatureStatus("Physical Home", homeReady && confirmed, if (interception) "Home-button interception is enabled and can mask default Home. Verification is incomplete. This wizard will not turn it off." else if (observed) "A native Home launch was observed. Confirm physical Home returned here." else "Start the test, then press physical Home. An explicit app launch is not proof.")
                FocusButton("Start Home test", "Minimal") { if (checkpoint { session.startHomeTest() }) { observed = false; confirmed = false; context.startActivity(Intent(context, za.co.rc.tv.HomeVerificationActivity::class.java)) } }
                if (homeReady) FocusButton("Yes, physical Home returned here", "Minimal") { confirmed = true }
                FocusButton("Check again", "Minimal") { refresh() }
                Text("Restart verification is pending until the owner tests a normal restart. No reboot is performed.")
            }
            SetupStep.RECOVERY -> {
                Text("Keep RC TV and the original launcher installed. Home selection cannot restore a disabled package.")
                Text("Independent rescue is not verified. Package-changing automation remains unavailable.")
                FocusButton("Review original-launcher recovery", "Minimal") { recovery = true }
                FocusButton("Open Android Settings", "Minimal") { system(Settings.ACTION_SETTINGS) }
            }
            SetupStep.COMPLETE -> {
                FeatureStatus("RC TV", true, if (homeReady && confirmed) "Physical Home confirmed this session. Restart check pending." else "Ready to use. Default Home is not fully verified; open RC TV from Apps if needed.")
                Text("Skipped steps: ${session.skipped().joinToString().ifBlank { "None recorded" }}")
                Text("Settings → System & Recovery → Easy setup lets you resume or rerun without resetting preferences.")
            }
        }
        if (step != SetupStep.COMPLETE) FocusButton("Skip this step", "Minimal") { move(SetupStep.entries[step.ordinal + 1], skipped = true) }
    }
}
