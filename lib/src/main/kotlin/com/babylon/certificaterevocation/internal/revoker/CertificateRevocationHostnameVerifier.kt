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

import com.babylon.certificaterevocation.CRLogger
import com.babylon.certificaterevocation.RevocationResult
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession
import javax.net.ssl.X509TrustManager

internal class CertificateRevocationHostnameVerifier(
    private val delegate: HostnameVerifier,
    crlSet: Set<CrlItem>,
    trustManager: X509TrustManager?,
    private val failOnError: Boolean = true,
    private val logger: CRLogger? = null
) : CertificateRevocationBase(crlSet, trustManager), HostnameVerifier {

    override fun verify(host: String, sslSession: SSLSession): Boolean {
        if (!delegate.verify(host, sslSession)) {
            return false
        }

        val result = verifyCertificateRevocation(host, sslSession.peerCertificates.toList())

        logger?.log(host, result)

        return !(result is RevocationResult.Failure && failOnError)
    }
}
