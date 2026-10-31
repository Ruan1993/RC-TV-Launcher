package za.co.rc.tv.link

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small app-private credential record, authenticated encryption with a non-exportable device key. */
class SecureRecord(context: Context, name: String) {
    private val file = AtomicFile(File(context.filesDir, "$name.enc"))
    private val alias = "rc-local-record-v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized fun read(): JSONObject? = runCatching {
        val bytes = file.readFully(); require(bytes.size in 29..32768)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
        JSONObject(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8))
    }.getOrNull()
    @Synchronized fun write(value: JSONObject) {
        val data = value.toString().toByteArray(Charsets.UTF_8); require(data.size <= 16000)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val output = file.startWrite()
        try { output.write(cipher.iv); output.write(cipher.doFinal(data)); file.finishWrite(output) }
        catch (failure: Exception) { file.failWrite(output); throw failure }
    }
    @Synchronized fun clear() { file.delete() }
}
