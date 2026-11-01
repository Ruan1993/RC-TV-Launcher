package za.co.rc.tv.core

import java.util.Locale

enum class MediaKind { FOLDER, VIDEO, AUDIO, IMAGE, BACKUP }
enum class MediaSort(val label: String) { NAME("Name"), DATE("Newest"), TYPE("File type") }
fun mediaKind(name: String, mime: String?): MediaKind? {
    val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when {
        mime?.startsWith("video/") == true || extension in setOf("mp4", "mkv", "avi", "mov", "m4v", "webm", "mpeg", "mpg", "ts", "m2ts", "vob", "3gp") -> MediaKind.VIDEO
        mime?.startsWith("audio/") == true || extension in setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma", "aiff") -> MediaKind.AUDIO
        mime?.startsWith("image/") == true || extension in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif") -> MediaKind.IMAGE
        extension == "json" -> MediaKind.BACKUP
        else -> null
    }
}
/** MediaStore virtual folders include only indexed, permission-accessible media. */
fun mediaChildFolder(parent: String, relativePath: String): String? {
    if (!relativePath.startsWith(parent) || relativePath == parent) return null
    return relativePath.removePrefix(parent).substringBefore('/').takeIf { it.isNotEmpty() && it != "." && it != ".." }
}
fun mediaReadPermissions(sdk: Int): Array<String> = if (sdk >= 34) arrayOf("android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_AUDIO", "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
    else if (sdk >= 33) arrayOf("android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_AUDIO", "android.permission.READ_MEDIA_IMAGES")
    else arrayOf("android.permission.READ_EXTERNAL_STORAGE")
