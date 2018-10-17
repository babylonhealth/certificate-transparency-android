@file:JvmName("CertificateTransparencyFactory")

package org.certificatetransparency.ctlog

import okhttp3.Interceptor
import okhttp3.internal.tls.OkHostnameVerifier
import org.certificatetransparency.ctlog.data.CertificateTransparencyHostnameVerifier
import org.certificatetransparency.ctlog.data.CertificateTransparencyInterceptor
import org.certificatetransparency.ctlog.data.verifier.LogSignatureVerifier
import org.certificatetransparency.ctlog.domain.datasource.DataSource
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.X509TrustManager

fun okHttpInterceptorBuilder() = InterceptorBuilder()

@JvmSynthetic
fun okHttpInterceptor(init: InterceptorBuilder.() -> Unit) = InterceptorBuilder()
    .apply(init)
    .build()

class InterceptorBuilder internal constructor() {
    private var trustManager: X509TrustManager? = null
    private var logListDataSource: DataSource<Map<String, LogSignatureVerifier>>? = null
    private val hosts = mutableSetOf<Host>()

    @Suppress("MemberVisibilityCanBePrivate")
    fun setTrustManager(trustManager: X509TrustManager) = apply { this.trustManager = trustManager }

    @JvmSynthetic
    @Suppress("unused")
    fun trustManager(init: () -> X509TrustManager) = setTrustManager(init())

    @Suppress("MemberVisibilityCanBePrivate")
    fun setLogListDataSource(logListDataSource: DataSource<Map<String, LogSignatureVerifier>>) = apply {
        this.logListDataSource = logListDataSource
    }

    @JvmSynthetic
    @Suppress("unused")
    fun logListDataSource(init: () -> DataSource<Map<String, LogSignatureVerifier>>) = setLogListDataSource(init())

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

    fun build(): Interceptor = CertificateTransparencyInterceptor(hosts, trustManager, logListDataSource)
}

fun hostnameVerifierBuilder(delegate: HostnameVerifier = OkHostnameVerifier.INSTANCE) = HostnameVerifierBuilder(delegate)

@JvmSynthetic
fun hostnameVerifier(delegate: HostnameVerifier = OkHostnameVerifier.INSTANCE, init: HostnameVerifierBuilder.() -> Unit) =
    HostnameVerifierBuilder(delegate)
        .apply(init)
        .build()

class HostnameVerifierBuilder internal constructor(
    @Suppress("MemberVisibilityCanBePrivate") val delegate: HostnameVerifier = OkHostnameVerifier.INSTANCE
) {
    private var trustManager: X509TrustManager? = null
    private var logListDataSource: DataSource<Map<String, LogSignatureVerifier>>? = null
    private val hosts = mutableSetOf<Host>()

    @Suppress("MemberVisibilityCanBePrivate")
    fun setTrustManager(trustManager: X509TrustManager) = apply { this.trustManager = trustManager }

    @JvmSynthetic
    @Suppress("unused")
    fun trustManager(init: () -> X509TrustManager) = setTrustManager(init())

    @Suppress("MemberVisibilityCanBePrivate")
    fun setLogListDataSource(logListDataSource: DataSource<Map<String, LogSignatureVerifier>>) = apply {
        this.logListDataSource = logListDataSource
    }

    @JvmSynthetic
    @Suppress("unused")
    fun logListDataSource(init: () -> DataSource<Map<String, LogSignatureVerifier>>) = setLogListDataSource(init())

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

    fun build(): HostnameVerifier = CertificateTransparencyHostnameVerifier(delegate, hosts, trustManager, logListDataSource)
}
