package org.certificatetransparency.ctlog.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class CertificateTransparencyInterceptor @JvmOverloads constructor(
    trustManager: X509TrustManager? = null
) : CertificateTransparencyBase(trustManager), Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val certs = chain.connection()?.handshake()?.peerCertificates()?.map { it as X509Certificate } ?: emptyList()

        val cleanedCerts = cleaner.clean(certs, chain.request()?.url()?.host()!!)

        if (!isGood(cleanedCerts)) {
            chain.call().cancel()
        }

        return chain.proceed(chain.request())
    }
}
