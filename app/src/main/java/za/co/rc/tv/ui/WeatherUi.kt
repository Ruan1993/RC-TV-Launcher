package za.co.rc.tv.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import za.co.rc.tv.core.*
import java.util.Locale

private fun degrees(value: Double) = String.format(Locale.getDefault(),"%.0f °C",value)
private fun updated(state: WeatherState, reading: WeatherReading?) = when {
    !state.enabled -> "Weather off · saved report"
    state.loading -> "Updating…"
    reading == null -> "Weather unavailable"
    else -> "${if(state.message.startsWith("Offline")) "Offline · " else ""}Updated ${java.text.SimpleDateFormat("MMM d HH:mm",Locale.getDefault()).format(java.util.Date(reading.observed))}"
}

@Composable fun WeatherGlance(repository: WeatherRepository, modifier: Modifier = Modifier, now: Long = System.currentTimeMillis(), details: () -> Unit = {}) {
    val state by repository.state.collectAsState()
    if(!state.enabled) return
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val contentReady = LocalHomeContentReady.current
    LaunchedEffect(state.place,lifecycle,contentReady) {
        if(contentReady && state.place!=null) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { repository.refreshIfDue(); kotlinx.coroutines.awaitCancellation() }
    }
    val place=state.place ?: return
    val reading=state.reading?.takeIf { WeatherPolicy.usable(now,it.observed) }
    val palette=LocalAppearance.current; var focused by remember { mutableStateOf(false) }
    Column(modifier.onFocusChanged { focused=it.isFocused }.background(palette.glass,RoundedCornerShape(12.dp))
        .border(if(focused) 2.dp else 1.dp,if(focused) palette.focus else palette.border,RoundedCornerShape(12.dp))
        .clickable(role=Role.Button,onClick=details).padding(horizontal=12.dp,vertical=4.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            WeatherMark(reading?.code,Modifier.size(30.dp))
            Text(reading?.let { degrees(it.celsius) } ?: "—",fontSize=24.sp,lineHeight=30.sp,fontWeight=if(palette.highReadability) FontWeight.Bold else FontWeight.Medium)
            Column(Modifier.weight(1f)) {
                Text(place.label,fontSize=14.sp,lineHeight=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(reading?.let { WeatherPolicy.condition(it.code) } ?: "Weather unavailable",fontSize=12.sp,lineHeight=16.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
        Text("${updated(state,reading)} · Details ›",Modifier.fillMaxWidth().padding(end=2.dp,top=2.dp),
            color=Color(0xFFB8C0C5),fontSize=10.sp,lineHeight=12.sp,textAlign=TextAlign.End,
            maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable private fun WeatherMark(code: Int?, modifier: Modifier = Modifier) {
    val accent=LocalAppearance.current.accent
    Canvas(modifier) {
        val w=size.width; val h=size.height; val stroke=2.dp.toPx()
        if(code==null || WeatherPolicy.condition(code)=="Conditions unavailable") { drawCircle(accent,w*.34f,style=Stroke(stroke)); drawLine(accent,Offset(w*.36f,h*.5f),Offset(w*.64f,h*.5f),stroke); return@Canvas }
        if(code in 0..2) {
            val center=if(code==2) Offset(w*.68f,h*.3f) else Offset(w*.5f,h*.5f)
            val radius=if(code==2) w*.16f else w*.23f
            drawCircle(Color(0xFFFFD67D),radius,center,style=Stroke(stroke))
            for(i in 0..7) { val a=i*Math.PI/4; val v=Offset(kotlin.math.cos(a).toFloat(),kotlin.math.sin(a).toFloat()); drawLine(Color(0xFFFFD67D),center+v*(radius+w*.09f),center+v*(radius+w*.17f),stroke) }
            if(code<2) return@Canvas
        }
        val cloud=Path().apply {
            moveTo(w*.22f,h*.68f); cubicTo(w*.02f,h*.68f,w*.02f,h*.39f,w*.27f,h*.4f)
            cubicTo(w*.3f,h*.12f,w*.68f,h*.16f,w*.72f,h*.4f)
            cubicTo(w*.98f,h*.37f,w*.99f,h*.68f,w*.78f,h*.68f); close()
        }
        drawPath(cloud,accent.copy(alpha=.22f)); drawPath(cloud,accent,style=Stroke(stroke))
        when(code) {
            45,48 -> for(y in listOf(.79f,.92f)) drawLine(accent,Offset(w*.2f,h*y),Offset(w*.8f,h*y),stroke)
            71,73,75,77,85,86 -> for(x in listOf(.3f,.55f,.8f)) drawCircle(Color.White,w*.035f,Offset(w*x,h*.87f))
            95,96,99 -> drawPath(Path().apply { moveTo(w*.53f,h*.65f); lineTo(w*.38f,h*.87f); lineTo(w*.55f,h*.87f); lineTo(w*.45f,h); lineTo(w*.72f,h*.77f); lineTo(w*.56f,h*.77f); close() },Color(0xFFFFD67D))
            in 51..82 -> for(x in listOf(.3f,.55f,.8f)) drawLine(Color(0xFFA9D9FF),Offset(w*x,h*.78f),Offset(w*(x-.05f),h*.94f),stroke)
        }
    }
}

@Composable fun WeatherDetails(repository: WeatherRepository, motion: String, close: () -> Unit) {
    val state by repository.state.collectAsState()
    val reading=state.reading?.takeIf { WeatherPolicy.usable(System.currentTimeMillis(),it.observed) }
    val backFocus = remember { FocusRequester() }
    val detailsFocus = remember { FocusRequester() }
    val input = LocalInputModeManager.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { withFrameNanos { }; input.requestInputMode(InputMode.Keyboard); backFocus.requestFocus() }
    AlertDialog(onDismissRequest=close,modifier=Modifier.widthIn(max=680.dp),title={ Text("Weather details") },text={
        Column(Modifier.heightIn(max=240.dp).verticalScroll(scroll).focusRequester(detailsFocus)
            .onPreviewKeyEvent { event ->
                val key = event.nativeKeyEvent
                val delta = when (key.keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> if (scroll.canScrollForward) 120f else null
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> if (scroll.canScrollBackward) -120f else null
                    else -> null
                }
                if (delta != null) { if (key.action == android.view.KeyEvent.ACTION_DOWN) scope.launch { scroll.scrollBy(delta) }; true } else false
            }.focusable(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(state.place?.label ?: "No location selected",style=MaterialTheme.typography.titleLarge)
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                WeatherMark(reading?.code,Modifier.size(44.dp))
                Text(reading?.let { degrees(it.celsius) } ?: "—",style=MaterialTheme.typography.headlineLarge)
            }
            Text(reading?.let { WeatherPolicy.condition(it.code) } ?: "Conditions unavailable")
            Text(updated(state,reading))
            if(reading != null) {
                val measurements = weatherDetailMeasurements(reading)
                measurements.forEach { (label, value) -> Text("$label: $value") }
                if(measurements.isEmpty()) Text("Extra measurements aren't available in this cached report.")
            }
            Text("Forecast data is not included in this report.", style=MaterialTheme.typography.bodySmall)
            Text("Open-Meteo model conditions · Cached reports expire after 24 hours. Opening details does not request another update.", style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={ FocusButton("Back",motion,Modifier.focusRequester(backFocus).focusProperties { up=detailsFocus },onClick=close) })
}

@Composable fun WeatherSettings(repository: WeatherRepository, motion: String, details: () -> Unit = {}) {
    val state by repository.state.collectAsState(); val scope=rememberCoroutineScope(); val context=LocalContext.current
    var editing by remember { mutableStateOf(false) }; var about by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }; var places by remember { mutableStateOf(emptyList<WeatherPlace>()) }
    var searching by remember { mutableStateOf(false) }; var feedback by remember { mutableStateOf("") }
    if(about) AlertDialog(onDismissRequest={ about=false },title={ Text("About weather") },text={
        Column(Modifier.heightIn(max=200.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            ReadableNote("Open-Meteo receives city searches, selected coordinates and your network address. No GPS. Weather refreshes at most hourly when Home is visible, with offline caching.")
            ReadableNote("Weather: Open-Meteo · Locations: GeoNames · CC BY 4.0. Conditions are summarized by RC TV and may differ from local observations. Licence: creativecommons.org/licenses/by/4.0. Free provider use is personal/noncommercial.")
            FocusButton("Provider and licence",motion) { runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://open-meteo.com/en/terms"))) }.onFailure { android.widget.Toast.makeText(context,"open-meteo.com/en/terms · geonames.org · creativecommons.org/licenses/by/4.0",android.widget.Toast.LENGTH_LONG).show() } }
        }
    },confirmButton={ FocusButton("Back",motion) { about=false } })
    FocusButton(if(state.place==null) "Set up weather" else "Weather: ${if(state.enabled) "On" else "Off"}",motion,selected=state.enabled) {
        if(state.place==null) editing=true else scope.launch { runCatching { repository.setEnabled(!state.enabled) }.onFailure { feedback="Could not save weather settings." } }
    }
    if(state.place!=null) {
        Text("Location",style=MaterialTheme.typography.titleMedium,color=LocalAppearance.current.accent)
        Text(state.place!!.label)
        SettingsOptions {
            FocusButton(if(editing) "Cancel location edit" else "Change location",motion) { editing=!editing; places=emptyList(); feedback="" }
            FocusButton("Weather details",motion,onClick=details)
        }
    }
    if(editing) {
        if(state.place==null) Text("Choose a city to enable weather. No GPS required.")
        OutlinedTextField(query,{ if(it.length<=80) query=it },label={ Text("City or town") },singleLine=true,modifier=Modifier.fillMaxWidth())
        FocusButton(if(searching) "Searching…" else "Search city",motion) {
            if(!searching) { searching=true; places=emptyList(); scope.launch {
                try { places=repository.search(query); feedback=if(places.isEmpty()) "No matching city. Try a nearby town." else "Choose a location below." }
                catch(_: Exception) { feedback="Search unavailable. Check the city name and connection." }
                finally { searching=false }
            } }
        }
        if(feedback.isNotEmpty()) Text(feedback)
        places.forEach { place -> FocusButton(place.label,motion) { scope.launch { runCatching { repository.select(place) }.onSuccess { places=emptyList(); query=""; editing=false; feedback="" }.onFailure { feedback="Could not save location." } } } }
        Text("Search and location are shared with Open-Meteo.",style=MaterialTheme.typography.bodySmall)
    }
    if(state.place!=null) FocusButton("Remove saved location",motion) { scope.launch { runCatching { repository.disable() }.onSuccess { editing=true; feedback=""; query=""; places=emptyList() }.onFailure { feedback="Could not remove location." } } }
    FocusButton("About weather & privacy",motion) { about=true }
}
