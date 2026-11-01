package za.co.rc.tv.core

const val ANDROID_SCREENSAVER = "Android / Xiaomi Screensaver"
const val RC_SCREENSAVER = "RC TV Screensaver"
val screensaverProviders = listOf(ANDROID_SCREENSAVER, RC_SCREENSAVER)
fun initialScreensaverProvider(saved: String?, existingInstallation: Boolean): String =
    saved?.takeIf { it in screensaverProviders } ?: if (saved == null && existingInstallation) RC_SCREENSAVER else ANDROID_SCREENSAVER

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
