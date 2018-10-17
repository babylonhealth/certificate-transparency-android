package org.certificatetransparency.ctlog.domain.logclient.model

sealed class LogEntry {
    /**
     * @property leafCertificate For V1 this entry just includes the certificate in the leaf_certificate field
     * @property certificateChain A chain from the leaf to a trusted root (excluding leaf and possibly root).
     */
    data class X509ChainEntry(
        val leafCertificate: ByteArray? = null,
        val certificateChain: List<ByteArray> = emptyList()
    ) : LogEntry() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as X509ChainEntry

            if (leafCertificate != null) {
                if (other.leafCertificate == null) return false
                if (!leafCertificate.contentEquals(other.leafCertificate)) return false
            } else if (other.leafCertificate != null) return false
            if (certificateChain != other.certificateChain) return false

            return true
        }

        override fun hashCode(): Int {
            var result = leafCertificate?.contentHashCode() ?: 0
            result = 31 * result + certificateChain.hashCode()
            return result
        }
    }

    data class PreCertificateChainEntry(
        // The chain certifying the pre-certificate, as submitted by the CA.
        val preCertificateChain: List<ByteArray> = emptyList(),

        // Pre-certificate input to the SCT. Can be computed from the above.
        // Store it alongside the entry data so that the signers don't have to parse certificates to recompute it.
        val preCertificate: PreCertificate
    ) : LogEntry()
}
