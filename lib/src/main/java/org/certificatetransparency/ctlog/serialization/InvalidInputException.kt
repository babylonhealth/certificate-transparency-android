package org.certificatetransparency.ctlog.serialization

import org.certificatetransparency.ctlog.CertificateTransparencyException

/** Input certificates or log key are invalid.  */
class InvalidInputException(message: String, cause: Throwable) : CertificateTransparencyException(message, cause) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
