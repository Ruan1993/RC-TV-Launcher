package za.co.rc.tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import za.co.rc.tv.core.*
import java.util.Locale

@Composable fun SpeedTestScreen(motion: String) {
    val test = remember { SpeedTest() }
    val state by test.state.collectAsState()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(test, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) test.cancel() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); test.close() }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Internet Speed Test", style = MaterialTheme.typography.headlineLarge)
        Text(basicNetworkInfo(context))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Download" to state.download, "Upload" to state.upload, "Latency (HTTP)" to state.latency).forEachIndexed { index, metric ->
                Column(Modifier.weight(1f).padding(8.dp)) {
                    Text(metric.first, style = MaterialTheme.typography.titleMedium)
                    Text(metric.second?.let { String.format(Locale.US, "%.1f", it) } ?: "—", style = MaterialTheme.typography.headlineLarge)
                    Text(if (index == 2) "ms" else "Mbps")
                }
            }
        }
        if (state.running) LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
        Text(state.phase)
        FocusButton(if (state.running) "Cancel test" else "Start Test", motion) { if (state.running) test.cancel() else test.start() }
        Text("Uses up to 125 MB of data and stops after about a minute. Cloudflare receives your network address under its privacy policy. No test runs automatically.", style = MaterialTheme.typography.bodySmall)
        Text("Measures this TV's HTTP connection to Cloudflare, including request overhead. Latency is an HTTP round trip, not ICMP ping. Your ISP maximum and other destinations may differ. Results are not saved.", style = MaterialTheme.typography.bodySmall)
    }
}
