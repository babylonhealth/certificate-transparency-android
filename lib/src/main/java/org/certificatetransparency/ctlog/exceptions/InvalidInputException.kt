package org.certificatetransparency.ctlog.exceptions

import org.certificatetransparency.ctlog.exceptions.CertificateTransparencyException

/** Input certificates or log key are invalid.  */
class InvalidInputException(message: String, cause: Throwable) : CertificateTransparencyException(message, cause) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
