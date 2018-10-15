package org.certificatetransparency.ctlog.okhttp

import org.certificatetransparency.ctlog.LogSignatureVerifier
import org.certificatetransparency.ctlog.datasource.DataSource
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSession
import javax.net.ssl.X509TrustManager

class CertificateTransparencyHostnameVerifier private constructor(
    private val delegate: HostnameVerifier,
    trustManager: X509TrustManager?,
    logListDataSource: DataSource<Map<String, LogSignatureVerifier>>?
) : CertificateTransparencyBase(trustManager, logListDataSource), HostnameVerifier {

    override fun verify(host: String, sslSession: SSLSession): Boolean {
        if (delegate.verify(host, sslSession)) {
            try {
                val cleanedCerts = cleaner.clean(sslSession.peerCertificates.toList(), host)

                return isGood(cleanedCerts)
            } catch (e: SSLException) {
                throw RuntimeException(e)
            }
        }

        return false
    }

    class Builder(val delegate: HostnameVerifier) {
        var trustManager: X509TrustManager? = null

        var logListDataSource: DataSource<Map<String, LogSignatureVerifier>>? = null

        fun build() = CertificateTransparencyHostnameVerifier(delegate, trustManager, logListDataSource)
    }
}
