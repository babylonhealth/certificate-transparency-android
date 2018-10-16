package org.certificatetransparency.ctlog

sealed class LogEntry {
    data class X509(val x509Entry: X509ChainEntry) : LogEntry()

    data class PreCertificate(val preCertificateEntry: PreCertificateChainEntry) : LogEntry()
}
