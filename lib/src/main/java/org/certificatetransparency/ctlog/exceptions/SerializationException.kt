package org.certificatetransparency.ctlog.exceptions

import org.certificatetransparency.ctlog.exceptions.CertificateTransparencyException

/** Error serializing / deserializing data.  */
class SerializationException : CertificateTransparencyException {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    companion object {
        private const val serialVersionUID = 1L
    }
}
