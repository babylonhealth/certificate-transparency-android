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

package org.certificatetransparency.ctlog.internal.verifier

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.Extensions
import org.bouncycastle.asn1.x509.TBSCertificate
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator
import org.bouncycastle.util.encoders.Base64
import org.certificatetransparency.ctlog.exceptions.CertificateTransparencyException
import org.certificatetransparency.ctlog.exceptions.UnsupportedCryptoPrimitiveException
import org.certificatetransparency.ctlog.internal.serialization.CTConstants
import org.certificatetransparency.ctlog.internal.serialization.CTConstants.LOG_ENTRY_TYPE_LENGTH
import org.certificatetransparency.ctlog.internal.serialization.CTConstants.MAX_CERTIFICATE_LENGTH
import org.certificatetransparency.ctlog.internal.serialization.CTConstants.MAX_EXTENSIONS_LENGTH
import org.certificatetransparency.ctlog.internal.serialization.CTConstants.TIMESTAMP_LENGTH
import org.certificatetransparency.ctlog.internal.serialization.CTConstants.VERSION_LENGTH
import org.certificatetransparency.ctlog.internal.serialization.Serializer
import org.certificatetransparency.ctlog.internal.utils.hasEmbeddedSct
import org.certificatetransparency.ctlog.internal.utils.isPreCertificate
import org.certificatetransparency.ctlog.internal.utils.isPreCertificateSigningCert
import org.certificatetransparency.ctlog.internal.utils.issuerInformation
import org.certificatetransparency.ctlog.internal.utils.issuerInformationFromPreCertificate
import org.certificatetransparency.ctlog.internal.verifier.model.IssuerInformation
import org.certificatetransparency.ctlog.internal.verifier.model.LogInfo
import org.certificatetransparency.ctlog.logclient.model.SignedCertificateTimestamp
import org.certificatetransparency.ctlog.logclient.model.Version
import org.certificatetransparency.ctlog.verifier.SignatureVerifier
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.Signature
import java.security.SignatureException
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

/**
 * Verifies signatures from a given CT Log.
 *
 * @constructor Creates a new LogSignatureVerifier which is associated with a single log.
 * @property logInfo information of the log this verifier is to be associated with.
 */
