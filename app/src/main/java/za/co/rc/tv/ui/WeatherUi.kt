package za.co.rc.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import za.co.rc.tv.core.*
import java.util.Locale

@Composable fun WeatherGlance(repository: WeatherRepository, modifier: Modifier = Modifier, now: Long = System.currentTimeMillis()) {
    val state by repository.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state.place, lifecycle) {
        if (state.place != null) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { repository.refreshIfDue(); kotlinx.coroutines.awaitCancellation() }
    }
    val place = state.place ?: return
    val reading = state.reading?.takeIf { WeatherPolicy.usable(now, it.observed) }
    val palette = LocalAppearance.current
    Column(modifier.background(palette.glass, RoundedCornerShape(12.dp)).border(1.dp,palette.border,RoundedCornerShape(12.dp)).padding(horizontal=12.dp,vertical=4.dp)) {
        Text(if (reading != null) "${String.format(Locale.getDefault(), "%.0f",reading.celsius)} °C · ${WeatherPolicy.condition(reading.code)} · ${place.label}" else "${place.label} · Weather unavailable", fontSize=15.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
        Text(if (reading == null) state.message else "${if (state.loading) "Updating · " else if (state.message.startsWith("Offline")) "Offline · " else ""}Cached ${java.text.SimpleDateFormat("MMM d HH:mm",Locale.getDefault()).format(java.util.Date(reading.observed))} · Open-Meteo",fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable fun WeatherSettings(repository: WeatherRepository, motion: String) {
    val state by repository.state.collectAsState(); val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }; var places by remember { mutableStateOf(emptyList<WeatherPlace>()) }
    var searching by remember { mutableStateOf(false) }; var feedback by remember { mutableStateOf("") }
    Text("Optional weather", style=MaterialTheme.typography.titleLarge, color=LocalAppearance.current.accent)
    Text("${state.place?.label ?: "Off until configured"}. A compact card uses the existing Home header and keeps favourite cards the same size.")
    Text("Search and select a city to turn weather on. Open-Meteo receives your city search, selected coordinates and network address. No GPS. Refreshes at most hourly when Home is visible; no background polling. Free provider service is for personal, noncommercial use.",style=MaterialTheme.typography.bodySmall)
    OutlinedTextField(query,{ if (it.length <= 80) query=it }, label={ Text("City or town") },singleLine=true, modifier=Modifier.fillMaxWidth())
    FocusButton(if (searching) "Searching…" else "Search city",motion) {
        if (!searching) { searching=true; places=emptyList(); scope.launch {
            try { places=repository.search(query); feedback=if(places.isEmpty()) "No matching city. Try a nearby town." else "Choose your location below to enable weather." }
            catch (_: Exception) { feedback="City search unavailable. Enter at least two letters and check your connection." }
            finally { searching=false }
        } }
    }
    if (feedback.isNotEmpty()) Text(feedback)
    places.forEach { place -> FocusButton(place.label,motion) { repository.select(place); places=emptyList(); query=""; feedback="Weather enabled. Return to Home to refresh." } }
    if (state.place != null) {
        Text(state.message)
        FocusButton("Turn weather off and remove location",motion) { repository.disable(); feedback="Weather is off. Saved location and cache removed." }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    FocusButton("Weather source and licence", motion) {
        runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://open-meteo.com/en/terms"))) }
            .onFailure { android.widget.Toast.makeText(context,"Sources: open-meteo.com and geonames.org · Licence: creativecommons.org/licenses/by/4.0",android.widget.Toast.LENGTH_LONG).show() }
    }
    Text("Weather: Open-Meteo · Locations: GeoNames · CC BY 4.0. Forecast model conditions may differ from local observations. Conditions are summarized in RC TV. Licence: creativecommons.org/licenses/by/4.0. Cached conditions older than 24 hours are hidden.",style=MaterialTheme.typography.bodySmall)
}
