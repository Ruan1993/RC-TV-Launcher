package za.co.rc.tv.core

internal const val TV_LISTINGS_PERMISSION = "android.permission.READ_TV_LISTINGS"
internal data class ContinueItem(val id: Long, val packageName: String, val title: String,
    val launchUri: String, val artworkUri: String?, val progress: Float?)

/** Watchlists, next episodes and preview recommendations are not unfinished playback. */
internal fun continueItem(id: Long, packageName: String?, title: String?, launchUri: String?, artworkUri: String?,
    type: Int?, browsable: Boolean, position: Long?, duration: Long?): ContinueItem? {
    if (id < 0 || type != 0 || !browsable || packageName == null ||
        !Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+").matches(packageName)) return null
    val label = title?.filterNot(Char::isISOControl)?.trim()?.take(240)?.takeIf(String::isNotEmpty) ?: return null
    val link = launchUri?.takeIf { it.isNotBlank() && it.length <= 8192 && it.none(Char::isISOControl) } ?: return null
    val progress = if (duration != null && duration > 0 && position != null && position in 0..duration)
        (position.toDouble() / duration.toDouble()).toFloat() else null
    return ContinueItem(id, packageName, label, link, safeWatchNextArtwork(artworkUri), progress)
}

internal fun safeWatchNextArtwork(value: String?): String? = runCatching {
    require(value != null && value.length <= 8192 && value.none(Char::isISOControl))
    val uri = java.net.URI(value)
    require(uri.userInfo == null)
    require((uri.scheme == "https" && !uri.host.isNullOrBlank()) ||
        (uri.scheme == "content" && !uri.authority.isNullOrBlank()))
    value
}.getOrNull()
