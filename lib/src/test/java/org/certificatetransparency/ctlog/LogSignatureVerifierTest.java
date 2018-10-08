package org.certificatetransparency.ctlog;

import static org.certificatetransparency.ctlog.TestData.INTERMEDIATE_CA_CERT;
import static org.certificatetransparency.ctlog.TestData.PRE_CERT_SIGNING_BY_INTERMEDIATE;
import static org.certificatetransparency.ctlog.TestData.PRE_CERT_SIGNING_CERT;
import static org.certificatetransparency.ctlog.TestData.ROOT_CA_CERT;
import static org.certificatetransparency.ctlog.TestData.TEST_CERT;
import static org.certificatetransparency.ctlog.TestData.TEST_CERT_SCT;
import static org.certificatetransparency.ctlog.TestData.TEST_CERT_SCT_RSA;
import static org.certificatetransparency.ctlog.TestData.TEST_INTERMEDIATE_CERT;
import static org.certificatetransparency.ctlog.TestData.TEST_INTERMEDIATE_CERT_SCT;
import static org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY;
import static org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_RSA;
import static org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_PILOT;
import static org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_SKYDIVER;
import static org.certificatetransparency.ctlog.TestData.TEST_LOG_KEY_DIGICERT;
import static org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT;
import static org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_PRECA_SCT;
import static org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE;
import static org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE_SCT;
import static org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_CERT;
import static org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE;
import static org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE_SCT;
import static org.certificatetransparency.ctlog.TestData.TEST_PRE_SCT;
import static org.certificatetransparency.ctlog.TestData.TEST_PRE_SCT_RSA;
import static org.certificatetransparency.ctlog.TestData.TEST_GITHUB_CHAIN;
import static org.certificatetransparency.ctlog.TestData.loadCertificates;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.util.encoders.Base64;
import org.certificatetransparency.ctlog.proto.Ct;
import org.certificatetransparency.ctlog.serialization.Deserializer;
import org.certificatetransparency.ctlog.utils.VerifySignature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.io.Files;

/**
 * This test verifies that the data is correctly serialized for signature comparison, so signature
 * verification is actually effective.
 */
