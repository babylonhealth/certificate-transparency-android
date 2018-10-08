package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.serialization.CryptoDataLoader

import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.util.Arrays

/**
 * Holds information about the log: Mainly, its public key and log ID (which is calculated from the
 * Log ID). Ideally created from a file with the Log's public key in PEM encoding.
 * @property key C'tor. Public key of the log.
 */
class LogInfo(val key: PublicKey) {

    val id: ByteArray = calculateLogId(key)

    val signatureAlgorithm: String = key.algorithm

    fun isSameLogId(idToCheck: ByteArray): Boolean = Arrays.equals(id, idToCheck)

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

        /**
         * Creates a LogInfo instance from the Log's public key file. Supports both EC and RSA keys.
         *
         * @param pemKeyFilePath Path of the log's public key file.
         * @return new LogInfo instance.
         */
        @JvmStatic
        fun fromKeyFile(pemKeyFilePath: String): LogInfo {
            val logPublicKey = CryptoDataLoader.keyFromFile(File(pemKeyFilePath))
            return LogInfo(logPublicKey)
        }
    }
}
