package za.co.rc.tv.core

val screensaverIntervals = listOf(10, 20, 30, 60, 120, 0)
val screensaverStyles = listOf("Cinematic Wallpapers", "Clock & Wallpaper", "Black Screen")
fun validScreensaverMinutes(value: Int?) = value?.takeIf { it in screensaverIntervals } ?: 30
fun validScreensaverStyle(value: String?) = value?.takeIf { it in screensaverStyles } ?: screensaverStyles.first()
fun screensaverIntervalLabel(minutes: Int) = when (minutes) {
    0 -> "Never"
    60 -> "1 hour"
    120 -> "2 hours"
    else -> "$minutes minutes"
}

/** Monotonic activity-local deadline; never schedules system sleep or background work. */
fun screensaverDelay(minutes: Int, lastInteraction: Long, now: Long): Long? =
    if (minutes == 0) null else (validScreensaverMinutes(minutes) * 60_000L - (now - lastInteraction).coerceAtLeast(0)).coerceAtLeast(0)

fun mayShowScreensaver(resumed: Boolean, focused: Boolean, interactive: Boolean, musicActive: Boolean, blocked: Boolean) =
    resumed && focused && interactive && !musicActive && !blocked
