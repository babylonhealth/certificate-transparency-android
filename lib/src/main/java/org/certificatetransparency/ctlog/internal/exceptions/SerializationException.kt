package org.certificatetransparency.ctlog.internal.exceptions

/** Error serializing / deserializing data.  */
internal class SerializationException : CertificateTransparencyException {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    companion object {
        private const val serialVersionUID = 1L
    }
}
