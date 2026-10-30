package za.co.rc.tv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import za.co.rc.tv.R
import za.co.rc.tv.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Ice = Color(0xFFC7E9E2)
private val Ink = Color(0xFF0B131B)
private val GlassShape = RoundedCornerShape(22.dp)

/** No backdrop blur or continuously running effects: safe for API 30 TV GPUs. */
@Composable
private fun Wallpaper(minimal: Boolean) {
    val context = LocalContext.current.applicationContext
    val wallpaper by produceState<android.graphics.Bitmap?>(null, minimal) {
        value = if (minimal) null else withContext(Dispatchers.IO) {
            android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.cinematic_horizon)
        }
    }
    Box(Modifier.fillMaxSize().background(Ink)) {
        if (!minimal && wallpaper != null) Image(wallpaper!!.asImageBitmap(), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Ink.copy(alpha = .9f), Ink.copy(alpha = .15f)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .3f), Ink.copy(alpha = .94f)))))
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(GlassShape).background(Ink.copy(alpha = .64f))
        .border(1.dp, Color.White.copy(alpha = .12f), GlassShape).padding(20.dp), content = content)
}

@Composable
private fun FocusButton(label: String, motion: String, modifier: Modifier = Modifier,
    selected: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused && motion != "Minimal") 1.025f else 1f,
        if (motion == "Minimal") snap() else spring(dampingRatio = .85f, stiffness = 550f), label = "buttonFocus")
    val tint by animateColorAsState(if (focused) Ice else if (selected) Color.White.copy(alpha = .18f) else Color.White.copy(alpha = .045f), animationSpec = if (motion == "Minimal") snap() else spring(stiffness = 600f), label = "buttonTint")
    Button(onClick, modifier.onFocusChanged { focused = it.isFocused }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .border(1.dp, if (focused) Ice else Color.White.copy(alpha = .10f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = tint,
            contentColor = if (focused) Ink else Color.White), contentPadding = PaddingValues(12.dp, 10.dp)) {
        if (label in listOf("H", "S", "A", "L", "···")) NavigationMark(label, if (focused) Ink else Ice)
        else Text(label, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun NavigationMark(mark: String, tint: Color) {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width; val h = size.height; val stroke = 1.6.dp.toPx()
        when (mark) {
            "H" -> {
                val path = Path().apply { moveTo(w*.12f,h*.45f); lineTo(w*.5f,h*.12f); lineTo(w*.88f,h*.45f); lineTo(w*.88f,h*.88f); lineTo(w*.60f,h*.88f); lineTo(w*.60f,h*.6f); lineTo(w*.4f,h*.6f); lineTo(w*.4f,h*.88f); lineTo(w*.12f,h*.88f); close() }
                drawPath(path, tint, style = Stroke(stroke))
            }
            "S" -> { drawCircle(tint, w*.28f, Offset(w*.42f,h*.42f), style = Stroke(stroke)); drawLine(tint, Offset(w*.63f,h*.63f), Offset(w*.9f,h*.9f), stroke) }
            "A" -> for (x in listOf(.12f,.58f)) for (y in listOf(.12f,.58f)) drawRect(tint, Offset(w*x,h*y), Size(w*.3f,h*.3f), style = Stroke(stroke))
            "L" -> { for (x in listOf(.18f,.4f,.7f)) drawLine(tint, Offset(w*x,h*.15f), Offset(w*(x+.05f),h*.88f), stroke) }
            else -> { drawCircle(tint, w*.3f, center, style = Stroke(stroke)); drawCircle(tint, w*.08f, center); for (i in 0..3) { val a = i * Math.PI / 2; drawLine(tint, Offset(center.x+(kotlin.math.cos(a)*w*.3).toFloat(),center.y+(kotlin.math.sin(a)*h*.3).toFloat()), Offset(center.x+(kotlin.math.cos(a)*w*.46).toFloat(),center.y+(kotlin.math.sin(a)*h*.46).toFloat()),stroke) } }
        }
    }
}

@Composable
private fun AppTile(app: LauncherApp, repository: AppRepository, generation: Int,
    pinned: Boolean, motion: String, onLaunch: () -> Unit, onPin: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused && motion != "Minimal") if (motion == "Cinematic") 1.055f else 1.035f else 1f,
        if (motion == "Minimal") snap() else spring(dampingRatio = .82f, stiffness = 500f), label = "tileFocus")
    val icon by produceState<android.graphics.Bitmap?>(null, app.packageName, generation) {
        value = null; value = repository.icon(app.packageName)
    }
    Column(Modifier.width(172.dp).padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Button(onLaunch, Modifier.fillMaxWidth().height(122.dp).onFocusChanged { focused = it.isFocused }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                if (focused && motion != "Minimal") drawRoundRect(
                    Brush.radialGradient(listOf(Ice.copy(alpha = .24f), Color.Transparent)),
                    topLeft = Offset(-8.dp.toPx(), -8.dp.toPx()),
                    size = Size(size.width + 16.dp.toPx(), size.height + 16.dp.toPx()),
                    cornerRadius = CornerRadius(26.dp.toPx()))
            }
            .border(if (focused) 2.dp else 1.dp, if (focused) Ice else Color.White.copy(alpha = .16f), GlassShape),
            shape = GlassShape, colors = ButtonDefaults.buttonColors(containerColor = if (focused) Color(0xCC29454C) else Color(0xB3192630)),
            contentPadding = PaddingValues(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (icon != null) Image(icon!!.asImageBitmap(), null, Modifier.size(44.dp))
                else Image(painterResource(R.drawable.rc_icon), null, Modifier.size(44.dp))
                Text(app.label, fontSize = 15.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        TextButton(onPin, Modifier.align(Alignment.CenterHorizontally).height(32.dp)) {
            Text(if (pinned) "Unpin" else "Pin", color = Ice.copy(alpha = .8f), fontSize = 12.sp)
        }
    }
}

@Composable
fun CinematicLauncher(
    discovery: AppDiscoveryState, repository: AppRepository, iconGeneration: Int,
    prefs: LauncherPreferences, section: String, onSection: (String) -> Unit,
    query: String, onQuery: (String) -> Unit, clock: String, homeStatus: String,
    onLaunch: (LauncherApp) -> Unit, onPin: (LauncherApp) -> Unit, onRetry: () -> Unit,
    onChooseHome: () -> Unit, onRestoreHome: () -> Unit, onAndroidSettings: () -> Unit,
    onTheme: (String) -> Unit, onMotion: (String) -> Unit, onReducedMotion: () -> Unit
) {
    val motion = if (prefs.reducedMotion) "Minimal" else prefs.motion
    var railFocused by remember { mutableStateOf(false) }
    val railWidth by animateDpAsState(if (railFocused) 146.dp else 76.dp,
        if (motion == "Minimal") snap() else spring(dampingRatio = 1f, stiffness = 600f), label = "railWidth")
    val visible = discovery.apps.filter { (section != "Search" || it.label.contains(query, true)) &&
        (section != "Home" || prefs.favourites.isEmpty() || it.packageName in prefs.favourites) }
    Box(Modifier.fillMaxSize()) {
        Wallpaper(prefs.theme == "Minimalist")
        Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            // Reserve the expanded rail width so focus never reflows the app shelf.
            Box(Modifier.width(146.dp).fillMaxHeight()) {
            Column(Modifier.width(railWidth).fillMaxHeight().onFocusChanged { railFocused = it.hasFocus }.focusGroup()
                .clip(GlassShape).background(Ink.copy(alpha = .72f)).border(1.dp, Color.White.copy(alpha = .12f), GlassShape)
                .padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("RC", color = Ice, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(6.dp, 15.dp))
                listOf("Home" to "H", "Search" to "S", "Apps" to "A", "Library" to "L", "Settings" to "···").forEach { (name, symbol) ->
                    Box(Modifier.semantics { contentDescription = name }) {
                        FocusButton(if (railFocused) name else symbol, motion, Modifier.fillMaxWidth(), selected = section == name) { onSection(name) }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("RC TV", fontSize = 10.sp, color = Color.White.copy(alpha = .45f), modifier = Modifier.padding(5.dp))
            }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("RC TV", fontSize = 20.sp, fontWeight = FontWeight.Light, letterSpacing = 3.sp, color = Color.White)
                        Text("Your world. In focus.", fontSize = 11.sp, color = Color.White.copy(alpha = .65f))
                    }
                    Text(clock, fontSize = 28.sp, fontWeight = FontWeight.Light, color = Color.White)
                }
                when (section) {
                    "Settings" -> GlassPanel(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                        Text("Make it yours", fontSize = 28.sp, fontWeight = FontWeight.Light)
                        Text("Appearance", color = Ice, modifier = Modifier.padding(top = 12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Cinematic", "Minimalist").forEach { theme -> FocusButton(theme, motion, selected = prefs.theme == theme) { onTheme(theme) } }
                        }
                        Text("Motion", color = Ice, modifier = Modifier.padding(top = 12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Minimal", "Balanced", "Cinematic").forEach { value -> FocusButton(value, motion, selected = prefs.motion == value) { onMotion(value) } }
                        }
                        FocusButton("Reduced motion: ${if (prefs.reducedMotion) "on" else "off"}", motion) { onReducedMotion() }
                        Text("Choose how much movement feels right. Reduced motion takes priority over the selected intensity.", fontSize = 12.sp, color = Color.White.copy(alpha = .65f))
                        HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = .12f))
                        Text("Launcher & recovery", fontSize = 20.sp)
                        Text(homeStatus, fontSize = 13.sp)
                        FocusButton("Android settings", motion) { onAndroidSettings() }
                        FocusButton("Choose default Home", motion) { onChooseHome() }
                        FocusButton("Open Home settings / restore", motion) { onRestoreHome() }
                        Text("Keep Xiaomi's original launcher installed and enabled. Restore it in Android Settings → Apps → Default apps → Home.", fontSize = 12.sp)
                    }
                    "Library" -> GlassPanel(Modifier.fillMaxWidth()) {
                        Text("A place for your stories", fontSize = 28.sp, fontWeight = FontWeight.Light)
                        Text("Local media and watchlists are planned. Your installed apps are ready now.", modifier = Modifier.padding(vertical = 16.dp))
                        FocusButton("Browse apps", motion) { onSection("Apps") }
                    }
                    else -> {
                        if (section == "Home") {
                            Spacer(Modifier.weight(1f))
                            Text(if (prefs.theme == "Minimalist") "Home" else "Quiet horizons.", fontSize = 44.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-1).sp, color = Color.White)
                            if (prefs.theme != "Minimalist") Text("BLUE HOUR  /  THE CINEMATIC COLLECTION", fontSize = 10.sp, letterSpacing = 2.sp, color = Ice.copy(alpha = .8f))
                        } else Text(section, fontSize = 28.sp, fontWeight = FontWeight.Light)
                        if (section == "Search") OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), label = { Text("Search installed apps") }, singleLine = true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (section == "Home" && prefs.favourites.isNotEmpty()) "Your favourites" else "Your applications", fontSize = 17.sp, color = Color.White)
                            Text("${visible.size} APPS", fontSize = 10.sp, letterSpacing = 1.sp, color = Ice.copy(alpha = .7f))
                        }
                        discovery.error?.let { Text(it, color = MaterialTheme.colorScheme.error); FocusButton("Retry discovery", motion) { onRetry() } }
                        if (!discovery.loaded) Text("Discovering apps…", color = Ice)
                        else if (visible.isEmpty()) Text("No matching applications. Try Apps or change your search.", color = Ice)
                        if (section == "Home") LazyRow(Modifier.fillMaxWidth().height(196.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(4.dp)) {
                            items(visible, key = { it.packageName }) { app -> AppTile(app, repository, iconGeneration, app.packageName in prefs.favourites, motion, { onLaunch(app) }, { onPin(app) }) }
                        } else LazyVerticalGrid(GridCells.Adaptive(172.dp), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(4.dp)) {
                            items(visible, key = { it.packageName }) { app -> AppTile(app, repository, iconGeneration, app.packageName in prefs.favourites, motion, { onLaunch(app) }, { onPin(app) }) }
                        }
                        Text("D-pad to explore    ·    Select to open    ·    Back to Home", fontSize = 10.sp, color = Color.White.copy(alpha = .5f))
                    }
                }
            }
        }
    }
}
