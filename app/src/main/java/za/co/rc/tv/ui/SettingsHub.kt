package za.co.rc.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp

internal data class SettingsCategory(val key: String, val title: String, val icon: String, val description: String)
internal val settingsCategories = listOf(
    SettingsCategory("appearance","Appearance","E","Wallpaper, colours, layout and motion"),
    SettingsCategory("apps","Home & Apps","A","Home launcher, favourites, media and app storage"),
    SettingsCategory("weather","Weather & Screensaver","W","Weather location, idle display and previews"),
    SettingsCategory("remote","Phone & Keyboard","R","Phone pairing, keyboard and accessibility"),
    SettingsCategory("connections","Connections & Devices","N","Wi-Fi, Bluetooth, casting, display and sound"),
    SettingsCategory("system","System & Recovery","···","Backup, setup, diagnostics, updates and power"),
    SettingsCategory("about","About RC TV","I","Launcher version and Android device information"))

// UI routes only: no stored launcher preferences or Android state are migrated.
internal fun settingsCategoryKey(key: String?): String? = when (key) {
    "launcher" -> "apps"
    "power" -> "weather"
    else -> key?.takeIf { candidate -> settingsCategories.any { it.key == candidate } }
}
internal data class SettingsReturnTarget(val page: String?, val entry: String?)
internal fun settingsBackTarget(page: String?) = if (page == "backup") SettingsReturnTarget("system", "backup")
    else SettingsReturnTarget(null, settingsCategoryKey(page))
internal fun settingsUtilityTarget(section: String): SettingsReturnTarget? = when (section) {
    "Apps" -> SettingsReturnTarget("apps", "apps")
    "Speed Test" -> SettingsReturnTarget("system", "speed")
    "Cast" -> SettingsReturnTarget("connections", "cast")
    else -> null
}
internal fun settingsEntryPage(entry: String): String? = when (entry) {
    "backup", "speed" -> "system"
    "wallpaper" -> "appearance"
    "apps" -> "apps"
    "cast" -> "connections"
    "weather-details" -> "weather"
    else -> null
}
internal fun systemDestinationCategory(key: String): String = when (key) {
    "about" -> "about"
    "display" -> "connections"
    "accessibility" -> "remote"
    "storage" -> "apps"
    else -> "system"
}

@Composable private fun ColumnScope.SettingsSubpage(key: String, motion: String, close: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            FocusButton("Back",motion,onClick=close)
            Text(if(key == "backup") "Backup & Restore" else settingsCategories.firstOrNull { it.key==settingsCategoryKey(key) }?.title ?: "Settings",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
        }
        val palette=LocalAppearance.current; val shape=RoundedCornerShape(22.dp)
        Column(Modifier.fillMaxWidth().weight(1f).clip(shape).background(palette.glass).border(1.dp,palette.border,shape)
            .verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun SettingsOptions(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp),content=content)
}

/** Persistent category rail; browsing categories never changes a preference or permission. */
@Composable internal fun ColumnScope.SettingsScaffold(page: String?, last: String, motion: String,
    browse: (String) -> Unit, enter: (String) -> Unit, close: () -> Unit,
    content: @Composable ColumnScope.(String) -> Unit) {
    val selected = if (page == "backup") "system" else settingsCategoryKey(page ?: last) ?: "appearance"
    val rail = remember { settingsCategories.associate { it.key to FocusRequester() } }
    val panel = remember { FocusRequester() }
    val savedPages = rememberSaveableStateHolder()
    val input = LocalInputModeManager.current
    LaunchedEffect(page == null) {
        withFrameNanos { }
        input.requestInputMode(InputMode.Keyboard)
        if (page == null) rail.getValue(selected).requestFocus()
    }
    CompositionLocalProvider(LocalSettingsControls provides true) {
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
        // Large type/short viewports retain a readable full-width page.
        val compact = maxWidth < 800.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            if (!compact || page == null) {
                Column(Modifier.then(if (compact) Modifier.fillMaxWidth() else Modifier.width(248.dp))
                    .fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Settings", style = MaterialTheme.typography.headlineMedium)
                    Text("Choose a category", style = MaterialTheme.typography.bodyMedium)
                    settingsCategories.forEach { category ->
                        FocusButton(category.title, motion, Modifier.fillMaxWidth().heightIn(min = 64.dp)
                            .focusRequester(rail.getValue(category.key))
                            .onFocusChanged { if (it.isFocused) browse(category.key) }
                            .onPreviewKeyEvent { event ->
                                if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) enter(category.key)
                                    true
                                } else false
                            }, selected = selected == category.key, quiet = true, icon = category.icon) { enter(category.key) }
                    }
                }
            }
            if (!compact || page != null) {
                Column(Modifier.weight(1f).fillMaxHeight().focusRequester(panel).focusRestorer().focusGroup()) {
                    savedPages.SaveableStateProvider(page ?: selected) {
                        SettingsSubpage(page ?: selected, motion, close) { content(page ?: selected) }
                    }
                }
                LaunchedEffect(page) { if (page != null) { withFrameNanos { }; panel.requestFocus() } }
            }
        }
    }
    }
}
