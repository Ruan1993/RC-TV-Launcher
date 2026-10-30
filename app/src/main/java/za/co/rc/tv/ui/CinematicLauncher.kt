package za.co.rc.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import kotlinx.coroutines.launch
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
private fun Wallpaper(minimal: Boolean) {
    SideEffect { RenderDiagnostics.wallpaperComposed() }
    val context = LocalContext.current.applicationContext
    val wallpaper by produceState<android.graphics.Bitmap?>(null, minimal) {
        value = if (minimal) null else withContext(Dispatchers.IO) {
            RenderDiagnostics.wallpaperDecoded()
            renderCinematicBackdrop(context)
        }
    }
    Box(Modifier.fillMaxSize().background(Ink)) {
        if (!minimal && wallpaper != null) Image(wallpaper!!.asImageBitmap(), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(GlassShape).background(Ink.copy(alpha = .64f))
        .border(1.dp, Color.White.copy(alpha = .12f), GlassShape).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
internal fun FocusButton(label: String, motion: String, modifier: Modifier = Modifier,
    selected: Boolean = false, quiet: Boolean = false, icon: String? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused && motion != "Minimal") 1.025f else 1f,
        if (motion == "Minimal") snap() else spring(dampingRatio = if (motion == "Cinematic") .78f else .9f, stiffness = 550f), label = "buttonFocus")
    val tint by animateColorAsState(if (focused) if (quiet) Ice.copy(alpha = .22f) else Ice else if (selected) Color.White.copy(alpha = if (quiet) .12f else .18f) else Color.White.copy(alpha = .045f), animationSpec = if (motion == "Minimal") snap() else spring(stiffness = 600f), label = "buttonTint")
    Button(onClick, modifier.heightIn(min = 48.dp).onFocusChanged { focused = it.isFocused }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .drawBehind { drawRoundRect(tint, cornerRadius = CornerRadius(14.dp.toPx())) }
        .border(1.dp, if (focused) Ice else Color.White.copy(alpha = .10f), RoundedCornerShape(14.dp)),
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
            else -> { drawCircle(tint, w*.3f, center, style = Stroke(stroke)); drawCircle(tint, w*.08f, center); for (i in 0..3) { val a = i * Math.PI / 2; drawLine(tint, Offset(center.x+(kotlin.math.cos(a)*w*.3).toFloat(),center.y+(kotlin.math.sin(a)*h*.3).toFloat()), Offset(center.x+(kotlin.math.cos(a)*w*.46).toFloat(),center.y+(kotlin.math.sin(a)*h*.46).toFloat()),stroke) } }
        }
    }
}

