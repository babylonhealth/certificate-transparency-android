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

import org.certificatetransparency.ctlog.Host
import org.certificatetransparency.ctlog.utils.LogListDataSourceTestFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.URL
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

        private val certificateTransparencyChecker = CertificateTransparencyBase(
            hosts = setOf(Host("ct.log")),
            logListDataSource = LogListDataSourceTestFactory.logListDataSource())
    }
}
