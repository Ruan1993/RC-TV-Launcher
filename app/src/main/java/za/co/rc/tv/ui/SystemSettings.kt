package za.co.rc.tv.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import za.co.rc.tv.core.*

@Composable internal fun StorageSummary() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val space by produceState<InternalStorageSpace?>(null, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { value = withContext(Dispatchers.IO) { readInternalStorage() } }
    }
    Text("Internal app storage", style = MaterialTheme.typography.titleMedium, color = LocalAppearance.current.accent)
    Text(space?.let { "${android.text.format.Formatter.formatFileSize(context, it.availableBytes)} available of ${android.text.format.Formatter.formatFileSize(context, it.totalBytes)}" }
        ?: "Open Android Storage settings for available space.")
    Text("Shared internal data space; Android manages apps, cache and connected USB storage.", style = MaterialTheme.typography.bodyMedium)
}

@Composable internal fun SystemSettingsLink(destination: SystemDestination, motion: String) {
    val context = LocalContext.current
    FocusButton(destination.label, motion) { openSystemDestination(context, destination) }
    if (resolvedSystemSettings(context, destination.actions) == null) {
        Text("In Android Settings: ${destination.route}", style = MaterialTheme.typography.bodyMedium)
    }
}
