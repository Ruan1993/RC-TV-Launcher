package za.co.rc.tv.ui

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import za.co.rc.tv.core.*

/** Guided, read-only framework. No package mutation or automatic authority changes. */
@Composable internal fun RestoreLauncherWizard(close: () -> Unit) {
    val context = LocalContext.current
    val progress = remember { context.getSharedPreferences("recovery-progress-v1", android.content.Context.MODE_PRIVATE) }
    var step by remember { mutableIntStateOf(progress.getInt("step", 0).coerceIn(0, 5)) }
    var candidates by remember { mutableStateOf(RecoverySnapshot.candidates(context)) }
    var selected by remember { mutableStateOf(progress.getString("target", null)) }
    var home by remember { mutableStateOf(HomeIntegration.inspect(context)) }
    var interception by remember { mutableStateOf(RecoverySnapshot.interceptionEnabled(context)) }
    var confirmed by remember { mutableStateOf(false) }
    var launched by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val target = candidates.firstOrNull { it.component == selected }
    fun refresh() {
        candidates = RecoverySnapshot.candidates(context); home = HomeIntegration.inspect(context)
        interception = RecoverySnapshot.interceptionEnabled(context)
        // Confirmation belongs to the current observed routing, not an old success screen.
        if (target == null || target.component != home.resolvedComponent || interception) confirmed = false
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { runCatching { RecoverySnapshot.capture(context) }.onFailure { message = "Could not save recovery observations. Keep RC TV installed." } }
    fun system(action: String, data: android.net.Uri? = null) {
        confirmed = false
        val intent = resolvedSystemSettings(context, listOf(action), data)
        if (intent == null || runCatching { context.startActivity(intent) }.isFailure) {
            openSystemDestination(context, androidSettingsDestination)
            message = "Direct screen unavailable. In Android Settings, inspect Apps or Default apps → Home."
        }
    }
    fun move(next: Int) {
        if (progress.edit().putInt("step", next).commit()) step = next else message = "Could not save recovery progress. Retry."
    }
    val resolvedTarget = target?.component != null && target.component == home.resolvedComponent
    val verified = SetupVerification.restored(target?.enabled == true, resolvedTarget, confirmed && launched, interception)
    val titles = listOf("Restore original launcher", "Review recovery", "Check Home-button handling", "Restore availability", "Choose original Home", "Verify original Home")
    GuidedPage(titles[step], step, titles.size, back = { if (step == 0) close() else move(step - 1) }, close = close,
        nextLabel = if (step == titles.lastIndex) "Done" else "Continue", next = { if (step == titles.lastIndex) { move(0); close() } else move(step + 1) }) {
        if (message.isNotBlank()) Text(message, color = LocalAppearance.current.accent)
        Text("RC TV settings, favourites, wallpapers and backups are preserved. No package changes are executed by this wizard.")
        when (step) {
            0 -> {
                Text(home.description(context.packageName))
                Text("Activation method: unknown / externally configured. Saved observations are not a pre-takeover backup.")
                Text("Choose the original system Home. A fallback or recovery activity is not a working launcher.")
                if (candidates.isEmpty()) Text("No original Home could be identified. Open Android Settings and inspect system apps.")
                candidates.forEach { item -> FocusButton("${item.label} · ${if (item.enabled) "Enabled" else "Disabled"}", "Minimal", selected = selected == item.component) {
                    if (progress.edit().putString("target", item.component).commit()) { selected = item.component; confirmed = false; launched = false }
                    else message = "Could not save selected launcher. Retry."
                } }
            }
            1 -> {
                Text(target?.let { "Selected: ${it.label}\n${it.component}\nObserved package override: ${it.override}" } ?: "No original launcher selected. Go Back to choose one.")
                Text("Keep both launchers installed. Package-changing recovery and phone ADB are unavailable until independent rescue is demonstrated and explicitly approved.")
                Text("On the documented Xiaomi 774 takeover, Google TV must be restored before Android TV Setup. Home selection cannot enable either disabled package. Use the device-specific recovery instructions with an authorised administrator.")
            }
            2 -> {
                FeatureStatus("Home-button interception", !interception, if (interception) "RC TV Home buttons are enabled. They can redirect Home independently of Android's selected launcher." else "No enabled RC Home-button interception service detected.")
                Text("Changing services is a separate explicit owner action. This wizard will not do it for you.")
                FocusButton("Review Home-button controls", "Minimal") { context.startActivity(Intent(context, za.co.rc.tv.HomeKeyExperimentActivity::class.java)) }
                FocusButton("Open Android Accessibility", "Minimal") { system(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
            }
            3 -> {
                FeatureStatus("Original launcher", target?.enabled == true, target?.let { if (it.enabled) "Enabled. Actual Home routing still needs verification." else "Installed but disabled. Inspect its Android app details for an Enable control; firmware may not offer one." } ?: "Select an original launcher first.")
                target?.let { item -> FocusButton("Open original app details", "Minimal") {
                    system(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${ComponentName.unflattenFromString(item.component)?.packageName}"))
                } }
                Text("If no supported Enable control exists, stop here and keep RC TV installed. No privileged operation is available in this build.")
            }
            4 -> {
                FocusButton("Open Android Home selection", "Minimal") { system(Settings.ACTION_HOME_SETTINGS) }
                Text("Select the original launcher. Opening this screen or granting a role is not proof that physical Home uses it.")
            }
            5 -> {
                FeatureStatus("Original Home", verified, if (verified) "Owner confirmed physical Home and Android resolves the selected original launcher. Restart verification is still pending." else "Needs enabled stock Home, matching resolution, no RC interception and owner confirmation after testing.")
                target?.takeIf { it.enabled }?.let { item -> FocusButton("Open original launcher to test", "Minimal") {
                    val component = ComponentName.unflattenFromString(item.component)
                    confirmed = false
                    launched = component != null && runCatching { context.startActivity(Intent(Intent.ACTION_MAIN).setComponent(component)) }.isSuccess
                    message = if (launched) "Now press physical Home. Reopen RC TV and confirm only if the original screen worked." else "Original launcher could not open. Keep RC TV installed."
                } }
                if (launched && resolvedTarget && !interception) FocusButton("I tested physical Home; the original works", "Minimal") { confirmed = true }
                Text("Keep RC TV installed by default. Uninstallation is deferred until external backup/image preservation and stronger recovery acceptance are available; Android uninstall would remove private RC data.")
            }
        }
        FocusButton("Check again", "Minimal") { refresh() }
        FocusButton("Open Android Settings", "Minimal") { system(Settings.ACTION_SETTINGS) }
    }
}
