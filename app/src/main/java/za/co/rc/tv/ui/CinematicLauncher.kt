package za.co.rc.tv.ui

import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
internal fun Wallpaper(minimal: Boolean, id: String, transition: String, motion: String, visibility: String) {
    SideEffect { RenderDiagnostics.wallpaperComposed() }
    val context = LocalContext.current.applicationContext
    val wallpaper by produceState<android.graphics.Bitmap?>(null, minimal, id, visibility) {
        value = if (minimal) null else withContext(Dispatchers.IO) {
            WallpaperCache.load(context, id, visibility)
        }
    }
    Box(Modifier.fillMaxSize().background(Ink)) {
        if (!minimal) AnimatedContent(wallpaper, Modifier.fillMaxSize(), transitionSpec = {
            val duration = if (motion == "Minimal" || transition == "Instant") 0 else if (motion == "Cinematic") 320 else 180
            if (transition == "Gentle Slide" && duration > 0)
                (fadeIn(tween(duration)) + slideInHorizontally(tween(duration)) { it / 40 }) togetherWith
                    (fadeOut(tween(duration)) + slideOutHorizontally(tween(duration)) { -it / 40 })
            else fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))
        }, label = "wallpaperTransition") { bitmap ->
            bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalAppearance.current
    Column(modifier.clip(GlassShape).background(palette.glass)
        .border(1.dp, palette.border, GlassShape).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
internal fun FocusButton(label: String, motion: String, modifier: Modifier = Modifier,
    selected: Boolean = false, quiet: Boolean = false, icon: String? = null, prominent: Boolean = false, onClick: () -> Unit) {
    val Ice = LocalAppearance.current.accent
    var focused by remember { mutableStateOf(false) }
    val tint by animateColorAsState(if (focused) if (quiet) Ice.copy(alpha = .22f) else Ice else if (selected) Color.White.copy(alpha = if (quiet) .12f else .18f) else Color.White.copy(alpha = .045f), animationSpec = if (motion == "Minimal") snap() else spring(stiffness = 600f), label = "buttonTint")
    Button(onClick, modifier.heightIn(min = 48.dp).onFocusChanged { focused = it.isFocused }
        .cinematicFocus()
        .background(if(prominent) Ice.copy(alpha=.12f) else Color.Transparent,RoundedCornerShape(14.dp))
        .drawBehind { drawRoundRect(tint, cornerRadius = CornerRadius(14.dp.toPx())) }
        .border(if(prominent) 1.5.dp else 1.dp, if (focused || prominent) Ice.copy(alpha=if(focused) 1f else .5f) else Color.White.copy(alpha = .10f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
            contentColor = if (focused && !quiet) Ink else Color.White), contentPadding = PaddingValues(12.dp, 10.dp)) {
        if (icon != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NavigationMark(icon, Ice)
            if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        } else if (label in listOf("H", "S", "A", "L", "···")) NavigationMark(label, if (focused && !quiet) Ink else Ice)
        else Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun NavigationMark(mark: String, tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height; val stroke = 1.6.dp.toPx()
        when (mark) {
            "H" -> {
                val path = Path().apply { moveTo(w*.12f,h*.45f); lineTo(w*.5f,h*.12f); lineTo(w*.88f,h*.45f); lineTo(w*.88f,h*.88f); lineTo(w*.60f,h*.88f); lineTo(w*.60f,h*.6f); lineTo(w*.4f,h*.6f); lineTo(w*.4f,h*.88f); lineTo(w*.12f,h*.88f); close() }
                drawPath(path, tint, style = Stroke(stroke))
            }
            "S" -> { drawCircle(tint, w*.28f, Offset(w*.42f,h*.42f), style = Stroke(stroke)); drawLine(tint, Offset(w*.63f,h*.63f), Offset(w*.9f,h*.9f), stroke) }
            "A" -> for (x in listOf(.12f,.58f)) for (y in listOf(.12f,.58f)) drawRect(tint, Offset(w*x,h*y), Size(w*.3f,h*.3f), style = Stroke(stroke))
            "L" -> { for (x in listOf(.18f,.4f,.7f)) drawLine(tint, Offset(w*x,h*.15f), Offset(w*(x+.05f),h*.88f), stroke) }
            "C" -> {
                // Open lower-left corner with two broadcast arcs, matching the navbar stroke.
                val screen = Path().apply {
                    moveTo(w*.12f,h*.43f); lineTo(w*.12f,h*.16f); lineTo(w*.9f,h*.16f)
                    lineTo(w*.9f,h*.78f); lineTo(w*.57f,h*.78f)
                }
                drawPath(screen, tint, style = Stroke(stroke))
                val origin = Offset(w*.12f,h*.84f)
                drawCircle(tint, w*.045f, origin)
                for (radius in listOf(.20f,.37f)) drawArc(tint, -90f, 90f, false,
                    Offset(origin.x-w*radius, origin.y-h*radius), Size(w*radius*2,h*radius*2), style = Stroke(stroke))
            }
            "N" -> {
                drawCircle(tint,w*.055f,Offset(w*.5f,h*.83f))
                for(radius in listOf(.23f,.4f)) drawArc(tint,225f,90f,false,Offset(w*(.5f-radius),h*(.83f-radius)),Size(w*radius*2,h*radius*2),style=Stroke(stroke))
            }
            "B" -> {
                val path=Path().apply { moveTo(w*.43f,h*.08f); lineTo(w*.75f,h*.32f); lineTo(w*.23f,h*.73f); moveTo(w*.23f,h*.27f); lineTo(w*.75f,h*.68f); lineTo(w*.43f,h*.92f); lineTo(w*.43f,h*.08f) }
                drawPath(path,tint,style=Stroke(stroke))
            }
            "E" -> {
                drawCircle(tint,w*.34f,Offset(w*.5f,h*.5f),style=Stroke(stroke))
                drawCircle(tint,w*.08f,Offset(w*.38f,h*.35f)); drawCircle(tint,w*.06f,Offset(w*.65f,h*.45f))
            }
            "W" -> {
                drawCircle(tint,w*.19f,Offset(w*.5f,h*.5f),style=Stroke(stroke))
                for(i in 0..7) { val a=i*Math.PI/4; drawLine(tint,Offset(w*(.5f+.29f*kotlin.math.cos(a).toFloat()),h*(.5f+.29f*kotlin.math.sin(a).toFloat())),Offset(w*(.5f+.41f*kotlin.math.cos(a).toFloat()),h*(.5f+.41f*kotlin.math.sin(a).toFloat())),stroke) }
            }
            "T" -> {
                drawArc(tint, 180f, 180f, false, Offset(w*.08f,h*.12f), Size(w*.84f,h*.84f), style = Stroke(stroke))
                drawLine(tint, Offset(w*.5f,h*.54f), Offset(w*.75f,h*.28f), stroke)
            }
            "R" -> {
                drawRoundRect(tint, Offset(w*.25f,h*.05f), Size(w*.5f,h*.9f), CornerRadius(w*.12f), style = Stroke(stroke))
                drawCircle(tint, w*.12f, Offset(w*.5f,h*.38f), style = Stroke(stroke))
                drawLine(tint, Offset(w*.4f,h*.68f), Offset(w*.6f,h*.68f), stroke)
            }
            "K" -> {
                drawRoundRect(tint, Offset(w*.05f,h*.2f), Size(w*.9f,h*.6f), CornerRadius(w*.08f), style = Stroke(stroke))
                for (x in listOf(.25f,.5f,.75f)) drawCircle(tint, w*.04f, Offset(w*x,h*.4f))
                drawLine(tint, Offset(w*.3f,h*.63f), Offset(w*.7f,h*.63f), stroke)
            }
            else -> {
                val cog = Path().apply {
                    for (i in 0 until 48) {
                        val angle = i * Math.PI / 24 - Math.PI / 2
                        val radius = w * if (i % 6 in 2..3) .43f else .32f
                        val x = center.x + kotlin.math.cos(angle).toFloat() * radius
                        val y = center.y + kotlin.math.sin(angle).toFloat() * radius
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(cog, tint, style = Stroke(stroke))
                drawCircle(tint, w * .13f, center, style = Stroke(stroke))
            }
        }
    }
}

@Composable
private fun AppTile(app: LauncherApp, repository: AppRepository, generation: Int,
    motion: String, onLaunch: () -> Unit, modifier: Modifier = Modifier) {
    SideEffect { RenderDiagnostics.tileComposed() }
    val Ice = LocalAppearance.current.accent
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused && motion != "Minimal") if (motion == "Cinematic") 1.055f else 1.035f else 1f,
        if (motion == "Minimal") snap() else spring(dampingRatio = .82f, stiffness = 500f), label = "tileFocus")
    val icon by produceState<android.graphics.Bitmap?>(null, app.packageName, generation) {
        value = null; value = repository.icon(app.packageName)
    }
    Column(Modifier.width(172.dp).padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Button(onLaunch, Modifier.then(modifier).fillMaxWidth().height(122.dp).onFocusChanged { focused = it.isFocused }
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

    }
}

@Composable
fun CinematicLauncher(
    discovery: AppDiscoveryState, repository: AppRepository, iconGeneration: Int,
    prefs: LauncherPreferences, section: String, onSection: (String) -> Unit,
    query: String, onQuery: (String) -> Unit, clock: String, homeStatus: String,
    keyboardStatus: String, onKeyboardPreview: () -> Unit, onKeyboardEnable: () -> Unit, onKeyboardChoose: () -> Unit, onCompanion: () -> Unit, onSystemControl: () -> Unit, onPhoneTyping: () -> Unit, onEasySetup: () -> Unit,
    onLaunch: (LauncherApp) -> Unit, onPin: (LauncherApp) -> Unit, onRetry: () -> Unit,
    onChooseHome: () -> Unit, onRestoreHome: () -> Unit, onAndroidSettings: () -> Unit,
    onWifiSettings: () -> Unit, onBluetoothSettings: () -> Unit,
    wallpapers: List<WallpaperOption>, wallpaperBusy: Boolean, onImportWallpaper: () -> Unit,
    onRemoveWallpaper: suspend (String) -> Boolean, onRotation: (Boolean) -> Unit,
    onRotationInterval: (Int) -> Unit, onRotationPool: (String) -> Unit,
    onTransition: (String) -> Unit, onRotateWallpaper: suspend () -> Unit,
    onApplyWallpaper: suspend (String) -> Boolean,
    onSaveOrder: suspend (List<String>) -> Boolean,
    onAccent: (String) -> Unit, onFontStyle: (String) -> Unit, onTextSize: (String) -> Unit, onWallpaperVisibility: (String) -> Unit, onFocusAnimation: (String) -> Unit, onResetAppearance: () -> Unit, onResetLauncherAppearance: suspend () -> Boolean,
    onScreensaverMinutes: (Int) -> Unit, onScreensaverStyle: (String) -> Unit, onPreviewScreensaver: () -> Unit, onScreensaverProvider: (String) -> Unit,
    onTheme: (String) -> Unit, onMotion: (String) -> Unit, onReducedMotion: () -> Unit
) {
    val weatherContext = LocalContext.current.applicationContext
    val weather = remember(weatherContext) { WeatherRepository(weatherContext) }
    val network = remember(weatherContext) { NetworkStatusMonitor(weatherContext) }
    val settingsFocus=remember { FocusRequester() }; val clockFocus=remember { FocusRequester() }
    val favouriteShelfFocus = remember { FocusRequester() }
    var previousSection by remember { mutableStateOf(section) }
    LaunchedEffect(section) {
        if(previousSection=="Settings" && section=="Home") { withFrameNanos { }; runCatching { settingsFocus.requestFocus() } }
        previousSection=section
    }
    val Ice = LocalAppearance.current.accent
    SideEffect { RenderDiagnostics.shellComposed() }
    var settingsPage by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var lastSettingsCategory by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("appearance") }
    val launcherEntryFocus = remember { FocusRequester() }
    var restoreLauncherEntry by remember { mutableStateOf(false) }
    var utilityReturn by remember { mutableStateOf<String?>(null) }
    var weatherDetails by remember { mutableStateOf(false) }
    var wallpaperSettingsOpen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var wallpaperChoice by remember { mutableStateOf<String?>(null) }
    var previewWallpaper by remember { mutableStateOf<String?>(null) }
    var applyingWallpaper by remember { mutableStateOf(false) }
    val previewApplyFocus = remember { FocusRequester() }
    val homeListState = rememberLazyListState()
    val searchResultsFocus = remember { FocusRequester() }
    val searchFieldFocus = remember { FocusRequester() }
    var draftOrder by remember { mutableStateOf<List<String>?>(null) }
    var movingPackage by remember { mutableStateOf<String?>(null) }
    var returnFocusPackage by remember { mutableStateOf<String?>(null) }
    var savingOrder by remember { mutableStateOf(false) }
    val movingFocus = remember { FocusRequester() }
    val saveFocus = remember { FocusRequester() }
    val inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
    val scope = rememberCoroutineScope()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) { draftOrder = null; movingPackage = null; returnFocusPackage = null; previewWallpaper = null }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val closeWallpapers = { if (!applyingWallpaper) { previewWallpaper = null; wallpaperSettingsOpen = false; wallpaperChoice = null } }
    val closeSettingsPage = {
        if (settingsPage == "launcher") { settingsPage = "system"; restoreLauncherEntry = true } else settingsPage = null
    }
    LaunchedEffect(settingsPage, restoreLauncherEntry) {
        if (settingsPage == "system" && restoreLauncherEntry) {
            withFrameNanos { }; withFrameNanos { }
            inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
            launcherEntryFocus.requestFocus(); restoreLauncherEntry = false
        }
    }
    // Root Back always stays in RC TV; later modal/preview/move handlers retain priority.
    BackHandler {
        if (section == "Settings" && wallpaperSettingsOpen) closeWallpapers()
        else if (section == "Settings" && settingsPage != null) closeSettingsPage()
        else if (utilityReturn != null && section in listOf("Speed Test","Apps")) {
            settingsPage=utilityReturn; utilityReturn=null; onSection("Settings")
        } else if (section != "Home") {
            closeWallpapers(); onSection("Home")
        } else {
            returnFocusPackage = orderedFavouritePackages(prefs, discovery.apps)
                .firstOrNull { pkg -> discovery.apps.any { it.packageName == pkg } }
        }
    }
    BackHandler(previewWallpaper != null) { if (!applyingWallpaper) previewWallpaper = null }
    val applyWallpaper: (String) -> Unit = { id ->
        if (!applyingWallpaper) {
            applyingWallpaper = true
            scope.launch {
                if (onApplyWallpaper(id)) previewWallpaper = null
                applyingWallpaper = false
            }
        }
    }
    LaunchedEffect(previewWallpaper) {
        if (previewWallpaper != null) { withFrameNanos { }; previewApplyFocus.requestFocus() }
    }
    val cancelMove = { if (!savingOrder) { returnFocusPackage = movingPackage; draftOrder = null; movingPackage = null } }
    BackHandler(draftOrder != null) { cancelMove() }
    val beginMove: (LauncherApp) -> Unit = { app ->
        draftOrder = orderedFavouritePackages(prefs, discovery.apps)
        movingPackage = app.packageName
        onSection("Home")
    }
    val saveMove: () -> Unit = {
        val order = draftOrder
        if (order != null && !savingOrder) {
            savingOrder = true
            scope.launch {
                if (onSaveOrder(order)) { returnFocusPackage = movingPackage; draftOrder = null; movingPackage = null }
                savingOrder = false
            }
        }
    }
    LaunchedEffect(draftOrder == null, returnFocusPackage) {
        if (draftOrder == null && returnFocusPackage != null) {
            val index = visibleFavouriteIndex(prefs, discovery.apps, returnFocusPackage!!)
            homeListState.scrollToItem(index)
            kotlinx.coroutines.delay(120)
            inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
            runCatching { movingFocus.requestFocus() }
            returnFocusPackage = null
        }
    }
    LaunchedEffect(movingPackage) {
        movingPackage?.let { pkg ->
            val index = draftOrder.orEmpty().filter { p -> discovery.apps.any { it.packageName == p } }.indexOf(pkg).coerceAtLeast(0)
            homeListState.scrollToItem(index)
            withFrameNanos { }
            movingFocus.requestFocus()
        }
    }
    val motion = if (prefs.reducedMotion) "Minimal" else prefs.motion
    LaunchedEffect(lifecycle, section, previewWallpaper != null, draftOrder != null,
        prefs.rotationEnabled, prefs.rotationMinutes, prefs.rotationPool, prefs.rotationAt, prefs.theme, wallpapers) {
        if (prefs.rotationEnabled && prefs.theme != "Minimalist" && section == "Home" && previewWallpaper == null && draftOrder == null &&
            nextWallpaper(prefs.wallpaper, prefs.rotationPool, wallpapers.map { it.id }) != null) {
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                kotlinx.coroutines.delay(rotationDelay(System.currentTimeMillis(), prefs.rotationAt, prefs.rotationMinutes))
                onRotateWallpaper()
            }
        }
    }
    val contentSection = if (previewWallpaper != null) "Home" else section
    val visible = remember(discovery.apps, contentSection, query, prefs.favourites, prefs.favouriteOrder, draftOrder) { discovery.apps.filter { (contentSection != "Search" || it.label.contains(query, true)) &&
        (contentSection != "Home" || prefs.favourites.isEmpty() || it.packageName in prefs.favourites) }.let { filtered ->
            if (contentSection == "Home" && (draftOrder != null || prefs.favouriteOrder.isNotEmpty())) {
                val order = draftOrder ?: orderedFavouritePackages(prefs, discovery.apps)
                filtered.sortedBy { order.indexOf(it.packageName).let { rank -> if (rank < 0) Int.MAX_VALUE else rank } }
            } else filtered
        } }
    if(weatherDetails) WeatherDetails(weather, motion, { weatherDetails=false })
    val vivid = prefs.wallpaperVisibility == "Vivid"
    Box(Modifier.fillMaxSize()) {
        Wallpaper(prefs.theme == "Minimalist" && previewWallpaper == null, previewWallpaper ?: prefs.wallpaper, prefs.wallpaperTransition, motion, prefs.wallpaperVisibility)
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CinematicNavigation(section, { next -> if (!savingOrder && !applyingWallpaper) { cancelMove(); closeWallpapers(); when (next) { "Remote" -> onCompanion(); "Keyboard" -> onPhoneTyping(); else -> { utilityReturn=null; settingsPage=null; onSection(next) } } } }, motion, settingsFocus,
                if (contentSection == "Home" && visible.isNotEmpty() && draftOrder == null && previewWallpaper == null) favouriteShelfFocus else clockFocus)
            Column(Modifier.weight(1f).fillMaxWidth().vividContrast(vivid && contentSection != "Home"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        BrandWordmark()
                        Text("Your world. In focus.", style=floatingTextStyle(), fontSize = 13.sp, color = Color.White.copy(alpha = .65f))
                    }
                    if (contentSection == "Home") {
                        WeatherGlance(weather, Modifier.weight(1f).padding(horizontal=16.dp), now = System.currentTimeMillis(), details = { weatherDetails=true })
                        NowPlayingGlance(Modifier.weight(1f).padding(horizontal=8.dp))
                    }
                    ClockNetwork(clock,network,clockFocus,settingsFocus) {
                        lastSettingsCategory="connections"; settingsPage="connections"; utilityReturn=null; onSection("Settings")
                    }
                }
                when (contentSection) {
                    "Speed Test" -> SpeedTestScreen(motion)
                    "Cast" -> CastScreen(motion)
                    "Settings" -> if (wallpaperSettingsOpen) Box(Modifier.weight(1f)) {
                        WallpaperSettings(wallpapers, prefs, onImportWallpaper, onRemoveWallpaper, onRotation, onRotationInterval,
                            onRotationPool, onTransition, { scope.launch { onRotateWallpaper() } },
                            wallpaperChoice ?: prefs.wallpaper, prefs.wallpaper, motion, applyingWallpaper || wallpaperBusy,
                            prefs.theme == "Minimalist", { wallpaperChoice = it },
                            { previewWallpaper = wallpaperChoice ?: prefs.wallpaper },
                            { applyWallpaper(wallpaperChoice ?: prefs.wallpaper) }, closeWallpapers)
                    } else if(settingsPage == null) SettingsCategories(lastSettingsCategory,motion) { key -> lastSettingsCategory=key; settingsPage=key }
                    else key(settingsPage) { SettingsSubpage(settingsPage!!,motion,closeSettingsPage) {
                        when(settingsPage) {
                            "power" -> ScreensaverSettings(prefs, motion, onScreensaverMinutes, onScreensaverStyle, onPreviewScreensaver, onScreensaverProvider)
                            "launcher" -> LauncherManagement(motion, onChooseHome, onRestoreHome, onAndroidSettings, onResetLauncherAppearance)
                            "appearance" -> {
                                Text("Wallpaper",style=MaterialTheme.typography.titleMedium,color=Ice)
                                Text(wallpapers.firstOrNull { it.id==prefs.wallpaper }?.title ?: "Mountain horizon")
                                FocusButton("Choose wallpaper",motion) { wallpaperSettingsOpen=true }
                                AppearanceSettings(prefs,motion,onAccent,onFontStyle,onTextSize,onWallpaperVisibility,onResetAppearance)
                                Text("Layout",style=MaterialTheme.typography.titleMedium,color=Ice)
                                SettingsOptions { listOf("Cinematic","Minimalist").forEach { value -> FocusButton(value,motion,selected=prefs.theme==value) { onTheme(value) } } }
                                Text("Animation Intensity",style=MaterialTheme.typography.titleMedium,color=Ice)
                                SettingsOptions { focusAnimations.forEach { value -> FocusButton(value,motion,Modifier.semantics { contentDescription="Focus animation $value" },selected=prefs.focusAnimation==value) { onFocusAnimation(value) } } }
                                if(prefs.reducedMotion) Text("Reduced Motion is on; focus effects are off.")
                                Text("Interface motion",style=MaterialTheme.typography.titleMedium,color=Ice)
                                SettingsOptions { listOf("Minimal","Balanced","Cinematic").forEach { value -> FocusButton(value,motion,selected=prefs.motion==value) { onMotion(value) } } }
                                FocusButton("Reduced motion: ${if(prefs.reducedMotion) "on" else "off"}",motion,onClick=onReducedMotion)
                            }
                            "connections" -> {
                                val status by network.state.collectAsState()
                                FocusButton("Wi-Fi & Internet",motion,icon="N",quiet=true,onClick=onWifiSettings)
                                Text(status.label,style=MaterialTheme.typography.titleMedium)
                                if(status.kind in listOf(NetworkKind.WIFI,NetworkKind.ETHERNET,NetworkKind.OTHER)) Text(if(status.internet) "Internet available" else "Internet not confirmed")
                                FocusButton("Bluetooth Devices",motion,icon="B",quiet=true,onClick=onBluetoothSettings)
                                Text("Manage networks and pair devices in Android Settings. If a direct page is unavailable, choose Network & Internet or Remotes & Accessories there.",style=MaterialTheme.typography.bodyMedium)
                                FocusButton("Internet Speed Test",motion) { utilityReturn="connections"; onSection("Speed Test") }
                            }
                            "apps" -> {
                                FocusButton("Browse installed apps",motion) { utilityReturn="apps"; onSection("Apps") }
                                SystemSettingsLink(appSettingsDestination, motion)
                                Text("Choose an app in Android to manage its permissions, storage and cache.")
                                StorageSummary()
                                SystemSettingsLink(storageSettingsDestination, motion)
                            }
                            "weather" -> WeatherSettings(weather,motion,{ weatherDetails=true })
                            "remote" -> {
                                Text("Phone remote",style=MaterialTheme.typography.titleMedium,color=Ice)
                                FocusButton("Phone pairing & download",motion,onClick=onCompanion)
                                FocusButton("Phone typing setup",motion,onClick=onPhoneTyping)
                                FocusButton("Phone control permissions",motion,onClick=onSystemControl)
                                Text("RC TV Keyboard",style=MaterialTheme.typography.titleMedium,color=Ice)
                                Text(keyboardStatus)
                                SettingsOptions {
                                    FocusButton("Try keyboard",motion,onClick=onKeyboardPreview)
                                    FocusButton("Enable in Android…",motion,onClick=onKeyboardEnable)
                                    FocusButton("Choose keyboard…",motion,onClick=onKeyboardChoose)
                                }
                            }
                            "system" -> {
                                Text("RC TV ${za.co.rc.tv.BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.titleMedium,color=Ice)
                                FocusButton("Easy setup",motion,onClick=onEasySetup)
                                essentialSystemDestinations.forEach { SystemSettingsLink(it, motion) }
                                FocusButton("Launcher Management", motion, Modifier.focusRequester(launcherEntryFocus)) { settingsPage = "launcher" }
                            }
                        }
                    } }
                    "Library" -> GlassPanel(Modifier.fillMaxWidth()) {
                        Text("A place for your stories", fontSize = 28.sp, fontWeight = FontWeight.Normal)
                        Text("Local media and watchlists are planned. Your installed apps are ready now.", modifier = Modifier.padding(vertical = 16.dp))
                        FocusButton("Browse apps", motion) { onSection("Apps") }
                    }
                    else -> {
                        if (contentSection == "Home") {
                            Spacer(Modifier.weight(1f))
                        } else Text(contentSection, fontSize = 28.sp, fontWeight = FontWeight.Normal)
                        if (contentSection == "Search") OutlinedTextField(query, onQuery, Modifier.fillMaxWidth().focusRequester(searchFieldFocus)
                            .onPreviewKeyEvent { event ->
                                val key = event.nativeKeyEvent
                                if (key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN && visible.isNotEmpty()) {
                                    if (key.action == android.view.KeyEvent.ACTION_DOWN) searchResultsFocus.requestFocus()
                                    true
                                } else false
                            }, label = { Text("Search installed apps") }, singleLine = true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (contentSection == "Home" && prefs.favourites.isNotEmpty()) "Your Favourites" else "Your applications", style=floatingTextStyle(), fontSize = 17.sp, color = Color.White)
                            Text("${visible.size} Apps", style=floatingTextStyle(), fontSize = 12.sp, letterSpacing = 1.sp, color = Ice.copy(alpha = if(vivid) 1f else .7f))
                        }
                        discovery.error?.let { Text(it, color = MaterialTheme.colorScheme.error); FocusButton("Retry discovery", motion) { onRetry() } }
                        if (!discovery.loaded) Text("Discovering apps…", color = Ice)
                        else if (visible.isEmpty()) Text("No matching applications. Try Apps or change your search.", color = Ice)
                        // Target the shelf, rather than a lazy item that may have been scrolled out.
                        // Up always returns to the fixed Settings button, including at Extra Large.
                        if (contentSection == "Home") LazyRow(Modifier.fillMaxWidth().height(220.dp)
                            .focusRequester(favouriteShelfFocus).focusGroup(), state = homeListState, horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                            items(visible, key = { it.packageName }) { app ->
                                if (previewWallpaper != null) {
                                    HomeAppCard(app, repository, iconGeneration, motion, {}, Modifier.focusProperties { canFocus = false })
                                } else if (draftOrder != null) {
                                    val selected = movingPackage == app.packageName
                                    val order = draftOrder.orEmpty()
                                    val position = order.indexOf(app.packageName)
                                    Box(Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null,
                                        placementSpec = if (motion == "Minimal") snap() else spring(dampingRatio = .9f, stiffness = 500f))) {
                                        HomeAppCard(app, repository, iconGeneration, "Minimal", {},
                                            Modifier.cinematicFocus(26.dp).focusProperties { canFocus = selected; down = saveFocus }
                                                .then(if (selected) Modifier.focusRequester(movingFocus) else Modifier)
                                                .onPreviewKeyEvent { event ->
                                                    val key = event.nativeKeyEvent
                                                    if (selected && key.keyCode in listOf(android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_RIGHT)) {
                                                        if (!savingOrder && key.action == android.view.KeyEvent.ACTION_DOWN) {
                                                            val currentOrder = draftOrder.orEmpty()
                                                            val currentPosition = currentOrder.indexOf(app.packageName)
                                                            val next = (currentPosition + if (key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1).coerceIn(0, currentOrder.lastIndex)
                                                            val first = homeListState.firstVisibleItemIndex
                                                            val full = homeListState.layoutInfo.visibleItemsInfo.count { it.offset >= 0 && it.offset + it.size <= homeListState.layoutInfo.viewportEndOffset }.coerceAtLeast(1)
                                                            val viewport = when { next < first -> next; next >= first + full -> next - full + 1; else -> first }
                                                            homeListState.requestScrollToItem(viewport.coerceAtLeast(0))
                                                            draftOrder = currentOrder.toMutableList().apply { add(next, removeAt(currentPosition)) }
                                                        }
                                                        true
                                                    } else if (selected && key.keyCode in listOf(android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER, android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                                                        if (key.action == android.view.KeyEvent.ACTION_UP && !key.isCanceled) saveMove()
                                                        true
                                                    } else false
                                                }, selected, position > 0, position < order.lastIndex)
                                    }
                                } else AppActionHost(app, repository, prefs, discovery.apps, motion, true, { onLaunch(app) }, { onPin(app) }, beginMove) { modifier ->
                                    HomeAppCard(app, repository, iconGeneration, "Minimal", { onLaunch(app) }, modifier.focusProperties { up = settingsFocus }.cinematicFocus(26.dp).then(if (returnFocusPackage == app.packageName) Modifier.focusRequester(movingFocus) else Modifier))
                                }
                            }
                        } else LazyVerticalGrid(GridCells.Adaptive(172.dp), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(4.dp)) {
                            items(visible, key = { it.packageName }) { app ->
                                AppActionHost(app, repository, prefs, discovery.apps, motion, false, { onLaunch(app) }, { onPin(app) }, beginMove) { modifier ->
                                    AppTile(app, repository, iconGeneration, "Minimal", { onLaunch(app) }, modifier.cinematicFocus(22.dp).then(if (contentSection == "Search" && app.packageName == visible.firstOrNull()?.packageName) Modifier.focusRequester(searchResultsFocus).focusProperties { up = searchFieldFocus } else Modifier))
                                }
                            }
                        }
                        if (previewWallpaper != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Preview · ${wallpapers.firstOrNull { it.id == previewWallpaper }?.title ?: "Wallpaper"}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                FocusButton(if (applyingWallpaper) "Applying…" else "Apply wallpaper", motion,
                                    Modifier.focusProperties { canFocus = true }.focusRequester(previewApplyFocus)) { applyWallpaper(previewWallpaper!!) }
                                FocusButton("Cancel preview", motion) { if (!applyingWallpaper) previewWallpaper = null }
                            }
                        } else if (draftOrder != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Left / Right to move · Select to save · Back to cancel", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                FocusButton(if (savingOrder) "Saving…" else "Save", motion, Modifier.focusRequester(saveFocus).focusProperties { up = movingFocus }, onClick = saveMove)
                                FocusButton("Cancel", motion, Modifier.focusProperties { up = movingFocus }, onClick = cancelMove)
                            }
                        } else if (textScale(prefs.textSize) <= 1.15f) {
                            // Reserve the former footer's line height so removing help never moves the cards.
                            Spacer(Modifier.height(with(androidx.compose.ui.platform.LocalDensity.current) { MaterialTheme.typography.bodyLarge.lineHeight.toDp() }))
                        }
                    }
                }
            }
        }
    }
}

