package za.co.rc.tv.companion

import java.security.MessageDigest
import java.security.SecureRandom

/** In-memory, expiring grants; never stores search text or device identifiers. */
class PairingGate(private val now: () -> Long = { System.nanoTime() / 1_000_000 }) {
    data class Pending(val id: String, val name: String, val until: Long, var token: String? = null)
    data class Session(val id: String, val name: String, val until: Long, var sequence: Long = 0, var lastCommand: Long = -1000)
    private val random = SecureRandom()
    private var code = ""
    private var codeUntil = 0L
    private var attemptWindow = 0L
    private var attempts = 0
    private var pending: Pending? = null
    private val sessions = mutableMapOf<String, Session>()
    private fun secret() = ByteArray(32).also(random::nextBytes).joinToString("") { "%02x".format(it) }
    private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    @Synchronized fun start(): String { clear(); code = random.nextInt(1_000_000).toString().padStart(6, '0'); codeUntil = now() + 120_000; return code }
    @Synchronized fun clear() { code = ""; codeUntil = 0; pending = null; sessions.clear(); attempts = 0; attemptWindow = now() }
    @Synchronized fun currentCode(): String = if (now() < codeUntil) code else ""
    @Synchronized fun request(value: String, name: String): String? {
        val time = now()
        if (time - attemptWindow >= 60_000) { attemptWindow = time; attempts = 0 }
        if (++attempts > 5 || code.isEmpty() || time >= codeUntil || !MessageDigest.isEqual(code.toByteArray(), value.toByteArray())) return null
        prune()
        if (pending != null || sessions.size >= 3) return null
        val clean = name.filter { it.isLetterOrDigit() || it == ' ' || it == '-' }.take(32).ifBlank { "Android phone" }
        return secret().also { pending = Pending(it, clean, time + 60_000) }
    }
    @Synchronized fun pending(): Pending? { prune(); return pending?.copy() }
    @Synchronized fun approve(id: String): Boolean {
        prune(); val request = pending?.takeIf { it.id == id && it.token == null } ?: return false
        val token = secret(); request.token = token
        sessions[digest(token)] = Session(secret().take(12), request.name, now() + 30 * 60_000)
        code = "" // A new TV-side start is required for another pairing attempt.
        return true
    }
    @Synchronized fun reject(id: String) { if (pending?.id == id) pending = null }
    @Synchronized fun poll(id: String): Pair<Boolean, String?>? { prune(); return pending?.takeIf { it.id == id }?.let { (it.token != null) to it.token } }
    @Synchronized fun authorize(token: String, sequence: Long): Boolean {
        prune(); val session = sessions[digest(token)] ?: return false
        val time = now()
        if (sequence <= session.sequence || time - session.lastCommand < 40) return false
        session.sequence = sequence; session.lastCommand = time; return true
    }
    @Synchronized fun authenticated(token: String): Boolean { prune(); return sessions.containsKey(digest(token)) }
    @Synchronized fun disconnect(token: String) { sessions.remove(digest(token)); if (pending?.token == token) pending = null }
    @Synchronized fun revoke(id: String) { val key = sessions.entries.firstOrNull { it.value.id == id }?.key; key?.let { sessions.remove(it) }; pending = null }
    @Synchronized fun sessions(): List<Session> { prune(); return sessions.values.map { it.copy() } }
    private fun prune() { val time = now(); if ((pending?.until ?: Long.MAX_VALUE) <= time) pending = null; sessions.entries.removeAll { it.value.until <= time } }
}
