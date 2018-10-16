package org.certificatetransparency.ctlog

sealed class SignedEntry {

    data class X509(val x509: ByteArray) : SignedEntry() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as X509

            if (!x509.contentEquals(other.x509)) return false

            return true
        }

        override fun hashCode(): Int {
            return x509.contentHashCode()
        }
    }

    data class PreCertificate(val preCertificate: org.certificatetransparency.ctlog.PreCertificate) : SignedEntry()
}
