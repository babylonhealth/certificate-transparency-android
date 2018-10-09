package org.certificatetransparency.ctlog.utils

import org.certificatetransparency.ctlog.comm.CtService
import org.certificatetransparency.ctlog.comm.HttpLogClient
import org.certificatetransparency.ctlog.serialization.CryptoDataLoader
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.security.cert.CertificateException
import java.security.spec.InvalidKeySpecException

/** Utility class for uploading a certificate.  */
object UploadCertificate {
    @Throws(IOException::class, CertificateException::class, InvalidKeySpecException::class, NoSuchAlgorithmException::class, InvalidKeyException::class, SignatureException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: ${UploadCertificate::class.java.simpleName} <certificates chain> [output file]")
            return
        }

        val pemFile = args[0]

        val certs = CryptoDataLoader.certificatesFromFile(File(pemFile))
        println("Total number of certificates in chain: ${certs.size}")

        val retrofit = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).baseUrl("http://ct.googleapis.com/pilot/ct/v1/").build()
        val client = HttpLogClient(retrofit.create(CtService::class.java))

        val resp = client.addCertificate(certs)

        println(resp)
        if (args.size >= 2) {
            val outputFile = args[1]
            //TODO(eranm): Binary encoding compatible with the C++ code.
            File(outputFile).writeBytes(resp?.toByteArray()!!)
        }
    }
}