@Composable
private fun AppTile(app: LauncherApp, repository: AppRepository, generation: Int,
    motion: String, onLaunch: () -> Unit, modifier: Modifier = Modifier) {
    SideEffect { RenderDiagnostics.tileComposed() }
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
    onLaunch: (LauncherApp) -> Unit, onPin: (LauncherApp) -> Unit, onRetry: () -> Unit,
    onChooseHome: () -> Unit, onRestoreHome: () -> Unit, onAndroidSettings: () -> Unit,
    onSaveOrder: suspend (List<String>) -> Boolean,
    onTheme: (String) -> Unit, onMotion: (String) -> Unit, onReducedMotion: () -> Unit
) {
    SideEffect { RenderDiagnostics.shellComposed() }
    val homeListState = rememberLazyListState()
    val searchResultsFocus = remember { FocusRequester() }
    val searchFieldFocus = remember { FocusRequester() }
    var draftOrder by remember { mutableStateOf<List<String>?>(null) }
    var movingPackage by remember { mutableStateOf<String?>(null) }
    var returnFocusPackage by remember { mutableStateOf<String?>(null) }
    var savingOrder by remember { mutableStateOf(false) }
    val movingFocus = remember { FocusRequester() }
    val saveFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) { draftOrder = null; movingPackage = null; returnFocusPackage = null }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
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
    val visible = remember(discovery.apps, section, query, prefs.favourites, prefs.favouriteOrder, draftOrder) { discovery.apps.filter { (section != "Search" || it.label.contains(query, true)) &&
        (section != "Home" || prefs.favourites.isEmpty() || it.packageName in prefs.favourites) }.let { filtered ->
            if (section == "Home" && (draftOrder != null || prefs.favouriteOrder.isNotEmpty())) {
                val order = draftOrder ?: orderedFavouritePackages(prefs, discovery.apps)
                filtered.sortedBy { order.indexOf(it.packageName).let { rank -> if (rank < 0) Int.MAX_VALUE else rank } }
            } else filtered
        } }
    Box(Modifier.fillMaxSize()) {
        Wallpaper(prefs.theme == "Minimalist")
        Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            CinematicNavigation(section, { next -> if (!savingOrder) { cancelMove(); onSection(next) } }, motion)
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("RC TV", fontSize = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 3.sp, color = Color.White)
                        Text("Your world. In focus.", fontSize = 13.sp, color = Color.White.copy(alpha = .65f))
                    }
                    Text(clock, fontSize = 28.sp, fontWeight = FontWeight.Normal, color = Color.White)
                }
                when (section) {
                    "Settings" -> Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text("Settings", style = MaterialTheme.typography.headlineLarge)
                        GlassPanel(Modifier.fillMaxWidth()) {
                                                Text("Appearance", style = MaterialTheme.typography.titleLarge, color = Ice)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Cinematic", "Minimalist").forEach { theme -> FocusButton(theme, motion, selected = prefs.theme == theme) { onTheme(theme) } }
                        }
                        }
                        GlassPanel(Modifier.fillMaxWidth()) {
                        Text("Motion", style = MaterialTheme.typography.titleLarge, color = Ice)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Minimal", "Balanced", "Cinematic").forEach { value -> FocusButton(value, motion, selected = prefs.motion == value) { onMotion(value) } }
                        }
                        FocusButton("Reduced motion: ${if (prefs.reducedMotion) "on" else "off"}", motion) { onReducedMotion() }
                        Text("Choose how much movement feels right. Reduced motion takes priority over the selected intensity.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .78f))
                        }
                        GlassPanel(Modifier.fillMaxWidth()) {
                        Text("Launcher & recovery", style = MaterialTheme.typography.titleLarge, color = Ice)
                        Text(homeStatus, style = MaterialTheme.typography.bodyMedium)
                        FocusButton("Android settings", motion) { onAndroidSettings() }
                        FocusButton("Choose default Home", motion) { onChooseHome() }
                        FocusButton("Open Home settings / restore", motion) { onRestoreHome() }
                        Text("Keep Xiaomi's original launcher installed and enabled. Restore it in Android Settings → Apps → Default apps → Home.", style = MaterialTheme.typography.bodyMedium)
                    }
                    }
                    "Library" -> GlassPanel(Modifier.fillMaxWidth()) {
                        Text("A place for your stories", fontSize = 28.sp, fontWeight = FontWeight.Normal)
                        Text("Local media and watchlists are planned. Your installed apps are ready now.", modifier = Modifier.padding(vertical = 16.dp))
                        FocusButton("Browse apps", motion) { onSection("Apps") }
                    }
                    else -> {
                        if (section == "Home") {
                            Spacer(Modifier.weight(1f))
                        } else Text(section, fontSize = 28.sp, fontWeight = FontWeight.Normal)
                        if (section == "Search") OutlinedTextField(query, onQuery, Modifier.fillMaxWidth().focusRequester(searchFieldFocus)
                            .onPreviewKeyEvent { event ->
                                val key = event.nativeKeyEvent
                                if (key.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN && visible.isNotEmpty()) {
                                    if (key.action == android.view.KeyEvent.ACTION_DOWN) searchResultsFocus.requestFocus()
                                    true
                                } else false
                            }, label = { Text("Search installed apps") }, singleLine = true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (section == "Home" && prefs.favourites.isNotEmpty()) "Your favourites" else "Your applications", fontSize = 17.sp, color = Color.White)
                            Text("${visible.size} APPS", fontSize = 12.sp, letterSpacing = 1.sp, color = Ice.copy(alpha = .7f))
                        }
                        discovery.error?.let { Text(it, color = MaterialTheme.colorScheme.error); FocusButton("Retry discovery", motion) { onRetry() } }
                        if (!discovery.loaded) Text("Discovering apps…", color = Ice)
                        else if (visible.isEmpty()) Text("No matching applications. Try Apps or change your search.", color = Ice)
                        if (section == "Home") LazyRow(Modifier.fillMaxWidth().height(220.dp), state = homeListState, horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                            items(visible, key = { it.packageName }) { app ->
                                if (draftOrder != null) {
                                    val selected = movingPackage == app.packageName
                                    val order = draftOrder.orEmpty()
                                    val position = order.indexOf(app.packageName)
                                    Box(Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null,
                                        placementSpec = if (motion == "Minimal") snap() else spring(dampingRatio = .9f, stiffness = 500f))) {
                                        HomeAppCard(app, repository, iconGeneration, motion, {},
                                            Modifier.focusProperties { canFocus = selected; down = saveFocus }
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
                                    HomeAppCard(app, repository, iconGeneration, motion, { onLaunch(app) }, modifier.then(if (returnFocusPackage == app.packageName) Modifier.focusRequester(movingFocus) else Modifier))
                                }
                            }
                        } else LazyVerticalGrid(GridCells.Adaptive(172.dp), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(4.dp)) {
                            items(visible, key = { it.packageName }) { app ->
                                AppActionHost(app, repository, prefs, discovery.apps, motion, false, { onLaunch(app) }, { onPin(app) }, beginMove) { modifier ->
                                    AppTile(app, repository, iconGeneration, motion, { onLaunch(app) }, modifier.then(if (section == "Search" && app.packageName == visible.firstOrNull()?.packageName) Modifier.focusRequester(searchResultsFocus).focusProperties { up = searchFieldFocus } else Modifier))
                                }
                            }
                        }
                        if (draftOrder != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Left / Right to move · Select to save · Back to cancel", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                FocusButton(if (savingOrder) "Saving…" else "Save", motion, Modifier.focusRequester(saveFocus).focusProperties { up = movingFocus }, onClick = saveMove)
                                FocusButton("Cancel", motion, Modifier.focusProperties { up = movingFocus }, onClick = cancelMove)
                            }
                        } else Text("D-pad to explore    ·    Select to open    ·    Hold Select for options    ·    Back to Home", fontSize = 12.sp, color = Color.White.copy(alpha = .5f))
                    }
                }
            }
        }
    }
}

