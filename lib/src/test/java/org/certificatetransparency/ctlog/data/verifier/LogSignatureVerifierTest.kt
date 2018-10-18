package org.certificatetransparency.ctlog.data.verifier

import org.certificatetransparency.ctlog.Base64
import org.certificatetransparency.ctlog.LogInfo
import org.certificatetransparency.ctlog.PublicKeyFactory
import org.certificatetransparency.ctlog.TestData
import org.certificatetransparency.ctlog.TestData.INTERMEDIATE_CA_CERT
import org.certificatetransparency.ctlog.TestData.PRE_CERT_SIGNING_BY_INTERMEDIATE
import org.certificatetransparency.ctlog.TestData.PRE_CERT_SIGNING_CERT
import org.certificatetransparency.ctlog.TestData.ROOT_CA_CERT
import org.certificatetransparency.ctlog.TestData.TEST_CERT
import org.certificatetransparency.ctlog.TestData.TEST_CERT_SCT
import org.certificatetransparency.ctlog.TestData.TEST_CERT_SCT_RSA
import org.certificatetransparency.ctlog.TestData.TEST_GITHUB_CHAIN
import org.certificatetransparency.ctlog.TestData.TEST_INTERMEDIATE_CERT
import org.certificatetransparency.ctlog.TestData.TEST_INTERMEDIATE_CERT_SCT
import org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY
import org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_DIGICERT
import org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_PILOT
import org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_RSA
import org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_SKYDIVER
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_PRECA_SCT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE_SCT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_CERT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE_SCT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_SCT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_SCT_RSA
import org.certificatetransparency.ctlog.TestData.loadCertificates
import org.certificatetransparency.ctlog.hasEmbeddedSct
import org.certificatetransparency.ctlog.issuerInformation
import org.certificatetransparency.ctlog.serialization.Deserializer
import org.certificatetransparency.ctlog.signedCertificateTimestamps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.File
import java.security.cert.Certificate
import java.security.cert.X509Certificate

/**
 * This test verifies that the data is correctly serialized for signature comparison, so signature
 * verification is actually effective.
 */
