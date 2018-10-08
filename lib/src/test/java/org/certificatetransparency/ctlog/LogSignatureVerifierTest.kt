package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.TestData.INTERMEDIATE_CA_CERT
import org.certificatetransparency.ctlog.TestData.PRE_CERT_SIGNING_BY_INTERMEDIATE
import org.certificatetransparency.ctlog.TestData.PRE_CERT_SIGNING_CERT
import org.certificatetransparency.ctlog.TestData.ROOT_CA_CERT
import org.certificatetransparency.ctlog.TestData.TEST_CERT
import org.certificatetransparency.ctlog.TestData.TEST_CERT_SCT
import org.certificatetransparency.ctlog.TestData.TEST_CERT_SCT_RSA
import org.certificatetransparency.ctlog.TestData.TEST_INTERMEDIATE_CERT
import org.certificatetransparency.ctlog.TestData.TEST_INTERMEDIATE_CERT_SCT
import org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY
import org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_RSA
import org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_PILOT
import org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_SKYDIVER
import org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_DIGICERT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_PRECA_SCT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE_SCT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_CERT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE_SCT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_SCT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_SCT_RSA
import org.certificatetransparency.ctlog.TestData.TEST_GITHUB_CHAIN
import org.certificatetransparency.ctlog.TestData.loadCertificates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.spec.InvalidKeySpecException
import java.util.ArrayList
import java.util.HashMap

