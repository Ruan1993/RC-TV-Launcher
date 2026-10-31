package za.co.rc.tv.companion

import android.content.Context

data class CommandResult(val accepted: Boolean, val message: String)
data class RemoteAccess(val optedIn: Boolean = false, val connected: Boolean = false)

/** One listener and one grant store, shared by the foreground UI and opt-in system service. */
object RemoteRuntime {
    private val mutableAccess = kotlinx.coroutines.flow.MutableStateFlow(RemoteAccess())
    val access: kotlinx.coroutines.flow.StateFlow<RemoteAccess> = mutableAccess
    private var instance: CompanionServer? = null
    var foreground: ((String, String) -> Boolean)? = null
    @Volatile var accessibility: RemoteAccessibilityService? = null
        private set
    fun server(context: Context): CompanionServer = instance ?: CompanionServer(context.applicationContext, ::dispatch)
        .also { instance = it; updateAccess(context) }
    private fun updateAccess(context: Context) { mutableAccess.value = RemoteAccess(optedIn(context), accessibility != null) }
    fun optedIn(context: Context) = context.getSharedPreferences("companion-connection", Context.MODE_PRIVATE)
        .getBoolean("system-wide", false)
    fun setOptIn(context: Context, enabled: Boolean) {
        context.getSharedPreferences("companion-connection", Context.MODE_PRIVATE).edit().putBoolean("system-wide", enabled).apply()
        updateAccess(context)
        // Invalidate leases on any authority change; encrypted approvals are preserved.
        server(context).suspendConnection("Remote permissions changed. Reconnecting…")
        if (foreground != null || (enabled && accessibility != null)) server(context).resume()
    }
    fun attach(service: RemoteAccessibilityService) {
        accessibility = service
        updateAccess(service)
        if (optedIn(service)) server(service).resume()
    }
    fun detach(service: RemoteAccessibilityService) {
        if (accessibility !== service) return
        accessibility = null
        updateAccess(service)
        server(service).suspendConnection("System control disconnected. Return to RC TV or enable accessibility again.")
        if (foreground != null) server(service).resume()
    }
    fun pause(context: Context) {
        foreground = null
        if (!optedIn(context) || accessibility == null) server(context).suspendConnection()
    }
    private fun dispatch(key: String, text: String): CommandResult {
        val service = accessibility?.takeIf { optedIn(it) }
        if (key == "HOME" && service != null) return service.command(key, text)
        if (key in setOf("MEDIA_PLAY_PAUSE", "MEDIA_NEXT", "MEDIA_PREVIOUS", "INPUT_TEXT")) {
            return service?.command(key, text) ?: CommandResult(false, "Enable system control in RC TV Phone companion settings first.")
        }
        foreground?.let { return CommandResult(it(key, text), "Close RC TV dialogs with the physical remote before navigating.") }
        return service?.command(key, text) ?: CommandResult(false, "Return to RC TV or enable system control on the TV.")
    }
}
