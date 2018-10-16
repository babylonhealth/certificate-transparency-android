package org.certificatetransparency.ctlog.serialization.model

/**
 * @property timestamp UTC time in milliseconds, since January 1, 1970, 00:00.
 */
data class SignedCertificateTimestamp(
    val version: Version = Version.UNKNOWN_VERSION,
    val id: LogID,
    val timestamp: Long,
    val signature: DigitallySigned,
    val extensions: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignedCertificateTimestamp

        if (version != other.version) return false
        if (id != other.id) return false
        if (timestamp != other.timestamp) return false
        if (signature != other.signature) return false
        if (!extensions.contentEquals(other.extensions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + signature.hashCode()
        result = 31 * result + extensions.contentHashCode()
        return result
    }
}
