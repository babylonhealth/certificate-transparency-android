package org.certificatetransparency.ctlog.domain.logclient.model

data class LogId(
    val keyId: ByteArray
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LogId

        if (!keyId.contentEquals(other.keyId)) return false

        return true
    }

    override fun hashCode(): Int {
        return keyId.contentHashCode()
    }
}