internal class LogSignatureVerifier(private val logInfo: LogInfo) : SignatureVerifier {

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
    override fun verifySignature(sct: SignedCertificateTimestamp, chain: List<Certificate>): Boolean {
        if (!logInfo.isSameLogId(sct.id.keyId)) {
            throw CertificateTransparencyException("Log ID of SCT (${Base64.toBase64String(sct.id.keyId)}) does not match this " +
                "log's ID (${Base64.toBase64String(logInfo.id)}).")
        }

        val leafCert = chain[0] as X509Certificate
        if (!leafCert.isPreCertificate() && !leafCert.hasEmbeddedSct()) {
            // When verifying final cert without embedded SCTs, we don't need the issuer but can verify directly
            val toVerify = serializeSignedSctData(leafCert, sct)
            return verifySctSignatureOverBytes(sct, toVerify)
        }
        require(chain.size >= 2) { "Chain with PreCertificate or Certificate must contain issuer." }
        // PreCertificate or final certificate with embedded SCTs, we want the issuerInformation
        val issuerCert = chain[1]
        val issuerInformation = if (!issuerCert.isPreCertificateSigningCert()) {
            // If signed by the real issuing CA
            issuerCert.issuerInformation()
        } else {
            require(chain.size >= 3) { "Chain with PreCertificate signed by PreCertificate Signing Cert must contain issuer." }
            issuerCert.issuerInformationFromPreCertificate(chain[2])
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
    override fun verifySignature(sct: SignedCertificateTimestamp, leafCert: Certificate): Boolean {
        if (!logInfo.isSameLogId(sct.id.keyId)) {
            throw CertificateTransparencyException("Log ID of SCT (${sct.id.keyId}) does not match this log's ID.")
        }
        val toVerify = serializeSignedSctData(leafCert, sct)

        return verifySctSignatureOverBytes(sct, toVerify)
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
        sct: SignedCertificateTimestamp,
        certificate: X509Certificate,
        issuerInfo: IssuerInformation): Boolean {
        requireNotNull(issuerInfo) { "At the very least, the issuer key hash is needed." }

        val preCertificateTBS = createTbsForVerification(certificate, issuerInfo)
        try {
            val toVerify = serializeSignedSctDataForPreCertificate(preCertificateTBS.encoded, issuerInfo.keyHash, sct)
            return verifySctSignatureOverBytes(sct, toVerify)
        } catch (e: IOException) {
            throw CertificateTransparencyException(
                "TBSCertificate part could not be encoded: ${e.message}", e)
        }
    }

    private fun createTbsForVerification(preCertificate: X509Certificate, issuerInformation: IssuerInformation): TBSCertificate = try {
        require(preCertificate.version >= 3)
        // We have to use bouncycastle's certificate parsing code because Java's X509 certificate
        // parsing discards the order of the extensions. The signature from SCT we're verifying
        // is over the TBSCertificate in its original form, including the order of the extensions.
        // Get the list of extensions, in its original order, minus the poison extension.
        ASN1InputStream(preCertificate.encoded).use { aIn ->
            val parsedPreCertificate = org.bouncycastle.asn1.x509.Certificate.getInstance(aIn.readObject())
            // Make sure that we have the X509akid of the real issuer if:
            // The PreCertificate has this extension, AND:
            // The PreCertificate was signed by a PreCertificate signing cert.
            if (parsedPreCertificate.hasX509AuthorityKeyIdentifier() && issuerInformation.issuedByPreCertificateSigningCert) {
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

    private fun getExtensionsWithoutPoisonAndSCT(extensions: Extensions, replacementX509authorityKeyIdentifier: Extension?): List<Extension> {
        val extensionsOidsArray = extensions.extensionOIDs
        val extensionsOids = extensionsOidsArray.iterator()

        // Order is important, which is why a list is used.
        val outputExtensions = mutableListOf<Extension>()
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

    private fun verifySctSignatureOverBytes(sct: SignedCertificateTimestamp, toVerify: ByteArray): Boolean {
        val sigAlg = when {
            logInfo.signatureAlgorithm == "EC" -> "SHA256withECDSA"
            logInfo.signatureAlgorithm == "RSA" -> "SHA256withRSA"
            else -> throw CertificateTransparencyException("Unsupported signature algorithm ${logInfo.signatureAlgorithm}")
        }

        try {
            val signature = Signature.getInstance(sigAlg)
            signature.initVerify(logInfo.key)
            signature.update(toVerify)
            return signature.verify(sct.signature.signature)
        } catch (e: SignatureException) {
            throw CertificateTransparencyException("Signature object not properly initialized or signature from SCT is improperly encoded.", e)
        } catch (e: InvalidKeyException) {
            throw CertificateTransparencyException("Log's public key cannot be used", e)
        } catch (e: NoSuchAlgorithmException) {
            throw UnsupportedCryptoPrimitiveException("$sigAlg not supported by this JVM", e)
        }
    }

    private fun org.bouncycastle.asn1.x509.Certificate.hasX509AuthorityKeyIdentifier(): Boolean {
        return tbsCertificate.extensions.getExtension(ASN1ObjectIdentifier(X509_AUTHORITY_KEY_IDENTIFIER)) != null
    }

    private fun serializeSignedSctData(certificate: Certificate, sct: SignedCertificateTimestamp?): ByteArray {
        val bos = ByteArrayOutputStream()
        serializeCommonSctFields(sct!!, bos)
        Serializer.writeUint(bos, X509_ENTRY, LOG_ENTRY_TYPE_LENGTH)
        try {
            Serializer.writeVariableLength(bos, certificate.encoded, MAX_CERTIFICATE_LENGTH)
        } catch (e: CertificateEncodingException) {
            throw CertificateTransparencyException("Error encoding certificate", e)
        }

        Serializer.writeVariableLength(bos, sct.extensions, MAX_EXTENSIONS_LENGTH)

        return bos.toByteArray()
    }

    private fun serializeSignedSctDataForPreCertificate(
        preCertBytes: ByteArray, issuerKeyHash: ByteArray, sct: SignedCertificateTimestamp?): ByteArray {
        val bos = ByteArrayOutputStream()
        serializeCommonSctFields(sct!!, bos)
        Serializer.writeUint(bos, PRECERT_ENTRY, LOG_ENTRY_TYPE_LENGTH)
        Serializer.writeFixedBytes(bos, issuerKeyHash)
        Serializer.writeVariableLength(bos, preCertBytes, MAX_CERTIFICATE_LENGTH)
        Serializer.writeVariableLength(bos, sct.extensions, MAX_EXTENSIONS_LENGTH)
        return bos.toByteArray()
    }

    private fun serializeCommonSctFields(sct: SignedCertificateTimestamp, bos: OutputStream) {
        require(sct.version == Version.V1) { "Can only serialize SCT v1 for now." }
        Serializer.writeUint(bos, sct.version.number.toLong(), VERSION_LENGTH) // ct::V1
        Serializer.writeUint(bos, 0, 1) // ct::CERTIFICATE_TIMESTAMP
        Serializer.writeUint(bos, sct.timestamp, TIMESTAMP_LENGTH) // Timestamp
    }

    companion object {
        private const val X509_AUTHORITY_KEY_IDENTIFIER = "2.5.29.35"

        private const val X509_ENTRY = 0L
        private const val PRECERT_ENTRY = 1L
    }
}
