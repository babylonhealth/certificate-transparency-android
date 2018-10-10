package org.certificatetransparency.ctlog.serialization.model

/**
 * @property timestamp UTC time in milliseconds, since January 1, 1970, 00:00.
 */
class SignedCertificateTimestamp(
    val version: Version = Version.UNKNOWN_VERSION,
    val id: LogID,
    val timestamp: Long,
    val signature: DigitallySigned,
    val extensions: ByteArray
)
