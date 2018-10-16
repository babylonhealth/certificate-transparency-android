package org.certificatetransparency.ctlog.data.logclient.model

import org.certificatetransparency.ctlog.domain.logclient.model.DigitallySigned
import org.certificatetransparency.ctlog.domain.logclient.model.Version
import java.util.Arrays

internal data class SignedTreeHead(
    val version: Version,
    val timestamp: Long = 0,
    val treeSize: Long = 0,
    val sha256RootHash: ByteArray? = null,
    val signature: DigitallySigned? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignedTreeHead

        if (version != other.version) return false
        if (timestamp != other.timestamp) return false
        if (treeSize != other.treeSize) return false
        if (!Arrays.equals(sha256RootHash, other.sha256RootHash)) return false
        if (signature != other.signature) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + treeSize.hashCode()
        result = 31 * result + (sha256RootHash?.let { Arrays.hashCode(it) } ?: 0)
        result = 31 * result + (signature?.hashCode() ?: 0)
        return result
    }
}
