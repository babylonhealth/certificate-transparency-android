package org.certificatetransparency.ctlog

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.Extensions
import org.bouncycastle.asn1.x509.TBSCertificate
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator
import org.bouncycastle.util.encoders.Base64
import org.certificatetransparency.ctlog.proto.Ct
import org.certificatetransparency.ctlog.serialization.CTConstants
import org.certificatetransparency.ctlog.serialization.CTConstants.LOG_ENTRY_TYPE_LENGTH
import org.certificatetransparency.ctlog.serialization.CTConstants.MAX_CERTIFICATE_LENGTH
import org.certificatetransparency.ctlog.serialization.CTConstants.MAX_EXTENSIONS_LENGTH
import org.certificatetransparency.ctlog.serialization.CTConstants.TIMESTAMP_LENGTH
import org.certificatetransparency.ctlog.serialization.CTConstants.VERSION_LENGTH
import org.certificatetransparency.ctlog.serialization.Serializer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.Signature
import java.security.SignatureException
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.ArrayList
import java.util.Arrays

/**
 * Verifies signatures from a given CT Log.
 *
 * @constructor Creates a new LogSignatureVerifier which is associated with a single log.
 * @property logInfo information of the log this verifier is to be associated with.
 *
 * */
class LogSignatureVerifier(private val logInfo: LogInfo) {

    internal class IssuerInformation(
        internal val name: X500Name?,
        internal val keyHash: ByteArray,
        internal val x509authorityKeyIdentifier: Extension?,
        private val issuedByPreCertificateSigningCert: Boolean) {

        internal fun issuedByPreCertificateSigningCert(): Boolean {
            return issuedByPreCertificateSigningCert
        }
    }

    /**
     * Verifies the CT Log's signature over the SCT and certificate. Works for the following cases:
     *
     *
     *  * Ordinary X509 certificate sent to the log.
     *  * PreCertificate signed by an ordinary CA certificate.
     *  * PreCertificate signed by a PreCertificate Signing Cert. In this case the PreCertificate
     * signing certificate must be 2nd on the chain, the CA cert itself 3rd.
     *
     *
     * @param sct SignedCertificateTimestamp received from the log.
     * @param chain The certificates chain as sent to the log.
     * @return true if the log's signature over this SCT can be verified, false otherwise.
     */
    fun verifySignature(sct: Ct.SignedCertificateTimestamp?, chain: List<Certificate>): Boolean {
        if (sct != null && !logInfo.isSameLogId(sct.id.keyId.toByteArray())) {
            throw CertificateTransparencyException("Log ID of SCT (${Base64.toBase64String(sct.id.keyId.toByteArray())}) does not match this " +
                "log's ID (${Base64.toBase64String(logInfo.id)}).")
        }

        val leafCert = chain[0] as X509Certificate
        if (!leafCert.isPreCertificate() && !leafCert.hasEmbeddedSCT()) {
            // When verifying final cert without embedded SCTs, we don't need the issuer but can verify directly
            val toVerify = serializeSignedSCTData(leafCert, sct)
            return verifySCTSignatureOverBytes(sct, toVerify)
        }
        require(chain.size >= 2) { "Chain with PreCertificate or Certificate must contain issuer." }
        // PreCertificate or final certificate with embedded SCTs, we want the issuerInformation
        val issuerCert = chain[1]
        val issuerInformation = if (!issuerCert.isPreCertificateSigningCert()) {
            // If signed by the real issuing CA
            issuerInformationFromCertificateIssuer(issuerCert)
        } else {
            require(chain.size >= 3) { "Chain with PreCertificate signed by PreCertificate Signing Cert must contain issuer." }
            issuerInformationFromPreCertificateSigningCert(issuerCert, getKeyHash(chain[2]))
        }
        return verifySCTOverPreCertificate(sct, leafCert, issuerInformation)
    }

    /**
     * Verifies the CT Log's signature over the SCT and leaf certificate.
     *
     * @param sct SignedCertificateTimestamp received from the log.
     * @param leafCert leaf certificate sent to the log.
     * @return true if the log's signature over this SCT can be verified, false otherwise.
     */
    fun verifySignature(sct: Ct.SignedCertificateTimestamp, leafCert: Certificate): Boolean {
        if (!logInfo.isSameLogId(sct.id.keyId.toByteArray())) {
            throw CertificateTransparencyException("Log ID of SCT (${sct.id.keyId}) does not match this log's ID.")
        }
        val toVerify = serializeSignedSCTData(leafCert, sct)

        return verifySCTSignatureOverBytes(sct, toVerify)
    }