@RunWith(JUnit4::class)
class LogSignatureVerifierTest {
    /** Returns a LogSignatureVerifier for the test log with an EC key  */
    private val verifier: LogSignatureVerifier
        get() {
            val logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY))
            return LogSignatureVerifier(logInfo)
        }

    /** Returns a LogSignatureVerifier for the test log with an RSA key  */
    private val verifierRSA: LogSignatureVerifier
        get() {
            val logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_RSA))
            return LogSignatureVerifier(logInfo)
        }

    /** Returns a Map of LogInfos with all log keys to verify the Github certificate  */
    private val logInfosGitHub: Map<String, LogInfo>
        get() {
            val logInfos = mutableMapOf<String, LogInfo>()
            var logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_PILOT))
            var id = Base64.toBase64String(logInfo.id)
            logInfos[id] = logInfo
            logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_SKYDIVER))
            id = Base64.toBase64String(logInfo.id)
            logInfos[id] = logInfo
            logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_DIGICERT))
            id = Base64.toBase64String(logInfo.id)
            logInfos[id] = logInfo
            return logInfos
        }

    /** Tests for package-visible methods.  */
    @Test
    fun signatureVerifies() {
        val certs = loadCertificates(TEST_CERT)
        val sct = Deserializer.parseSctFromBinary(ByteArrayInputStream(TestData.file(TEST_CERT_SCT).readBytes()))
        val verifier = verifier
        assertTrue(verifier.verifySignature(sct, certs[0]))
    }

    @Test
    fun signatureVerifiesRSA() {
        val certs = loadCertificates(TEST_CERT)
        val sct = Deserializer.parseSctFromBinary(ByteArrayInputStream(TestData.file(TEST_CERT_SCT_RSA).readBytes()))
        val verifier = verifierRSA
        assertTrue(verifier.verifySignature(sct, certs[0]))
    }

    @Test
    fun signatureOnPreCertificateVerifies() {
        val preCertificatesList = loadCertificates(TEST_PRE_CERT)
        assertEquals(1, preCertificatesList.size.toLong())
        val preCertificate = preCertificatesList[0]

        val caList = loadCertificates(ROOT_CA_CERT)
        assertEquals(1, caList.size.toLong())
        val signerCert = caList[0]

        val sct = Deserializer.parseSctFromBinary(ByteArrayInputStream(TestData.file(TEST_PRE_SCT).readBytes()))

        val verifier = verifier
        assertTrue(
            "Expected signature to verify OK",
            verifier.verifySCTOverPreCertificate(
                sct,
                preCertificate as X509Certificate,
                signerCert.issuerInformation()))
    }

    @Test
    fun signatureOnPreCertificateVerifiesRSA() {
        val preCertificatesList = loadCertificates(TEST_PRE_CERT)
        assertEquals(1, preCertificatesList.size.toLong())
        val preCertificate = preCertificatesList[0]

        val caList = loadCertificates(ROOT_CA_CERT)
        assertEquals(1, caList.size.toLong())
        val signerCert = caList[0]

        val sct = Deserializer.parseSctFromBinary(ByteArrayInputStream(TestData.file(TEST_PRE_SCT_RSA).readBytes()))

        val verifier = verifierRSA
        assertTrue(
            "Expected signature to verify OK",
            verifier.verifySCTOverPreCertificate(
                sct,
                preCertificate as X509Certificate,
                signerCert.issuerInformation()))
    }

    /** Tests for the public verifySignature method taking a chain of certificates.  */
    @Test
    fun signatureOnRegularCertChainVerifies() {
        // Flow:
        // test-cert.pem -> ca-cert.pem
        val certs = loadCertificates(TEST_CERT)
        val sct = Deserializer.parseSctFromBinary(ByteArrayInputStream(TestData.file(TEST_CERT_SCT).readBytes()))

        assertTrue(verifier.verifySignature(sct, certs))
    }

    @Test
    fun signatureOnCertSignedByIntermediateVerifies() {
        // Flow:
        // test-intermediate-cert.pem -> intermediate-cert.pem -> ca-cert.pem
        val certsChain = arrayListOf<Certificate>()
        certsChain.addAll(loadCertificates(TEST_INTERMEDIATE_CERT))
        certsChain.addAll(loadCertificates(INTERMEDIATE_CA_CERT))
        certsChain.addAll(loadCertificates(ROOT_CA_CERT))
        val sct = Deserializer.parseSctFromBinary(ByteArrayInputStream(TestData.file(TEST_INTERMEDIATE_CERT_SCT).readBytes()))

        assertTrue(verifier.verifySignature(sct, certsChain))
    }

    @Test
    fun signatureOnPreCertificateCertsChainVerifies() {
        // Flow:
        // test-embedded-pre-cert.pem -> ca-cert.pem
        val certsChain = arrayListOf<Certificate>()
        certsChain.addAll(loadCertificates(TEST_PRE_CERT))
        certsChain.addAll(loadCertificates(ROOT_CA_CERT))

        val sct = Deserializer.parseSctFromBinary(ByteArrayInputStream(TestData.file(TEST_PRE_SCT).readBytes()))

        assertTrue(verifier.verifySignature(sct, certsChain))
    }

    @Test
    fun signatureOnPreCertificateSignedByPreCertificateSigningCertVerifies() {
        // Flow:
        // test-embedded-with-preca-pre-cert.pem -> ca-pre-cert.pem -> ca-cert.pem
        val certsChain = arrayListOf<Certificate>()
        certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_PRECA_CERT))
        certsChain.addAll(loadCertificates(PRE_CERT_SIGNING_CERT))
        certsChain.addAll(loadCertificates(ROOT_CA_CERT))

        val sct = Deserializer.parseSctFromBinary(ByteArrayInputStream(TestData.file(TEST_PRE_CERT_PRECA_SCT).readBytes()))

        assertTrue("Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
    }

    @Test
    fun signatureOnPreCertificateSignedByIntermediateVerifies() {
        // Flow:
        // test-embedded-with-intermediate-cert.pem -> intermediate-cert.pem -> ca-cert.pem
        val certsChain = arrayListOf<Certificate>()
        certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE))
        certsChain.addAll(loadCertificates(INTERMEDIATE_CA_CERT))
        certsChain.addAll(loadCertificates(ROOT_CA_CERT))

        val sct = Deserializer.parseSctFromBinary(ByteArrayInputStream(TestData.file(TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE_SCT).readBytes()))

        assertTrue(
            "Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
    }

    @Test
    fun signatureOnPreCertificateSignedByPreCertSigningCertSignedByIntermediateVerifies() {
        // Flow:
        // test-embedded-with-intermediate-preca-pre-cert.pem -> intermediate-pre-cert.pem
        //   -> intermediate-cert.pem -> ca-cert.pem
        val certsChain = arrayListOf<Certificate>()
        certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE))
        certsChain.addAll(loadCertificates(PRE_CERT_SIGNING_BY_INTERMEDIATE))
        certsChain.addAll(loadCertificates(INTERMEDIATE_CA_CERT))
        certsChain.addAll(loadCertificates(ROOT_CA_CERT))

        val sct = Deserializer.parseSctFromBinary(
            ByteArrayInputStream(TestData.file(TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE_SCT).readBytes()))

        assertTrue("Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
    }

    @Test
    fun throwsWhenChainWithPreCertificateSignedByPreCertificateSigningCertMissingIssuer() {
        val certsChain = arrayListOf<Certificate>()
        certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_PRECA_CERT))
        certsChain.addAll(loadCertificates(PRE_CERT_SIGNING_CERT))

        val sct = Deserializer.parseSctFromBinary(ByteArrayInputStream(TestData.file(TEST_PRE_CERT_PRECA_SCT).readBytes()))

        try {
            verifier.verifySignature(sct, certsChain)
            fail("Expected verifySignature to throw since the issuer certificate is missing.")
        } catch (expected: IllegalArgumentException) {
            assertNotNull("Exception should have message, but was: $expected", expected.message)
            assertTrue(
                "Expected exception to warn about missing issuer cert",
                expected.message?.contains("must contain issuer") == true)
        }
    }

    @Test
    fun signatureOnEmbeddedSCTsInFinalCertificateVerifies() {
        // Flow:
        // github-chain.txt contains leaf certificate signed by issuing CA.
        // Leafcert contains three embedded SCTs, we verify them all
        val certsChain = arrayListOf<Certificate>()
        certsChain.addAll(loadCertificates(TEST_GITHUB_CHAIN))

        // the leaf cert is the first one in this test data
        val leafcert = certsChain[0] as X509Certificate
        val issuerCert = certsChain[1]
        assertTrue("The test certificate does have embedded SCTs", leafcert.hasEmbeddedSct())
        val scts = leafcert.signedCertificateTimestamps()
        assertEquals("Expected 3 SCTs in the test certificate", 3, scts.size.toLong())
        val logInfos = logInfosGitHub
        for (sct in scts) {
            val id = Base64.toBase64String(sct.id.keyId)
            val logInfo = logInfos[id]
            println(id)
            val verifier = LogSignatureVerifier(logInfo!!)

            assertTrue(
                "Expected signature to verify OK",
                verifier.verifySCTOverPreCertificate(
                    sct,
                    leafcert,
                    issuerCert.issuerInformation()))
            assertTrue("Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
        }
    }

    /**
     * Creates a LogInfo instance from the Log's public key file. Supports both EC and RSA keys.
     *
     * @param pemKeyFilePath Path of the log's public key file.
     * @return new LogInfo instance.
     */
    private fun LogInfo.Companion.fromKeyFile(pemKeyFilePath: String): LogInfo {
        val logPublicKey = PublicKeyFactory.fromPemFile(File(pemKeyFilePath))
        return LogInfo(logPublicKey)
    }
}
