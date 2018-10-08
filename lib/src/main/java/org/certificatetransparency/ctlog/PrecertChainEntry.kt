package org.certificatetransparency.ctlog

class PrecertChainEntry {
    // The chain certifying the precertificate, as submitted by the CA.
    var precertificateChain = mutableListOf<ByteArray>()

    // PreCert input to the SCT. Can be computed from the above.
    // Store it alongside the entry data so that the signers don't have to parse certificates to recompute it.
    var preCert: PreCert? = null
}
