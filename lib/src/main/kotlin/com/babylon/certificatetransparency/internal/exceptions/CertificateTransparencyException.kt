package com.babylon.certificatetransparency.internal.exceptions

/** Base class for CT errors.  */
internal open class CertificateTransparencyException : RuntimeException {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super("$message: ${cause.message}", cause)

    companion object {
        private const val serialVersionUID = 1L
    }
}
