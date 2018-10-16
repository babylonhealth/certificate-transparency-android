package org.certificatetransparency.ctlog

data class PreCertificateChainEntry(
    // The chain certifying the pre-certificate, as submitted by the CA.
    val preCertificateChain: List<ByteArray> = emptyList(),

    // Pre-certificate input to the SCT. Can be computed from the above.
    // Store it alongside the entry data so that the signers don't have to parse certificates to recompute it.
    val preCertificate: PreCertificate
)
