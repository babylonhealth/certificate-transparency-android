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
 */

package com.babylon.certificaterevocation.internal.revoker

import com.babylon.certificaterevocation.RevocationResult
import com.babylon.certificatetransparency.chaincleaner.CertificateChainCleaner
import com.babylon.certificatetransparency.chaincleaner.CertificateChainCleanerFactory
import java.io.IOException
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal open class CertificateRevocationBase(
    private val crlSet: Set<CrlItem> = emptySet(),
    private val certificateChainCleanerFactory: CertificateChainCleanerFactory? = null,
    trustManager: X509TrustManager? = null
) {
    private val cleaner: CertificateChainCleaner by lazy {
        val localTrustManager = trustManager ?: (TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }.trustManagers.first { it is X509TrustManager } as X509TrustManager)

        certificateChainCleanerFactory?.get(localTrustManager) ?: CertificateChainCleaner.get(localTrustManager)
    }

    fun verifyCertificateRevocation(host: String, certificates: List<Certificate>): RevocationResult {
        return if (certificates.isEmpty()) {
            RevocationResult.Failure.NoCertificates
        } else {
            val cleanedCerts = cleaner.clean(certificates.filterIsInstance<X509Certificate>(), host)
            if (cleanedCerts.isEmpty()) {
                RevocationResult.Failure.NoCertificates
            } else {
                hasRevokedCertificate(cleanedCerts)
            }
        }
    }

    /**
     * Check if the certificates provided by a server contain any known revoked certificates.
     *
     * @property certificates the certificate chain provided by the server
     * @return [RevocationResult.Success] if the certificates can be trusted, [RevocationResult.Failure] otherwise.
     */
    @Suppress("ReturnCount")
    private fun hasRevokedCertificate(certificates: List<X509Certificate>): RevocationResult {
        return try {
            certificates.forEach { certificate ->
                val isRevoked = crlSet.any { pin ->
                    pin.issuerDistinguishedName == certificate.issuerX500Principal && pin.serialNumbers.contains(certificate.serialNumber)
                }

                if (isRevoked) {
                    return RevocationResult.Failure.CertificateRevoked(certificate)
                }
            }

            RevocationResult.Success.Trusted
        } catch (e: IOException) {
            RevocationResult.Failure.UnknownIoException(e)
        }
    }
}
