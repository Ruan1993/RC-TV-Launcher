package za.co.rc.tv.link

import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.security.MessageDigest
import java.security.cert.X509Certificate

/** One bounded JSON request/reply per TLS connection; no HTTP, browser, shell or file routes. */
object LinkWire {
    const val SERVICE = "_rctv._tcp."
    const val MAX_BYTES = 4096
    fun read(socket: Socket): JSONObject {
        val input = DataInputStream(socket.getInputStream())
        val size = input.readInt(); require(size in 2..MAX_BYTES)
        val bytes = ByteArray(size); input.readFully(bytes)
        return JSONObject(bytes.toString(Charsets.UTF_8))
    }
    fun write(socket: Socket, value: JSONObject) {
        val bytes = value.toString().toByteArray(Charsets.UTF_8); require(bytes.size <= MAX_BYTES)
        DataOutputStream(socket.getOutputStream()).apply { writeInt(bytes.size); write(bytes); flush() }
    }
    fun fingerprint(cert: X509Certificate): String = MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString("") { "%02X".format(it) }
    fun displayFingerprint(value: String) = value.chunked(8).joinToString(" ")
    val COMMANDS = setOf("UP", "DOWN", "LEFT", "RIGHT", "OK", "BACK", "HOME", "SEARCH_TEXT")
}
