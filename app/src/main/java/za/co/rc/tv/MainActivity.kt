package za.co.rc.tv

import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import za.co.rc.tv.core.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val repository by lazy { AppRepository(this) }
    private val preferences by lazy { Preferences(this) }
    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    private fun openSettings() {
        runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            .onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }.onFailure { message("Open Android Settings from your original launcher.") } }
    }
    private fun selectHome() {
        val roles = getSystemService(RoleManager::class.java)
        if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
            runCatching { startActivity(roles.createRequestRoleIntent(RoleManager.ROLE_HOME)) }.onFailure { openSettings() }
        } else openSettings()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF9FE9D5), background = Color(0xFF09151F), surface = Color(0xFF182D39))) {
                val prefs by preferences.state.collectAsStateWithLifecycle(LauncherPreferences())
                var discovery by remember { mutableStateOf(AppDiscoveryState()) }
                val apps = discovery.apps
                var section by rememberSaveable { mutableStateOf("Home") }
                var query by rememberSaveable { mutableStateOf("") }
                var homeStatus by remember { mutableStateOf("") }
                var refresh by remember { mutableIntStateOf(0) }
                var iconGeneration by remember { mutableIntStateOf(0) }
                var clock by remember { mutableStateOf("") }
                val scope = rememberCoroutineScope()
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                LaunchedEffect(lifecycle, refresh) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        discovery = refreshDiscovery(discovery, { repository.discover() }) {
                            android.util.Log.e("RcDiscovery", "Installed app discovery failed", it)
                        }
                        repository.invalidateIcons()
                        iconGeneration++
                        android.util.Log.d("RcDiscovery", "Refresh complete: ${discovery.apps.size} apps, error=${discovery.error != null}")
                        val current = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                        homeStatus = if (current?.activityInfo?.packageName == packageName) "RC TV Launcher is your default Home." else "Current Home: ${current?.loadLabel(packageManager) ?: "System chooser / unavailable"}"
                    }
                }
                LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()); kotlinx.coroutines.delay(30_000) } } }
                BackHandler(section != "Home") { section = "Home" }
                Row(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF09151F), Color(0xFF193E49), Color(0xFF101925)))).padding(28.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Column(Modifier.width(150.dp).fillMaxHeight().background(Color.Black.copy(alpha = .22f), RoundedCornerShape(24.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("RC TV", fontSize = 24.sp, color = Color(0xFF9FE9D5), modifier = Modifier.padding(8.dp, 20.dp))
                        listOf("Home", "Search", "Apps", "Library", "Settings").forEach { item -> GlassButton(item, Modifier.fillMaxWidth(), selected = section == item) { section = item } }
                        Spacer(Modifier.weight(1f))
                        Text("FOUNDATION\n0.1.0", fontSize = 12.sp, color = Color.White.copy(alpha = .5f), modifier = Modifier.padding(8.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("RC TV", fontSize = 24.sp, color = Color(0xFF9FE9D5))
                                Text("Your world. In focus.", fontSize = 12.sp, color = Color.White.copy(alpha = .65f))
                                if (section != "Home") Text(section, fontSize = 24.sp)
                            }
                            Text(clock, fontSize = 36.sp)
                        }
                        when (section) {
                            "Settings" -> {
                                Text("Launcher setup", fontSize = 24.sp)
                                Text(homeStatus)
                                GlassButton("Choose default Home") { selectHome() }
                                GlassButton("Android settings") { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }.onFailure { message("Settings are unavailable on this firmware.") } }
                                Text("Restore original launcher", fontSize = 24.sp)
                                Text("Open Android Settings → Apps → Default apps → Home app. Select the original Xiaomi / Google TV launcher. Menu names vary by firmware. RC TV never disables or modifies it.", fontSize = 16.sp)
                                GlassButton("Open Home settings / restore") { openSettings() }
                                GlassButton("Reduced motion: ${if (prefs.reducedMotion) "on" else "off"}") { scope.launch { runCatching { preferences.setReducedMotion(!prefs.reducedMotion) }.onFailure { message("Could not save preference.") } } }
                                Text("This foundation uses no blur or animated transitions. Manufacturer Home-button overrides and reboot behaviour require physical-device testing.", fontSize = 14.sp)
                            }
                            "Library" -> {
                                Text("Your library starts in the next milestone.", fontSize = 24.sp)
                                Text("Local media, USB document access and the watchlist are not implemented in this foundation build. Installed apps are available from Home and Apps.")
                                GlassButton("Browse installed apps") { section = "Apps" }
                            }
                            else -> {
                                if (section == "Search") OutlinedTextField(query, { query = it }, label = { Text("Search installed apps") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                val visible = apps.filter { (section != "Search" || it.label.contains(query, true)) && (section != "Home" || prefs.favourites.isEmpty() || it.packageName in prefs.favourites) }
                                Text(if (section == "Home" && prefs.favourites.isNotEmpty()) "Favourites" else "Installed applications", fontSize = 20.sp)
                                discovery.error?.let { error ->
                                    Text(error, color = MaterialTheme.colorScheme.error)
                                    GlassButton("Retry discovery") { refresh++ }
                                }
                                if (!discovery.loaded) Text("Discovering apps…")
                                else if (visible.isEmpty()) Text("No matching applications. Try Apps or change your search.")
                                LazyVerticalGrid(columns = GridCells.Adaptive(190.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                                    items(visible, key = { it.packageName }) { app ->
                                        Column(Modifier.background(Color.Black.copy(alpha = .22f), RoundedCornerShape(18.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            GlassButton(app.label, Modifier.fillMaxWidth().heightIn(min = 112.dp), icon = { AppIcon(app.packageName, repository, iconGeneration) }) { if (!repository.launch(app)) message("This app cannot currently be opened. It may have been removed or restricted.") }
                                            GlassButton(if (app.packageName in prefs.favourites) "★ Unpin" else "☆ Pin") { scope.launch { runCatching { preferences.toggleFavourite(app.packageName) }.onFailure { message("Could not save favourite.") } } }
                                        }
                                    }
                                }
                                Text("Use your remote to move • Select to open • Pin your favourites", fontSize = 13.sp, color = Color.White.copy(alpha = .65f))
                            }
                        }
                    }
                }
            }
        }
        // Android 11's Window getter can throw before the decor view exists.
        // setContent creates it; posting waits for attachment before hiding bars.
        window.decorView.post {
            window.decorView.windowInsetsController?.apply {
                hide(android.view.WindowInsets.Type.systemBars())
                systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}

@Composable
private fun GlassButton(label: String, modifier: Modifier = Modifier, selected: Boolean = false, icon: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Button(onClick, modifier.onFocusChanged { focused = it.isFocused }.border(if (focused) 3.dp else 1.dp, if (focused) Color(0xFF9FE9D5) else Color.White.copy(alpha = .12f), RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected || focused) Color(0xFF315560) else Color(0xFF1A303C), contentColor = Color.White), contentPadding = PaddingValues(14.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            icon?.invoke()
            Text(label, fontSize = 16.sp, maxLines = 2)
        }
    }
}


@Composable
private fun AppIcon(packageName: String, repository: AppRepository, generation: Int) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, packageName, generation) {
        value = null
        value = repository.icon(packageName)
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.size(48.dp))
    else Image(painterResource(R.drawable.rc_icon), contentDescription = null, modifier = Modifier.size(48.dp))
}
