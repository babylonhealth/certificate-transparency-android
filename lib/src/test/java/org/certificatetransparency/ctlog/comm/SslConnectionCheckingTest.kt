package org.certificatetransparency.ctlog.comm

import com.google.gson.GsonBuilder
import kotlinx.coroutines.GlobalScope
import org.certificatetransparency.ctlog.Base64
import org.certificatetransparency.ctlog.Host
import org.certificatetransparency.ctlog.LogInfo
import org.certificatetransparency.ctlog.PublicKeyFactory
import org.certificatetransparency.ctlog.TestData
import org.certificatetransparency.ctlog.data.CertificateTransparencyBase
import org.certificatetransparency.ctlog.data.loglist.model.LogList
import org.certificatetransparency.ctlog.data.verifier.LogSignatureVerifier
import org.certificatetransparency.ctlog.domain.datasource.DataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.security.cert.Certificate
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

            println(urlString)

            assertEquals(certificateTransparencyChecker.check(con.serverCertificates.toList()), shouldPass)

            val statusCode = con.responseCode
            when (statusCode) {
                200, 403 -> {
                }
                404 -> println("404 status code returned")
                else -> fail(String.format("Unexpected HTTP status code: %d", statusCode))
            }
        } catch (e: IOException) {
            fail(e.toString())
        } finally {
            con?.disconnect()
        }
    }

    companion object {

        private fun logListDataSource(): DataSource<Map<String, LogSignatureVerifier>> {
            val hasher = MessageDigest.getInstance("SHA-256")

            // Collection of CT logs that are trusted from https://www.gstatic.com/ct/log_list/log_list.json
            val json = TestData.file(TestData.TEST_LOG_LIST_JSON).readText()
            val trustedLogKeys = GsonBuilder().create().fromJson(json, LogList::class.java).logs.map { it.key }

            val map = trustedLogKeys.map { Base64.decode(it) }.associateBy({
                Base64.toBase64String(hasher.digest(it))
            }) {
                LogSignatureVerifier(LogInfo(PublicKeyFactory.fromByteArray(it)))
            }

            return object : DataSource<Map<String, LogSignatureVerifier>> {
                override suspend fun get() = map

                override suspend fun set(value: Map<String, LogSignatureVerifier>) = Unit

                override val coroutineContext = GlobalScope.coroutineContext
            }
        }

        private val certificateTransparencyChecker = object : CertificateTransparencyBase(setOf(Host("anonyome.com")), logListDataSource = logListDataSource()) {
            @Suppress("unused")
            fun check(certificates: List<Certificate>) = isGood(certificates)
        }
    }
}
