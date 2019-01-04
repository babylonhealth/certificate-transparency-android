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

package com.babylon.certificatetransparency.internal.verifier

import com.babylon.certificatetransparency.SctVerificationResult
import com.babylon.certificatetransparency.VerificationResult
import com.babylon.certificatetransparency.datasource.DataSource
import com.babylon.certificatetransparency.internal.loglist.LogListDataSourceFactory
import com.babylon.certificatetransparency.internal.loglist.NoLogServers
import com.babylon.certificatetransparency.internal.utils.Base64
import com.babylon.certificatetransparency.internal.utils.hasEmbeddedSct
import com.babylon.certificatetransparency.internal.utils.signedCertificateTimestamps
import com.babylon.certificatetransparency.internal.verifier.model.Host
import com.babylon.certificatetransparency.loglist.LogListResult
import kotlinx.coroutines.runBlocking
import okhttp3.internal.tls.CertificateChainCleaner
import java.io.IOException
import java.security.KeyStore
import java.security.cert.Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal open class CertificateTransparencyBase(
    private val hosts: Set<Host>,
    trustManager: X509TrustManager? = null,
    logListDataSource: DataSource<LogListResult>? = null,
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

    private val logListDataSource = (logListDataSource ?: LogListDataSourceFactory.create())

    fun verifyCertificateTransparency(host: String, certificates: List<Certificate>): VerificationResult {
        return if (!enabledForCertificateTransparency(host)) {
            VerificationResult.Success.DisabledForHost(host)
        } else if (certificates.isEmpty()) {
            VerificationResult.Failure.NoCertificates
        } else {
            val cleanedCerts = cleaner.clean(certificates, host)
            hasValidSignedCertificateTimestamp(cleanedCerts)
        }
    }

    /**
     * Check if the certificates provided by a server contain Signed Certificate Timestamps
     * from a trusted CT log.
     *
     * @property certificates the certificate chain provided by the server
     * @return true if the certificates can be trusted, false otherwise.
     */
    @Suppress("ReturnCount")
    private fun hasValidSignedCertificateTimestamp(certificates: List<Certificate>): VerificationResult {

        val result = runBlocking {
            logListDataSource.get()
        }

        val verifiers = when (result) {
            is LogListResult.Valid -> result.servers.associateBy({ Base64.toBase64String(it.id) }) { LogSignatureVerifier(it) }
            is LogListResult.Invalid -> return VerificationResult.Failure.LogServersFailed(result)
            null -> return VerificationResult.Failure.LogServersFailed(NoLogServers)
        }

        val leafCertificate = certificates[0]

        if (!leafCertificate.hasEmbeddedSct()) {
            return VerificationResult.Failure.NoScts
        }

        return try {
            val sctResults = leafCertificate.signedCertificateTimestamps()
                .associateBy { Base64.toBase64String(it.id.keyId) }
                .mapValues { (logId, sct) ->
                    verifiers[logId]?.verifySignature(sct, certificates) ?: SctVerificationResult.Invalid.NoTrustedLogServerFound
                }

            policyVerificationResult(sctResults)
        } catch (e: IOException) {
            VerificationResult.Failure.UnknownIoException(e)
        }
    }

    private fun policyVerificationResult(sctResults: Map<String, SctVerificationResult>): VerificationResult {
        return if (sctResults.count { it.value is SctVerificationResult.Valid } < minimumValidSignedCertificateTimestamps) {
            VerificationResult.Failure.TooFewSctsTrusted(sctResults, minimumValidSignedCertificateTimestamps)
        } else {
            VerificationResult.Success.Trusted(sctResults)
        }
    }

    private fun enabledForCertificateTransparency(host: String) = hosts.any { it.matches(host) }
}
