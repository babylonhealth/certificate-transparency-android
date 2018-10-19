/*
 * Copyright 2018 Babylon Healthcare Services Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Code derived from https://github.com/google/certificate-transparency-java
 */

package org.certificatetransparency.ctlog.internal.utils

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import org.bouncycastle.util.io.pem.PemReader
import org.certificatetransparency.ctlog.exceptions.InvalidInputException
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