    /**
     * Verifies the CT Log's signature over the SCT and the PreCertificate, or a final certificate.
     *
     * @param sct SignedCertificateTimestamp received from the log.
     * @param certificate the PreCertificate sent to the log for addition, or the final certificate
     * with the embedded SCTs.
     * @param issuerInfo Information on the issuer which will (or did) ultimately sign this
     * PreCertificate. If the PreCertificate was signed using by a PreCertificate Signing Cert,
     * the issuerInfo contains data on the final CA certificate used for signing.
     * @return true if the SCT verifies, false otherwise.
     */
    internal fun verifySCTOverPreCertificate(
        sct: Ct.SignedCertificateTimestamp?,
        certificate: X509Certificate,
        issuerInfo: IssuerInformation): Boolean {
        requireNotNull(issuerInfo) { "At the very least, the issuer key hash is needed." }

        val preCertificateTBS = createTbsForVerification(certificate, issuerInfo)
        try {
            val toVerify = serializeSignedSCTDataForPreCertificate(preCertificateTBS.encoded, issuerInfo.keyHash, sct)
            return verifySCTSignatureOverBytes(sct, toVerify)
        } catch (e: IOException) {
            throw CertificateTransparencyException(
                "TBSCertificate part could not be encoded: ${e.message}", e)
        }
    }

    private fun createTbsForVerification(
        preCertificate: X509Certificate, issuerInformation: IssuerInformation): TBSCertificate {
        require(preCertificate.version >= 3)
        // We have to use bouncycastle's certificate parsing code because Java's X509 certificate
        // parsing discards the order of the extensions. The signature from SCT we're verifying
        // is over the TBSCertificate in its original form, including the order of the extensions.
        // Get the list of extensions, in its original order, minus the poison extension.
        try {
            ASN1InputStream(preCertificate.encoded).use { aIn ->
                val parsedPreCertificate = org.bouncycastle.asn1.x509.Certificate.getInstance(aIn.readObject())
                // Make sure that we have the X509akid of the real issuer if:
                // The PreCertificate has this extension, AND:
                // The PreCertificate was signed by a PreCertificate signing cert.
                if (hasX509AuthorityKeyIdentifier(parsedPreCertificate) && issuerInformation.issuedByPreCertificateSigningCert()) {
                    require(issuerInformation.x509authorityKeyIdentifier != null)
                }

                val orderedExtensions = getExtensionsWithoutPoisonAndSCT(
                    parsedPreCertificate.tbsCertificate.extensions,
                    issuerInformation.x509authorityKeyIdentifier)

                return V3TBSCertificateGenerator().apply {
                    val tbsPart = parsedPreCertificate.tbsCertificate
                    // Copy certificate.
                    // Version 3 is implied by the generator.
                    setSerialNumber(tbsPart.serialNumber)
                    setSignature(tbsPart.signature)
                    if (issuerInformation.name != null) {
                        setIssuer(issuerInformation.name)
                    } else {
                        setIssuer(tbsPart.issuer)
                    }
                    setStartDate(tbsPart.startDate)
                    setEndDate(tbsPart.endDate)
                    setSubject(tbsPart.subject)
                    setSubjectPublicKeyInfo(tbsPart.subjectPublicKeyInfo)
                    setIssuerUniqueID(tbsPart.issuerUniqueId)
                    setSubjectUniqueID(tbsPart.subjectUniqueId)
                    setExtensions(Extensions(orderedExtensions.toTypedArray()))
                }.generateTBSCertificate()
            }
        } catch (e: CertificateException) {
            throw CertificateTransparencyException("Certificate error: ${e.message}", e)
        } catch (e: IOException) {
            throw CertificateTransparencyException("Error deleting extension: ${e.message}", e)
        }
    }

    private fun getExtensionsWithoutPoisonAndSCT(
        extensions: Extensions, replacementX509authorityKeyIdentifier: Extension?): List<Extension> {
        val extensionsOidsArray = extensions.extensionOIDs
        val extensionsOids = Arrays.asList(*extensionsOidsArray).iterator()

        // Order is important, which is why a list is used.
        val outputExtensions = ArrayList<Extension>()
        while (extensionsOids.hasNext()) {
            val extn = extensionsOids.next()
            val extnId = extn.id
            if (extnId == CTConstants.POISON_EXTENSION_OID) {
                // Do nothing - skip copying this extension
            } else if (extnId == CTConstants.SCT_CERTIFICATE_OID) {
                // Do nothing - skip copying this extension
            } else if (extnId == X509_AUTHORITY_KEY_IDENTIFIER && replacementX509authorityKeyIdentifier != null) {
                // Use the real issuer's authority key identifier, since it's present.
                outputExtensions.add(replacementX509authorityKeyIdentifier)
            } else {
                // Copy the extension as-is.
                outputExtensions.add(extensions.getExtension(extn))
            }
        }
        return outputExtensions
    }

    private fun verifySCTSignatureOverBytes(sct: Ct.SignedCertificateTimestamp?, toVerify: ByteArray): Boolean {
        val sigAlg = when {
            logInfo.signatureAlgorithm == "EC" -> "SHA256withECDSA"
            logInfo.signatureAlgorithm == "RSA" -> "SHA256withRSA"
            else -> throw CertificateTransparencyException("Unsupported signature algorithm ${logInfo.signatureAlgorithm}")
        }

        try {
            val signature = Signature.getInstance(sigAlg)
            signature.initVerify(logInfo.key)
            signature.update(toVerify)
            return signature.verify(sct!!.signature.signature.toByteArray())
        } catch (e: SignatureException) {
            throw CertificateTransparencyException("Signature object not properly initialized or signature from SCT is improperly encoded.", e)
        } catch (e: InvalidKeyException) {
            throw CertificateTransparencyException("Log's public key cannot be used", e)
        } catch (e: NoSuchAlgorithmException) {
            throw UnsupportedCryptoPrimitiveException("$sigAlg not supported by this JVM", e)
        }
    }

