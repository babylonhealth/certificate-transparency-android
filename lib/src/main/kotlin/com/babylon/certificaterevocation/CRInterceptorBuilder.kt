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

package com.babylon.certificaterevocation

import com.babylon.certificaterevocation.internal.revoker.CertificateRevocationInterceptor
import com.babylon.certificaterevocation.internal.revoker.CrlItem
import com.babylon.certificatetransparency.internal.utils.Base64
import okhttp3.Interceptor
import java.math.BigInteger
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

/**
 * Builder to create an OkHttp network interceptor that will reject cert chains containing revoked certificates
 */
class CRInterceptorBuilder {
    private var trustManager: X509TrustManager? = null
    private val crlSet = mutableSetOf<CrlItem>()

    /**
     * Determine if a failure to pass certificate revocation results in the connection being closed. A value of true ensures the connection is
     * closed on errors
     * Default: true
     */
    // public for access in DSL
    @Suppress("MemberVisibilityCanBePrivate")
    var failOnError: Boolean = true
        @JvmSynthetic get
        @JvmSynthetic set

    /**
     * [CRLogger] which will be called with all results
     * Default: none
     */
    // public for access in DSL
    @Suppress("MemberVisibilityCanBePrivate")
    var logger: CRLogger? = null
        @JvmSynthetic get
        @JvmSynthetic set

    /**
     * [X509TrustManager] used to clean the certificate chain
     * Default: Platform default [X509TrustManager] created through [TrustManagerFactory]
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun setTrustManager(trustManager: X509TrustManager) =
        apply { this.trustManager = trustManager }

    /**
     * [X509TrustManager] used to clean the certificate chain
     * Default: Platform default [X509TrustManager] created through [TrustManagerFactory]
     */
    @JvmSynthetic
    @Suppress("unused")
    fun trustManager(init: () -> X509TrustManager) {
        setTrustManager(init())
    }

    /**
     * Determine if a failure to pass certificate revocation results in the connection being closed. [failOnError] set to true closes the
     * connection on errors
     * Default: true
     */
    @Suppress("unused")
    fun setFailOnError(failOnError: Boolean) = apply { this.failOnError = failOnError }

    /**
     * [CRLogger] which will be called with all results
     * Default: none
     */
    @Suppress("unused")
    fun setLogger(logger: CRLogger) = apply { this.logger = logger }

    /**
     * Verify certificate revocation for certificates that match [issuerDistinguishedName] and [serialNumber].
     *
     * @property pattern lower-case host name or wildcard pattern such as `*.example.com`.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun addCrl(issuerDistinguishedName: String, serialNumbers: List<String>) = apply {
        val decodedIssuerDistinguishedName = X500Principal(Base64.decode(issuerDistinguishedName))
        val decodedSerialNumbers = serialNumbers.map { BigInteger(Base64.decode(it)) }

        crlSet.add(CrlItem(decodedIssuerDistinguishedName, decodedSerialNumbers))
    }

    /**
     * Build the network [Interceptor]
     */
    fun build(): Interceptor = CertificateRevocationInterceptor(crlSet.toSet(), trustManager, failOnError, logger)
}
