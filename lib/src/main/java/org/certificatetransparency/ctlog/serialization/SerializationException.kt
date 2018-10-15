package org.certificatetransparency.ctlog.serialization

import org.certificatetransparency.ctlog.CertificateTransparencyException

/** Error serializing / deserializing data.  */
class SerializationException : CertificateTransparencyException {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    companion object {
        private const val serialVersionUID = 1L
    }
}
