package za.co.rc.tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import za.co.rc.tv.core.*

data class AppearanceTokens(val accent: Color = Color(0xFFC7E9E2), val focus: Color = Color(0xFFD5F5EB),
    val halo: Color = Color(0xFFBDE8DE), val primary: Color = Color(0xFF9FE9D5),
    val glass: Color = Color(0xFF0B131B).copy(alpha = .64f), val border: Color = Color.White.copy(alpha = .12f), val highReadability: Boolean = false)
val LocalAppearance = staticCompositionLocalOf { AppearanceTokens() }
fun appearanceTokens(name: String): AppearanceTokens = when (name) {
    "Emerald Green" -> AppearanceTokens(Color(0xFF9EE8BE), Color(0xFFC0FFDC), Color(0xFF52CE94), Color(0xFF9EE8BE))
    "Ocean Blue" -> AppearanceTokens(Color(0xFFA9D9FF), Color(0xFFD0EAFF), Color(0xFF65B8F2), Color(0xFFA9D9FF))
    "Rose Pink" -> AppearanceTokens(Color(0xFFFFC4E1), Color(0xFFFFDFF0), Color(0xFFE784B6), Color(0xFFFFC4E1))
    "Ruby Red" -> AppearanceTokens(Color(0xFFFFB9BF), Color(0xFFFFDADF), Color(0xFFE65E70), Color(0xFFFFB9BF))
    "Midnight Black" -> AppearanceTokens(Color(0xFFDDE5F2), Color(0xFFF2F5FB), Color(0xFFB1BDD1), Color(0xFFDDE5F2), Color(0xFF03060B).copy(alpha = .84f))
    "Slate Grey" -> AppearanceTokens(Color(0xFFCFDCE8), Color(0xFFECF2F7), Color(0xFF9EAFBF), Color(0xFFCFDCE8), Color(0xFF22313D).copy(alpha = .76f))
    else -> AppearanceTokens()
}

@Composable fun LauncherAppearance(prefs: LauncherPreferences, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val palette = appearanceTokens(prefs.accent).copy(highReadability = prefs.fontStyle == "Bold / High Readability")
    val family = if (prefs.fontStyle == "Classic") FontFamily.Serif else FontFamily.SansSerif
    fun style(base: TextStyle) = base.copy(fontFamily = family, fontWeight = if (palette.highReadability) FontWeight.Bold else base.fontWeight)
    val base = LauncherTypography
    val typography = if (prefs.fontStyle == "Modern") base else base.copy(
        headlineLarge = style(base.headlineLarge), headlineMedium = style(base.headlineMedium),
        titleLarge = style(base.titleLarge), titleMedium = style(base.titleMedium), bodyLarge = style(base.bodyLarge),
        bodyMedium = style(base.bodyMedium), bodySmall = style(base.bodySmall), labelLarge = style(base.labelLarge),
        labelMedium = style(base.labelMedium), labelSmall = style(base.labelSmall))
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * textScale(prefs.textSize)), LocalAppearance provides palette) {
        MaterialTheme(typography = typography, colorScheme = darkColorScheme(primary = palette.primary, background = Color(0xFF09151F), surface = Color(0xFF182D39))) {
            CompositionLocalProvider(LocalContentColor provides Color.White, content = content)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun AppearanceSettings(prefs: LauncherPreferences, motion: String, accent: (String) -> Unit,
    font: (String) -> Unit, size: (String) -> Unit, visibility: (String) -> Unit, reset: () -> Unit) {
    Text("Accent theme", style = MaterialTheme.typography.titleLarge, color = LocalAppearance.current.accent)
    Text("Changes save automatically.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        accentThemes.forEach { FocusButton(it, motion, selected = prefs.accent == it) { accent(it) } }
    }
    Text("Font style", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { fontStyles.forEach { FocusButton(it, motion, selected = prefs.fontStyle == it) { font(it) } } }
    Text("Text size", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { textSizes.forEach { FocusButton(it, motion, selected = prefs.textSize == it) { size(it) } } }
    Text("Wallpaper visibility", style = MaterialTheme.typography.titleMedium)
    Text("Subtle darkens the wallpaper. Balanced keeps the original look. Vivid reveals more detail.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        wallpaperVisibilities.forEach { FocusButton(it, motion, selected = prefs.wallpaperVisibility == it) { visibility(it) } }
    }
    FocusButton("Reset visibility to Balanced", motion) { visibility("Balanced") }
    FocusButton("Reset to Original", motion, onClick = reset)
}
