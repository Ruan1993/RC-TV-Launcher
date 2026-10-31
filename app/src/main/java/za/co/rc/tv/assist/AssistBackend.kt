package za.co.rc.tv.assist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI
import javax.net.ssl.HttpsURLConnection

data class AssistBackendSession(val endpoint: String, val bearer: String, val expires: Long)
sealed interface OnlineAssistResult {
    data class Answer(val text: String) : OnlineAssistResult
    data object NotConfigured : OnlineAssistResult
    data object Unavailable : OnlineAssistResult
}

/** Ready for an authorised backend session provider. No endpoint, key or shared secret in the APK. */
class AssistBackend(private val session: () -> AssistBackendSession? = { null }) {
    suspend fun reply(prompt: String): OnlineAssistResult = withContext(Dispatchers.IO) {
        val auth = try { session() } catch (_: Exception) { return@withContext OnlineAssistResult.Unavailable }
            ?: return@withContext OnlineAssistResult.NotConfigured
        if (prompt.isBlank() || prompt.length > 500) return@withContext OnlineAssistResult.Unavailable
        var connection: HttpsURLConnection? = null
        try {
            val uri = URI(auth.endpoint)
            require(uri.scheme == "https" && uri.host != null && uri.userInfo == null && uri.fragment == null)
            require(auth.bearer.isNotBlank() && '\n' !in auth.bearer && '\r' !in auth.bearer)
            require(auth.expires in (System.currentTimeMillis() + 1)..(System.currentTimeMillis() + 15 * 60_000))
            connection = uri.toURL().openConnection() as HttpsURLConnection
            connection.apply {
                requestMethod = "POST"; instanceFollowRedirects = false; connectTimeout = 5000; readTimeout = 10000; doOutput = true
                setRequestProperty("Authorization", "Bearer ${auth.bearer}"); setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(JSONObject().put("version", 1).put("prompt", prompt).toString().toByteArray(Charsets.UTF_8)) }
            }
            if (connection.responseCode != 200) return@withContext OnlineAssistResult.Unavailable
            val bytes = connection.inputStream.use { it.readBytesBounded() }
            val text = JSONObject(bytes.toString(Charsets.UTF_8)).getString("answer").take(4000)
            // Text is displayed as unverified assistant prose. Backend output cannot execute actions.
            OnlineAssistResult.Answer(text)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { OnlineAssistResult.Unavailable }
        finally { connection?.disconnect() }
    }
    private fun java.io.InputStream.readBytesBounded(): ByteArray {
        val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(2048)
        while (true) { val count = read(buffer); if (count < 0) break; require(output.size() + count <= 32768); output.write(buffer, 0, count) }
        return output.toByteArray()
    }
}
