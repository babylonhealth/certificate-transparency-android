package org.certificatetransparency.ctlog.serialization

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import org.bouncycastle.util.encoders.Base64
import org.certificatetransparency.ctlog.UnsupportedCryptoPrimitiveException
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import java.util.Arrays

/** Class for reading various crypto structures off disk.  */
object CryptoDataLoader {
    /**
     * Returns a list of certificates from an input stream of PEM-encoded certs.
     *
     * @param pemStream input stream with PEM bytes
     * @return A list of certificates in the PEM file.
     */
    private fun parseCertificates(pemStream: InputStream): List<Certificate> {
        val factory: CertificateFactory
        try {
            factory = CertificateFactory.getInstance("X.509")
        } catch (e: CertificateException) {
            throw UnsupportedCryptoPrimitiveException("Failure getting X.509 factory", e)
        }

        try {
            val certs = factory.generateCertificates(pemStream)
            val toReturn = certs.toTypedArray()
            return Arrays.asList(*toReturn)
        } catch (e: CertificateException) {
            throw InvalidInputException("Not a valid PEM stream", e)
        }
    }

    /**
     * Parses a PEM-encoded file containing a list of certificates.
     *
     * @param pemCertsFile File to parse.
     * @return A list of certificates from the certificates in the file.
     * @throws InvalidInputException If the file is not present.
     */
    @JvmStatic
    fun certificatesFromFile(pemCertsFile: File): List<Certificate> {
        try {
            return parseCertificates(BufferedInputStream(FileInputStream(pemCertsFile)))
        } catch (e: FileNotFoundException) {
            throw InvalidInputException(
                String.format("Could not find certificate chain file %s.", pemCertsFile), e)
        }
    }

    private fun parsePublicKey(pemLines: List<String>): PublicKey {
        //TODO(eranm): Filter out non PEM blocks.
        // Are the contents PEM-encoded?
        val correctHeader = pemLines[0] == "-----BEGIN PUBLIC KEY-----"
        val correctFooter = pemLines[pemLines.size - 1] == "-----END PUBLIC KEY-----"
        if (!correctHeader || !correctFooter) {
            throw IllegalArgumentException(
                String.format("Input is not a PEM-encoded key file: $pemLines"))
        }

        // The contents are PEM encoded - first and last lines are header and footer.
        val b64string = pemLines.subList(1, pemLines.size - 1).joinToString("")
        // Extract public key
        val keyBytes = Base64.decode(b64string)
        val keyAlg = determineKeyAlg(keyBytes)
        val spec = X509EncodedKeySpec(keyBytes)
        val kf: KeyFactory
        try {
            // Note: EC KeyFactory does not exist in openjdk, only Oracle's JDK.
            kf = KeyFactory.getInstance(keyAlg)
            return kf.generatePublic(spec)
        } catch (e: NoSuchAlgorithmException) {
            // EC is known to be missing from openjdk; Oracle's JDK must be used.
            throw UnsupportedCryptoPrimitiveException("$keyAlg support missing", e)
        } catch (e: InvalidKeySpecException) {
            throw InvalidInputException("Log public key is invalid", e)
        }
    }

    /**
     * Parses the beginning of a key, and determines the key algorithm (RSA or EC) based on the OID
     */
    private fun determineKeyAlg(keyBytes: ByteArray): String {
        val seq = ASN1Sequence.getInstance(keyBytes)
        val seq1 = seq.objects.nextElement() as DLSequence
        val oid = seq1.objects.nextElement() as ASN1ObjectIdentifier
        return when (oid) {
            PKCSObjectIdentifiers.rsaEncryption -> "RSA"
            X9ObjectIdentifiers.id_ecPublicKey -> "EC"
            else -> throw IllegalArgumentException("Unsupported key type: $oid")
        }
    }

    /**
     * Load EC or RSA public key from a PEM file.
     *
     * @param pemFile File containing the key.
     * @return Public key represented by this file.
     */
    fun keyFromFile(pemFile: File): PublicKey {
        try {
            return parsePublicKey(pemFile.readLines(Charset.defaultCharset()))
        } catch (e: IOException) {
            throw InvalidInputException(String.format("Error reading input file %s", pemFile), e)
        }
    }
}
