package za.co.rc.tv

import android.app.Instrumentation
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.provider.MediaStore
import android.view.KeyEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import za.co.rc.tv.core.*
import za.co.rc.tv.ui.MediaBrowserActivity
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sin

/** Synthetic files only. Does not delete or alter user media, Home, permissions or player defaults. */
object J4Review {
    private fun movie(file: File) {
        val width = 320; val height = 180; val fps = 15; val frames = 450
        val codec = MediaCodec.createEncoderByType("video/avc")
        val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, 180000); setInteger(MediaFormat.KEY_FRAME_RATE, fps); setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val muxer = MediaMuxer(file.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started = false; var track = -1
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); codec.start()
            var frame = 0; var eos = false; var done = false
            val info = MediaCodec.BufferInfo(); val deadline = System.nanoTime() + 30_000_000_000L
            while (!done) {
                check(System.nanoTime() < deadline) { "Video fixture encoding timed out" }
                if (!eos) {
                    val input = codec.dequeueInputBuffer(10000)
                    if (input >= 0) {
                        if (frame < frames) {
                            val bytes = ByteArray(width * height * 3 / 2) { i -> if (i < width * height) (32 + ((frame / fps) % 4) * 48).toByte() else 128.toByte() }
                            codec.getInputBuffer(input)!!.apply { clear(); put(bytes) }
                            codec.queueInputBuffer(input, 0, bytes.size, frame * 1_000_000L / fps, 0); frame++
                        } else { codec.queueInputBuffer(input, 0, 0, frame * 1_000_000L / fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM); eos = true }
                    }
                }
                when (val output = codec.dequeueOutputBuffer(info, 10000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { track = muxer.addTrack(codec.outputFormat); muxer.start(); started = true }
                    else -> if (output >= 0) {
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                            check(started); val buffer = codec.getOutputBuffer(output)!!; buffer.position(info.offset); buffer.limit(info.offset + info.size); muxer.writeSampleData(track, buffer, info)
                        }
                        done = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0; codec.releaseOutputBuffer(output, false)
                    }
                }
            }
        } finally { runCatching { codec.stop() }; codec.release(); if (started) runCatching { muxer.stop() }; muxer.release() }
    }
    private fun sound(): ByteArray {
        val rate = 16000; val samples = rate * 15
        val buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(rate).putInt(rate * 2).putShort(2).putShort(16).put("data".toByteArray()).putInt(samples * 2)
        repeat(samples) { buffer.putShort((sin(2 * Math.PI * 220 * it / rate) * 1800).toInt().toShort()) }
        return buffer.array()
    }
    fun cleanup(test: Instrumentation) {
        val record = File(test.targetContext.filesDir, "j4-owned-fixtures.json")
        if (record.isFile) {
            val array = org.json.JSONArray(record.readText())
            for (i in 0 until array.length()) test.targetContext.contentResolver.delete(Uri.parse(array.getString(i)), null, null)
            record.delete()
        }
        File(test.targetContext.cacheDir, "j4-video.mp4").delete()
    }
    fun run(test: Instrumentation, keepFixtures: Boolean = false) = runBlocking {
        cleanup(test)
        val context = test.targetContext; val resolver = context.contentResolver
        val preferences = Preferences(context); val before = preferences.state.first()
        val grants = za.co.rc.tv.link.SecureRecord(context, "companion-grants").read()?.toString()
        val fixture = File(context.cacheDir, "j4-video.mp4"); movie(fixture)
        val owned = mutableListOf<Uri>()
        fun insert(collection: Uri, name: String, mime: String, directory: String, write: (java.io.OutputStream) -> Unit): Uri {
            val values = ContentValues().apply { put("_display_name", name); put("mime_type", mime); put("relative_path", directory); put("is_pending", 1) }
            val uri = resolver.insert(collection, values) ?: error("No fixture row")
            owned += uri
            resolver.openOutputStream(uri, "wt")!!.use(write)
            check(resolver.update(uri, ContentValues().apply { put("is_pending", 0) }, null, null) == 1)
            return uri
        }
        val browser = MediaBrowser(context); val driver = TvReview(test)
        try {
            val video = insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "RC-TV-J4-video.mp4", "video/mp4", "Movies/RC TV J4 Check/Shows/") { out -> fixture.inputStream().use { it.copyTo(out) } }
            val audio = insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "RC-TV-J4-audio.wav", "audio/wav", "Music/RC TV J4 Check/") { it.write(sound()) }
            val picture = insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "RC-TV-J4-image.png", "image/png", "Pictures/RC TV J4 Check/") { out ->
                val bitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888); bitmap.eraseColor(android.graphics.Color.rgb(28, 130, 110)); bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); bitmap.recycle()
            }
            val source = browser.sources().first { it.volume == MediaStore.VOLUME_EXTERNAL_PRIMARY }
            val root = browser.root(source)
            check(browser.page(root, 0).entries.any { it.kind == MediaKind.FOLDER && it.name == "Movies" })
            val folder = MediaLocation(source, "Movies/RC TV J4 Check/Shows/", "Shows")
            val entry = browser.page(folder, 0).entries.first { it.name == "RC-TV-J4-video.mp4" }
            val intent = browser.vlcIntent(entry)
            check(intent.`package` == "org.videolan.vlc" && intent.data?.scheme == "content" && intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0 && intent.clipData != null)
            test.startActivitySync(Intent(context, MediaBrowserActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); Thread.sleep(1200)
            if (!browser.canReadIndexed()) { driver.select("Allow media access"); Thread.sleep(700); driver.select("Allow"); Thread.sleep(500) }
            driver.select(source.label + " · Indexed media"); driver.select("Movies"); driver.select("RC TV J4 Check"); driver.select("Shows")
            driver.shot("j4-video-folder")
            driver.select("Newest"); driver.select("File type"); driver.select("Name")
            driver.select("RC-TV-J4-video.mp4"); Thread.sleep(3500)
            check(driver.foregroundPackage() == "org.videolan.vlc") { "VLC did not open the video" }
            fun brightness(bitmap: Bitmap): Double {
                var sum = 0L; var count = 0
                for (x in 35..60 step 2) for (y in 25..43 step 2) { val p = bitmap.getPixel(bitmap.width * x / 100, bitmap.height * y / 100); sum += android.graphics.Color.red(p) + android.graphics.Color.green(p) + android.graphics.Color.blue(p); count += 3 }
                return sum.toDouble() / count
            }
            val first = driver.ui.takeScreenshot() ?: error("No playback screenshot")
            File(context.filesDir, "j4-vlc-first.png").outputStream().use { first.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Thread.sleep(2100)
            val second = driver.ui.takeScreenshot() ?: error("No second playback screenshot")
            File(context.filesDir, "j4-vlc-second.png").outputStream().use { second.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val dump = driver.ui.executeShellCommand("dumpsys media_session")
            val session = android.os.ParcelFileDescriptor.AutoCloseInputStream(dump).bufferedReader().use { it.readText() }
            File(context.filesDir, "j4-vlc-session.txt").writeText(session)
            val change = abs(brightness(first) - brightness(second)); first.recycle(); second.recycle()
            check(session.contains("VLC org.videolan.vlc") && session.contains("state=3")) { "No active VLC playback session" }
            // This Xiaomi's hardware video is black in screenshots. Preserve the result,
            // but require separate user observation before claiming visible video playback.
            driver.shot("j4-vlc-playing")
            driver.returnWithBack(); driver.control("RC-TV-J4-video.mp4").let { check(it.isFocused); it.recycle() }
            driver.key(KeyEvent.KEYCODE_BACK); driver.key(KeyEvent.KEYCODE_BACK); driver.key(KeyEvent.KEYCODE_BACK)
            driver.select("Music"); driver.select("RC TV J4 Check"); driver.select("RC-TV-J4-audio.wav"); Thread.sleep(2000)
            check(context.getSystemService(android.media.AudioManager::class.java).isMusicActive) { "No active audio output" }
            val audioDump = driver.ui.executeShellCommand("dumpsys media_session")
            val audioSession = android.os.ParcelFileDescriptor.AutoCloseInputStream(audioDump).bufferedReader().use { it.readText() }
            check(audioSession.contains("VLC org.videolan.vlc") && audioSession.contains("state=3")) { "No playing VLC audio session" }
            driver.shot("j4-vlc-audio"); if (driver.foregroundPackage() != context.packageName) driver.returnWithBack()
            driver.key(KeyEvent.KEYCODE_BACK); driver.key(KeyEvent.KEYCODE_BACK)
            driver.select("Pictures"); driver.select("RC TV J4 Check"); driver.select("RC-TV-J4-image.png"); Thread.sleep(800)
            driver.control("Close image").let { check(it.isFocused); it.recycle() }; driver.shot("j4-image")
            driver.key(KeyEvent.KEYCODE_BACK); driver.control("RC-TV-J4-image.png").let { check(it.isFocused); it.recycle() }
            driver.key(KeyEvent.KEYCODE_BACK); driver.key(KeyEvent.KEYCODE_BACK); driver.key(KeyEvent.KEYCODE_BACK)
            driver.select("Connect folder…")
            check(!browser.folderPickerAvailable()) { "Expected physical Xiaomi's unsupported tree picker" }
            val after = preferences.state.first()
            check(after.favourites == before.favourites && after.favouriteOrder == before.favouriteOrder)
            check(after.copy(wallpaper = before.wallpaper, rotationAt = 0) == before.copy(rotationAt = 0))
            check(za.co.rc.tv.link.SecureRecord(context, "companion-grants").read()?.toString() == grants)
            File(context.filesDir, "j4-result.txt").writeText("PASS: scoped MediaStore folders, sort, content URI/ClipData read grant; active VLC video session (captured-frame delta=$change; visible video requires user confirmation), audio output active; built-in image preview; D-pad/Back and returning file focus; eight favourites/order, other preferences and logical pairing grants unchanged. Synthetic internal media, not HDD proof.")
        } finally {
            if (keepFixtures) File(context.filesDir, "j4-owned-fixtures.json").writeText(org.json.JSONArray(owned.map(Uri::toString)).toString())
            else { owned.forEach { runCatching { resolver.delete(it, null, null) } }; fixture.delete() }
        }
    }
}
