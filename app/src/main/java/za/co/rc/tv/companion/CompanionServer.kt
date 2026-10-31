package za.co.rc.tv.companion

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import za.co.rc.tv.link.LinkWire
import za.co.rc.tv.link.SecureRecord

data class CompanionState(val running: Boolean = false, val message: String = "Off. Start when you want to pair a phone.",
    val address: String = "", val code: String = "", val fingerprint: String = "",
    val pending: PairingGate.Pending? = null, val devices: List<PairingGate.Session> = emptyList(), val connectionEvent: Long = 0)

/** Explicit opt-in listener bound to one private LAN address, never INADDR_ANY. */
class CompanionServer(context: Context, private val command: (String, String) -> CommandResult) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val gate = PairingGate()
    private val storage = SecureRecord(context, "companion-grants")
    private val config = context.getSharedPreferences("companion-connection", Context.MODE_PRIVATE)
    private var loaded = false
    private var connectionEvent = 0L
    private val mutable = MutableStateFlow(CompanionState())
    val state = mutable.asStateFlow()
    @Volatile private var running = false
    @Volatile private var generation = 0
    private val clients = java.util.concurrent.ConcurrentHashMap.newKeySet<SSLSocket>()
    private var socket: SSLServerSocket? = null
    private var workers: ThreadPoolExecutor? = null
    private var registration: NsdManager.RegistrationListener? = null
    private val nsd = context.getSystemService(NsdManager::class.java)
    private var networkWatch: ConnectivityManager.NetworkCallback? = null
    fun resume() { if (config.getBoolean("enabled", false)) start(false) }
    fun preparePairing() { if (state.value.running) { gate.preparePairing(); refresh() } }
    fun refreshPairingDisplay() { if (state.value.running) refresh() }
    @Synchronized fun start(newPairing: Boolean = true) {
        config.edit().putBoolean("enabled", true).apply()
        if (running) { if (newPairing) { gate.pairingCode(); refresh() }; return }
        val current = ++generation
        running = true; mutable.value = CompanionState(message = "Starting encrypted local connection…")
        Thread({
            try {
                val network = context.getSystemService(ConnectivityManager::class.java)
                val active = network.activeNetwork ?: error("Connect the TV to Wi-Fi or Ethernet first.")
                val caps = network.getNetworkCapabilities(active) ?: error("Local network unavailable.")
                check(!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) { "Use a local Wi-Fi or Ethernet network without a VPN." }
                val link = network.getLinkProperties(active)?.linkAddresses?.firstOrNull { it.address is Inet4Address && it.address.isSiteLocalAddress }
                    ?: error("A private IPv4 local address is required.")
                val identity = LocalIdentity()
                val factory = identity.context().serverSocketFactory
                val listener = runCatching { factory.createServerSocket(config.getInt("port", 0), 4, link.address) }
                    .getOrElse { factory.createServerSocket(0, 4, link.address) } as SSLServerSocket
                listener.enabledProtocols = arrayOf("TLSv1.2")
                synchronized(this) {
                    if (!running || current != generation) { listener.close(); return@Thread }
                    socket = listener
                    if (!loaded) {
                        val records = storage.read()?.optJSONArray("grants")
                        gate.restore((0 until (records?.length() ?: 0)).map { i -> records!!.getJSONObject(i).let { PairingGate.Record(it.getString("hash"), it.getString("id"), it.getString("name"), it.getLong("until")) } })
                        loaded = true
                    }
                    config.edit().putInt("port", listener.localPort).apply()
                    workers = ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, ArrayBlockingQueue(4))
                    val code = if (newPairing || gate.records().isEmpty()) gate.pairingCode() else ""
                    mutable.value = CompanionState(true, "Ready. System control requires separate TV opt-in and accessibility consent. Pairing expires after seven days.",
                        "${link.address.hostAddress}:${listener.localPort}", code, identity.fingerprint, devices = gate.sessions())
                    networkWatch = object : ConnectivityManager.NetworkCallback() { override fun onLost(lost: android.net.Network) { if (lost == active) suspendConnection("Network changed. Return to RC TV or retry to reconnect.") } }
                    network.registerDefaultNetworkCallback(networkWatch!!)
                    advertise(listener.localPort)
                }
                while (running && current == generation) {
                    val client = listener.accept() as SSLSocket
                    if (!sameSubnet(client.inetAddress, link.address, link.prefixLength)) { client.close(); continue }
                    client.soTimeout = 4000
                    clients.add(client)
                    try { workers?.execute { serve(client, current) } ?: client.close() } catch (_: Exception) { clients.remove(client); client.close() }
                }
            } catch (failure: Exception) { synchronized(this) { if (running && current == generation) suspendConnection(failure.message?.take(100) ?: "Local connection unavailable.") } }
        }, "RC-local-listener").apply { isDaemon = true; start() }
    }
    @Synchronized fun stop(message: String = "Off. Connections and pairing grants cleared.") {
        config.edit().putBoolean("enabled", false).apply()
        suspendConnection(message); gate.clear(); storage.clear(); loaded = true
        mutable.value = CompanionState(message = message)
    }
    @Synchronized fun suspendConnection(message: String = "Paused. Approved phones reconnect when you return to RC TV.") {
        running = false; generation++; runCatching { socket?.close() }; socket = null
        clients.forEach { runCatching { it.close() } }; clients.clear()
        workers?.shutdownNow(); workers = null
        registration?.let { runCatching { nsd.unregisterService(it) } }; registration = null
        networkWatch?.let { runCatching { context.getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it) } }; networkWatch = null
        gate.suspendLeases(); mutable.value = CompanionState(message = message, devices = gate.sessions())
    }
    private fun persist() { storage.write(JSONObject().put("grants", org.json.JSONArray().apply { gate.records().forEach { put(JSONObject().put("hash", it.hash).put("id", it.id).put("name", it.name).put("until", it.until)) } })) }
    fun approve(id: String) { if (gate.approve(id)) { try { persist() } catch (_: Exception) { gate.clear(); storage.clear() } }; refresh() }
    fun reject(id: String) { gate.reject(id); refresh() }
    fun revoke(id: String) { gate.revoke(id); runCatching { persist() }.onFailure { stop("Credential storage unavailable; all devices forgotten.") }; refresh() }
    private fun refresh() { mutable.value = mutable.value.copy(code = gate.currentCode(), pending = gate.pending()?.takeIf { it.token == null }, devices = gate.sessions()) }
    private fun advertise(port: Int) {
        val callback = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) { if (!running) runCatching { nsd.unregisterService(this) } }
            override fun onRegistrationFailed(info: NsdServiceInfo, error: Int) { mutable.value = mutable.value.copy(message = "Discovery unavailable. Enter the displayed address on your phone.") }
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, error: Int) {}
        }
        registration = callback
        nsd.registerService(NsdServiceInfo().apply { serviceName = "RC TV"; serviceType = LinkWire.SERVICE; setPort(port) }, NsdManager.PROTOCOL_DNS_SD, callback)
    }
    private fun serve(client: SSLSocket, current: Int) {
        client.use {
            try {
                try { it.startHandshake() } catch (failure: Exception) {
                    if (za.co.rc.tv.BuildConfig.DEBUG) android.util.Log.w("RcLink", "TLS handshake failed", failure)
                    return@use
                }
                val request = LinkWire.read(it)
                val response = if (running && current == generation) handle(request) else errorReply("Stopped")
                LinkWire.write(it, response)
            } catch (_: Exception) { /* Deliberately no payload/credential logging. */ }
        }
        clients.remove(client)
    }
    private fun handle(request: JSONObject): JSONObject {
        val token = request.optString("token").take(128)
        val result = when (request.optString("op")) {
            "hello" -> JSONObject().put("ok", true).put("name", "RC TV").put("protocol", 2)
            "pair" -> gate.request(request.optString("code").take(16), request.optString("name"))?.let { JSONObject().put("ok", true).put("request", it) } ?: errorReply("Pairing unavailable, expired or rate limited. Check the TV.")
            "poll" -> gate.poll(request.optString("request").take(128))?.let { JSONObject().put("ok", true).put("approved", it.first).apply { it.second?.let { value -> put("token", value) } } } ?: errorReply("Pairing expired or declined.")
            "resume" -> gate.resume(token)?.let { lease ->
                main.post { connectionEvent++; mutable.value = mutable.value.copy(connectionEvent = connectionEvent) }
                accessReply().put("lease", lease)
            } ?: errorReply("Pairing expired or revoked.", "revoked")
            "status" -> if (!gate.authenticated(token)) errorReply("Disconnected or revoked. Pair again.", "revoked")
                else if (!gate.validLease(token, request.optString("lease"))) errorReply("Resume this connection.", "resume") else accessReply()
            "disconnect" -> if (gate.authenticated(token)) { gate.disconnect(token); persist(); JSONObject().put("ok", true) } else errorReply("Not authorised", "revoked")
            "command" -> {
                val key = request.optString("key")
                val text = request.optString("text")
                if (!gate.authenticated(token)) errorReply("Pairing expired or revoked.", "revoked")
                else if (!gate.validLease(token, request.optString("lease"))) errorReply("Resume this connection.", "resume")
                else if (!RemotePolicy.validPayload(key, text) || !gate.authorize(token, request.optLong("sequence", -1), request.optString("lease"))) errorReply("Command rejected")
                else {
                    val task = FutureTask { if (running) command(key, text) else CommandResult(false, "Remote stopped.") }; main.post(task)
                    val outcome = runCatching { task.get(2, TimeUnit.SECONDS) }.getOrNull()
                    if (outcome == null) { task.cancel(false); errorReply("TV busy. Use the physical remote; this request was not confirmed.") }
                    else if (outcome.accepted) JSONObject().put("ok", true).put("message", if (key.startsWith("MEDIA_")) outcome.message else "Command accepted by Android or RC TV; app behaviour may vary.")
                    else errorReply(outcome.message)
                }
            }
            else -> errorReply("Unsupported operation")
        }
        refresh(); return result
    }
    private fun errorReply(message: String, reason: String = "rejected") = JSONObject().put("ok", false).put("error", message).put("reason", reason)
    private fun accessReply() = JSONObject().put("ok", true)
        .put("systemControl", RemoteRuntime.optedIn(context))
        .put("accessibilityConnected", RemoteRuntime.accessibility != null)
    private fun sameSubnet(peer: InetAddress, local: InetAddress, prefix: Int): Boolean {
        if (peer !is Inet4Address || !peer.isSiteLocalAddress || prefix !in 1..32) return false
        val a = peer.address; val b = local.address
        return a.indices.all { i -> val bits = (prefix - i * 8).coerceIn(0, 8); val mask = (0xff shl (8 - bits)) and 0xff; (a[i].toInt() and mask) == (b[i].toInt() and mask) }
    }
}
