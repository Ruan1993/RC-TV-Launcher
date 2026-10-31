package za.co.rc.tv.core

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

/** Public destinations only; an actual launch can fail even after resolution. */
internal fun launchSettingsWithFallback(action: String, launch: (String) -> Boolean): Boolean =
    launch(action) || launch(Settings.ACTION_SETTINGS)

fun openConnectionSettings(context: Context, bluetooth: Boolean) {
    val action=if(bluetooth) Settings.ACTION_BLUETOOTH_SETTINGS else Settings.ACTION_WIFI_SETTINGS
    val opened=launchSettingsWithFallback(action) { target ->
        if(target==Settings.ACTION_SETTINGS) Toast.makeText(context,
            if(bluetooth) "Direct Bluetooth settings unavailable. In Android Settings, choose Remotes & Accessories."
            else "Direct Wi-Fi settings unavailable. In Android Settings, choose Network & Internet.",Toast.LENGTH_LONG).show()
        runCatching { context.startActivity(Intent(target)); true }.getOrDefault(false)
    }
    if(!opened) Toast.makeText(context,"Android Settings unavailable. Open Settings from your original launcher.",Toast.LENGTH_LONG).show()
}
