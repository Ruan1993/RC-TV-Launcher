package za.co.rc.tv.core

import android.content.*
import android.net.*
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkKind { WIFI, WIFI_DISCONNECTED, WIFI_OFF, ETHERNET, OTHER, UNKNOWN }
data class NetworkStatus(val kind: NetworkKind=NetworkKind.UNKNOWN,val level: Int?=null,val internet: Boolean=false) {
    val label: String get() = when(kind) { NetworkKind.WIFI -> "Wi-Fi connected"; NetworkKind.WIFI_DISCONNECTED -> "Wi-Fi disconnected"; NetworkKind.WIFI_OFF -> "Wi-Fi off"; NetworkKind.ETHERNET -> "Ethernet connected"; NetworkKind.OTHER -> "Network connected"; NetworkKind.UNKNOWN -> "Network status unavailable" }
    val description: String get() = label + if(kind==NetworkKind.WIFI) (level?.let { ", signal level $it of 4" } ?: ", signal strength unavailable") else ""
}
fun wifiLevel(rssi: Int?): Int? = rssi?.takeIf { it in -126..-1 }?.let { (it.coerceIn(-100,-55)+100)*4/45 }
fun classifyNetwork(wifi: Boolean, ethernet: Boolean, other: Boolean, wifiEnabled: Boolean?, rssi: Int?, internet: Boolean=false) = when {
    ethernet -> NetworkStatus(NetworkKind.ETHERNET,internet=internet)
    wifi -> NetworkStatus(NetworkKind.WIFI,wifiLevel(rssi),internet)
    other -> NetworkStatus(NetworkKind.OTHER,internet=internet)
    wifiEnabled==false -> NetworkStatus(NetworkKind.WIFI_OFF)
    wifiEnabled==true -> NetworkStatus(NetworkKind.WIFI_DISCONNECTED)
    else -> NetworkStatus()
}

/** Activity-visible callbacks only. Never reads/stores SSID, BSSID, addresses or location. */
class NetworkStatusMonitor(context: Context) {
    private val context=context.applicationContext
    private val manager=this.context.getSystemService(ConnectivityManager::class.java)
    private val wifi=this.context.getSystemService(WifiManager::class.java)
    private val mutable=MutableStateFlow(NetworkStatus()); val state=mutable.asStateFlow()
    private var callbackRegistered=false; private var receiverRegistered=false
    private var current: Network?=null
    private fun snapshot(caps: NetworkCapabilities?=manager.getNetworkCapabilities(manager.activeNetwork)) {
        val wifiActive=caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)==true
        val enabled=runCatching { wifi?.isWifiEnabled }.getOrNull()
        @Suppress("DEPRECATION")
        val rssi=if(wifiActive) runCatching { (caps?.transportInfo as? WifiInfo)?.rssi ?: wifi?.connectionInfo?.rssi }.getOrNull() else null
        mutable.value=classifyNetwork(wifiActive,caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)==true,caps!=null && !wifiActive,enabled,rssi,caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true)
    }
    private val callback=object: ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { current=network }
        override fun onCapabilitiesChanged(network: Network,caps: NetworkCapabilities) { current=network; snapshot(caps) }
        override fun onLost(network: Network) { if(current==network) { current=null; runCatching { snapshot() }.onFailure { mutable.value=NetworkStatus() } } }
    }
    private val receiver=object: BroadcastReceiver() { override fun onReceive(context: Context?,intent: Intent?) { runCatching { snapshot() }.onFailure { mutable.value=NetworkStatus() } } }
    fun start() {
        runCatching { snapshot() }.onFailure { mutable.value=NetworkStatus() }
        if(!callbackRegistered) runCatching { manager.registerDefaultNetworkCallback(callback); callbackRegistered=true }
        if(!receiverRegistered) runCatching {
            val filter=IntentFilter().apply { addAction(WifiManager.WIFI_STATE_CHANGED_ACTION); addAction(WifiManager.RSSI_CHANGED_ACTION); addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION) }
            // Wi-Fi broadcasts can originate from the privileged Wi-Fi process. Extras are ignored;
            // every event re-reads public system state, so incoming broadcasts cannot invent status.
            if(Build.VERSION.SDK_INT>=33) context.registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED) else context.registerReceiver(receiver,filter)
            receiverRegistered=true
        }
    }
    fun stop() {
        if(callbackRegistered) { runCatching { manager.unregisterNetworkCallback(callback) }; callbackRegistered=false }
        if(receiverRegistered) { runCatching { context.unregisterReceiver(receiver) }; receiverRegistered=false }
        current=null
    }
}
