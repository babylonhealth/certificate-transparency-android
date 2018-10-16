package org.certificatetransparency.ctlog

import okhttp3.Interceptor
import org.certificatetransparency.ctlog.data.CertificateTransparencyHostnameVerifier
import org.certificatetransparency.ctlog.data.CertificateTransparencyInterceptor
import org.certificatetransparency.ctlog.data.verifier.LogSignatureVerifier
import org.certificatetransparency.ctlog.domain.datasource.DataSource
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.X509TrustManager

object CertificateTransparencyFactory {

    fun okHttpInterceptor() = InterceptorBuilder()

    fun hostnameVerifier(delegate: HostnameVerifier) = HostnameVerifierBuilder(delegate)

    class InterceptorBuilder internal constructor() {
        @Suppress("MemberVisibilityCanBePrivate")
        var trustManager: X509TrustManager? = null

        @Suppress("MemberVisibilityCanBePrivate")
        var logListDataSource: DataSource<Map<String, LogSignatureVerifier>>? = null

        fun build(): Interceptor = CertificateTransparencyInterceptor(trustManager, logListDataSource)
    }

    class HostnameVerifierBuilder internal constructor(@Suppress("MemberVisibilityCanBePrivate") val delegate: HostnameVerifier) {
        @Suppress("MemberVisibilityCanBePrivate")
        var trustManager: X509TrustManager? = null

        @Suppress("MemberVisibilityCanBePrivate")
        var logListDataSource: DataSource<Map<String, LogSignatureVerifier>>? = null

        fun build(): HostnameVerifier = CertificateTransparencyHostnameVerifier(delegate, trustManager, logListDataSource)
    }
}
