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

import org.certificatetransparency.ctlog.Logger
import org.certificatetransparency.ctlog.Result
import org.certificatetransparency.ctlog.datasource.DataSource
import org.certificatetransparency.ctlog.internal.verifier.model.Host
import org.certificatetransparency.ctlog.loglist.LogServer
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession
import javax.net.ssl.X509TrustManager

internal class CertificateTransparencyHostnameVerifier(
    private val delegate: HostnameVerifier,
    hosts: Set<Host>,
    trustManager: X509TrustManager?,
    logListDataSource: DataSource<Map<String, LogServer>>?,
    private val failOnError: Boolean = true,
    private val logger: Logger? = null
) : CertificateTransparencyBase(hosts, trustManager, logListDataSource), HostnameVerifier {

    override fun verify(host: String, sslSession: SSLSession): Boolean {
        if (!delegate.verify(host, sslSession)) {
            return false
        }

        val result = verifyCertificateTransparency(host, sslSession.peerCertificates.toList())

        logger?.log(host, result)

        return !(result is Result.Failure && failOnError)
    }
}
