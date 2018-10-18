package org.certificatetransparency.ctlog.utils

import org.certificatetransparency.ctlog.UnsupportedCryptoPrimitiveException
import org.certificatetransparency.ctlog.serialization.InvalidInputException
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory

/** Class for reading various crypto structures off disk.  */
object CryptoDataLoader {
    /**
     * Returns a list of certificates from an input stream of PEM-encoded certs.
     *
     * @param pemStream input stream with PEM bytes
     * @return A list of certificates in the PEM file.
     */
    private fun parseCertificates(pemStream: InputStream): List<Certificate> {
        val factory = try {
            CertificateFactory.getInstance("X.509")
        } catch (e: CertificateException) {
            throw UnsupportedCryptoPrimitiveException("Failure getting X.509 factory", e)
        }

        try {
            return factory.generateCertificates(pemStream).toList()
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
            return parseCertificates(pemCertsFile.inputStream())
        } catch (e: FileNotFoundException) {
            throw InvalidInputException("Could not find certificate chain file $pemCertsFile.", e)
        }
    }
}
