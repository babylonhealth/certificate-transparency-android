package org.certificatetransparency.ctlog.utils

import org.bouncycastle.util.io.pem.PemReader
import org.certificatetransparency.ctlog.internal.utils.PublicKeyFactory
import java.io.File
import java.io.FileReader
import java.security.PublicKey

/**
 * Load EC or RSA public key from a PEM file.
 *
 * @param pemFile File containing the key.
 * @return Public key represented by this file.
 */
fun File.readPemFile(): PublicKey {
    return PemReader(FileReader(this)).use {
        PublicKeyFactory.fromByteArray(it.readPemObject().content)
    }
}
