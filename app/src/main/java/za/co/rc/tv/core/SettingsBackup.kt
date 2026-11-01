package za.co.rc.tv.core

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

internal data class BackupRule(val default: Any, val accepts: (Any) -> Boolean)

/** Closed, typed allowlist: never serialise a whole application directory or credential store. */
internal object BackupSchema {
    private fun choice(default: String, allowed: List<String>) = BackupRule(default) { it is String && it in allowed }
    private fun flag(default: Boolean = false) = BackupRule(default) { it is Boolean }
    private fun number(default: Int, allowed: List<Int>) = BackupRule(default) { it is Int && it in allowed }
    private fun packages(value: Any) = value is Set<*> && value.size <= 256 && value.all { it is String && it.length <= 255 && Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+").matches(it) }
    val rules = linkedMapOf(
        "favourites" to BackupRule(emptySet<String>(), ::packages),
        "favourite_order" to BackupRule("") { it is String && it.length <= 65536 },
        "reduced_motion" to flag(),
        "theme" to choice("Cinematic", listOf("Cinematic", "Minimalist")),
        "animation_intensity" to choice("Minimal", listOf("Minimal", "Balanced", "Cinematic")),
        "wallpaper" to BackupRule("mountain") { it is String && validWallpaper(it) == it },
        "wallpaper_rotation" to flag(),
        "wallpaper_interval" to number(15, rotationIntervals),
        "wallpaper_pool" to BackupRule(setOf("mountain", "coast", "desert")) { it is Set<*> && it.size <= 27 && it.all { id -> id is String && validWallpaper(id) == id } },
        "wallpaper_transition" to choice("Instant", listOf("Instant", "Crossfade", "Gentle Slide")),
        "accent_theme" to choice(accentThemes.first(), accentThemes),
        "font_style" to choice(fontStyles.first(), fontStyles),
        "text_size" to choice(textSizes.first(), textSizes),
        "wallpaper_visibility" to choice("Balanced", wallpaperVisibilities),
        "focus_animation" to choice("Standard", focusAnimations),
        "screensaver_provider" to choice(ANDROID_SCREENSAVER, screensaverProviders),
        "screensaver_minutes" to number(30, screensaverIntervals),
        "screensaver_style" to choice(screensaverStyles.first(), screensaverStyles),
        "weather_configuration" to BackupRule(WeatherConfiguration().encode()) { it is String && runCatching { WeatherConfiguration.decode(it) }.isSuccess }
    )
    fun snapshot(raw: Map<String, Any>): Map<String, Any> {
        val result = rules.mapValues { (key, rule) -> raw[key] ?: rule.default }.toMutableMap()
        if (raw["focus_animation"] == null) result["focus_animation"] = legacyFocusAnimation(raw["animation_intensity"] as? String)
        val favourites = result.getValue("favourites") as Set<*>
        val order = (result.getValue("favourite_order") as String).split('\n').filter { it in favourites }.distinct()
        result["favourite_order"] = (order + favourites.filterIsInstance<String>().sorted().filterNot { it in order }).joinToString("\n")
        validate(result)
        return result.toMap()
    }
    fun validate(values: Map<String, Any>) {
        require(values.keys == rules.keys) { "The backup has missing or unsupported settings." }
        rules.forEach { (key, rule) -> require(rule.accepts(values.getValue(key))) { "The backup contains an invalid setting: $key." } }
        val favourites = values.getValue("favourites") as Set<*>
        val order = (values.getValue("favourite_order") as String).split('\n').filter(String::isNotEmpty)
        require(order.size == order.distinct().size && order.toSet() == favourites) { "Favourite order is inconsistent." }
        require((values.getValue("favourite_order") as String) == order.joinToString("\n")) { "Favourite order is malformed." }
    }
}

internal data class BackupPreview(val values: Map<String, Any>, val createdAt: Long, val appVersion: String,
    val favourites: List<String>, val notes: List<String>)
internal data class LocalBackup(val name: String, val uri: Uri)

internal object BackupCodec {
    const val MAX_BYTES = 256 * 1024
    fun hash(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    fun encode(values: Map<String, Any>, version: String, now: Long = System.currentTimeMillis()): ByteArray {
        BackupSchema.validate(values)
        val payload = JSONObject().apply { values.forEach { (key, value) -> put(key, if (value is Set<*>) JSONArray(value.filterIsInstance<String>().sorted()) else value) } }.toString()
        return JSONObject().put("format", "rc-tv-settings").put("schemaVersion", 1).put("createdAt", now).put("appVersion", version)
            .put("payload", payload).put("sha256", hash(payload)).toString(2).toByteArray(Charsets.UTF_8).also { require(it.size <= MAX_BYTES) }
    }
    fun decode(bytes: ByteArray): BackupPreview {
        require(bytes.size in 1..MAX_BYTES) { "Choose an RC TV settings backup smaller than 256 KB." }
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        val envelope = parseBackupObject(text)
        require(envelope.keys().asSequence().toSet() == setOf("format", "schemaVersion", "createdAt", "appVersion", "payload", "sha256")) { "Unsupported backup structure." }
        require(envelope.get("format") == "rc-tv-settings" && envelope.get("schemaVersion") == 1) { "Unsupported RC TV backup version." }
        require(envelope.get("payload") is String && envelope.get("sha256") is String && envelope.get("appVersion") is String)
        val payload = envelope.getString("payload")
        require(hash(payload) == envelope.getString("sha256")) { "The backup is damaged: its integrity check failed." }
        val created = envelope.get("createdAt")
        require(created is Number && created.toString().matches(Regex("[0-9]{1,13}")) && created.toLong() in 1..4_102_444_800_000L)
        val version = envelope.getString("appVersion")
        require(version.length in 1..80 && version.none(Char::isISOControl))
        val json = parseBackupObject(payload)
        require(json.keys().asSequence().toSet() == BackupSchema.rules.keys) { "Missing or unsupported backup settings." }
        val values = BackupSchema.rules.mapValues { (key, rule) ->
            val value = json.get(key)
            if (rule.default is Set<*>) {
                require(value is JSONArray && value.length() <= 256)
                val list = (0 until value.length()).map { require(value.get(it) is String); value.getString(it) }
                require(list.distinct().size == list.size) { "Duplicate list entries." }
                list.toSet()
            } else value
        }
        BackupSchema.validate(values)
        return BackupPreview(values, created.toLong(), version,
            (values.getValue("favourite_order") as String).split('\n').filter(String::isNotEmpty), emptyList())
    }
    fun read(stream: java.io.InputStream): ByteArray = stream.use {
        val out = java.io.ByteArrayOutputStream(); val block = ByteArray(4096)
        while (true) { val n = it.read(block); if (n < 0) break; require(out.size() + n <= MAX_BYTES) { "Backup exceeds 256 KB." }; out.write(block, 0, n) }
        out.toByteArray()
    }
}

/** Bound parser recursion before JSONTokener sees untrusted input; reject trailing content. */
internal fun parseBackupObject(text: String): JSONObject {
    var depth = 0; var quoted = false; var escaped = false
    text.forEach { c ->
        if (quoted) {
            if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false
        } else when (c) {
            '"' -> quoted = true
            '{', '[' -> { depth++; require(depth <= 12) { "Backup structure is too deeply nested." } }
            '}', ']' -> { depth--; require(depth >= 0) { "Malformed backup structure." } }
        }
    }
    require(!quoted && depth == 0) { "Incomplete backup structure." }
    val parser = org.json.JSONTokener(text)
    val value = parser.nextValue()
    require(value is JSONObject && parser.nextClean() == '\u0000') { "Unexpected content after backup." }
    return value
}

internal class SettingsBackup(context: Context) {
    private val context = context.applicationContext
    private val preferences = Preferences(context)
    fun pickerAvailable(action: String): Boolean {
        val intent = android.content.Intent(action).addCategory(android.content.Intent.CATEGORY_OPENABLE).setType("application/json")
        val activity = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo ?: return false
        return activity.packageName != "com.google.android.tv.frameworkpackagestubs" && !activity.name.contains("EmptyStubActivity")
    }
    suspend fun saveToDownloads(): LocalBackup = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val name = "RC-TV-settings-${java.text.SimpleDateFormat("yyyy-MM-dd-HHmmss-SSS", java.util.Locale.ROOT).format(java.util.Date())}.json"
        val row = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/RC TV")
            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, row) ?: error("TV Downloads is unavailable.")
        try {
            export(uri)
            check(resolver.update(uri, android.content.ContentValues().apply { put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) == 1)
            LocalBackup(name, uri)
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }; throw e
        }
    }
    suspend fun downloads(): List<LocalBackup> = withContext(Dispatchers.IO) {
        val collection = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val result = mutableListOf<LocalBackup>()
        context.contentResolver.query(collection, arrayOf("_id", "_display_name"),
            "owner_package_name=? AND mime_type=? AND is_pending=0", arrayOf(context.packageName, "application/json"), "date_added DESC")?.use { cursor ->
            while (cursor.moveToNext() && result.size < 50) {
                val name = cursor.getString(1)
                if (name.startsWith("RC-TV-settings-")) result += LocalBackup(name, android.content.ContentUris.withAppendedId(collection, cursor.getLong(0)))
            }
        }
        result
    }
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val bytes = BackupCodec.encode(BackupSchema.snapshot(preferences.backupValues()), za.co.rc.tv.BuildConfig.VERSION_NAME)
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot write to this destination.")
        out.use { it.write(bytes); it.flush() }
        val readBack = context.contentResolver.openInputStream(uri) ?: error("Saved file could not be read back. Choose another destination.")
        val saved = BackupCodec.read(readBack)
        check(saved.contentEquals(bytes)) { "Saved file could not be verified. Choose another destination." }
        BackupCodec.decode(saved)
        bytes.size
    }
    suspend fun inspect(uri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: error("Cannot read the selected backup.")
        val preview = BackupCodec.decode(BackupCodec.read(stream))
        prepare(preview)
    }
    private fun prepare(preview: BackupPreview): BackupPreview {
        val notes = mutableListOf<String>()
        val values = preview.values.toMutableMap()
        fun available(id: String) = !isImportedWallpaper(id) || WallpaperStore.image(context, id).isFile
        val wallpaper = values.getValue("wallpaper") as String
        if (!available(wallpaper)) { values["wallpaper"] = "mountain"; notes += "The selected imported image is not on this TV. Mountain will be used; import the image again later." }
        @Suppress("UNCHECKED_CAST") val pool = values.getValue("wallpaper_pool") as Set<String>
        val availablePool = pool.filter(::available).toSet()
        if (availablePool != pool) { values["wallpaper_pool"] = availablePool; notes += "Missing imported images will be removed from the rotation pool." }
        val installed = context.packageManager.queryIntentActivities(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LEANBACK_LAUNCHER), 0).map { it.activityInfo.packageName }.toSet() +
            context.packageManager.queryIntentActivities(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER), 0).map { it.activityInfo.packageName }
        val missing = preview.favourites.count { it !in installed }
        if (missing > 0) notes += "$missing favourite app(s) are not currently available. Their saved positions will be kept; apps are not installed by restore."
        return preview.copy(values = values.toMap(), notes = notes.toList())
    }
    suspend fun restore(preview: BackupPreview) = withContext(Dispatchers.IO) {
        // Revalidate just before the single durable edit; no other stores are written.
        BackupSchema.validate(preview.values)
        check(prepare(preview).values == preview.values) { "Wallpaper availability changed. Choose the backup again to review it." }
        preferences.restoreBackup(preview.values)
    }
}
