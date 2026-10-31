package za.co.rc.tv.assist

data class AssistApp(val id: String, val label: String)
data class AssistSnapshot(val apps: List<AssistApp>, val favourites: List<String>, val wallpaper: String, val rotation: Boolean)
enum class AssistKind { NAVIGATE, LAUNCH, WALLPAPER, ROTATION }
data class AssistAction(val kind: AssistKind, val value: String, val description: String, val expected: String = "", val expires: Long = System.currentTimeMillis() + 120_000) {
    fun valid(snapshot: AssistSnapshot, now: Long = System.currentTimeMillis()): Boolean = now < expires && when (kind) {
        AssistKind.NAVIGATE -> value in setOf("Home", "Search", "Apps", "Library", "Settings")
        AssistKind.LAUNCH -> snapshot.apps.any { it.id == value }
        AssistKind.WALLPAPER -> value in setOf("mountain", "coast", "desert") && snapshot.wallpaper == expected
        AssistKind.ROTATION -> value in setOf("true", "false") && snapshot.rotation.toString() == expected
    }
}
data class AssistReply(val text: String, val action: AssistAction? = null, val needsOnline: Boolean = false)

/** Deterministic local commands, explicitly distinct from online model responses. */
object AssistActions {
    private val wallpapers = mapOf("mountain horizon" to "mountain", "tidal dusk" to "coast", "desert afterglow" to "desert")
    fun interpret(input: String, state: AssistSnapshot): AssistReply {
        val query = input.trim().lowercase().replace(Regex("[?!.]+$"), "").replace(Regex("\\s+"), " ")
        if (query.isEmpty() || input.length > 500) return AssistReply("Enter a request of up to 500 characters.")
        if (Regex("\\b(shell|adb|root|delete|uninstall|disable|password)\\b").containsMatchIn(query)) return AssistReply("That action is outside RC Assist's supported controls. No action was taken.")
        if (query in setOf("what applications are installed", "what apps are installed", "list apps", "list applications", "show installed apps"))
            return AssistReply(if (state.apps.isEmpty()) "No installed apps are currently available. Try Apps > Retry." else "Installed apps (${state.apps.size}): ${state.apps.take(50).joinToString { it.label }}.")
        if (query in setOf("show my favourites", "show favourites", "show my favorites", "show favorites")) {
            val names = state.favourites.mapNotNull { id -> state.apps.firstOrNull { it.id == id }?.label }
            return AssistReply(if (names.isEmpty()) "No available favourites yet." else "Your favourites: ${names.joinToString()}.", AssistAction(AssistKind.NAVIGATE, "Home", "Show favourites on Home"))
        }
        if (query in setOf("open settings", "show settings", "go to settings")) return AssistReply("Opening RC TV Settings.", AssistAction(AssistKind.NAVIGATE, "Settings", "Open RC TV Settings"))
        if (query in setOf("go home", "open home", "show home")) return AssistReply("Returning to RC TV Home.", AssistAction(AssistKind.NAVIGATE, "Home", "Open RC TV Home"))
        val find = Regex("(?:find|search for) (.+)").matchEntire(query)
        val launch = Regex("open (.+)").matchEntire(query)
        if (find != null || launch != null) {
            val name = (find ?: launch)!!.groupValues[1]
            val exact = state.apps.filter { it.label.equals(name, true) }
            val matches = if (exact.isNotEmpty()) exact else state.apps.filter { it.label.lowercase().contains(name) }
            return when {
                matches.isEmpty() -> AssistReply("No installed application matches '$name'.")
                launch != null && matches.size == 1 -> AssistReply("Review this app launch.", AssistAction(AssistKind.LAUNCH, matches.single().id, "Open ${matches.single().label}"))
                else -> AssistReply("Found: ${matches.take(15).joinToString { it.label }}. Say 'Open' followed by the exact app name to launch it.")
            }
        }
        Regex("(?:change|set) (?:my |the )?wallpaper (?:to )?(.+)").matchEntire(query)?.let {
            val id = wallpapers[it.groupValues[1]] ?: return AssistReply("Choose a bundled scene: Mountain horizon, Tidal dusk or Desert afterglow.")
            return AssistReply("Review the wallpaper change.", AssistAction(AssistKind.WALLPAPER, id, "Change wallpaper to ${it.groupValues[1]}", state.wallpaper))
        }
        Regex("turn (on|off) (?:automatic )?wallpaper rotation").matchEntire(query)?.let {
            val enabled = it.groupValues[1] == "on"
            return AssistReply("Review the rotation change.", AssistAction(AssistKind.ROTATION, enabled.toString(), "Turn ${if (enabled) "on" else "off"} wallpaper rotation", state.rotation.toString()))
        }
        return AssistReply("This request needs the online AI service. Local commands are available.", needsOnline = true)
    }
}
