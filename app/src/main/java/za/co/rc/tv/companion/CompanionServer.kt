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

data class CompanionState(val running: Boolean = false, val message: String = "Off. Start when you want to pair a phone.",
    val address: String = "", val code: String = "", val fingerprint: String = "",
    val pending: PairingGate.Pending? = null, val devices: List<PairingGate.Session> = emptyList())

/** Explicit foreground-only listener bound to one private LAN address, never INADDR_ANY. */
class CompanionServer(context: Context, private val command: (String, String) -> Boolean) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val gate = PairingGate()
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
    @Synchronized fun start() {
        if (running) return
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
                val listener = identity.context().serverSocketFactory.createServerSocket(0, 4, link.address) as SSLServerSocket
                listener.enabledProtocols = arrayOf("TLSv1.2")
                synchronized(this) {
                    if (!running || current != generation) { listener.close(); return@Thread }
                    socket = listener
                    workers = ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, ArrayBlockingQueue(4))
                    val code = gate.start()
                    mutable.value = CompanionState(true, "Available while RC TV is on screen. Sessions expire after 30 minutes.",
                        "${link.address.hostAddress}:${listener.localPort}", code, identity.fingerprint)
                    networkWatch = object : ConnectivityManager.NetworkCallback() { override fun onLost(lost: android.net.Network) { if (lost == active) stop("Network changed. Start again to reconnect.") } }
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
            } catch (failure: Exception) { synchronized(this) { if (running && current == generation) stop(failure.message?.take(100) ?: "Local connection unavailable.") } }
        }, "RC-local-listener").apply { isDaemon = true; start() }
    }
    @Synchronized fun stop(message: String = "Off. Connections and pairing grants cleared.") {
        running = false; generation++; runCatching { socket?.close() }; socket = null
        clients.forEach { runCatching { it.close() } }; clients.clear()
        workers?.shutdownNow(); workers = null
        registration?.let { runCatching { nsd.unregisterService(it) } }; registration = null
        networkWatch?.let { runCatching { context.getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it) } }; networkWatch = null
        gate.clear(); mutable.value = CompanionState(message = message)
    }
    fun approve(id: String) { gate.approve(id); refresh() }
    fun reject(id: String) { gate.reject(id); refresh() }
    fun revoke(id: String) { gate.revoke(id); refresh() }
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
            "hello" -> JSONObject().put("ok", true).put("name", "RC TV").put("protocol", 1)
            "pair" -> gate.request(request.optString("code").take(16), request.optString("name"))?.let { JSONObject().put("ok", true).put("request", it) } ?: errorReply("Pairing unavailable, expired or rate limited. Check the TV.")
            "poll" -> gate.poll(request.optString("request").take(128))?.let { JSONObject().put("ok", true).put("approved", it.first).apply { it.second?.let { value -> put("token", value) } } } ?: errorReply("Pairing expired or declined.")
            "status" -> if (gate.authenticated(token)) JSONObject().put("ok", true) else errorReply("Disconnected or revoked. Pair again.")
            "disconnect" -> if (gate.authenticated(token)) { gate.disconnect(token); JSONObject().put("ok", true) } else errorReply("Not authorised")
            "command" -> {
                val key = request.optString("key")
                val text = request.optString("text")
                if (key !in LinkWire.COMMANDS || text.length > 160 || (key != "SEARCH_TEXT" && text.isNotEmpty()) || !gate.authorize(token, request.optLong("sequence", -1))) errorReply("Command rejected")
                else {
                    val task = FutureTask { running && command(key, text) }; main.post(task)
                    if (runCatching { task.get(2, TimeUnit.SECONDS) }.getOrDefault(false)) JSONObject().put("ok", true) else { task.cancel(false); errorReply("Use the physical remote to close dialogs, or return to RC TV.") }
                }
            }
            else -> errorReply("Unsupported operation")
        }
        refresh(); return result
    }
    private fun errorReply(message: String) = JSONObject().put("ok", false).put("error", message)
    private fun sameSubnet(peer: InetAddress, local: InetAddress, prefix: Int): Boolean {
        if (peer !is Inet4Address || !peer.isSiteLocalAddress || prefix !in 1..32) return false
        val a = peer.address; val b = local.address
        return a.indices.all { i -> val bits = (prefix - i * 8).coerceIn(0, 8); val mask = (0xff shl (8 - bits)) and 0xff; (a[i].toInt() and mask) == (b[i].toInt() and mask) }
    }
}
