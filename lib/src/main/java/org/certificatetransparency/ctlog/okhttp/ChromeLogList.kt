package org.certificatetransparency.ctlog.okhttp

import com.google.gson.GsonBuilder
import org.bouncycastle.util.io.pem.PemReader
import org.certificatetransparency.ctlog.okhttp.model.LogList
import retrofit2.Retrofit
import java.io.StringReader
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

// Collection of CT logs that are trusted for the purposes of this test from https://www.gstatic.com/ct/log_list/log_list.json
object ChromeLogList {

    val trustedLogKeys: List<String>

    private val publicKey = parsePublicKey(
        """-----BEGIN PUBLIC KEY-----
           MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAsu0BHGnQ++W2CTdyZyxv
           HHRALOZPlnu/VMVgo2m+JZ8MNbAOH2cgXb8mvOj8flsX/qPMuKIaauO+PwROMjiq
           fUpcFm80Kl7i97ZQyBDYKm3MkEYYpGN+skAR2OebX9G2DfDqFY8+jUpOOWtBNr3L
           rmVcwx+FcFdMjGDlrZ5JRmoJ/SeGKiORkbbu9eY1Wd0uVhz/xI5bQb0OgII7hEj+
           i/IPbJqOHgB8xQ5zWAJJ0DmG+FM6o7gk403v6W3S8qRYiR84c50KppGwe4YqSMkF
           bLDleGQWLoaDSpEWtESisb4JiLaY4H+Kk0EyAhPSb+49JfUozYl+lf7iFN3qRq/S
           IXXTh6z0S7Qa8EYDhKGCrpI03/+qprwy+my6fpWHi6aUIk4holUCmWvFxZDfixox
           K0RlqbFDl2JXMBquwlQpm8u5wrsic1ksIv9z8x9zh4PJqNpCah0ciemI3YGRQqSe
           /mRRXBiSn9YQBUPcaeqCYan+snGADFwHuXCd9xIAdFBolw9R9HTedHGUfVXPJDiF
           4VusfX6BRR/qaadB+bqEArF/TzuDUr6FvOR4o8lUUxgLuZ/7HO+bHnaPFKYHHSm+
           +z1lVDhhYuSZ8ax3T0C3FZpb7HMjZtpEorSV5ElKJEJwrhrBCMOD8L01EoSPrGlS
           1w22i9uGHMn/uGQKo28u7AsCAwEAAQ==
           -----END PUBLIC KEY-----""")

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.gstatic.com/ct/log_list/")
            .build()

        val logService = retrofit.create(LogService::class.java)

        val logListRaw = logService.getLogList().execute().body()!!.string()

        val signature = logService.getLogListSignature().execute().body()!!.bytes()

        val verified = verify(logListRaw, signature, publicKey)
        println(verified)

        val logList = GsonBuilder().setLenient().create().fromJson(logListRaw, LogList::class.java)
        trustedLogKeys = logList.logs.map { it.key }
    }

    private fun parsePublicKey(keyText: String): PublicKey {
        val pemContent = PemReader(StringReader(keyText)).readPemObject().content
        val keySpec = X509EncodedKeySpec(pemContent)

        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    private fun verify(message: String, signature: ByteArray, publicKey: PublicKey): Boolean {
        // signature is not thread-safe
        val sig = Signature.getInstance("SHA256WithRSA")
        sig.initVerify(publicKey)
        sig.update(message.toByteArray())

        return sig.verify(signature)
    }
}
