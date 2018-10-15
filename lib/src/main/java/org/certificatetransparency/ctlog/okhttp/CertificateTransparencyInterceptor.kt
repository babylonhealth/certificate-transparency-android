package org.certificatetransparency.ctlog.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import org.certificatetransparency.ctlog.LogSignatureVerifier
import org.certificatetransparency.ctlog.datasource.DataSource
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class CertificateTransparencyInterceptor private constructor(
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

    class Builder {
        var trustManager: X509TrustManager? = null

        var logListDataSource: DataSource<Map<String, LogSignatureVerifier>>? = null

        fun build() = CertificateTransparencyInterceptor(trustManager, logListDataSource)
    }
}
