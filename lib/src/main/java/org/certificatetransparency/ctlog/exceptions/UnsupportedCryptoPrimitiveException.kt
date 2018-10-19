package org.certificatetransparency.ctlog.exceptions

/** Indicate basic crypto primitive (X.509, SHA-256, EC) not supported by this platform.  */
class UnsupportedCryptoPrimitiveException(message: String, cause: Throwable) : CertificateTransparencyException(message, cause) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