/** Scrolls only enough to reveal the focused item; labels are never compressed to fit. */
@Composable
private fun CinematicNavigation(section: String, onSection: (String) -> Unit, motion: String, settingsFocus: FocusRequester, contentFocus: FocusRequester) {
    val state=rememberLazyListState(); val scope=rememberCoroutineScope(); val last=remember { FocusRequester() }
    val items=listOf("Home" to "H","Search" to "S","Apps" to "A","Cast" to "C","Speed Test" to "T")
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
        LazyRow(Modifier.weight(1f).focusGroup().clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xCF263A45),Color(0xB30B131B),Color(0xD910202A))))
            .border(1.dp,Color.White.copy(alpha=.20f),RoundedCornerShape(22.dp)),
            state=state,horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(8.dp)) {
            items(items,key={ it.first }) { (name,symbol) ->
                val bring=remember { BringIntoViewRequester() }
                Box(Modifier.bringIntoViewRequester(bring).onFocusChanged { if(it.hasFocus) scope.launch { bring.bringIntoView() } }.semantics { contentDescription=name }) {
                    FocusButton(name,motion,Modifier.widthIn(min=108.dp).focusProperties { down=contentFocus }
                        .then(if(name=="Speed Test") Modifier.focusRequester(last).focusProperties { right=settingsFocus } else Modifier),selected=section==name,quiet=true,icon=symbol) { onSection(name) }
                }
            }
        }
        FocusButton("Settings",motion,Modifier.focusRequester(settingsFocus).focusProperties { down=contentFocus }
            .onPreviewKeyEvent { event ->
                val key=event.nativeKeyEvent
                if(key.keyCode==android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                    if(key.action==android.view.KeyEvent.ACTION_DOWN) {
                        val moved=runCatching { last.requestFocus(androidx.compose.ui.focus.FocusDirection.Left) }.getOrDefault(false)
                        if(!moved) scope.launch { state.scrollToItem(items.lastIndex); withFrameNanos { }; withFrameNanos { }; runCatching { last.requestFocus(androidx.compose.ui.focus.FocusDirection.Left) } }
                    }
                    true
                } else false
            }.semantics { contentDescription="Settings" },selected=section=="Settings",quiet=true,icon="···",prominent=true) { onSection("Settings") }
    }
}

private fun visibleFavouriteIndex(prefs: LauncherPreferences, apps: List<LauncherApp>, pkg: String): Int =
    orderedFavouritePackages(prefs, apps).filter { p -> apps.any { it.packageName == p } }.indexOf(pkg).coerceAtLeast(0)

private fun Modifier.vividContrast(enabled: Boolean) = if(enabled) background(Ink.copy(alpha=.78f),RoundedCornerShape(12.dp)) else this
