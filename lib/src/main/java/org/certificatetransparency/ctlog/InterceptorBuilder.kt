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
import org.certificatetransparency.ctlog.datasource.DataSource
import org.certificatetransparency.ctlog.internal.verifier.CertificateTransparencyInterceptor
import org.certificatetransparency.ctlog.internal.verifier.model.Host
import org.certificatetransparency.ctlog.loglist.LogServer
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Builder to create an OkHttp network interceptor that will verify a host is trusted using
 * certificate transparency
 */
class InterceptorBuilder {
    private var trustManager: X509TrustManager? = null
    private var logListDataSource: DataSource<Map<String, LogServer>>? = null
    private val hosts = mutableSetOf<Host>()

    /**
     * Determine if a failure to pass certificate transparency results in the connection being closed. A value of true ensures the connection is
     * closed on errors
     * Default: true
     */
    // public for access in DSL
    @Suppress("MemberVisibilityCanBePrivate")
    var failOnError: Boolean = true
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
     * A [DataSource] providing a map of log list key id's to a [LogServer]
     * Default: In memory cached log list loaded from https://www.gstatic.com/ct/log_list/log_list.json
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun setLogListDataSource(logListDataSource: DataSource<Map<String, LogServer>>) =
        apply {
            this.logListDataSource = logListDataSource
        }

    /**
     * A [DataSource] providing a map of log list key id's to a [LogServer]
     * Default: In memory cached log list loaded from https://www.gstatic.com/ct/log_list/log_list.json
     */
    @JvmSynthetic
    @Suppress("unused")
    fun logListDataSource(init: () -> DataSource<Map<String, LogServer>>) {
        setLogListDataSource(init())
    }

    /**
     * Determine if a failure to pass certificate transparency results in the connection being closed. A value of true ensures the connection is
     * closed on errors
     * Default: true
     */
    @Suppress("unused")
    fun setFailOnError(failOnError: Boolean) = apply { this.failOnError = failOnError }

    /**
     * Verify certificate transparency for hosts that match [pattern].
     *
     * @param pattern lower-case host name or wildcard pattern such as `*.example.com`.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun addHost(pattern: String) = apply {
        hosts.add(Host(pattern))
    }

    /**
     * Verify certificate transparency for host that match [this].
     *
     * @receiver lower-case host name or wildcard pattern such as `*.example.com`.
     */
    @JvmSynthetic
    operator fun String.unaryPlus() {
        addHost(this)
    }

    /**
     * Verify certificate transparency for hosts that match one of [this].
     *
     * @receiver [List] of lower-case host name or wildcard pattern such as `*.example.com`.
     */
    @JvmSynthetic
    operator fun List<String>.unaryPlus() {
        forEach { addHost(it) }
    }

    /**
     * Build the network [Interceptor]
     */
    fun build(): Interceptor = CertificateTransparencyInterceptor(hosts, trustManager, logListDataSource, failOnError)
}
