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
 */

package org.certificatetransparency.ctlog.internal.verifier

import okhttp3.Interceptor
import okhttp3.Response
import org.certificatetransparency.ctlog.Logger
import org.certificatetransparency.ctlog.datasource.DataSource
import org.certificatetransparency.ctlog.internal.verifier.model.Host
import org.certificatetransparency.ctlog.Result
import org.certificatetransparency.ctlog.verifier.SignatureVerifier
import java.security.cert.X509Certificate
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.X509TrustManager

internal class CertificateTransparencyInterceptor(
    hosts: Set<Host>,
    trustManager: X509TrustManager?,
    logListDataSource: DataSource<Map<String, SignatureVerifier>>?,
    private val failOnError: Boolean = true,
    private val logger: Logger? = null
) : CertificateTransparencyBase(hosts, trustManager, logListDataSource), Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        chain.request()?.url()?.host()?.let { host ->
            val certs = chain.connection()?.handshake()?.peerCertificates()?.map { it as X509Certificate } ?: emptyList()

            val result = verifyCertificateTransparency(host, certs)

            logger?.log(host, result)

            if (result is Result.Failure && failOnError) {
                throw SSLPeerUnverifiedException("Certificate transparency failed")
            }
        }

        return chain.proceed(chain.request())
    }
}
