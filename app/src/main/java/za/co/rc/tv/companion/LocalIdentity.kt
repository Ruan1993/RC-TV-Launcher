package za.co.rc.tv.companion

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.*
import javax.security.auth.x500.X500Principal
import za.co.rc.tv.link.LinkWire

class LocalIdentity {
    private val alias = "rc-companion-local-tls-ec-v2"
    private val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    init {
        if (!store.containsAlias(alias)) KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                // Conscrypt passes an already-hashed TLS transcript to the key-store signer.
                .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256)
                .setCertificateSubject(X500Principal("CN=RC TV Local Companion")).setCertificateSerialNumber(BigInteger.ONE)
                .setCertificateNotBefore(Date(0)).setCertificateNotAfter(Date(4_102_444_800_000L)).build())
            generateKeyPair()
        }
    }
    val fingerprint: String get() = LinkWire.fingerprint(store.getCertificate(alias) as X509Certificate)
    fun context(): SSLContext {
        val keys = object : X509ExtendedKeyManager() {
            override fun getCertificateChain(value: String?) = if (value == alias) arrayOf(store.getCertificate(alias) as X509Certificate) else null
            override fun getPrivateKey(value: String?) = if (value == alias) store.getKey(alias, null) as PrivateKey else null
            override fun getServerAliases(type: String?, issuers: Array<out Principal>?) = if (type == "EC") arrayOf(alias) else null
            override fun chooseServerAlias(type: String?, issuers: Array<out Principal>?, socket: Socket?) = if (type == "EC") alias else null
            override fun getClientAliases(type: String?, issuers: Array<out Principal>?) = null
            override fun chooseClientAlias(types: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) = null
        }
        return SSLContext.getInstance("TLS").apply { init(arrayOf(keys), null, null) }
    }
}
