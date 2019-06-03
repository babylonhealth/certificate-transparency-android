package com.babylon.certificatetransparency.chaincleaner

import javax.net.ssl.X509TrustManager

interface CertificateChainCleanerFactory {
    fun get(trustManager: X509TrustManager): CertificateChainCleaner
}