/** Focus/width state is scoped here: rail animation cannot recompose the screen. */
@Composable
private fun CinematicNavigation(section: String, onSection: (String) -> Unit, motion: String) {
    var railFocused by remember { mutableStateOf(false) }
    val railWidth by animateDpAsState(if (railFocused) 146.dp else 76.dp,
        if (motion == "Minimal") snap() else spring(dampingRatio = if (motion == "Cinematic") .88f else 1f, stiffness = 600f), label = "railWidth")
    Box(Modifier.width(146.dp).fillMaxHeight()) {
        Column(Modifier.width(146.dp).fillMaxHeight().onFocusChanged { railFocused = it.hasFocus }.focusGroup()
            .drawWithCache {
                val material = Brush.verticalGradient(listOf(Color(0xCF263A45), Color(0xB30B131B), Color(0xD910202A)))
                val edge = Brush.linearGradient(listOf(Color(0x88DAF3EE), Color(0x18FFFFFF), Color(0x4DC7E9E2)))
                val highlight = Brush.verticalGradient(listOf(Color.White.copy(alpha = .07f), Color.Transparent))
                onDrawWithContent {
                val width = railWidth.toPx()
                val panelSize = Size(width, size.height)
                val corners = CornerRadius(22.dp.toPx())
                drawRoundRect(material, size = panelSize, cornerRadius = corners)
                drawRoundRect(highlight, Offset(5.dp.toPx(), 5.dp.toPx()), Size(width - 10.dp.toPx(), size.height * .3f), corners)
                drawRoundRect(edge, size = panelSize, cornerRadius = corners, style = Stroke(1.dp.toPx()))
                clipRect(right = width) { this@onDrawWithContent.drawContent() }
                }
            }
            .padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(Modifier.height(10.dp))
            listOf("Home" to "H", "Search" to "S", "Apps" to "A", "Library" to "L", "Settings" to "···").forEach { (name, symbol) ->
                Box(Modifier.semantics { contentDescription = name }) {
                    FocusButton(if (railFocused) name else "", motion, Modifier.width(if (railFocused) 126.dp else 56.dp), selected = section == name, quiet = true, icon = symbol) { onSection(name) }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("RC TV", fontSize = 12.sp, color = Color.White.copy(alpha = .45f), modifier = Modifier.padding(5.dp))
        }
    }

}

private fun visibleFavouriteIndex(prefs: LauncherPreferences, apps: List<LauncherApp>, pkg: String): Int =
    orderedFavouritePackages(prefs, apps).filter { p -> apps.any { it.packageName == p } }.indexOf(pkg).coerceAtLeast(0)
