package org.certificatetransparency.ctlog.comm

import com.google.gson.GsonBuilder
import org.certificatetransparency.ctlog.Base64
import org.certificatetransparency.ctlog.LogInfo
import org.certificatetransparency.ctlog.data.verifier.LogSignatureVerifier
import org.certificatetransparency.ctlog.PublicKeyFactory
import org.certificatetransparency.ctlog.TestData
import org.certificatetransparency.ctlog.TestData.TEST_LOG_LIST_JSON
import org.certificatetransparency.ctlog.hasEmbeddedSct
import org.certificatetransparency.ctlog.signedCertificateTimestamps
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.security.spec.InvalidKeySpecException
import javax.net.ssl.HttpsURLConnection

/**
 * This test checks that SSL connections to servers with a known good certificate can be
 * verified and connections without can be rejected. It serves as a programming example
 * on how to use the ctlog library.
 *
 * There are three ways that certificate transparency information can be exchanged in the
 * connection handshake:
 * - X509v3 certificate extension
 * - TLS extension
 * - OSCP stapling
 * This test only demonstrates how to validate using the first approach.
 *
 * @author Warwick Hunter
 * @since 0.1.3
 */
@RunWith(JUnit4::class)
class SslConnectionCheckingTest {

    private val verifiers = mutableMapOf<String, LogSignatureVerifier>()

    init {
        buildLogSignatureVerifiers()
    }

    @Test
    fun testBabylonHealth() {
        checkConnection("https://babylonhealth.com", false)
    }

    @Test
    fun testBabylonHealthPhp() {
        checkConnection("https://app2.babylonpartners.com", true)
    }

    @Test
    fun testBabylonHealthRuby() {
        checkConnection("https://app.babylonpartners.com", true)
    }

    @Test
    fun testBabylonHealthAi() {
        checkConnection("https://services.babylonpartners.com", true)
    }

    @Test
    fun testBabylonHealthWebApp() {
        checkConnection("https://online.babylonhealth.com", true)
    }

    @Test
    fun testBabylonHealthHealthReport() {
        checkConnection("https://health-report-uk.babylonpartners.com/", true)
    }

    @Test
    fun testBabylonHealthWeb() {
        checkConnection("https://www.babylonhealth.com", true)
    }

    @Test
    fun testBabylonHealthSupport() {
        checkConnection("https://support.babylonhealth.com", true)
    }

    @Test
    @Ignore
    // Disabled as this domain fails occassionally as there are both valid and invalid certificates
    fun testBabylonHealthBlog() {
        checkConnection("https://blog.babylonhealth.com", true)
    }

    @Test
    fun testAnonyome() {
        checkConnection("https://anonyome.com", true)
    }

    @Test
    fun testLetsEncrypt() {
        checkConnection("https://letsencrypt.org", true)
    }

    @Test
    fun testInvalid() {
        checkConnection("https://invalid-expected-sct.badssl.com/", false)
    }

    /**
     * Check if the certificates provided by a server have good certificate
     * transparency information in them that can be verified against a trusted
     * certificate transparency log.
     *
     * @param urlString  the URL of the server to check.
     * @param shouldPass true if the server will give good certificates, false otherwise.
     */
    private fun checkConnection(urlString: String, shouldPass: Boolean) {
        var con: HttpsURLConnection? = null
        try {
            val url = URL(urlString)
            con = url.openConnection() as HttpsURLConnection
            con.connect()

            v(urlString)
            assertEquals(isGood(con.serverCertificates), shouldPass)

            val statusCode = con.responseCode
            when (statusCode) {
                200, 403 -> {
                }
                404 -> v("404 status code returned")
                else -> fail(String.format("Unexpected HTTP status code: %d", statusCode))
            }
        } catch (e: IOException) {
            fail(e.toString())
        } finally {
            con?.disconnect()
        }
    }

    /**
     * Check if the certificates provided by a server contain Signed Certificate Timestamps
     * from a trusted CT log.
     *
     * @param certificates the certificate chain provided by the server
     * @return true if the certificates can be trusted, false otherwise.
     */
    private fun isGood(certificates: Array<Certificate>): Boolean {

        if (certificates[0] !is X509Certificate) {
            v("  This test only supports SCTs carried in X509 certificates, of which there are none.")
            return false
        }

        val leafCertificate = certificates[0] as X509Certificate

        if (!leafCertificate.hasEmbeddedSct()) {
            v("  This certificate does not have any Signed Certificate Timestamps in it.")
            return false
        }


        try {
            val sctsInCertificate = leafCertificate.signedCertificateTimestamps()
            if (sctsInCertificate.size < MIN_VALID_SCTS) {
                v("  Too few SCTs are present, I want at least $MIN_VALID_SCTS CT logs to be nominated.")
                return false
            }

            val certificateList = certificates.toList()

            var validSctCount = 0
            for (sct in sctsInCertificate) {
                val logId = Base64.toBase64String(sct.id.keyId)
                if (verifiers.containsKey(logId)) {
                    v("  SCT trusted log $logId")
                    if (verifiers[logId]?.verifySignature(sct, certificateList) == true) {
                        ++validSctCount
                    }
                } else {
                    v("  SCT untrusted log $logId")
                }
            }

            if (validSctCount < MIN_VALID_SCTS) {
                v("  Too few trusted SCTs are present, I want at least $MIN_VALID_SCTS trusted CT logs.")
            }
            return validSctCount >= MIN_VALID_SCTS
        } catch (e: IOException) {
            e.printStackTrace()
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
        val hasher = MessageDigest.getInstance(LOG_ID_HASH_ALGORITHM)

        // Collection of CT logs that are trusted for the purposes of this test from https://www.gstatic.com/ct/log_list/log_list.json
        val json = TestData.file(TEST_LOG_LIST_JSON).readText()
        val trustedLogKeys = GsonBuilder().create().fromJson(json, LogList::class.java).logs.map { it.key }

        for (trustedLogKey in trustedLogKeys) {
            hasher.reset()
            val keyBytes = Base64.decode(trustedLogKey)
            val logId = Base64.toBase64String(hasher.digest(keyBytes))
            val publicKey = PublicKeyFactory.fromByteArray(keyBytes)

            verifiers[logId] = LogSignatureVerifier(LogInfo(publicKey))
        }
    }

    data class LogList(
        val logs: List<Log>,
        val operators: List<Operator>
    ) {
        data class Log(
            val description: String,
            val key: String,
            val url: String,
            val maximum_merge_delay: Long,
            val operated_by: List<Int>,
            val dns_api_endpoint: String
        )

        data class Operator(
            val name: String,
            val id: Int
        )
    }

    private fun v(message: String) {
        println(message)
    }

    companion object {

        /** I want at least two different CT logs to verify the certificate  */
        private const val MIN_VALID_SCTS = 2

        /** A CT log's Id is created by using this hash algorithm on the CT log public key  */
        private const val LOG_ID_HASH_ALGORITHM = "SHA-256"
    }
}
