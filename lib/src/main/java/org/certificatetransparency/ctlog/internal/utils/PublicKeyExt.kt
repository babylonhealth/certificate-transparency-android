package org.certificatetransparency.ctlog.internal.utils

import java.security.MessageDigest
import java.security.PublicKey

/**
 * TODO
 * @throws NoSuchAlgorithmException
 */
internal fun PublicKey.hash(): ByteArray = MessageDigest.getInstance("SHA-256").digest(encoded)
