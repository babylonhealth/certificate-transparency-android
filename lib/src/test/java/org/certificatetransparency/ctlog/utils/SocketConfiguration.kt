package org.certificatetransparency.ctlog.utils

import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

data class SocketConfiguration(
    val sslSocketFactory: SSLSocketFactory,
    val trustManager: X509TrustManager
)
