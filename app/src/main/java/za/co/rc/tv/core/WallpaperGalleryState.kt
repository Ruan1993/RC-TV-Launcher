package za.co.rc.tv.core

/** Session-only metadata history; no bitmaps or persistent browsing cache. Numbers remain absolute. */
class WallpaperPageHistory(private val limit: Int = 10) {
    data class Entry(val number: Int, val cursor: OnlineWallpaperCursor?, var page: OnlineWallpaperPage? = null,
        var focused: Long? = null)
    private val entries = linkedMapOf(1 to Entry(1, null))
    var current = 1; private set
    init { require(limit >= 2) }
    fun entry(number: Int = current) = entries.getValue(number)
    fun numbers() = entries.keys.sorted()
    fun visit(number: Int) { require(number in entries); current = number }
    fun store(page: OnlineWallpaperPage) {
        val seen = entries.values.filter { it.number != current }.flatMap { it.page?.images.orEmpty() }.map { it.page }.toSet()
        entry().page = page.copy(images = page.images.distinctBy { it.page }.filterNot { it.page in seen })
    }
    fun next(): Boolean {
        val nextNumber = current + 1
        if (nextNumber in entries) { current = nextNumber; return true }
        val cursor = entry().page?.next ?: return false
        if (entries.values.any { it.cursor == cursor }) return false
        entries[nextNumber] = Entry(nextNumber, cursor)
        current = nextNumber
        while (entries.size > limit) entries.remove(entries.keys.first { it != current })
        return true
    }
}

fun canDeleteGalleryWallpaper(id: String, current: String, rotation: Set<String>) = id != current && id !in rotation
fun addGalleryRotation(current: Set<String>, selected: Set<String>): Set<String> {
    require(selected.all(::isImportedWallpaper))
    return current + selected
}
