package org.certificatetransparency.ctlog.comm

import org.certificatetransparency.ctlog.CertificateTransparencyException

/** Indicates the log was unreadable - HTTP communication with it was not possible.  */
class LogCommunicationException : CertificateTransparencyException {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    companion object {
        private const val serialVersionUID = 1L
    }
}
