package org.certificatetransparency.ctlog.internal.utils

import org.certificatetransparency.ctlog.exceptions.UnsupportedCryptoPrimitiveException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey

internal fun PublicKey.hash(): ByteArray {
    return try {
        MessageDigest.getInstance("SHA-256").digest(encoded)
    } catch (e: NoSuchAlgorithmException) {
        throw UnsupportedCryptoPrimitiveException("Missing SHA-256", e)
    }
}
