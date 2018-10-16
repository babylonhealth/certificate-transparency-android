package org.certificatetransparency.ctlog.serialization

import org.bouncycastle.util.io.pem.PemReader
import org.certificatetransparency.ctlog.PublicKeyFactory
import org.certificatetransparency.ctlog.UnsupportedCryptoPrimitiveException
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
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
            throw InvalidInputException("Could not find certificate chain file $pemCertsFile.", e)
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
            val pemContent = PemReader(FileReader(pemFile)).readPemObject().content
            return PublicKeyFactory.fromByteArray(pemContent)
        } catch (e: IOException) {
            throw InvalidInputException("Error reading input file $pemFile", e)
        }
    }
}
