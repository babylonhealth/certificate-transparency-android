package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.comm.CtService
import org.certificatetransparency.ctlog.comm.HttpLogClient
import org.certificatetransparency.ctlog.proto.Ct
import org.certificatetransparency.ctlog.serialization.CryptoDataLoader
import org.certificatetransparency.ctlog.serialization.Serializer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.security.cert.Certificate

/** The main CT log client. Currently only knows how to upload certificate chains to the ctlog.  */
class CTLogClient(baseLogUrl: String, logInfo: LogInfo) {
    private val retrofit = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).baseUrl(baseLogUrl).build()

    private val httpClient: HttpLogClient = HttpLogClient(retrofit.create(CtService::class.java))

    private val signatureVerifier: LogSignatureVerifier = LogSignatureVerifier(logInfo)

    /** Result of the certificate upload. Contains the SCT and verification result.  */
    class UploadResult(val sct: Ct.SignedCertificateTimestamp, val isVerified: Boolean)

    fun uploadCertificatesChain(chain: List<Certificate>): UploadResult {
        val sct = httpClient.addCertificate(chain)!!
        return UploadResult(sct, signatureVerifier.verifySignature(sct, chain[0]))
    }

    companion object {

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size < 3) {
                println("Usage: ${CTLogClient::class.java.simpleName} <Certificate chain> <Log URL> <Log public key> [output file]")
                return
            }

            val pemFile = args[0]
            val logUrl = getBaseUrl(args[1])
            val logPublicKeyFile = args[2]
            var outputSctFile: String? = null
            if (args.size >= 4) {
                outputSctFile = args[3]
            }

            val client = CTLogClient(logUrl, LogInfo.fromKeyFile(logPublicKeyFile))
            val certs = CryptoDataLoader.certificatesFromFile(File(pemFile))
            println(String.format("Total number of certificates: %d", certs.size))

            val result = client.uploadCertificatesChain(certs)
            if (result.isVerified) {
                println("Upload successful ")
                if (outputSctFile != null) {
                    val serialized = Serializer.serializeSctToBinary(result.sct)
                    File(outputSctFile).writeBytes(serialized)
                }
            } else {
                println("Log signature verification FAILED.")
            }
        }

        private fun getBaseUrl(url: String) = "http://${url}/ct/v1/"
    }
}
