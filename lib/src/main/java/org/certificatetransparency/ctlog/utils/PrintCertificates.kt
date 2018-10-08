package org.certificatetransparency.ctlog.utils

import org.certificatetransparency.ctlog.serialization.CryptoDataLoader
import java.io.File
import java.io.IOException
import java.security.cert.CertificateException

/** Utility class for printing certificate chains. Openssl is probably better for this.  */
object PrintCertificates {
    @Throws(IOException::class, CertificateException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: ${PrintCertificates::class.java.simpleName} <certificate chain>")
            return
        }

        val pemFile = args[0]

        val certs = CryptoDataLoader.certificatesFromFile(File(pemFile))

        println("Total number of certificates in chain: ${certs.size}")
        for (cert in certs) {
            println("------------------------------------------")
            println(cert)
        }
    }
}
