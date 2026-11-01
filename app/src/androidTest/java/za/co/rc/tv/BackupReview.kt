package za.co.rc.tv

import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import za.co.rc.tv.core.*
import java.io.File

/** Physical-device contract and transaction checks. Never clears data or changes Android settings. */
object BackupReview {
    fun saf(test: Instrumentation) {
        val driver = TvReview(test)
        val preferences = Preferences(test.targetContext)
        val original = runBlocking { BackupSchema.snapshot(preferences.backupValues()) }
        test.startActivitySync(Intent(test.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Thread.sleep(1500)
        driver.select("Settings"); driver.select("System & Recovery"); driver.select("Backup & Restore"); driver.select("Create backup")
        Thread.sleep(1000)
        check(driver.foregroundPackage() == "com.google.android.documentsui")
        var filename = ""
        fun findName(node: android.view.accessibility.AccessibilityNodeInfo) {
            val text = node.text?.toString().orEmpty()
            if (text.startsWith("RC-TV-settings-") && text.endsWith(".json")) filename = text
            for (i in 0 until node.childCount) node.getChild(i)?.let { findName(it); it.recycle() }
        }
        driver.ui.rootInActiveWindow?.let { findName(it); it.recycle() }
        check(filename.isNotEmpty()) { "No SAF filename" }
        // DocumentsUI is a phone UI on this emulator; use its normal accessible click.
        driver.control("Save").let { check(it.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)); it.recycle() }
        Thread.sleep(1600)
        check(driver.foregroundPackage() == test.targetContext.packageName)
        val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2F${Uri.encode(filename)}")
        // The real persisted picker result grants access to this document.
        check(runBlocking { SettingsBackup(test.targetContext).inspect(uri) }.values == original)
        try {
            runBlocking { preferences.setAccent(if (original["accent_theme"] == accentThemes[1]) accentThemes[2] else accentThemes[1]) }
            driver.select("Restore from backup"); Thread.sleep(900)
            driver.control(filename).let { check(it.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)); it.recycle() }
            Thread.sleep(900)
            driver.control("Cancel").let { check(it.isFocused); it.recycle() }
            driver.select("Restore settings"); Thread.sleep(900)
            check(runBlocking { BackupSchema.snapshot(preferences.backupValues()) } == original)
        } finally { runBlocking { preferences.restoreBackup(original) } }
        File(test.targetContext.filesDir, "j3f-saf-result.txt").writeText("PASS: genuine DocumentsUI create/save, provider readback, open/import, preview with Cancel focus, confirmed restore; $filename")
    }
    fun core(test: Instrumentation) = runBlocking {
        val context = test.targetContext
        val preferences = Preferences(context)
        val original = BackupSchema.snapshot(preferences.backupValues())
        val initialState = preferences.state.first()
        check(initialState.favourites.size == 8) { "Expected the physical Xiaomi's eight favourites" }
        val grantStore = za.co.rc.tv.link.SecureRecord(context, "companion-grants")
        val grants = grantStore.read()?.toString()
        val remoteConfig = context.getSharedPreferences("companion-connection", 0).all.toMap()
        val keyboard = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)
        val service = SettingsBackup(context)
        val file = File(context.filesDir, "j3f-settings-roundtrip.json")
        val uri = Uri.fromFile(file)
        val bytes = BackupCodec.encode(original, BuildConfig.VERSION_NAME)
        check(BackupCodec.decode(bytes).values == original)
        val location = WeatherConfiguration.decode(original.getValue("weather_configuration") as String)
        val legacy = WeatherConfiguration.fromLegacy(context.getSharedPreferences("weather", 0).getString("record", null))
        check(location == legacy) { "Weather migration changed the saved configuration" }
        suspend fun unchanged() { check(BackupSchema.snapshot(preferences.backupValues()) == original) }
        fun invalid(bytes: ByteArray) { check(runCatching { BackupCodec.decode(bytes) }.isFailure) }
        invalid("not json".toByteArray()); invalid(ByteArray(BackupCodec.MAX_BYTES + 1))
        invalid(byteArrayOf(0xc3.toByte(), 0x28))
        invalid((String(bytes) + "trailing content").toByteArray())
        invalid(("{\"nested\":".repeat(1000) + "0" + "}".repeat(1000)).toByteArray())
        val envelope = JSONObject(String(bytes))
        invalid(JSONObject(envelope.toString()).put("schemaVersion", 99).toString().toByteArray())
        invalid(JSONObject(envelope.toString()).put("sha256", "0".repeat(64)).toString().toByteArray())
        fun invalidPayload(change: (JSONObject) -> Unit) {
            val payload = JSONObject(envelope.getString("payload")); change(payload)
            val text = payload.toString()
            invalid(JSONObject(envelope.toString()).put("payload", text).put("sha256", BackupCodec.hash(text)).toString().toByteArray())
        }
        invalidPayload { it.put("screensaver_minutes", -1) }
        invalidPayload { it.put("reduced_motion", "false") }
        invalidPayload { it.put("favourite_order", "com.example.missing") }
        invalidPayload { it.put("pairing_token", "must never be accepted") }
        invalidPayload { it.remove("theme") }
        invalidPayload { it.put("weather_configuration", "{\"enabled\":true,\"place\":null}") }
        check(runCatching { preferences.restoreBackup(original + ("screensaver_minutes" to -1)) }.isFailure)
        unchanged()
        file.writeText("broken")
        check(runCatching { service.inspect(uri) }.isFailure); unchanged()
        check(runCatching { service.export(Uri.parse("content://invalid.rc.tv.provider/failure")) }.isFailure); unchanged()
        val size = service.export(uri)
        check(size == file.length().toInt())
        val preview = service.inspect(uri)
        check(preview.values == original) { "Original wallpaper assets unavailable; stop rather than change the physical configuration" }
        try {
            preferences.setAccent(if (initialState.accent == accentThemes[1]) accentThemes[2] else accentThemes[1])
            preferences.setScreensaverMinutes(if (initialState.screensaverMinutes == 20) 30 else 20)
            if (location.place != null) preferences.setWeatherConfiguration(location.copy(enabled = !location.enabled))
            check(preferences.state.first().accent != initialState.accent)
            service.restore(preview)
            unchanged()
            check(preferences.state.first().favourites == initialState.favourites)
            check(preferences.state.first().favouriteOrder == initialState.favouriteOrder)
            check(preferences.state.first().copy(rotationAt = 0) == initialState.copy(rotationAt = 0))
            check(preferences.weatherConfiguration.first() == location)
            check(grantStore.read()?.toString() == grants) { "Logical encrypted pairing grants changed" }
            check(context.getSharedPreferences("companion-connection", 0).all.toMap() == remoteConfig)
            check(android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD) == keyboard)
        } finally {
            preferences.restoreBackup(original)
        }
        check(String(bytes).contains("pairing_token").not())
        File(context.filesDir, "j3f-core-result.txt").writeText("PASS: codec/type/schema/integrity/size/UTF-8 rejection; failed I/O preserves settings; controlled accent restore; all 8 favourites/order, supported preferences/weather, logical pairing grants, remote configuration and keyboard preserved. bytes=$size")
    }
    fun ui(test: Instrumentation) {
        val driver = TvReview(test)
        test.startActivitySync(Intent(test.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Thread.sleep(1400)
        driver.select("Settings"); driver.select("System & Recovery"); driver.select("Backup & Restore")
        driver.control("Create backup").recycle(); driver.control("Restore from backup").recycle()
        driver.key(KeyEvent.KEYCODE_BACK)
        driver.control("Launcher Management").recycle()
        driver.select("Backup & Restore")
        driver.shot("backup-page")
        driver.select("Create backup")
        val service = SettingsBackup(test.targetContext)
        if (!service.pickerAvailable(Intent.ACTION_CREATE_DOCUMENT)) {
            driver.node("This TV firmware does not provide a working Android save picker. USB/HDD export is unavailable here. You can save a local copy to TV Downloads instead.")?.recycle() ?: error("Missing honest picker-unavailable message")
            driver.select("Save a copy to TV Downloads")
            Thread.sleep(1200)
            val saved = runBlocking { service.downloads().first() }
            val original = runBlocking { BackupSchema.snapshot(Preferences(test.targetContext).backupValues()) }
            check(runBlocking { service.inspect(saved.uri) }.values == original)
            val preferences = Preferences(test.targetContext)
            val grants = za.co.rc.tv.link.SecureRecord(test.targetContext, "companion-grants").read()?.toString()
            try {
                runBlocking { preferences.setAccent(if (original["accent_theme"] == accentThemes[1]) accentThemes[2] else accentThemes[1]) }
                driver.select("Restore a TV Downloads copy")
                driver.select(saved.name)
                driver.control("Cancel").let { check(it.isFocused); it.recycle() }
                driver.shot("backup-preview")
                driver.key(KeyEvent.KEYCODE_BACK)
                check(runBlocking { BackupSchema.snapshot(preferences.backupValues()) } != original) { "Back unexpectedly restored" }
                driver.select("Restore a TV Downloads copy"); driver.select(saved.name)
                driver.select("Restore settings")
                Thread.sleep(700)
                check(runBlocking { BackupSchema.snapshot(preferences.backupValues()) } == original)
                check(za.co.rc.tv.link.SecureRecord(test.targetContext, "companion-grants").read()?.toString() == grants)
                driver.shot("backup-restored")
                File(test.targetContext.filesDir, "j3f-ui-backup-location.txt").writeText("TV Downloads / RC TV / ${saved.name}\n${saved.uri}")
            } finally { runBlocking { preferences.restoreBackup(original) } }
            driver.key(KeyEvent.KEYCODE_BACK)
            driver.control("Backup & Restore").let { check(it.isFocused); it.recycle() }
        } else {
            Thread.sleep(700)
            check(driver.foregroundPackage() != test.targetContext.packageName) { "SAF picker did not open" }
        }
    }
}
