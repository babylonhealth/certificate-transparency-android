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

import kotlinx.coroutines.runBlocking
import okhttp3.internal.tls.CertificateChainCleaner
import org.certificatetransparency.ctlog.Base64
import org.certificatetransparency.ctlog.Host
import org.certificatetransparency.ctlog.data.loglist.LogListDataSourceFactory
import org.certificatetransparency.ctlog.data.verifier.LogSignatureVerifier
import org.certificatetransparency.ctlog.domain.datasource.DataSource
import org.certificatetransparency.ctlog.hasEmbeddedSct
import org.certificatetransparency.ctlog.signedCertificateTimestamps
import java.io.IOException
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal open class CertificateTransparencyBase(
    private val hosts: Set<Host>,
    trustManager: X509TrustManager? = null,
    logListDataSource: DataSource<Map<String, LogSignatureVerifier>>? = null
) {
    init {
        require(hosts.isNotEmpty()) { "Please provide at least one host to enable certificate transparency verification" }
    }

    private val cleaner: CertificateChainCleaner by lazy {
        val localTrustManager = trustManager ?: (TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }.trustManagers.first { it is X509TrustManager } as X509TrustManager)

        CertificateChainCleaner.get(localTrustManager)
    }

    private val logListDataSource = logListDataSource ?: LogListDataSourceFactory.create()

    fun verifyCertificateTransparency(host: String, certificates: List<Certificate>): Boolean {
        if (!enabledForCertificateTransparency(host)) {
            return true
        }

        if (certificates.isEmpty()) {
            v("  No certificates to check against")
            return false
        }

        val cleanedCerts = cleaner.clean(certificates, host)
        return hasValidSignedCertificateTimestamp(cleanedCerts)
    }

    /**
     * Check if the certificates provided by a server contain Signed Certificate Timestamps
     * from a trusted CT log.
     *
     * @param certificates the certificate chain provided by the server
     * @return true if the certificates can be trusted, false otherwise.
     */
    private fun hasValidSignedCertificateTimestamp(certificates: List<Certificate>): Boolean {

        val verifiers = runBlocking {
            logListDataSource.get()
        }

        if (verifiers == null) {
            v("  No verifiers to check against")
            return false
        }

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

    private fun enabledForCertificateTransparency(host: String) = hosts.any { it.matches(host) }

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
