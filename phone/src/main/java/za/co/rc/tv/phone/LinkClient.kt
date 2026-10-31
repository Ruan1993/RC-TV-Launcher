package za.co.rc.tv.phone

import org.json.JSONObject
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.*
import za.co.rc.tv.link.LinkWire

/** Bootstrap sends hello only; all pairing secrets and commands require the compared certificate pin. */
class LinkRejected(val reason: String, message: String) : Exception(message)
class LinkClient(val host: String, val port: Int, pin: String? = null) {
    var fingerprint: String? = pin
        private set
    @Volatile var token: String? = null
    private var sequence = 0L
    private var lease = ""
    fun probe(): String {
        check(fingerprint == null)
        val result = exchange(JSONObject().put("op", "hello"), bootstrap = true)
        check(result.optInt("protocol") == 2) { "Update both RC TV and Remote" }
        return fingerprint ?: error("Missing identity")
    }
    fun request(value: JSONObject): JSONObject = exchange(value, false)
    fun resume(): JSONObject {
        val response = request(JSONObject().put("op", "resume").put("token", token ?: error("Pair first")))
        lease = response.getString("lease"); sequence = 0
        return response
    }
    fun status() = request(JSONObject().put("op", "status").put("token", token).put("lease", lease))
    fun command(key: String, text: String = "") = request(JSONObject().put("op", "command").put("token", token ?: error("Pair first"))
        .put("lease", lease).put("sequence", ++sequence).put("key", key).put("text", text))
    fun disconnect() { val value = token; token = null; if (value != null) runCatching { request(JSONObject().put("op", "disconnect").put("token", value)) } }
    private fun exchange(value: JSONObject, bootstrap: Boolean): JSONObject {
        require(InetAddress.getByName(host).isSiteLocalAddress) { "Use the TV's private local-network address" }
        require(port in 1..65535)
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) { error("Not a server") }
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val cert = chain?.firstOrNull() ?: error("Missing certificate"); cert.checkValidity()
                val actual = LinkWire.fingerprint(cert)
                if (bootstrap) fingerprint = actual
                else if (fingerprint == null || !MessageDigest.isEqual(actual.toByteArray(), fingerprint!!.toByteArray())) throw java.security.cert.CertificateException("TV identity changed. Verify and pair again.")
            }
        }
        val tls = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }
        val socket = tls.socketFactory.createSocket() as SSLSocket
        socket.use {
            it.soTimeout = 4000; it.enabledProtocols = arrayOf("TLSv1.2"); it.connect(InetSocketAddress(host, port), 4000); it.startHandshake()
            LinkWire.write(it, value); return LinkWire.read(it).also { result -> if (!result.optBoolean("ok")) throw LinkRejected(result.optString("reason"), result.optString("error", "Connection failed")) }
        }
    }
}
