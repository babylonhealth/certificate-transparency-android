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
 *
 * Code derived from https://github.com/google/certificate-transparency-java
 */

package org.certificatetransparency.ctlog.data

import com.google.gson.GsonBuilder
import kotlinx.coroutines.GlobalScope
import org.certificatetransparency.ctlog.Base64
import org.certificatetransparency.ctlog.Host
import org.certificatetransparency.ctlog.LogInfo
import org.certificatetransparency.ctlog.PublicKeyFactory
import org.certificatetransparency.ctlog.TestData
import org.certificatetransparency.ctlog.data.loglist.model.LogList
import org.certificatetransparency.ctlog.data.verifier.LogSignatureVerifier
import org.certificatetransparency.ctlog.domain.datasource.DataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

@RunWith(JUnit4::class)
class CertificateTransparencyBaseIntegrationTest {

    @Test
    fun testBabylonHealth() {
        checkConnection("https://www.babylonhealth.com", true)
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

            assertEquals(certificateTransparencyChecker.verifyCertificateTransparency("ct.log", con.serverCertificates.toList()), shouldPass)

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

        private val certificateTransparencyChecker = CertificateTransparencyBase(setOf(Host("ct.log")), logListDataSource = logListDataSource())
    }
}
