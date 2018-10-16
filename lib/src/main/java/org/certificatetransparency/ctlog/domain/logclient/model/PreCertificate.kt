package org.certificatetransparency.ctlog.domain.logclient.model

data class PreCertificate(
    val issuerKeyHash: ByteArray? = null,
    val tbsCertificate: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreCertificate

        if (issuerKeyHash != null) {
            if (other.issuerKeyHash == null) return false
            if (!issuerKeyHash.contentEquals(other.issuerKeyHash)) return false
        } else if (other.issuerKeyHash != null) return false
        if (tbsCertificate != null) {
            if (other.tbsCertificate == null) return false
            if (!tbsCertificate.contentEquals(other.tbsCertificate)) return false
        } else if (other.tbsCertificate != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = issuerKeyHash?.contentHashCode() ?: 0
        result = 31 * result + (tbsCertificate?.contentHashCode() ?: 0)
        return result
    }
}
