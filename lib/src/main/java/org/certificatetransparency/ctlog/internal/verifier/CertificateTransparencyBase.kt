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

package org.certificatetransparency.ctlog.internal.verifier

import kotlinx.coroutines.runBlocking
import okhttp3.internal.tls.CertificateChainCleaner
import org.certificatetransparency.ctlog.datasource.DataSource
import org.certificatetransparency.ctlog.internal.loglist.LogListDataSourceFactory
import org.certificatetransparency.ctlog.internal.utils.Base64
import org.certificatetransparency.ctlog.internal.utils.hasEmbeddedSct
import org.certificatetransparency.ctlog.internal.utils.signedCertificateTimestamps
import org.certificatetransparency.ctlog.internal.verifier.model.Host
import org.certificatetransparency.ctlog.internal.verifier.model.Result
import org.certificatetransparency.ctlog.verifier.SignatureVerifier
import java.io.IOException
import java.security.KeyStore
import java.security.cert.Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal open class CertificateTransparencyBase(
    private val hosts: Set<Host>,
    trustManager: X509TrustManager? = null,
    logListDataSource: DataSource<Map<String, SignatureVerifier>>? = null,
    private val minimumValidSignedCertificateTimestamps: Int = 2
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

    fun verifyCertificateTransparency(host: String, certificates: List<Certificate>): Result {
        if (!enabledForCertificateTransparency(host)) {
            return Result.Success.DisabledForHost(host)
        }

        return if (certificates.isEmpty()) {
            Result.Failure.NoCertificates
        } else {
            val cleanedCerts = cleaner.clean(certificates, host)
            hasValidSignedCertificateTimestamp(cleanedCerts)
        }
    }

    /**
     * Check if the certificates provided by a server contain Signed Certificate Timestamps
     * from a trusted CT log.
     *
     * @param certificates the certificate chain provided by the server
     * @return true if the certificates can be trusted, false otherwise.
     */
    private fun hasValidSignedCertificateTimestamp(certificates: List<Certificate>): Result {

        val verifiers = runBlocking {
            logListDataSource.get()
        } ?: return Result.Failure.NoVerifiers

        val leafCertificate = certificates[0]

        if (!leafCertificate.hasEmbeddedSct()) {
            return Result.Failure.NoScts
        }

        try {
            val sctsInCertificate = leafCertificate.signedCertificateTimestamps()
            if (sctsInCertificate.size < minimumValidSignedCertificateTimestamps) {
                return Result.Failure.TooFewSctsPresent(sctsInCertificate.size, minimumValidSignedCertificateTimestamps)
            }

            val validScts = sctsInCertificate.map { sct ->
                Pair(sct, Base64.toBase64String(sct.id.keyId))
            }.filter { (_, logId) ->
                verifiers.containsKey(logId)
            }.filter { (sct, logId) ->
                verifiers[logId]?.verifySignature(sct, certificates) == true
            }

            if (validScts.size < minimumValidSignedCertificateTimestamps) {
                return Result.Failure.TooFewSctsTrusted(validScts.size, minimumValidSignedCertificateTimestamps)
            }

            return Result.Success.Trusted(validScts.map { (_, logId) -> logId })
        } catch (e: IOException) {
            return Result.Failure.UnknownIoException(e)
        }
    }

    private fun enabledForCertificateTransparency(host: String) = hosts.any { it.matches(host) }
}
