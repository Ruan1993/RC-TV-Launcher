package za.co.rc.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp

internal data class SettingsCategory(val key: String, val title: String, val icon: String, val description: String)
internal val settingsCategories = listOf(
    SettingsCategory("appearance","Appearance","E","Wallpaper, colours, layout and motion"),
    SettingsCategory("apps","Home & Apps","A","Home launcher, favourites, media and app storage"),
    SettingsCategory("weather","Weather & Screensaver","W","Weather location, idle display and previews"),
    SettingsCategory("remote","Remote & Input","R","Phone pairing, keyboard and accessibility"),
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

@Composable internal fun ColumnScope.SettingsCategories(last: String, motion: String, choose: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    val input = LocalInputModeManager.current
    val selected = settingsCategoryKey(last) ?: settingsCategories.first().key
    LaunchedEffect(Unit) { withFrameNanos { }; input.requestInputMode(InputMode.Keyboard); focus.requestFocus() }
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Settings",style=MaterialTheme.typography.headlineLarge)
        settingsCategories.forEach { category ->
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                FocusButton(category.title,motion,Modifier.fillMaxWidth().then(if(category.key==selected) Modifier.focusRequester(focus) else Modifier),icon=category.icon,quiet=true) { choose(category.key) }
                Text(category.description,Modifier.padding(horizontal=12.dp),style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable internal fun ColumnScope.SettingsSubpage(key: String, motion: String, close: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val backFocus=remember { FocusRequester() }; val input=LocalInputModeManager.current
    LaunchedEffect(key) { withFrameNanos { }; input.requestInputMode(InputMode.Keyboard); backFocus.requestFocus() }
    Column(Modifier.fillMaxWidth().weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            FocusButton("Back",motion,Modifier.focusRequester(backFocus),onClick=close)
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
