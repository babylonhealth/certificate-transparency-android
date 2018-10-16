package org.certificatetransparency.ctlog

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey

/**
 * Holds information about the log: Mainly, its public key and log ID (which is calculated from the
 * Log ID). Ideally created from a file with the Log's public key in PEM encoding.
 *
 * @constructor C'tor.
 * @property key Public key of the log.
 */
data class LogInfo(val key: PublicKey) {

    val id: ByteArray = calculateLogId(key)

    val signatureAlgorithm: String = key.algorithm

    fun isSameLogId(idToCheck: ByteArray): Boolean = id.contentEquals(idToCheck)

    companion object {

        private fun calculateLogId(logKey: PublicKey): ByteArray {
            try {
                val sha256 = MessageDigest.getInstance("SHA-256")
                sha256.update(logKey.encoded)
                return sha256.digest()
            } catch (e: NoSuchAlgorithmException) {
                throw UnsupportedCryptoPrimitiveException("Missing SHA-256", e)
            }
        }
    }
}
