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

package org.certificatetransparency.ctlog

import okhttp3.Interceptor
import okhttp3.internal.tls.OkHostnameVerifier
import org.certificatetransparency.ctlog.datasource.DataSource
import org.certificatetransparency.ctlog.internal.verifier.CertificateTransparencyHostnameVerifier
import org.certificatetransparency.ctlog.internal.verifier.CertificateTransparencyInterceptor
import org.certificatetransparency.ctlog.internal.verifier.model.Host
import org.certificatetransparency.ctlog.verifier.SignatureVerifier
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.X509TrustManager

object CertificateTransparencyFactory {

    class InterceptorBuilder {
        private var trustManager: X509TrustManager? = null
        private var logListDataSource: DataSource<Map<String, SignatureVerifier>>? = null
        private val hosts = mutableSetOf<Host>()

        // public for access in DSL
        @Suppress("MemberVisibilityCanBePrivate")
        var failOnError: Boolean = true
            @JvmSynthetic get
            @JvmSynthetic set

        @Suppress("MemberVisibilityCanBePrivate")
        fun setTrustManager(trustManager: X509TrustManager) = apply { this.trustManager = trustManager }

        @JvmSynthetic
        @Suppress("unused")
        fun trustManager(init: () -> X509TrustManager) = setTrustManager(init())

        @Suppress("MemberVisibilityCanBePrivate")
        fun setLogListDataSource(logListDataSource: DataSource<Map<String, SignatureVerifier>>) = apply {
            this.logListDataSource = logListDataSource
        }

        @JvmSynthetic
        @Suppress("unused")
        fun logListDataSource(init: () -> DataSource<Map<String, SignatureVerifier>>) = setLogListDataSource(init())

        @Suppress("unused")
        fun setFailOnError(failOnError: Boolean) = apply { this.failOnError = failOnError }

        /**
         * Check certificate transparency for {@code pattern}.
         *
         * @param pattern lower-case host name or wildcard pattern such as {@code *.example.com}.
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun addHosts(pattern: String) = apply {
            hosts.add(Host(pattern))
        }

        @JvmSynthetic
        operator fun String.unaryPlus() {
            addHosts(this)
        }

        @JvmSynthetic
        operator fun List<String>.unaryPlus() {
            forEach { addHosts(it) }
        }

        fun build(): Interceptor = CertificateTransparencyInterceptor(hosts, trustManager, logListDataSource, failOnError)
    }

    class HostnameVerifierBuilder(
        @Suppress("MemberVisibilityCanBePrivate") val delegate: HostnameVerifier = OkHostnameVerifier.INSTANCE
    ) {
        private var trustManager: X509TrustManager? = null
        private var logListDataSource: DataSource<Map<String, SignatureVerifier>>? = null
        private val hosts = mutableSetOf<Host>()

        @Suppress("MemberVisibilityCanBePrivate")
        fun setTrustManager(trustManager: X509TrustManager) = apply { this.trustManager = trustManager }

        @JvmSynthetic
        @Suppress("unused")
        fun trustManager(init: () -> X509TrustManager) = setTrustManager(init())

        @Suppress("MemberVisibilityCanBePrivate")
        fun setLogListDataSource(logListDataSource: DataSource<Map<String, SignatureVerifier>>) = apply {
            this.logListDataSource = logListDataSource
        }

        @JvmSynthetic
        @Suppress("unused")
        fun logListDataSource(init: () -> DataSource<Map<String, SignatureVerifier>>) = setLogListDataSource(init())

        /**
         * Check certificate transparency for {@code pattern}.
         *
         * @param pattern lower-case host name or wildcard pattern such as {@code *.example.com}.
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun addHosts(vararg pattern: String) = apply {
            pattern.forEach { hosts.add(Host(it)) }
        }

        @JvmSynthetic
        operator fun String.unaryPlus() {
            addHosts(this)
        }

        @JvmSynthetic
        operator fun List<String>.unaryPlus() {
            forEach { addHosts(it) }
        }

        fun build(): HostnameVerifier = CertificateTransparencyHostnameVerifier(delegate, hosts.toSet(), trustManager, logListDataSource)
    }
}

@JvmSynthetic
fun certificateTransparencyInterceptor(
    init: CertificateTransparencyFactory.InterceptorBuilder.() -> Unit = {}
) = CertificateTransparencyFactory.InterceptorBuilder()
    .apply(init)
    .build()

@JvmSynthetic
fun certificateTransparencyHostnameVerifier(
    delegate: HostnameVerifier = OkHostnameVerifier.INSTANCE,
    init: CertificateTransparencyFactory.HostnameVerifierBuilder.() -> Unit = {}
) = CertificateTransparencyFactory.HostnameVerifierBuilder(delegate)
    .apply(init)
    .build()
