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

internal data class SettingsCategory(val key: String, val title: String, val icon: String)
internal val settingsCategories = listOf(
    SettingsCategory("appearance","Appearance & Personalisation","E"),
    SettingsCategory("remote","Remote & Keyboard","R"),
    SettingsCategory("connections","Connections & Devices","R"),
    SettingsCategory("apps","Apps & Media","A"),
    SettingsCategory("weather","Weather & Widgets","W"),
    SettingsCategory("power","Screensaver & Power","T"),
    SettingsCategory("system","System & Recovery","···"))

@Composable internal fun ColumnScope.SettingsCategories(last: String, motion: String, choose: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    val input = LocalInputModeManager.current
    LaunchedEffect(Unit) { withFrameNanos { }; input.requestInputMode(InputMode.Keyboard); focus.requestFocus() }
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Settings",style=MaterialTheme.typography.headlineLarge)
        settingsCategories.forEach { category ->
            FocusButton(category.title,motion,Modifier.fillMaxWidth().then(if(category.key==last) Modifier.focusRequester(focus) else Modifier),icon=category.icon,quiet=true) { choose(category.key) }
        }
    }
}

@Composable internal fun ColumnScope.SettingsSubpage(key: String, motion: String, close: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val backFocus=remember { FocusRequester() }; val input=LocalInputModeManager.current
    LaunchedEffect(key) { withFrameNanos { }; input.requestInputMode(InputMode.Keyboard); backFocus.requestFocus() }
    Column(Modifier.fillMaxWidth().weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            FocusButton("Back",motion,Modifier.focusRequester(backFocus),onClick=close)
            Text(settingsCategories.first { it.key==key }.title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
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
