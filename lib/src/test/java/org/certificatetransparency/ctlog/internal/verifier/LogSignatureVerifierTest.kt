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

import org.certificatetransparency.ctlog.internal.serialization.Deserializer
import org.certificatetransparency.ctlog.internal.utils.Base64
import org.certificatetransparency.ctlog.internal.utils.PublicKeyFactory
import org.certificatetransparency.ctlog.internal.utils.hasEmbeddedSct
import org.certificatetransparency.ctlog.internal.utils.issuerInformation
import org.certificatetransparency.ctlog.internal.utils.signedCertificateTimestamps
import org.certificatetransparency.ctlog.internal.verifier.model.LogInfo
import org.certificatetransparency.ctlog.utils.TestData
import org.certificatetransparency.ctlog.utils.TestData.INTERMEDIATE_CA_CERT
import org.certificatetransparency.ctlog.utils.TestData.PRE_CERT_SIGNING_BY_INTERMEDIATE
import org.certificatetransparency.ctlog.utils.TestData.PRE_CERT_SIGNING_CERT
import org.certificatetransparency.ctlog.utils.TestData.ROOT_CA_CERT
import org.certificatetransparency.ctlog.utils.TestData.TEST_CERT
import org.certificatetransparency.ctlog.utils.TestData.TEST_CERT_SCT
import org.certificatetransparency.ctlog.utils.TestData.TEST_CERT_SCT_RSA
import org.certificatetransparency.ctlog.utils.TestData.TEST_GITHUB_CHAIN
import org.certificatetransparency.ctlog.utils.TestData.TEST_INTERMEDIATE_CERT
import org.certificatetransparency.ctlog.utils.TestData.TEST_INTERMEDIATE_CERT_SCT
import org.certificatetransparency.ctlog.utils.TestData.TEST_LOG_KEY
import org.certificatetransparency.ctlog.utils.TestData.TEST_LOG_KEY_DIGICERT
import org.certificatetransparency.ctlog.utils.TestData.TEST_LOG_KEY_PILOT
import org.certificatetransparency.ctlog.utils.TestData.TEST_LOG_KEY_RSA
import org.certificatetransparency.ctlog.utils.TestData.TEST_LOG_KEY_SKYDIVER
import org.certificatetransparency.ctlog.utils.TestData.TEST_PRE_CERT
import org.certificatetransparency.ctlog.utils.TestData.TEST_PRE_CERT_PRECA_SCT
import org.certificatetransparency.ctlog.utils.TestData.TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE
import org.certificatetransparency.ctlog.utils.TestData.TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE_SCT
import org.certificatetransparency.ctlog.utils.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_CERT
import org.certificatetransparency.ctlog.utils.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE
import org.certificatetransparency.ctlog.utils.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE_SCT
import org.certificatetransparency.ctlog.utils.TestData.TEST_PRE_SCT
import org.certificatetransparency.ctlog.utils.TestData.TEST_PRE_SCT_RSA
import org.certificatetransparency.ctlog.utils.TestData.loadCertificates
import org.certificatetransparency.ctlog.utils.assertIsA
import org.certificatetransparency.ctlog.verifier.SctResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.io.File
import java.security.cert.X509Certificate

/**
 * This test verifies that the data is correctly serialized for signature comparison, so signature
 * verification is actually effective.
 */
