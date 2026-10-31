package za.co.rc.tv.core

/** Stable IDs, independent of labels/resource names, for forward-compatible preferences. */
enum class BundledWallpaper(val id: String, val title: String) {
    MOUNTAIN("mountain", "Mountain horizon"),
    COAST("coast", "Tidal dusk"),
    DESERT("desert", "Desert afterglow");

    companion object {
        fun fromId(id: String?): BundledWallpaper = entries.firstOrNull { it.id == id } ?: MOUNTAIN
    }
}

data class WallpaperOption(val id: String, val title: String)
val bundledWallpapers get() = BundledWallpaper.entries.map { WallpaperOption(it.id, it.title) }
fun isImportedWallpaper(id: String) = Regex("local_[a-f0-9]{32}").matches(id)
fun validWallpaper(value: String?) = value?.takeIf { isImportedWallpaper(it) || BundledWallpaper.entries.any { w -> w.id == it } } ?: "mountain"
val rotationIntervals = listOf(5, 15, 30, 60, 1440)
fun validRotationInterval(value: Int) = value.takeIf { it in rotationIntervals } ?: 15
fun validWallpaperTransition(value: String?) = value?.takeIf { it in listOf("Instant", "Crossfade", "Gentle Slide") } ?: "Instant"
fun nextWallpaper(current: String, included: Set<String>, available: List<String>): String? {
    val pool = available.distinct().filter { it in included }
    if (pool.size < 2) return null
    val index = pool.indexOf(current)
    return pool[(index + 1).mod(pool.size)]
}
fun rotationDelay(now: Long, last: Long, minutes: Int): Long {
    val interval = validRotationInterval(minutes) * 60_000L
    return if (last <= 0 || last > now) interval else (interval - (now - last)).coerceAtLeast(0)
}
