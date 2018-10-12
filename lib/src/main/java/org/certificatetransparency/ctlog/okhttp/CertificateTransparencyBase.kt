package org.certificatetransparency.ctlog.okhttp

import okhttp3.internal.tls.CertificateChainCleaner
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import org.bouncycastle.util.encoders.Base64
import org.certificatetransparency.ctlog.LogInfo
import org.certificatetransparency.ctlog.LogSignatureVerifier
import org.certificatetransparency.ctlog.hasEmbeddedSCT
import org.certificatetransparency.ctlog.utils.VerifySignature
import java.io.IOException
import java.security.KeyFactory
import java.security.KeyStore
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import java.util.HashMap
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

open class CertificateTransparencyBase(
    trustManager: X509TrustManager? = null
) {
    protected val cleaner: CertificateChainCleaner by lazy {
        val localTrustManager = trustManager ?: (TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }.trustManagers.first { it is X509TrustManager } as X509TrustManager)

        CertificateChainCleaner.get(localTrustManager)
    }

    private val verifiers = HashMap<String, LogSignatureVerifier>()

    init {
        buildLogSignatureVerifiers()
    }

    /**
     * Check if the certificates provided by a server contain Signed Certificate Timestamps
     * from a trusted CT log.
     *
     * @param certificates the certificate chain provided by the server
     * @return true if the certificates can be trusted, false otherwise.
     */
    protected fun isGood(certificates: List<Certificate>): Boolean {

        if (certificates[0] !is X509Certificate) {
            v("  This test only supports SCTs carried in X509 certificates, of which there are none.")
            return false
        }

        val leafCertificate = certificates[0] as X509Certificate

        if (!leafCertificate.hasEmbeddedSCT()) {
            v("  This certificate does not have any Signed Certificate Timestamps in it.")
            return false
        }

        try {
            val sctsInCertificate = VerifySignature.parseSCTsFromCert(leafCertificate)
            if (sctsInCertificate.size < MIN_VALID_SCTS) {
                v("  Too few SCTs are present, I want at least $MIN_VALID_SCTS CT logs to be nominated.")
                return false
            }

            val validSctCount = sctsInCertificate.asSequence().map { sct ->
                Pair(sct, Base64.toBase64String(sct.id.keyId))
            }.filter { (_, logId) ->
                verifiers.containsKey(logId)
            }.count { (sct, logId) ->
                v("  SCT trusted log $logId")
                verifiers[logId]?.verifySignature(sct, certificates) == true
            }

            if (validSctCount < MIN_VALID_SCTS) {
                v("  Too few trusted SCTs are present, I want at least $MIN_VALID_SCTS trusted CT logs.")
            }
            return validSctCount >= MIN_VALID_SCTS
        } catch (e: IOException) {
            if (VERBOSE) {
                e.printStackTrace()
            }
            return false
        }
    }

    /**
     * Construct LogSignatureVerifiers for each of the trusted CT logs.
     *
     * @throws InvalidKeySpecException the CT log key isn't RSA or EC, the key is probably corrupt.
     * @throws NoSuchAlgorithmException the crypto provider couldn't supply the hashing algorithm
     * or the key algorithm. This probably means you are using an ancient or bad crypto provider.
     */
    @Throws(InvalidKeySpecException::class, NoSuchAlgorithmException::class)
    private fun buildLogSignatureVerifiers() {
        verifiers.clear()

        // A CT log's Id is created by using this hash algorithm on the CT log public key
        val hasher = MessageDigest.getInstance("SHA-256")

        for (trustedLogKey in ChromeLogList.trustedLogKeys) {
            hasher.reset()
            val keyBytes = Base64.decode(trustedLogKey)
            val logId = Base64.toBase64String(hasher.digest(keyBytes))
            val keyFactory = KeyFactory.getInstance(determineKeyAlgorithm(keyBytes))
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
            verifiers[logId] = LogSignatureVerifier(LogInfo(publicKey))
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

    private fun v(message: String) {
        if (VERBOSE) {
            println(message)
        }
    }

    companion object {
        /** I want at least two different CT logs to verify the certificate  */
        private const val MIN_VALID_SCTS = 2

        private const val VERBOSE = true
    }
}
