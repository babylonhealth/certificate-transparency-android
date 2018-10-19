package org.certificatetransparency.ctlog.exceptions

/** Base class for CT errors.  */
open class CertificateTransparencyException : RuntimeException {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super("$message: ${cause.message}", cause)

    companion object {
        private const val serialVersionUID = 1L
    }
}