class LogSignatureVerifierTest {

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    /** Returns a LogSignatureVerifier for the test log with an EC key  */
    private val verifier by lazy {
        val logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY))
        LogSignatureVerifier(logInfo)
    }

    /** Returns a LogSignatureVerifier for the test log with an RSA key  */
    private val verifierRSA by lazy {
        val logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_RSA))
        LogSignatureVerifier(logInfo)
    }

    /** Returns a Map of LogInfos with all log keys to verify the Github certificate  */
    private val logInfosGitHub by lazy {
        listOf(
            LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_PILOT)),
            LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_SKYDIVER)),
            LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_DIGICERT))
        ).associateBy { Base64.toBase64String(it.id) }
    }

    @Test
    fun signatureVerifies() {
        val certs = loadCertificates(TEST_CERT)
        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_CERT_SCT).inputStream())
        assertIsA<SctResult.Valid>(verifier.verifySignature(sct, certs))
    }

    @Test
    fun signatureVerifiesRSA() {
        val certs = loadCertificates(TEST_CERT)
        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_CERT_SCT_RSA).inputStream())
        assertIsA<SctResult.Valid>(verifierRSA.verifySignature(sct, certs))
    }

    @Test
    fun signatureOnPreCertificateVerifies() {
        val preCertificatesList = loadCertificates(TEST_PRE_CERT)
        assertEquals(1, preCertificatesList.size.toLong())
        val preCertificate = preCertificatesList[0]

        val caList = loadCertificates(ROOT_CA_CERT)
        assertEquals(1, caList.size.toLong())
        val signerCert = caList[0]

        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_PRE_SCT).inputStream())

        val verifier = verifier
        assertIsA<SctResult.Valid>(
            "Expected signature to verify OK",
            verifier.verifySCTOverPreCertificate(sct, preCertificate as X509Certificate, signerCert.issuerInformation())
        )
    }

    @Test
    fun signatureOnPreCertificateVerifiesRSA() {
        val preCertificatesList = loadCertificates(TEST_PRE_CERT)
        assertEquals(1, preCertificatesList.size.toLong())
        val preCertificate = preCertificatesList[0]

        val caList = loadCertificates(ROOT_CA_CERT)
        assertEquals(1, caList.size.toLong())
        val signerCert = caList[0]

        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_PRE_SCT_RSA).inputStream())

        val verifier = verifierRSA
        assertIsA<SctResult.Valid>(
            "Expected signature to verify OK",
            verifier.verifySCTOverPreCertificate(sct, preCertificate as X509Certificate, signerCert.issuerInformation())
        )
    }

    /** Tests for the public verifySignature method taking a chain of certificates.  */
    @Test
    fun signatureOnRegularCertChainVerifies() {
        // Flow:
        // test-cert.pem -> ca-cert.pem
        val certs = loadCertificates(TEST_CERT)
        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_CERT_SCT).inputStream())

        assertIsA<SctResult.Valid>(verifier.verifySignature(sct, certs))
    }

    @Test
    fun signatureOnCertSignedByIntermediateVerifies() {
        // Flow:
        // test-intermediate-cert.pem -> intermediate-cert.pem -> ca-cert.pem
        val certsChain = listOf(TEST_INTERMEDIATE_CERT, INTERMEDIATE_CA_CERT, ROOT_CA_CERT).flatMap(::loadCertificates)

        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_INTERMEDIATE_CERT_SCT).inputStream())

        assertIsA<SctResult.Valid>(verifier.verifySignature(sct, certsChain))
    }

    @Test
    fun signatureOnPreCertificateCertsChainVerifies() {
        // Flow:
        // test-embedded-pre-cert.pem -> ca-cert.pem
        val certsChain = listOf(TEST_PRE_CERT, ROOT_CA_CERT).flatMap(::loadCertificates)

        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_PRE_SCT).inputStream())

        assertIsA<SctResult.Valid>(verifier.verifySignature(sct, certsChain))
    }

    @Test
    fun signatureOnPreCertificateSignedByPreCertificateSigningCertVerifies() {
        // Flow:
        // test-embedded-with-preca-pre-cert.pem -> ca-pre-cert.pem -> ca-cert.pem
        val certsChain = listOf(TEST_PRE_CERT_SIGNED_BY_PRECA_CERT, PRE_CERT_SIGNING_CERT, ROOT_CA_CERT).flatMap(::loadCertificates)

        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_PRE_CERT_PRECA_SCT).inputStream())

        assertIsA<SctResult.Valid>("Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
    }

    @Test
    fun signatureOnPreCertificateSignedByIntermediateVerifies() {
        // Flow:
        // test-embedded-with-intermediate-pre-cert.pem -> intermediate-cert.pem -> ca-cert.pem
        val certsChain = listOf(TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE, INTERMEDIATE_CA_CERT, ROOT_CA_CERT).flatMap(::loadCertificates)

        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE_SCT).inputStream())

        assertIsA<SctResult.Valid>("Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
    }

    @Test
    fun signatureOnPreCertificateSignedByPreCertSigningCertSignedByIntermediateVerifies() {
        // Flow:
        // test-embedded-with-intermediate-preca-pre-cert.pem -> intermediate-pre-cert.pem
        //   -> intermediate-cert.pem -> ca-cert.pem
        val certsChain = listOf(TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE, PRE_CERT_SIGNING_BY_INTERMEDIATE, INTERMEDIATE_CA_CERT, ROOT_CA_CERT)
            .flatMap(::loadCertificates)

        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE_SCT).inputStream())

        assertIsA<SctResult.Valid>("Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
    }

    @Test
    fun returnNoIssuerWithPreCertWhenChainWithPreCertificateSignedByPreCertificateSigningCertMissingIssuer() {
        val certsChain = listOf(TEST_PRE_CERT_SIGNED_BY_PRECA_CERT, PRE_CERT_SIGNING_CERT).flatMap(::loadCertificates)

        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_PRE_CERT_PRECA_SCT).inputStream())

        assertIsA<NoIssuerWithPreCert>(verifier.verifySignature(sct, certsChain))
    }

    @Test
    fun signatureOnEmbeddedSCTsInFinalCertificateVerifies() {
        // Flow:
        // github-chain.txt contains leaf certificate signed by issuing CA.
        // Leafcert contains three embedded SCTs, we verify them all
        val certsChain = loadCertificates(TEST_GITHUB_CHAIN)

        // the leaf cert is the first one in this test data
        val leafcert = certsChain[0] as X509Certificate
        val issuerCert = certsChain[1]
        assertTrue("The test certificate does have embedded SCTs", leafcert.hasEmbeddedSct())

        val scts = leafcert.signedCertificateTimestamps()
        assertEquals("Expected 3 SCTs in the test certificate", 3, scts.size.toLong())

        for (sct in scts) {
            val id = Base64.toBase64String(sct.id.keyId)
            val logInfo = logInfosGitHub[id]
            val verifier = LogSignatureVerifier(logInfo!!)

            assertIsA<SctResult.Valid>(
                "Expected signature to verify OK",
                verifier.verifySCTOverPreCertificate(sct, leafcert, issuerCert.issuerInformation())
            )
            assertIsA<SctResult.Valid>("Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain))
        }
    }

    @Test
    fun returnInvalidFutureTimestampWhenSctTimestampInFuture() {
        // given we have an SCT with a future timestamp
        val certs = loadCertificates(TEST_CERT)
        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_CERT_SCT).inputStream())
        val futureSct = sct.copy(timestamp = System.currentTimeMillis() + 10000)

        // when the signature is verified
        assertIsA<SctResult.Invalid.FutureTimestamp>(verifier.verifySignature(futureSct, certs))
    }

    @Test
    fun signatureInvalidWhenLogServerNoLongerTrusted() {
        // given we have an SCT
        val certs = loadCertificates(TEST_CERT)
        val sct = Deserializer.parseSctFromBinary(TestData.file(TEST_CERT_SCT).inputStream())

        // when we have a log server which is no longer valid
        val logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY))
        val verifier = LogSignatureVerifier(logInfo.copy(validUntil = sct.timestamp - 10000))

        // then the signature is rejected
        assertIsA<SctResult.Invalid.LogServerUntrusted>(verifier.verifySignature(sct, certs))
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
