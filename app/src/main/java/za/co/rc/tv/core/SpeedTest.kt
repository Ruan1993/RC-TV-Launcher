package za.co.rc.tv.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class SpeedState(val running: Boolean = false, val phase: String = "Ready. Tests run only when you choose Start Test.",
    val progress: Float = 0f, val download: Double? = null, val upload: Double? = null, val latency: Double? = null, val bytes: Long = 0)

/** Bounded, explicit HTTP measurement against the documented Cloudflare speedtest APIs. */
class SpeedTest {
    companion object { @Volatile private var lastStart = Long.MIN_VALUE; private val launchLock = Any() }
    private val mutable = MutableStateFlow(SpeedState())
    val state = mutable.asStateFlow()
    @Volatile private var stopped = true
    @Volatile private var connection: HttpURLConnection? = null
    private var executor: java.util.concurrent.ExecutorService? = null
    private var deadline = 0L
    private var transferred = 0L
    @Synchronized fun start() {
        if (mutable.value.running) return
        val now = System.nanoTime()
        synchronized(launchLock) {
            if (lastStart != Long.MIN_VALUE && now - lastStart < TimeUnit.SECONDS.toNanos(60)) {
                mutable.value = mutable.value.copy(phase = "Please wait one minute between tests."); return
            }
            lastStart = now
        }
        stopped = false; transferred = 0; deadline = now + TimeUnit.SECONDS.toNanos(55)
        mutable.value = SpeedState(true, "Checking latency…")
        val worker = Executors.newSingleThreadExecutor(); executor = worker
        worker.execute {
            try {
                transfer(0, false) // Warm DNS/TLS, excluded from latency statistics.
                val latency = SpeedMeasurements.medianMillis(List(5) { transfer(0, false).nanos })
                mutable.value = mutable.value.copy(latency = latency, phase = "Measuring download…", progress = .15f)
                transfer(SpeedMeasurements.MIB, false) // Warm-up is not a reported broadband measurement.
                val down = mutableListOf<TransferSample>()
                for (size in listOf(8L, 24L, 24L)) {
                    down += transfer(size * SpeedMeasurements.MIB, false)
                    if (down.count { it.nanos >= 1_200_000_000L } >= 2) break
                }
                val download = SpeedMeasurements.mbps(down)
                mutable.value = mutable.value.copy(download = download, phase = "Measuring upload…", progress = .55f)
                val up = mutableListOf<TransferSample>()
                for (size in listOf(2L, 8L, 24L, 24L)) {
                    up += transfer(size * SpeedMeasurements.MIB, true)
                    if (up.count { it.nanos >= 1_200_000_000L } >= 2) break
                }
                val upload = SpeedMeasurements.mbps(up)
                mutable.value = mutable.value.copy(running = false, upload = upload, progress = 1f,
                    phase = if (download != null && upload != null) "Test complete. Measured HTTP throughput to Cloudflare."
                    else "Test finished, but some samples were too short for a reliable speed. Those results are left blank.")
            } catch (e: Exception) {
                mutable.value = mutable.value.copy(running = false, phase = if (stopped) "Test cancelled. No incomplete result is shown."
                    else "Test could not finish: ${e.message?.take(120) ?: "connection unavailable"}. Completed stages, if any, remain visible.")
            } finally { connection?.disconnect(); connection = null; worker.shutdown(); synchronized(this) { if (executor === worker) executor = null } }
        }
    }
    fun cancel() { stopped = true; if (mutable.value.running) mutable.value = mutable.value.copy(phase = "Stopping test…"); connection?.disconnect() }
    fun close() { cancel(); executor?.shutdownNow() }
    private fun checkActive() {
        check(!stopped && !Thread.currentThread().isInterrupted) { "cancelled" }
        check(System.nanoTime() < deadline) { "55-second safety limit reached" }
        check(transferred <= 116 * SpeedMeasurements.MIB) { "transfer limit reached" }
    }
    private fun transfer(bytes: Long, upload: Boolean): TransferSample {
        checkActive()
        val target = if (upload) "https://speed.cloudflare.com/__up" else "https://speed.cloudflare.com/__down?bytes=$bytes"
        val conn = URL(target).openConnection() as HttpURLConnection
        connection = conn
        conn.connectTimeout = 5000; conn.readTimeout = 5000; conn.useCaches = false; conn.instanceFollowRedirects = false
        conn.setRequestProperty("Accept-Encoding", "identity"); conn.setRequestProperty("Cache-Control", "no-store")
        conn.setRequestProperty("User-Agent", "RC-TV-SpeedTest/0.9 personal-preview")
        val block = ByteArray(64 * 1024)
        if (upload) { SecureRandom().nextBytes(block); conn.requestMethod = "POST"; conn.doOutput = true; conn.setRequestProperty("Content-Type", "application/octet-stream"); conn.setFixedLengthStreamingMode(bytes) }
        val start = System.nanoTime()
        var received = 0L
        try {
            if (upload) conn.outputStream.use { stream ->
                var remaining = bytes
                while (remaining > 0) { checkActive(); val count = minOf(remaining, block.size.toLong()).toInt(); stream.write(block, 0, count); remaining -= count; transferred += count }
            }
            val status = conn.responseCode
            check(status in 200..299) { if (status == 429) "provider rate limit; try later" else "provider returned HTTP $status" }
            check(conn.contentEncoding.isNullOrBlank() || conn.contentEncoding.equals("identity", true)) { "compressed response is unsuitable" }
            conn.inputStream.use { stream ->
                while (true) {
                    checkActive(); val count = stream.read(block); if (count < 0) break
                    received += count; transferred += count
                    check(received <= if (upload) 64 * 1024L else bytes) { "unexpected response size" }
                }
            }
            if (!upload) check(received == bytes) { "download incomplete" }
            checkActive(); val sample = TransferSample(if (upload) bytes else received, System.nanoTime() - start)
            mutable.value = mutable.value.copy(bytes = transferred)
            return sample
        } finally { conn.disconnect(); if (connection === conn) connection = null }
    }
}

fun basicNetworkInfo(context: Context): String {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return "No active network"
    val type = when { caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"; caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"; else -> "Network" }
    return "$type · ${if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) "Internet available" else "Internet not confirmed"} · ${if (manager.isActiveNetworkMetered) "Metered" else "Unmetered"}"
}
