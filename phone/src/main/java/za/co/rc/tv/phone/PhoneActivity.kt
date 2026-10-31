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
import android.view.ViewGroup
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
    private val credentials by lazy { za.co.rc.tv.link.SecureRecord(this, "approved-tv") }
    private var connected = false
    private var setupStatus: TextView? = null
    private lateinit var connectionPanel: LinearLayout
    private lateinit var connectionState: TextView
    private lateinit var deviceTitle: TextView
    private var typingDialog: android.app.Dialog? = null
    private var typingFeedback: TextView? = null
    private val appearance by lazy { getSharedPreferences("remote-ui", MODE_PRIVATE) }
    private val nsd by lazy { getSystemService(NsdManager::class.java) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF09151F.toInt(); window.navigationBarColor = 0xFF09151F.toInt()
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF152F3A.toInt(), 0xFF09151F.toInt(), 0xFF101E2B.toInt()))
            setOnApplyWindowInsetsListener { view, insets ->
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    val safe = insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout())
                    view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
                } else view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                insets
            }
        }
        content = column().apply { setPadding(dp(20), dp(12), dp(20), dp(16)) }; scroll.addView(content); setContentView(scroll)
        val header = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL }; content.addView(header)
        val heading = column(); header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        text("RC TV REMOTE", 12, heading).letterSpacing = .14f
        deviceTitle = text("Your TV", 25, heading)
        connectionState = text("Not connected", 14, heading)
        val more = button("More", column()) { showOptions() }
        (more.parent as ViewGroup).removeView(more); header.addView(more, LinearLayout.LayoutParams(dp(76), dp(52)))
        status = text("Choose your TV to get started.", 15)
        val actions = LinearLayout(this); content.addView(actions)
        rowButton("Reconnect", actions) { val saved = client; if (saved?.token != null) { generation++; reconnect(saved, generation, 0) } else discover() }
        rowButton("Change TV", actions) { generation++; connected = false; controls.visibility = View.GONE; connectionPanel.visibility = View.VISIBLE; discover() }
        connectionPanel = column().apply { setPadding(0, dp(20), 0, 0) }; content.addView(connectionPanel)
        text("Connect your phone", 24, connectionPanel)
        text("On your TV, open RC TV > Remote. Keep both devices on the same Wi-Fi or local network.", 16, connectionPanel)
        button("Find my TV", connectionPanel) { discover() }
        devices = column(); connectionPanel.addView(devices)
        text("No TV listed? Check that RC TV is open on the TV. Manual address entry is in More.", 14, connectionPanel)
        controls = column(); content.addView(controls)
    }

    override fun onStart() {
        super.onStart(); visible = true; discover()
        val current = generation
        background {
            val saved = credentials.read()
            main.post { if (visible && current == generation && saved != null) runCatching {
                val restored = LinkClient(saved.getString("host"), saved.getInt("port"), saved.getString("pin")).apply { token = saved.getString("token") }
                client = restored; reconnect(restored, current, 0)
            }.onFailure { credentials.clear(); message("Disconnected. Pair with your TV again.") } }
        }
    }
    override fun onStop() {
        visible = false; generation++; main.removeCallbacksAndMessages(null); stopDiscovery()
        typingDialog?.dismiss(); typingDialog = null
        client = null; connected = false; controls.removeAllViews()
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
        text = label; contentDescription = label; isAllCaps = false; textSize = 16f
        setTextColor(0xFFE3F3EE.toInt()); minHeight = dp(56); minimumWidth = 0
        background = android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x449FE9D5), GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xE32C4652.toInt(), 0xEF192F3C.toInt())).apply {
            cornerRadius = dp(18).toFloat(); setStroke(dp(1), 0x557AABA8)
        }, null)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(3), dp(5), dp(3), dp(5)) })
        setOnTouchListener { v, event ->
            val reduce = android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            if (!reduce) when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> v.animate().scaleX(.97f).scaleY(.97f).setDuration(70).start()
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            false
        }
        setOnClickListener { if (appearance.getBoolean("haptics", false)) performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); click() }
    }
    private fun rowButton(label: String, row: LinearLayout, click: () -> Unit): Button {
        val holder = column(); row.addView(holder, LinearLayout.LayoutParams(0, -2, 1f)); return button(label, holder, click)
    }
    private fun message(value: String) {
        if (!visible) return
        connectionState.text = if (value.startsWith("Reconnecting")) "Reconnecting…" else if (connected) "Connected · Secure" else "Not connected"
        connectionState.setTextColor(if (connected) 0xFFB6EDDC.toInt() else 0xFFF1D39F.toInt())
        status.text = value
        status.visibility = if (value == "Ready when you are") View.GONE else View.VISIBLE
    }
    private fun showOptions() {
        AlertDialog.Builder(this).setTitle("Remote options").setItems(arrayOf("${if (appearance.getBoolean("haptics", false)) "Turn off" else "Turn on"} button vibration", "Search apps in RC TV", "Enter TV address", "Close setup on TV", "Forget this TV")) { _, which ->
            when (which) {
                0 -> appearance.edit().putBoolean("haptics", !appearance.getBoolean("haptics", false)).apply()
                1 -> {
                    val query = editor("App name", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
                    query.filters = arrayOf(android.text.InputFilter.LengthFilter(160))
                    AlertDialog.Builder(this).setTitle("Search apps in RC TV").setMessage("Open RC TV on the television first.").setView(query)
                        .setNegativeButton("Cancel", null).setPositiveButton("Search") { _, _ -> val text = query.text.toString(); query.text.clear(); send("SEARCH_TEXT", text) }.show()
                }
                2 -> {
                    val address = editor("Address shown on TV", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
                    AlertDialog.Builder(this).setTitle("Connect by address").setView(address).setNegativeButton("Cancel", null).setPositiveButton("Connect") { _, _ ->
                        val parts = address.text.toString().trim().split(':')
                        if (parts.size == 2 && parts[1].toIntOrNull() in 1..65535) connect(parts[0], parts[1].toInt()) else message("Use the address and port shown on the TV.")
                    }.show()
                }
                3 -> send("CLOSE_SETUP")
                4 -> AlertDialog.Builder(this).setTitle("Forget this TV?").setMessage("You can connect again using a new code on the TV.").setNegativeButton("Keep TV", null).setPositiveButton("Forget") { _, _ ->
                    generation++; val old = client; client = null; connected = false; controls.removeAllViews(); controls.visibility = View.GONE; connectionPanel.visibility = View.VISIBLE
                    background { credentials.clear(); old?.disconnect() }; message("TV forgotten. Choose a TV to connect.")
                }.show()
            }
        }.show()
    }
    private fun background(action: () -> Unit) { try { io.execute(action) } catch (_: Exception) { message("Connection busy. Try again shortly.") } }
    private fun stopDiscovery() {
        discovery?.let { runCatching { nsd.stopServiceDiscovery(it) } }; discovery = null; resolveQueue.clear(); found.clear()
    }
    private fun discover() {
        if (!connected) { connectionPanel.visibility = View.VISIBLE; controls.visibility = View.GONE }
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
                    if (host != null && ':' !in host && found.add("$host:${info.port}")) {
                        button(info.serviceName, devices) { connect(host, info.port) }
                        val old = client
                        if (!connected && old?.token != null && (old.host != host || old.port != info.port)) {
                            val replacement = LinkClient(host, info.port, old.fingerprint).apply { token = old.token }
                            generation++; client = replacement; reconnect(replacement, generation, 0)
                        }
                    }
                    if (client == null) message("Select your TV. Compare its security fingerprint before pairing.")
                }
                if (visible) resolveNext(generation)
            } }
        })
    }
    private fun connect(host: String, port: Int) {
        stopDiscovery()
        generation++; val current = generation; connected = false; controls.removeAllViews()
        background {
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
        val code = editor("6-digit TV code", InputType.TYPE_CLASS_NUMBER).apply {
            textSize = 26f; gravity = android.view.Gravity.CENTER; filters = arrayOf(android.text.InputFilter.LengthFilter(6))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        val dialog = AlertDialog.Builder(this).setTitle("Enter the code on your TV")
            .setMessage("Then approve this phone using your TV remote.").setView(code)
            .setNegativeButton("Cancel", null).setPositiveButton("Connect", null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!visible || current != generation) { dialog.dismiss(); return@setOnClickListener }
            val value = code.text.toString()
            if (!value.matches(Regex("[0-9]{6}"))) { code.error = "Enter all six digits from the TV"; return@setOnClickListener }
            dialog.dismiss()
            background {
                try {
                    val response = candidate.request(JSONObject().put("op", "pair").put("code", value).put("name", android.os.Build.MODEL.take(32)))
                    main.post { if (visible && current == generation) { message("Approve this phone on your TV using the physical remote."); poll(candidate, response.getString("request"), current, 0) } }
                } catch (_: Exception) { main.post { if (visible && current == generation) message("That code could not be used. Choose New pairing code on the TV and try again.") } }
            }
        } }
        dialog.setOnDismissListener { code.text.clear() }; dialog.show()
    }
    private fun poll(candidate: LinkClient, request: String, current: Int, count: Int) {
        if (!visible || current != generation || count >= 60) return
        main.postDelayed({ background {
            try {
                val response = candidate.request(JSONObject().put("op", "poll").put("request", request))
                main.post {
                    if (visible && current == generation) {
                        if (response.optBoolean("approved")) { candidate.token = response.getString("token"); client = candidate; reconnect(candidate, current, 0) }
                        else poll(candidate, request, current, count + 1)
                    }
                }
            } catch (_: Exception) { main.post { if (current == generation) message("Pairing declined, stopped or expired.") } }
        } }, 1000)
    }
    private fun updateSetupStatus(response: JSONObject) {
        setupStatus?.visibility = if (response.optBoolean("systemControl") && response.optBoolean("accessibilityConnected")) View.GONE else View.VISIBLE
        setupStatus?.text = if (response.optBoolean("systemControl") && response.optBoolean("accessibilityConnected"))
            "Ready for compatible TV apps."
        else "To control other apps, finish Remote setup on your TV."
    }
    private fun remote(response: JSONObject) {
        connected = true; connectionPanel.visibility = View.GONE; controls.visibility = View.VISIBLE; controls.removeAllViews()
        deviceTitle.text = "RC TV"; message("Ready when you are")
        stopDiscovery()
        setupStatus = text("", 14, controls); updateSetupStatus(response)
        val padSize = minOf(resources.configuration.screenWidthDp - 52, (resources.configuration.screenHeightDp - 440).coerceAtLeast(192), 300)
        val pad = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF29444F.toInt(), 0xFF132A36.toInt())).apply { cornerRadius = dp(44).toFloat(); setStroke(dp(1), 0x557BA9A7) }
        }
        controls.addView(pad, LinearLayout.LayoutParams(dp(padSize), dp(padSize)).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL; setMargins(0, dp(10), 0, dp(10)) })
        val cell = padSize / 3
        for ((label, key, position) in listOf(Triple("↑", "UP", 1 to 0), Triple("←", "LEFT", 0 to 1), Triple("OK", "OK", 1 to 1), Triple("→", "RIGHT", 2 to 1), Triple("↓", "DOWN", 1 to 2))) {
            val holder = column(); val control = button(label, holder) { send(key) }
            holder.removeView(control); control.textSize = if (key == "OK") 24f else 30f
            control.contentDescription = mapOf("UP" to "Up", "DOWN" to "Down", "LEFT" to "Left", "RIGHT" to "Right", "OK" to "OK, select")[key]
            if (key == "OK") { control.setTextColor(0xFF102A32.toInt()); control.backgroundTintList = ColorStateList.valueOf(0xFFB8E8DA.toInt()) }
            pad.addView(control, FrameLayout.LayoutParams(dp(cell - 8), dp(cell - 8)).apply { leftMargin = dp(position.first * cell + 4); topMargin = dp(position.second * cell + 4) })
        }
        val navigation = LinearLayout(this); controls.addView(navigation)
        rowButton("Back", navigation) { send("BACK") }; rowButton("Home", navigation) { send("HOME") }; rowButton("Keyboard", navigation) { showTyping() }
        val media = LinearLayout(this); controls.addView(media)
        rowButton("Previous", media) { send("MEDIA_PREVIOUS") }; rowButton("Play / Pause", media) { send("MEDIA_PLAY_PAUSE") }; rowButton("Next", media) { send("MEDIA_NEXT") }
        text("Your world. In focus.", 13, controls).gravity = android.view.Gravity.CENTER
    }
    private fun showTyping() {
        if (!connected) { message("Reconnect to your TV before typing."); return }
        if (typingDialog?.isShowing == true) return
        val dialog = android.app.Dialog(this); dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val sheet = column().apply { setPadding(dp(20), dp(18), dp(20), dp(18)); setBackgroundColor(0xFF152C38.toInt()) }
        val titleRow = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL }; sheet.addView(titleRow)
        text("Type on your TV", 23, column()).also { title ->
            (title.parent as ViewGroup).removeView(title); titleRow.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        }
        val done = button("Done", column()) { dialog.dismiss() }
        (done.parent as ViewGroup).removeView(done); titleRow.addView(done, LinearLayout.LayoutParams(dp(72), -2))
        text("Select a TV text box first. RC TV Keyboard is needed. No password fields.", 14, sheet).setPadding(0, dp(3), 0, dp(3))
        val input = editor("Write something…", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS).apply {
            setSingleLine(false); minLines = 1; maxLines = 3; filters = arrayOf(android.text.InputFilter.LengthFilter(160))
            imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
        }
        sheet.addView(input, LinearLayout.LayoutParams(-1, -2))
        typingFeedback = text("Up to 160 characters · sent only when you tap Send", 13, sheet).apply { setPadding(0, dp(3), 0, dp(3)) }
        val edit = LinearLayout(this); sheet.addView(edit)
        rowButton("Send to TV", edit) {
            val value = input.text.toString()
            if (value.isBlank()) { typingFeedback?.text = "Type something first." }
            else { typingFeedback?.text = "Sending…"; send("INPUT_TEXT", value) { if (input.text.toString() == value) input.text.clear(); typingFeedback?.text = "Text sent to TV" } }
        }
        rowButton("Delete on TV", edit) { send("INPUT_DELETE") }
        dialog.setContentView(ScrollView(this).apply { addView(sheet) }); dialog.setOnDismissListener { input.text.clear(); typingFeedback = null; typingDialog = null }
        dialog.show(); dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent); setLayout(-1, -2); setGravity(android.view.Gravity.BOTTOM)
            setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        typingDialog = dialog; input.requestFocus(); input.postDelayed({ if (dialog.isShowing) getSystemService(android.view.inputmethod.InputMethodManager::class.java).showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT) }, 150)
    }
    private fun send(key: String, text: String = "", accepted: () -> Unit = {}) {
        if (SystemClock.uptimeMillis() - lastCommand < 80) return
        if (!connected) { typingFeedback?.text = "Connection lost. Your text is kept. Reconnect, then send again."; return }
        lastCommand = SystemClock.uptimeMillis(); val connection = client ?: return; val current = generation
        background { if (!visible || current != generation) return@background
            try { val response = connection.command(key, text); main.post { if (current == generation) {
                message(if (key.startsWith("MEDIA_")) "Media request sent" else if (key == "INPUT_TEXT") "Text sent to TV" else "Ready when you are"); accepted()
                if (key == "INPUT_TEXT") Toast.makeText(this, "Text sent to TV", Toast.LENGTH_SHORT).show()
            } } }
            catch (failure: LinkRejected) { main.post { if (current == generation) {
                if (failure.reason in listOf("resume", "revoked")) { generation++; reconnect(connection, generation, 0) }
                else { typingFeedback?.text = failure.message ?: "This field does not accept phone typing."; message(failure.message ?: "Use the physical remote for this screen."); Toast.makeText(this, failure.message ?: "Control unavailable", Toast.LENGTH_LONG).show() }
            } } }
            catch (_: Exception) { main.post { if (current == generation) { generation++; reconnect(connection, generation, 0) } } } }
    }
    private fun heartbeat(connection: LinkClient, current: Int) {
        main.postDelayed({ if (visible && current == generation) background {
            try { val response = connection.status(); main.post { if (current == generation) { updateSetupStatus(response); heartbeat(connection, current) } } }
            catch (_: Exception) { main.post { if (current == generation) { generation++; reconnect(connection, generation, 0) } } }
        } }, 5000)
    }
    private fun reconnect(connection: LinkClient, current: Int, attempt: Int) {
        if (!visible || current != generation) return
        connected = false; client = connection; connectionPanel.visibility = View.GONE; message("Reconnecting… Keep your TV on."); typingFeedback?.text = "Connection lost. Your text is kept. Reconnect, then send again."
        main.postDelayed({ if (visible && current == generation) background {
            try {
                val response = connection.resume()
                credentials.write(JSONObject().put("host", connection.host).put("port", connection.port).put("pin", connection.fingerprint).put("token", connection.token))
                main.post { if (visible && current == generation) { remote(response); heartbeat(connection, current) } }
            } catch (failure: LinkRejected) { main.post { if (current == generation) {
                client = null; connected = false; credentials.clear(); controls.removeAllViews(); connectionPanel.visibility = View.VISIBLE; message("TV approval expired or was removed. Connect again.")
            } } }
            catch (failure: javax.net.ssl.SSLException) { main.post {
                val identityFailure = generateSequence<Throwable>(failure) { it.cause }.any { it is java.security.cert.CertificateException }
                if (current == generation && identityFailure) { client = null; controls.removeAllViews(); connectionPanel.visibility = View.VISIBLE; message("TV identity changed. Connect again and compare the security fingerprint.") }
                else reconnect(connection, current, attempt + 1)
            } }
            catch (_: Exception) { main.post { reconnect(connection, current, attempt + 1) } }
        } }, if (attempt == 0) 0 else (1000L shl attempt.coerceAtMost(4)).coerceAtMost(15000))
    }
}
