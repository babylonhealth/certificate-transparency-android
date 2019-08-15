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

package com.babylon.certificatetransparency.internal.verifier

import com.babylon.certificatetransparency.*
import com.babylon.certificatetransparency.cache.*
import com.babylon.certificatetransparency.datasource.*
import com.babylon.certificatetransparency.internal.verifier.model.*
import com.babylon.certificatetransparency.loglist.*
import okhttp3.*
import javax.net.ssl.*

internal class CertificateTransparencyInterceptor(
        hosts: Set<Host>,
        trustManager: X509TrustManager?,
        logListDataSource: DataSource<LogListResult>?,
        diskCache: DiskCache? = null,
        private val failOnError: Boolean = true,
        private val logger: CTLogger? = null
) : CertificateTransparencyBase(hosts, trustManager, logListDataSource, diskCache), Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val host = chain.request().url().host()
        val certs = chain.connection()?.handshake()?.peerCertificates() ?: emptyList()

        val result = if (chain.connection()?.socket() is SSLSocket) {
            verifyCertificateTransparency(host, certs)
        } else {
            VerificationResult.Success.InsecureConnection(host)
        }

        logger?.log(host, result)

        if (result is VerificationResult.Failure && failOnError) {
            throw SSLPeerUnverifiedException("Certificate transparency failed")
        }

        return chain.proceed(chain.request())
    }
}
