/*
 * Copyright 2018 Babylon Healthcare Services Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.certificatetransparency.ctlog.okhttp

import com.google.gson.GsonBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.io.pem.PemReader
import org.certificatetransparency.ctlog.LogInfo
import org.certificatetransparency.ctlog.LogSignatureVerifier
import org.certificatetransparency.ctlog.datasource.DataSource
import org.certificatetransparency.ctlog.okhttp.model.LogList
import retrofit2.Retrofit
import java.io.StringReader
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

// Collection of CT logs that are trusted for the purposes of this test from https://www.gstatic.com/ct/log_list/log_list.json
class LogListNetworkDataSource : DataSource<Map<String, LogSignatureVerifier>> {

    override val coroutineContext = GlobalScope.coroutineContext

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

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.gstatic.com/ct/log_list/")
        .build()

    private val logService = retrofit.create(LogService::class.java)

    override suspend fun get(): Map<String, LogSignatureVerifier>? {
        println("Loading log-list.json from network")

        val logListJob = async { logService.getLogList().execute().body()?.string() }
        val signatureJob = async { logService.getLogListSignature().execute().body()?.bytes() }

        val logListJson = logListJob.await()
        val signature = signatureJob.await()

        if (logListJson == null || signature == null) {
            return null
        }

        if (verify(logListJson, signature, publicKey)) {
            val logList = GsonBuilder().setLenient().create().fromJson(logListJson, LogList::class.java)
            return buildLogSignatureVerifiers(logList)
        }

        return null
    }

    override suspend fun set(value: Map<String, LogSignatureVerifier>) = Unit

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

    /**
     * Construct LogSignatureVerifiers for each of the trusted CT logs.
     *
     * @throws InvalidKeySpecException the CT log key isn't RSA or EC, the key is probably corrupt.
     * @throws NoSuchAlgorithmException the crypto provider couldn't supply the hashing algorithm
     * or the key algorithm. This probably means you are using an ancient or bad crypto provider.
     */
    @Throws(InvalidKeySpecException::class, NoSuchAlgorithmException::class)
    private fun buildLogSignatureVerifiers(logList: LogList): Map<String, LogSignatureVerifier> {
        // A CT log's Id is created by using this hash algorithm on the CT log public key
        val hasher = MessageDigest.getInstance("SHA-256")

        return logList.logs.map { Base64.decode(it.key) }.associateBy({
            hasher.reset()
            Base64.toBase64String(hasher.digest(it))
        }) {
            val keyFactory = KeyFactory.getInstance(determineKeyAlgorithm(it))
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(it))
            LogSignatureVerifier(LogInfo(publicKey))
        }
    }

    /** Parses a key and determines the key algorithm (RSA or EC) based on the ASN1 OID.  */
    private fun determineKeyAlgorithm(keyBytes: ByteArray): String {
        val seq = ASN1Sequence.getInstance(keyBytes)
        val seq1 = seq.objects.nextElement() as DLSequence
        val oid = seq1.objects.nextElement() as ASN1ObjectIdentifier
        return when (oid) {
            PKCSObjectIdentifiers.rsaEncryption -> "RSA"
            X9ObjectIdentifiers.id_ecPublicKey -> "EC"
            else -> throw IllegalArgumentException("Unsupported key type $oid")
        }
    }
}
