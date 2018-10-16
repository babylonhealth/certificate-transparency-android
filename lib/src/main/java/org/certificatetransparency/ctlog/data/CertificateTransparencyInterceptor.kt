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

package org.certificatetransparency.ctlog.data

import okhttp3.Interceptor
import okhttp3.Response
import org.certificatetransparency.ctlog.data.verifier.LogSignatureVerifier
import org.certificatetransparency.ctlog.domain.datasource.DataSource
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

internal class CertificateTransparencyInterceptor(
    trustManager: X509TrustManager? = null,
    logListDataSource: DataSource<Map<String, LogSignatureVerifier>>?
) : CertificateTransparencyBase(trustManager, logListDataSource), Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val certs = chain.connection()?.handshake()?.peerCertificates()?.map { it as X509Certificate } ?: emptyList()

        val cleanedCerts = cleaner.clean(certs, chain.request()?.url()?.host()!!)

        if (!isGood(cleanedCerts)) {
            chain.call().cancel()
        }

        return chain.proceed(chain.request())
    }
}
