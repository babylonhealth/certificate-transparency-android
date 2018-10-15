package org.certificatetransparency.ctlog.utils

import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.crypto.tls.TlsUtils
import org.certificatetransparency.ctlog.LogInfo
import org.certificatetransparency.ctlog.LogSignatureVerifier
import org.certificatetransparency.ctlog.der.Base64
import org.certificatetransparency.ctlog.hasEmbeddedSCT
import org.certificatetransparency.ctlog.serialization.CTConstants
import org.certificatetransparency.ctlog.serialization.CryptoDataLoader
import org.certificatetransparency.ctlog.serialization.Deserializer
import org.certificatetransparency.ctlog.serialization.model.SignedCertificateTimestamp
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.ArrayList
import java.util.HashMap

/** Utility for verifying a log's signature from an SCT.  */
object VerifySignature {
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 3) {
            println("Usage: ${VerifySignature::class.java.simpleName} <certificates chain> <sct> <log public key>")
            println("<sct> can be set to 'null' in which case embedded SCTs in the leaf certificate will be extracted and verified.")
            println("<log public key> may point to a directory containing multiple public keys which will be matched against the correct SCT " +
                "to verify.")
            return
        }

        val pemFile = args[0]
        val sctFile = args[1]
        val logPublicKeyFileorDir = args[2]

        val certs = CryptoDataLoader.certificatesFromFile(File(pemFile))
        if (certs.isEmpty()) {
            println("ERROR: Certificates chain does not contain any certificates.")
            System.exit(-1)
        }
        var scts: MutableList<SignedCertificateTimestamp> = ArrayList()
        if ("null" == sctFile) {
            println("No SCTs as input, assuming there are some in the cert")
            val leafCert = certs[0] as X509Certificate
            if (leafCert.hasEmbeddedSCT()) {
                // Get the SCT(s) from the certificate
                println("The leafcert does have some SCTs")
                scts = parseSCTsFromCert(leafCert)
            }
        } else {
            val sctBytes = File(sctFile).readBytes()
            try {
                scts.add(Deserializer.parseSCTFromBinary(ByteArrayInputStream(sctBytes)))
            } catch (e: Exception) {
                println("Not a valid buffer")
            }
        }
        if (scts.isEmpty()) {
            println("ERROR: Certificate does not contain SCTs, and no SCTs provided as input.")
            System.exit(-1)
        }

        // Read log keys
        val logInfos = readLogKeys(logPublicKeyFileorDir)

        // Verify the SCTs one at a time
        var success = true
        for (sct in scts) {
            val id = Base64.toBase64String(sct.id.keyId)
            println("SCT to verify with keyID: $id")
            println(sct.toString())
            val logInfo = logInfos[id]
            if (logInfo == null) {
                println("No log with ID: $id found among loaded log keys, skipping verification with FAILURE")
                success = false
            } else {
                val verifier = LogSignatureVerifier(logInfo)
                if (verifier.verifySignature(sct, certs)) {
                    println("Signature verified OK.")
                } else {
                    println("Signature verification FAILURE.")
                    success = false
                }
            }
        }
        if (!success) {
            System.exit(-1)
        }
    }

    @Throws(IOException::class)
    fun parseSCTsFromCert(leafCert: X509Certificate): MutableList<SignedCertificateTimestamp> {
        val bytes = leafCert.getExtensionValue(CTConstants.SCT_CERTIFICATE_OID)
        val p = ASN1Primitive.fromByteArray(ASN1OctetString.getInstance(bytes).octets) as DEROctetString

        // These are serialized SCTs, we must de-serialize them into an array with one SCT each
        return parseSCTsFromCertExtension(p.octets).toMutableList()
    }

    /**
     * Reads CT log public key from a file, or all keys that reside in a directory
     *
     * @param logPublicKeyFileorDir a public key PEM file or a directory with public key PEM files
     * @return Map with id, LogInfo, a LogInfo can thus be obtained from the map if you know the log
     * ID
     */
    private fun readLogKeys(logPublicKeyFileorDir: String): Map<String, LogInfo> {
        val logInfos = HashMap<String, LogInfo>()
        val file = File(logPublicKeyFileorDir)
        if (file.isDirectory) {
            // Read all public keys in the directory into a hashmap
            // then choose the correct one based on Log ID
            val files = file.listFiles()
            for (keyfile in files!!) {
                val logInfo = LogInfo.fromKeyFile(keyfile.absolutePath)
                val id = Base64.toBase64String(logInfo.id)
                println("Log ID: $id, ${keyfile.absolutePath}")
                if (logInfos.put(id, logInfo) != null) {
                    println("A logInfo with ID $id was already present, replacing the old entry with this one.")
                }
            }
        } else {
            val logInfo = LogInfo.fromKeyFile(logPublicKeyFileorDir)
            println("Log ID: ${Base64.toBase64String(logInfo.id)}, ${file.absolutePath}")
            logInfos[Base64.toBase64String(logInfo.id)] = logInfo
        }
        return logInfos
    }

    @Throws(IOException::class)
    private fun parseSCTsFromCertExtension(extensionvalue: ByteArray): Array<SignedCertificateTimestamp> {
        val sctList = ArrayList<SignedCertificateTimestamp>()
        val bis = ByteArrayInputStream(extensionvalue)
        TlsUtils.readUint16(bis) // first one is the length of all SCTs concatenated, we don't actually need this
        while (bis.available() > 2) {
            val sctBytes = TlsUtils.readOpaque16(bis)
            // System.out.println("Read SCT bytes (excluding length): " + sctBytes.length);
            sctList.add(Deserializer.parseSCTFromBinary(ByteArrayInputStream(sctBytes)))
        }
        return sctList.toTypedArray()
    }
}
