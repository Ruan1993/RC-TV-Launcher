package za.co.rc.tv.phone

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import za.co.rc.tv.link.LinkWire

class PhoneActivity : Activity() {
    private val main = Handler(Looper.getMainLooper())
    private val io = ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, ArrayBlockingQueue(8))
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var devices: LinearLayout
    private lateinit var controls: LinearLayout
    private var client: LinkClient? = null
    private var discovery: NsdManager.DiscoveryListener? = null
    private val found = mutableSetOf<String>()
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    @Volatile private var visible = false
    @Volatile private var generation = 0
    private var lastCommand = 0L
    private val nsd by lazy { getSystemService(NsdManager::class.java) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply { setBackgroundColor(0xFF09151F.toInt()) }
        content = column().apply { setPadding(dp(24), dp(32), dp(24), dp(24)) }; scroll.addView(content); setContentView(scroll)
        text("RC TV", 32); text("Your world. In focus.", 16)
        status = text("Start Phone companion in RC TV Settings. Both devices must use the same trusted local network.", 16)
        button("Find my TV", content) { discover() }
        devices = column(); content.addView(devices)
        val address = editor("TV address:port", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        content.addView(address)
        button("Connect by address", content) {
            val parts = address.text.toString().trim().split(':')
            if (parts.size != 2 || parts[1].toIntOrNull() !in 1..65535) message("Enter the private address and port shown on your TV.")
            else connect(parts[0], parts[1].toInt())
        }
        controls = column(); content.addView(controls)
        text("Controls work inside RC TV only. Leaving RC TV stops the TV connection. Your physical remote remains available. Pairing lasts for this foreground session only.", 14)
    }
    override fun onStart() { super.onStart(); visible = true; discover() }
    override fun onStop() {
        visible = false; generation++; main.removeCallbacksAndMessages(null); stopDiscovery()
        val old = client; client = null; controls.removeAllViews()
        if (old != null) background { old.disconnect() }
        super.onStop()
    }
    override fun onDestroy() { io.shutdown(); super.onDestroy() }
    private fun dp(value: Int) = (resources.displayMetrics.density * value).toInt()
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; isSaveEnabled = false }
    private fun text(value: String, size: Int, parent: LinearLayout = content): TextView = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(0xFFD4EDE7.toInt()); setPadding(0, dp(8), 0, dp(8)); parent.addView(this)
    }
    private fun editor(hintValue: String, kind: Int) = EditText(this).apply {
        hint = hintValue; inputType = kind; setTextColor(Color.WHITE); setHintTextColor(0xFF91A9AA.toInt()); isSaveEnabled = false
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS; setSingleLine()
    }
    private fun button(label: String, parent: LinearLayout, click: () -> Unit): Button = Button(this).apply {
        text = label; isAllCaps = false; textSize = 17f; setTextColor(ColorStateList.valueOf(0xFF11272D.toInt()))
        backgroundTintList = ColorStateList.valueOf(0xFFBAE4DA.toInt()); minHeight = dp(56)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(5), 0, dp(5)) }); setOnClickListener { click() }
    }
    private fun message(value: String) { if (visible) status.text = value }
    private fun background(action: () -> Unit) { try { io.execute(action) } catch (_: Exception) { message("Connection busy. Try again shortly.") } }
    private fun stopDiscovery() {
        discovery?.let { runCatching { nsd.stopServiceDiscovery(it) } }; discovery = null; resolveQueue.clear(); found.clear()
    }
    private fun discover() {
        stopDiscovery(); devices.removeAllViews(); message("Finding RC TV on your local network…")
        val scan = generation
        discovery = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) {}
            override fun onDiscoveryStopped(type: String) {}
            override fun onStartDiscoveryFailed(type: String, code: Int) { main.post { message("Discovery unavailable. Use the address shown on TV.") } }
            override fun onStopDiscoveryFailed(type: String, code: Int) {}
            override fun onServiceLost(info: NsdServiceInfo) {}
            override fun onServiceFound(info: NsdServiceInfo) { main.post { if (visible && scan == generation) { resolveQueue.add(info); resolveNext(scan) } } }
        }
        runCatching { nsd.discoverServices(LinkWire.SERVICE, NsdManager.PROTOCOL_DNS_SD, discovery!!) }.onFailure { message("Use the address shown on your TV.") }
    }
    private fun resolveNext(scan: Int) {
        if (resolving || resolveQueue.isEmpty()) return
        resolving = true
        nsd.resolveService(resolveQueue.removeFirst(), object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, code: Int) { main.post { resolving = false; if (visible) resolveNext(generation) } }
            override fun onServiceResolved(info: NsdServiceInfo) { main.post {
                resolving = false
                if (visible && scan == generation) {
                    val host = info.host?.hostAddress
                    if (host != null && ':' !in host && found.add("$host:${info.port}")) button("${info.serviceName} · $host", devices) { connect(host, info.port) }
                    message("Select your TV. Compare its security fingerprint before pairing.")
                }
                if (visible) resolveNext(generation)
            } }
        })
    }
    private fun connect(host: String, port: Int) {
        stopDiscovery()
        generation++; val current = generation; val old = client; client = null; controls.removeAllViews()
        background {
            old?.disconnect()
            val candidate = LinkClient(host, port)
            try {
                val fingerprint = candidate.probe()
                main.post { if (visible && current == generation) AlertDialog.Builder(this)
                    .setTitle("Verify your TV")
                    .setMessage("Compare EVERY group with Phone companion on the TV. Do not continue if they differ.\n\n${LinkWire.displayFingerprint(fingerprint)}")
                    .setNegativeButton("Cancel", null).setPositiveButton("Matches my TV") { _, _ -> pair(candidate, current) }.show() }
            } catch (_: Exception) { main.post { if (current == generation) message("Cannot connect securely. Check the address, TV screen and local network.") } }
        }
    }
    private fun pair(candidate: LinkClient, current: Int) {
        val code = editor("Six-digit code on TV", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        AlertDialog.Builder(this).setTitle("Pair with RC TV").setView(code).setNegativeButton("Cancel", null).setPositiveButton("Request pairing") { _, _ ->
            if (!visible || current != generation) { code.text.clear(); return@setPositiveButton }
            val value = code.text.toString(); code.text.clear()
            if (!value.matches(Regex("[0-9]{6}"))) { message("Enter the six-digit TV code."); return@setPositiveButton }
            background {
                try {
                    val response = candidate.request(JSONObject().put("op", "pair").put("code", value).put("name", android.os.Build.MODEL.take(32)))
                    main.post { message("Approve this phone on your TV using the physical remote."); poll(candidate, response.getString("request"), current, 0) }
                } catch (_: Exception) { main.post { message("Pairing failed or expired. Restart pairing on the TV and try again.") } }
            }
        }.show()
    }
    private fun poll(candidate: LinkClient, request: String, current: Int, count: Int) {
        if (!visible || current != generation || count >= 60) return
        main.postDelayed({ background {
            try {
                val response = candidate.request(JSONObject().put("op", "poll").put("request", request))
                main.post {
                    if (visible && current == generation) {
                        if (response.optBoolean("approved")) { candidate.token = response.getString("token"); client = candidate; remote(); heartbeat(candidate, current) }
                        else poll(candidate, request, current, count + 1)
                    }
                }
            } catch (_: Exception) { main.post { if (current == generation) message("Pairing declined, stopped or expired.") } }
        } }, 1000)
    }
    private fun remote() {
        controls.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xF5283D49.toInt(), 0xFA11232D.toInt())).apply { cornerRadius = dp(20).toFloat(); setStroke(dp(1), 0x557DA99F) }
        controls.setPadding(dp(16), dp(12), dp(16), dp(12))
        controls.removeAllViews(); message("Connected · encrypted local control")
        button("↑", controls) { send("UP") }
        val row = LinearLayout(this); controls.addView(row)
        for ((label, key) in listOf("←" to "LEFT", "OK" to "OK", "→" to "RIGHT")) {
            val holder = column(); row.addView(holder, LinearLayout.LayoutParams(0, -2, 1f)); button(label, holder) { send(key) }
        }
        button("↓", controls) { send("DOWN") }; button("Back", controls) { send("BACK") }; button("RC TV Home", controls) { send("HOME") }
        val search = editor("App name · never enter passwords", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); controls.addView(search)
        button("Search apps on TV", controls) { val value = search.text.toString().take(160); search.text.clear(); send("SEARCH_TEXT", value) }
        button("Disconnect", controls) {
            generation++; val old = client; client = null; controls.removeAllViews(); background { old?.disconnect() }; message("Disconnected. Start pairing again on the TV to reconnect.")
        }
    }
    private fun send(key: String, text: String = "") {
        if (SystemClock.uptimeMillis() - lastCommand < 80) return
        lastCommand = SystemClock.uptimeMillis(); val connection = client ?: return; val current = generation
        background { if (!visible || current != generation) return@background
            try { connection.command(key, text); main.post { message("Connected · encrypted local control") } }
            catch (_: Exception) { main.post { message("Connection ended or command unavailable. Check the TV; use the physical remote if another app opened.") } } }
    }
    private fun heartbeat(connection: LinkClient, current: Int) {
        main.postDelayed({ if (visible && current == generation) background {
            try { connection.request(JSONObject().put("op", "status").put("token", connection.token)); main.post { if (current == generation) heartbeat(connection, current) } }
            catch (_: Exception) { main.post { if (current == generation) { client = null; controls.removeAllViews(); message("Disconnected or revoked. Start pairing again on the TV.") } } }
        } }, 5000)
    }
}
