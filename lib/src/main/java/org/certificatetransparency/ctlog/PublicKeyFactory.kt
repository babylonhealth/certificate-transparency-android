package org.certificatetransparency.ctlog

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import org.bouncycastle.util.io.pem.PemReader
import org.certificatetransparency.ctlog.serialization.InvalidInputException
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.StringReader
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

internal object PublicKeyFactory {

    fun fromByteArray(bytes: ByteArray): PublicKey {
        val keyFactory = KeyFactory.getInstance(determineKeyAlgorithm(bytes))
        return keyFactory.generatePublic(X509EncodedKeySpec(bytes))
    }

    /**
     * Load EC or RSA public key from a PEM file.
     *
     * @param pemFile File containing the key.
     * @return Public key represented by this file.
     */
    fun fromPemFile(pemFile: File): PublicKey {
        try {
            val pemContent = PemReader(FileReader(pemFile)).readPemObject().content
            return fromByteArray(pemContent)
        } catch (e: IOException) {
            throw InvalidInputException("Error reading input file $pemFile", e)
        }
    }

    fun fromPemString(keyText: String): PublicKey {
        val pemContent = PemReader(StringReader(keyText)).readPemObject().content
        return fromByteArray(pemContent)
    }

    /**
     * Parses the beginning of a key, and determines the key algorithm (RSA or EC) based on the OID
     */
    private fun determineKeyAlgorithm(keyBytes: ByteArray): String {
        val seq = ASN1Sequence.getInstance(keyBytes)
        val seq1 = seq.objects.nextElement() as DLSequence
        val oid = seq1.objects.nextElement() as ASN1ObjectIdentifier
        return when (oid) {
            PKCSObjectIdentifiers.rsaEncryption -> "RSA"
            X9ObjectIdentifiers.id_ecPublicKey -> "EC"
            else -> throw IllegalArgumentException("Unsupported key type $oid")
        }
    }
}
