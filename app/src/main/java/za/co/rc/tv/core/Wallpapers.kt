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

fun validWallpaper(value: String?) = BundledWallpaper.fromId(value).id
