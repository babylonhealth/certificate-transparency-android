/*
 * Copyright 2019 Babylon Partners Limited
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

import com.babylon.certificatetransparency.CTPolicy
import com.babylon.certificatetransparency.SctVerificationResult
import com.babylon.certificatetransparency.VerificationResult
import com.babylon.certificatetransparency.cache.DiskCache
import com.babylon.certificatetransparency.chaincleaner.CertificateChainCleaner
import com.babylon.certificatetransparency.chaincleaner.CertificateChainCleanerFactory
import com.babylon.certificatetransparency.datasource.DataSource
import com.babylon.certificatetransparency.internal.loglist.LogListDataSourceFactory
import com.babylon.certificatetransparency.internal.loglist.NoLogServers
import com.babylon.certificatetransparency.internal.utils.Base64
import com.babylon.certificatetransparency.internal.utils.hasEmbeddedSct
import com.babylon.certificatetransparency.internal.utils.signedCertificateTimestamps
import com.babylon.certificatetransparency.internal.verifier.model.Host
import com.babylon.certificatetransparency.loglist.LogListResult
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Suppress("LongParameterList")
internal open class CertificateTransparencyBase(
    private val includeHosts: Set<Host>,
    private val excludeHosts: Set<Host> = emptySet(),
    private val certificateChainCleanerFactory: CertificateChainCleanerFactory? = null,
    trustManager: X509TrustManager? = null,
    logListDataSource: DataSource<LogListResult>? = null,
    policy: CTPolicy? = null,
    diskCache: DiskCache? = null
) {
    init {
        require(includeHosts.isNotEmpty()) { "Please provide at least one host to enable certificate transparency verification" }
        excludeHosts.forEach {
            require(!it.startsWithWildcard) { "Certificate transparency exclusions cannot use wildcards" }
            require(!includeHosts.contains(it)) { "Certificate transparency exclusions must not match include directly" }
        }
    }

    private val cleaner: CertificateChainCleaner by lazy {
        val localTrustManager = trustManager ?: (TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }.trustManagers.first { it is X509TrustManager } as X509TrustManager)

        certificateChainCleanerFactory?.get(localTrustManager) ?: CertificateChainCleaner.get(localTrustManager)
    }

    private val logListDataSource = (logListDataSource ?: LogListDataSourceFactory.create(diskCache))

    private val policy = (policy ?: DefaultPolicy())

    fun verifyCertificateTransparency(host: String, certificates: List<Certificate>): VerificationResult {
        return if (!enabledForCertificateTransparency(host)) {
            VerificationResult.Success.DisabledForHost(host)
        } else if (certificates.isEmpty()) {
            VerificationResult.Failure.NoCertificates
        } else {
            val cleanedCerts = cleaner.clean(certificates.filterIsInstance<X509Certificate>(), host)

            if (cleanedCerts.isEmpty()) {
                VerificationResult.Failure.NoCertificates
            } else {
                hasValidSignedCertificateTimestamp(cleanedCerts)
            }
        }
    }

    /**
     * Check if the certificates provided by a server contain Signed Certificate Timestamps
     * from a trusted CT log.
     *
     * @property certificates the certificate chain provided by the server
     * @return [VerificationResult.Success] if the certificates can be trusted, [VerificationResult.Failure] otherwise.
     */
    @Suppress("ReturnCount")
    private fun hasValidSignedCertificateTimestamp(certificates: List<X509Certificate>): VerificationResult {

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

            policy.policyVerificationResult(leafCertificate, sctResults)
        } catch (e: IOException) {
            VerificationResult.Failure.UnknownIoException(e)
        }
    }

    private fun enabledForCertificateTransparency(host: String) = includeHosts.any { it.matches(host) } && !excludeHosts.any { it.matches(host) }
}
