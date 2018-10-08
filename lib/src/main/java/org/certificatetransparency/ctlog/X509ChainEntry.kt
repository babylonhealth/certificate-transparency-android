package org.certificatetransparency.ctlog

class X509ChainEntry {
    // For V1 this entry just includes the certificate in the leaf_certificate field
    var leafCertificate: ByteArray? = null

    // A chain from the leaf to a trusted root (excluding leaf and possibly root).
    val certificateChain = mutableListOf<ByteArray>()
}
