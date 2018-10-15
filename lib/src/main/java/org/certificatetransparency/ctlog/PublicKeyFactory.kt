package org.certificatetransparency.ctlog

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

object PublicKeyFactory {

    fun fromByteArray(bytes: ByteArray): PublicKey {
        val keyFactory = KeyFactory.getInstance(determineKeyAlgorithm(bytes))
        return keyFactory.generatePublic(X509EncodedKeySpec(bytes))
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
