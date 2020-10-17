package com.babylon.certificatetransparency.chaincleaner

import javax.net.ssl.X509TrustManager

public interface CertificateChainCleanerFactory {
    public fun get(trustManager: X509TrustManager): CertificateChainCleaner
}