@RunWith(JUnit4.class)
public class LogSignatureVerifierTest {
  /** Returns a LogSignatureVerifier for the test log with an EC key */
  private LogSignatureVerifier getVerifier() {
    LogInfo logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY));
    return new LogSignatureVerifier(logInfo);
  }

  /** Returns a LogSignatureVerifier for the test log with an RSA key */
  private LogSignatureVerifier getVerifierRSA() {
    LogInfo logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_RSA));
    return new LogSignatureVerifier(logInfo);
  }

  /** Returns a Map of LogInfos with all log keys to verify the Github certificate */
  private Map<String, LogInfo> getLogInfosGitHub() {
    Map<String, LogInfo> logInfos = new HashMap<>();
    LogInfo logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_PILOT));
    String id = Base64.toBase64String(logInfo.getID());
    logInfos.put(id, logInfo);
    logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_SKYDIVER));
    id = Base64.toBase64String(logInfo.getID());
    logInfos.put(id, logInfo);
    logInfo = LogInfo.fromKeyFile(TestData.fileName(TEST_LOG_KEY_DIGICERT));
    id = Base64.toBase64String(logInfo.getID());
    logInfos.put(id, logInfo);
    return logInfos;
  }

  /** Tests for package-visible methods. */
  @Test
  public void signatureVerifies()
      throws IOException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException,
          SignatureException, InvalidKeyException {
    List<Certificate> certs = loadCertificates(TEST_CERT);
    Ct.SignedCertificateTimestamp sct =
        Deserializer.parseSCTFromBinary(
            new ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_CERT_SCT))));
    LogSignatureVerifier verifier = getVerifier();
    assertTrue(verifier.verifySignature(sct, certs.get(0)));
  }

  @Test
  public void signatureVerifiesRSA() throws IOException {
    List<Certificate> certs = loadCertificates(TEST_CERT);
    Ct.SignedCertificateTimestamp sct =
        Deserializer.parseSCTFromBinary(
            new ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_CERT_SCT_RSA))));
    LogSignatureVerifier verifier = getVerifierRSA();
    assertTrue(verifier.verifySignature(sct, certs.get(0)));
  }

  @Test
  public void signatureOnPreCertificateVerifies() throws IOException {
    List<Certificate> preCertificatesList = loadCertificates(TEST_PRE_CERT);
    assertEquals(1, preCertificatesList.size());
    Certificate preCertificate = preCertificatesList.get(0);

    List<Certificate> caList = loadCertificates(ROOT_CA_CERT);
    assertEquals(1, caList.size());
    Certificate signerCert = caList.get(0);

    Ct.SignedCertificateTimestamp sct =
        Deserializer.parseSCTFromBinary(
            new ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_PRE_SCT))));

    LogSignatureVerifier verifier = getVerifier();
    assertTrue(
        "Expected signature to verify OK",
        verifier.verifySCTOverPreCertificate(
            sct,
            (X509Certificate) preCertificate,
            LogSignatureVerifier.issuerInformationFromCertificateIssuer(signerCert)));
  }

  @Test
  public void signatureOnPreCertificateVerifiesRSA() throws IOException {
    List<Certificate> preCertificatesList = loadCertificates(TEST_PRE_CERT);
    assertEquals(1, preCertificatesList.size());
    Certificate preCertificate = preCertificatesList.get(0);

    List<Certificate> caList = loadCertificates(ROOT_CA_CERT);
    assertEquals(1, caList.size());
    Certificate signerCert = caList.get(0);

    Ct.SignedCertificateTimestamp sct =
        Deserializer.parseSCTFromBinary(
            new ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_PRE_SCT_RSA))));

    LogSignatureVerifier verifier = getVerifierRSA();
    assertTrue(
        "Expected signature to verify OK",
        verifier.verifySCTOverPreCertificate(
            sct,
            (X509Certificate) preCertificate,
            LogSignatureVerifier.issuerInformationFromCertificateIssuer(signerCert)));
  }

  /** Tests for the public verifySignature method taking a chain of certificates. */
  @Test
  public void signatureOnRegularCertChainVerifies() throws IOException {
    // Flow:
    // test-cert.pem -> ca-cert.pem
    List<Certificate> certs = loadCertificates(TEST_CERT);
    Ct.SignedCertificateTimestamp sct =
        Deserializer.parseSCTFromBinary(
            new ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_CERT_SCT))));

    assertTrue(getVerifier().verifySignature(sct, certs));
  }

  @Test
  public void signatureOnCertSignedByIntermediateVerifies() throws IOException {
    // Flow:
    // test-intermediate-cert.pem -> intermediate-cert.pem -> ca-cert.pem
    List<Certificate> certsChain = new ArrayList<>();
    certsChain.addAll(loadCertificates(TEST_INTERMEDIATE_CERT));
    certsChain.addAll(loadCertificates(INTERMEDIATE_CA_CERT));
    certsChain.addAll(loadCertificates(ROOT_CA_CERT));
    Ct.SignedCertificateTimestamp sct =
        Deserializer.parseSCTFromBinary(
            new ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_INTERMEDIATE_CERT_SCT))));

    assertTrue(getVerifier().verifySignature(sct, certsChain));
  }

  @Test
  public void signatureOnPreCertificateCertsChainVerifies() throws IOException {
    // Flow:
    // test-embedded-pre-cert.pem -> ca-cert.pem
    List<Certificate> certsChain = new ArrayList<>();
    certsChain.addAll(loadCertificates(TEST_PRE_CERT));
    certsChain.addAll(loadCertificates(ROOT_CA_CERT));

    Ct.SignedCertificateTimestamp sct =
        Deserializer.parseSCTFromBinary(
            new ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_PRE_SCT))));

    assertTrue(getVerifier().verifySignature(sct, certsChain));
  }

  @Test
  public void signatureOnPreCertificateSignedByPreCertificateSigningCertVerifies()
      throws IOException {
    // Flow:
    // test-embedded-with-preca-pre-cert.pem -> ca-pre-cert.pem -> ca-cert.pem
    List<Certificate> certsChain = new ArrayList<>();
    certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_PRECA_CERT));
    certsChain.addAll(loadCertificates(PRE_CERT_SIGNING_CERT));
    certsChain.addAll(loadCertificates(ROOT_CA_CERT));

    Ct.SignedCertificateTimestamp sct =
        Deserializer.parseSCTFromBinary(
            new ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_PRE_CERT_PRECA_SCT))));

    assertTrue(
        "Expected PreCertificate to verify OK", getVerifier().verifySignature(sct, certsChain));
  }

  @Test
  public void signatureOnPreCertificateSignedByIntermediateVerifies() throws IOException {
    // Flow:
    // test-embedded-with-intermediate-cert.pem -> intermediate-cert.pem -> ca-cert.pem
    List<Certificate> certsChain = new ArrayList<>();
    certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE));
    certsChain.addAll(loadCertificates(INTERMEDIATE_CA_CERT));
    certsChain.addAll(loadCertificates(ROOT_CA_CERT));

    Ct.SignedCertificateTimestamp sct =
        Deserializer.parseSCTFromBinary(
            new ByteArrayInputStream(
                Files.toByteArray(TestData.file(TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE_SCT))));

    assertTrue(
        "Expected PreCertificate to verify OK", getVerifier().verifySignature(sct, certsChain));
  }

  @Test
  public void signatureOnPreCertificateSignedByPreCertSigningCertSignedByIntermediateVerifies()
      throws IOException {
    // Flow:
    // test-embedded-with-intermediate-preca-pre-cert.pem -> intermediate-pre-cert.pem
    //   -> intermediate-cert.pem -> ca-cert.pem
    List<Certificate> certsChain = new ArrayList<>();
    certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE));
    certsChain.addAll(loadCertificates(PRE_CERT_SIGNING_BY_INTERMEDIATE));
    certsChain.addAll(loadCertificates(INTERMEDIATE_CA_CERT));
    certsChain.addAll(loadCertificates(ROOT_CA_CERT));

    Ct.SignedCertificateTimestamp sct =
        Deserializer.parseSCTFromBinary(
            new ByteArrayInputStream(
                Files.toByteArray(TestData.file(TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE_SCT))));

    assertTrue(
        "Expected PreCertificate to verify OK", getVerifier().verifySignature(sct, certsChain));
  }

  @Test
  public void throwsWhenChainWithPreCertificateSignedByPreCertificateSigningCertMissingIssuer()
      throws IOException {
    List<Certificate> certsChain = new ArrayList<>();
    certsChain.addAll(loadCertificates(TEST_PRE_CERT_SIGNED_BY_PRECA_CERT));
    certsChain.addAll(loadCertificates(PRE_CERT_SIGNING_CERT));

    Ct.SignedCertificateTimestamp sct =
        Deserializer.parseSCTFromBinary(
            new ByteArrayInputStream(Files.toByteArray(TestData.file(TEST_PRE_CERT_PRECA_SCT))));

    try {
      getVerifier().verifySignature(sct, certsChain);
      fail("Expected verifySignature to throw since the issuer certificate is missing.");
    } catch (IllegalArgumentException expected) {
      assertNotNull("Exception should have message, but was: " + expected, expected.getMessage());
      assertTrue(
          "Expected exception to warn about missing issuer cert",
          expected.getMessage().contains("must contain issuer"));
    }
  }

  @Test
  public void signatureOnEmbeddedSCTsInFinalCertificateVerifies()
      throws IOException, CertificateEncodingException {
    // Flow:
    // github-chain.txt contains leaf certificate signed by issuing CA.
    // Leafcert contains three embedded SCTs, we verify them all
    List<Certificate> certsChain = new ArrayList<>();
    certsChain.addAll(loadCertificates(TEST_GITHUB_CHAIN));

    // the leaf cert is the first one in this test data
    X509Certificate leafcert = (X509Certificate) certsChain.get(0);
    Certificate issuerCert = certsChain.get(1);
    assertTrue(
        "The test certificate does have embedded SCTs", CertificateInfo.hasEmbeddedSCT(leafcert));
    List<Ct.SignedCertificateTimestamp> scts = VerifySignature.parseSCTsFromCert(leafcert);
    assertEquals("Expected 3 SCTs in the test certificate", 3, scts.size());
    Map<String, LogInfo> logInfos = getLogInfosGitHub();
    for (Ct.SignedCertificateTimestamp sct : scts) {
      String id = Base64.toBase64String(sct.getId().getKeyId().toByteArray());
      LogInfo logInfo = logInfos.get(id);
      System.out.println(id);
      LogSignatureVerifier verifier = new LogSignatureVerifier(logInfo);

      assertTrue(
          "Expected signature to verify OK",
          verifier.verifySCTOverPreCertificate(
              sct,
              leafcert,
              LogSignatureVerifier.issuerInformationFromCertificateIssuer(issuerCert)));
      assertTrue("Expected PreCertificate to verify OK", verifier.verifySignature(sct, certsChain));
    }
  }
}
