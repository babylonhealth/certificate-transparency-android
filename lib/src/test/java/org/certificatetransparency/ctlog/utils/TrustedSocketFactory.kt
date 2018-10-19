package org.certificatetransparency.ctlog.utils

import java.security.KeyStore
import java.security.cert.Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class TrustedSocketFactory {
    private fun trustManagerForCertificates(certificates: List<Certificate>): X509TrustManager {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)

            certificates.forEachIndexed { i, certificate ->
                setCertificateEntry("$i", certificate)
            }
        }

        val trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }.trustManagers

        return trustManagers.first { it is X509TrustManager } as X509TrustManager
    }

    fun create(certificates: List<Certificate>): SocketConfiguration {
        val trustManager = trustManagerForCertificates(certificates)
        val sslSocketFactory = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }.socketFactory

        return SocketConfiguration(sslSocketFactory, trustManager)
    }
}
