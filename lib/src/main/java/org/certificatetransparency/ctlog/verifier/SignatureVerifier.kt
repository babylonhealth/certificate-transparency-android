package org.certificatetransparency.ctlog.verifier

import org.certificatetransparency.ctlog.logclient.model.SignedCertificateTimestamp
import java.security.cert.Certificate

interface SignatureVerifier {

    fun verifySignature(sct: SignedCertificateTimestamp, chain: List<Certificate>): Boolean

    fun verifySignature(sct: SignedCertificateTimestamp, leafCert: Certificate): Boolean
}