    companion object {
        const val X509_AUTHORITY_KEY_IDENTIFIER = "2.5.29.35"

        internal fun issuerInformationFromPreCertificateSigningCert(
            certificate: Certificate, keyHash: ByteArray): IssuerInformation {
            try {
                ASN1InputStream(certificate.encoded).use { aIssuerIn ->
                    val parsedIssuerCert = org.bouncycastle.asn1.x509.Certificate.getInstance(aIssuerIn.readObject())

                    val issuerExtensions = parsedIssuerCert.tbsCertificate.extensions
                    val x509authorityKeyIdentifier = issuerExtensions?.getExtension(ASN1ObjectIdentifier(X509_AUTHORITY_KEY_IDENTIFIER))

                    return IssuerInformation(parsedIssuerCert.issuer, keyHash, x509authorityKeyIdentifier, true)
                }
            } catch (e: CertificateEncodingException) {
                throw CertificateTransparencyException("Certificate could not be encoded: ${e.message}", e)
            } catch (e: IOException) {
                throw CertificateTransparencyException("Error during ASN.1 parsing of certificate: ${e.message}", e)
            }
        }

        // Produces issuer information in case the PreCertificate is signed by a regular CA cert,
        // not PreCertificate Signing Cert. In this case, the only thing that's needed is the
        // issuer key hash - the Precertificate will already have the right value for the issuer
        // name and K509 Authority Key Identifier extension.
        internal fun issuerInformationFromCertificateIssuer(certificate: Certificate): IssuerInformation {
            return IssuerInformation(null, getKeyHash(certificate), null, false)
        }

        private fun hasX509AuthorityKeyIdentifier(
            cert: org.bouncycastle.asn1.x509.Certificate): Boolean {
            val extensions = cert.tbsCertificate.extensions
            return extensions.getExtension(ASN1ObjectIdentifier(X509_AUTHORITY_KEY_IDENTIFIER)) != null
        }

        internal fun serializeSignedSCTData(certificate: Certificate, sct: Ct.SignedCertificateTimestamp?): ByteArray {
            val bos = ByteArrayOutputStream()
            serializeCommonSCTFields(sct!!, bos)
            Serializer.writeUint(bos, Ct.LogEntryType.X509_ENTRY_VALUE.toLong(), LOG_ENTRY_TYPE_LENGTH)
            try {
                Serializer.writeVariableLength(bos, certificate.encoded, MAX_CERTIFICATE_LENGTH)
            } catch (e: CertificateEncodingException) {
                throw CertificateTransparencyException("Error encoding certificate", e)
            }

            Serializer.writeVariableLength(bos, sct.extensions.toByteArray(), MAX_EXTENSIONS_LENGTH)

            return bos.toByteArray()
        }

        internal fun serializeSignedSCTDataForPreCertificate(
            preCertBytes: ByteArray, issuerKeyHash: ByteArray, sct: Ct.SignedCertificateTimestamp?): ByteArray {
            val bos = ByteArrayOutputStream()
            serializeCommonSCTFields(sct!!, bos)
            Serializer.writeUint(bos, Ct.LogEntryType.PRECERT_ENTRY_VALUE.toLong(), LOG_ENTRY_TYPE_LENGTH)
            Serializer.writeFixedBytes(bos, issuerKeyHash)
            Serializer.writeVariableLength(bos, preCertBytes, MAX_CERTIFICATE_LENGTH)
            Serializer.writeVariableLength(bos, sct.extensions.toByteArray(), MAX_EXTENSIONS_LENGTH)
            return bos.toByteArray()
        }

        private fun getKeyHash(signerCert: Certificate): ByteArray {
            try {
                return MessageDigest.getInstance("SHA-256").digest(signerCert.publicKey.encoded)
            } catch (e: NoSuchAlgorithmException) {
                throw UnsupportedCryptoPrimitiveException("SHA-256 not supported: ${e.message}", e)
            }
        }

        private fun serializeCommonSCTFields(sct: Ct.SignedCertificateTimestamp, bos: ByteArrayOutputStream) {
            require(sct.version == Ct.Version.V1) { "Can only serialize SCT v1 for now." }
            Serializer.writeUint(bos, sct.version.number.toLong(), VERSION_LENGTH) // ct::V1
            Serializer.writeUint(bos, 0, 1) // ct::CERTIFICATE_TIMESTAMP
            Serializer.writeUint(bos, sct.timestamp, TIMESTAMP_LENGTH) // Timestamp
        }
    }
}