import org.bouncycastle.util.encoders.Base64
import org.certificatetransparency.ctlog.proto.Ct
import org.certificatetransparency.ctlog.serialization.Deserializer
import org.certificatetransparency.ctlog.utils.VerifySignature
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import com.google.common.io.Files

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
            val logInfos = HashMap<String, LogInfo>()
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
    @Throws(IOException::class, CertificateException::class, InvalidKeySpecException::class, NoSuchAlgorithmException::class, SignatureException::class, InvalidKeyException::class)
    fun signatureVerifies() {
        val certs = loadCertificates(TEST_CERT)
        val sct = Deserializer.parseSCTFromBinary(
            ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_CERT_SCT))))
        val verifier = verifier
        assertTrue(verifier.verifySignature(sct, certs[0]))
    }

    @Test
    @Throws(IOException::class)
    fun signatureVerifiesRSA() {
        val certs = loadCertificates(TEST_CERT)
        val sct = Deserializer.parseSCTFromBinary(
            ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_CERT_SCT_RSA))))
        val verifier = verifierRSA
        assertTrue(verifier.verifySignature(sct, certs[0]))
    }

    @Test
    @Throws(IOException::class)
    fun signatureOnPreCertificateVerifies() {
        val preCertificatesList = loadCertificates(TEST_PRE_CERT)
        assertEquals(1, preCertificatesList.size.toLong())
        val preCertificate = preCertificatesList[0]

        val caList = loadCertificates(ROOT_CA_CERT)
        assertEquals(1, caList.size.toLong())
        val signerCert = caList[0]

        val sct = Deserializer.parseSCTFromBinary(
            ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_PRE_SCT))))

        val verifier = verifier
        assertTrue(
            "Expected signature to verify OK",
            verifier.verifySCTOverPreCertificate(
                sct,
                preCertificate as X509Certificate,
                LogSignatureVerifier.issuerInformationFromCertificateIssuer(signerCert)))
    }

    @Test
    @Throws(IOException::class)
    fun signatureOnPreCertificateVerifiesRSA() {
        val preCertificatesList = loadCertificates(TEST_PRE_CERT)
        assertEquals(1, preCertificatesList.size.toLong())
        val preCertificate = preCertificatesList[0]

        val caList = loadCertificates(ROOT_CA_CERT)
        assertEquals(1, caList.size.toLong())
        val signerCert = caList[0]

        val sct = Deserializer.parseSCTFromBinary(
            ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_PRE_SCT_RSA))))

        val verifier = verifierRSA
        assertTrue(
            "Expected signature to verify OK",
            verifier.verifySCTOverPreCertificate(
                sct,
                preCertificate as X509Certificate,
                LogSignatureVerifier.issuerInformationFromCertificateIssuer(signerCert)))
    }

    /** Tests for the public verifySignature method taking a chain of certificates.  */
    @Test
    @Throws(IOException::class)
    fun signatureOnRegularCertChainVerifies() {
        // Flow:
        // test-cert.pem -> ca-cert.pem
        val certs = loadCertificates(TEST_CERT)
        val sct = Deserializer.parseSCTFromBinary(
            ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_CERT_SCT))))

        assertTrue(verifier.verifySignature(sct, certs))
    }

    @Test
    @Throws(IOException::class)
    fun signatureOnCertSignedByIntermediateVerifies() {
        // Flow:
        // test-intermediate-cert.pem -> intermediate-cert.pem -> ca-cert.pem
        val certsChain = ArrayList<Certificate>()
        certsChain.addAll(loadCertificates(TEST_INTERMEDIATE_CERT))
        certsChain.addAll(loadCertificates(INTERMEDIATE_CA_CERT))
        certsChain.addAll(loadCertificates(ROOT_CA_CERT))
        val sct = Deserializer.parseSCTFromBinary(
            ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_INTERMEDIATE_CERT_SCT))))

        assertTrue(verifier.verifySignature(sct, certsChain))
    }

    @Test
    @Throws(IOException::class)
    fun signatureOnPreCertificateCertsChainVerifies() {
        // Flow:
        // test-embedded-pre-cert.pem -> ca-cert.pem
        val certsChain = ArrayList<Certificate>()
        certsChain.addAll(loadCertificates(TEST_PRE_CERT))
        certsChain.addAll(loadCertificates(ROOT_CA_CERT))

        val sct = Deserializer.parseSCTFromBinary(
            ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_PRE_SCT))))

        assertTrue(verifier.verifySignature(sct, certsChain))
    }

    @Test
    @Throws(IOException::class)
    fun signatureOnPreCertificateSignedByPreCertificateSigningCertVerifies() {
        // Flow:
        // test-embedded-with-preca-pre-cert.pem -> ca-pre-cert.pem -> ca-cert.pem
        val certsChain = ArrayList<Certificate>()
        certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_PRECA_CERT))
        certsChain.addAll(loadCertificates(PRE_CERT_SIGNING_CERT))
        certsChain.addAll(loadCertificates(ROOT_CA_CERT))

        val sct = Deserializer.parseSCTFromBinary(
            ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_PRE_CERT_PRECA_SCT))))

        assertTrue(
            "Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
    }

    @Test
    @Throws(IOException::class)
    fun signatureOnPreCertificateSignedByIntermediateVerifies() {
        // Flow:
        // test-embedded-with-intermediate-cert.pem -> intermediate-cert.pem -> ca-cert.pem
        val certsChain = ArrayList<Certificate>()
        certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE))
        certsChain.addAll(loadCertificates(INTERMEDIATE_CA_CERT))
        certsChain.addAll(loadCertificates(ROOT_CA_CERT))

        val sct = Deserializer.parseSCTFromBinary(
            ByteArrayInputStream(
                Files.toByteArray(TestData.file(TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE_SCT))))

        assertTrue(
            "Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
    }

    @Test
    @Throws(IOException::class)
    fun signatureOnPreCertificateSignedByPreCertSigningCertSignedByIntermediateVerifies() {
        // Flow:
        // test-embedded-with-intermediate-preca-pre-cert.pem -> intermediate-pre-cert.pem
        //   -> intermediate-cert.pem -> ca-cert.pem
        val certsChain = ArrayList<Certificate>()
        certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE))
        certsChain.addAll(loadCertificates(PRE_CERT_SIGNING_BY_INTERMEDIATE))
        certsChain.addAll(loadCertificates(INTERMEDIATE_CA_CERT))
        certsChain.addAll(loadCertificates(ROOT_CA_CERT))

        val sct = Deserializer.parseSCTFromBinary(
            ByteArrayInputStream(
                Files.toByteArray(TestData.file(TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE_SCT))))

        assertTrue(
            "Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
    }

    @Test
    @Throws(IOException::class)
    fun throwsWhenChainWithPreCertificateSignedByPreCertificateSigningCertMissingIssuer() {
        val certsChain = ArrayList<Certificate>()
        certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_PRECA_CERT))
        certsChain.addAll(loadCertificates(PRE_CERT_SIGNING_CERT))

        val sct = Deserializer.parseSCTFromBinary(
            ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_PRE_CERT_PRECA_SCT))))

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
    @Throws(IOException::class, CertificateEncodingException::class)
    fun signatureOnEmbeddedSCTsInFinalCertificateVerifies() {
        // Flow:
        // github-chain.txt contains leaf certificate signed by issuing CA.
        // Leafcert contains three embedded SCTs, we verify them all
        val certsChain = ArrayList<Certificate>()
        certsChain.addAll(loadCertificates(TEST_GITHUB_CHAIN))

        // the leaf cert is the first one in this test data
        val leafcert = certsChain[0] as X509Certificate
        val issuerCert = certsChain[1]
        assertTrue(
            "The test certificate does have embedded SCTs", leafcert.hasEmbeddedSCT())
        val scts = VerifySignature.parseSCTsFromCert(leafcert)
        assertEquals("Expected 3 SCTs in the test certificate", 3, scts.size.toLong())
        val logInfos = logInfosGitHub
        for (sct in scts) {
            val id = Base64.toBase64String(sct.id.keyId.toByteArray())
            val logInfo = logInfos[id]
            println(id)
            val verifier = LogSignatureVerifier(logInfo!!)

            assertTrue(
                "Expected signature to verify OK",
                verifier.verifySCTOverPreCertificate(
                    sct,
                    leafcert,
                    LogSignatureVerifier.issuerInformationFromCertificateIssuer(issuerCert)))
            assertTrue("Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
        }
    }
}
