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

package com.babylon.certificatetransparency

import com.babylon.certificatetransparency.cache.*
import com.babylon.certificatetransparency.datasource.*
import com.babylon.certificatetransparency.internal.verifier.*
import com.babylon.certificatetransparency.internal.verifier.model.*
import com.babylon.certificatetransparency.loglist.*
import javax.net.ssl.*

/**
 * Builder to create a [HostnameVerifier] that will verify a host is trusted using certificate
 * transparency
 * @property delegate [HostnameVerifier] to delegate to before performing certificate transparency checks
 */
class CTHostnameVerifierBuilder(
        @Suppress("MemberVisibilityCanBePrivate") val delegate: HostnameVerifier
) {
    private var trustManager: X509TrustManager? = null
    private var logListDataSource: DataSource<LogListResult>? = null
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
     * [CTLogger] which will be called with all results
     * Default: none
     */
    // public for access in DSL
    @Suppress("MemberVisibilityCanBePrivate")
    var logger: CTLogger? = null
        @JvmSynthetic get
        @JvmSynthetic set

    /**
     * [DiskCache] which will cache the log list
     * Default: none
     */
    // public for access in DSL
    @Suppress("MemberVisibilityCanBePrivate")
    var diskCache: DiskCache? = null
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
     * A [DataSource] providing a list of [LogServer]
     * Default: In memory cached log list loaded from https://www.gstatic.com/ct/log_list/log_list.json
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun setLogListDataSource(logListDataSource: DataSource<LogListResult>) =
            apply {
                this.logListDataSource = logListDataSource
            }

    /**
     * A [DataSource] providing a list of [LogServer]
     * Default: In memory cached log list loaded from https://www.gstatic.com/ct/log_list/log_list.json
     */
    @JvmSynthetic
    @Suppress("unused")
    fun logListDataSource(init: () -> DataSource<LogListResult>) {
        setLogListDataSource(init())
    }

    /**
     * Determine if a failure to pass certificate transparency results in the connection being closed. [failOnError] set to true closes the
     * connection on errors
     * Default: true
     */
    @Suppress("unused")
    fun setFailOnError(failOnError: Boolean) = apply { this.failOnError = failOnError }

    /**
     * [CTLogger] which will be called with all results
     * Default: none
     */
    @Suppress("unused")
    fun setLogger(logger: CTLogger) = apply { this.logger = logger }

    /**
     * [DiskCache] which will cache the log list
     * Default: none
     */
    @Suppress("unused")
    fun setDiskCache(diskCache: DiskCache) = apply { this.diskCache = diskCache }

    /**
     * Verify certificate transparency for hosts that match [pattern].
     *
     * @property pattern lower-case host name or wildcard pattern such as `*.example.com`.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun addHost(vararg pattern: String) = apply {
        pattern.forEach { hosts.add(Host(it)) }
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
     * Build the [HostnameVerifier]
     */
    fun build(): HostnameVerifier = CertificateTransparencyHostnameVerifier(
            delegate,
            hosts.toSet(),
            trustManager,
            logListDataSource,
            diskCache,
            failOnError,
            logger
    )
}
