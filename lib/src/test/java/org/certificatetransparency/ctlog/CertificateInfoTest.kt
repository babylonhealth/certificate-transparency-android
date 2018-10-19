package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.TestData.PRE_CERT_SIGNING_CERT
import org.certificatetransparency.ctlog.TestData.ROOT_CA_CERT
import org.certificatetransparency.ctlog.TestData.TEST_CERT
import org.certificatetransparency.ctlog.TestData.TEST_PRE_CERT
import org.certificatetransparency.ctlog.TestData.loadCertificates
import org.certificatetransparency.ctlog.internal.utils.isPreCertificate
import org.certificatetransparency.ctlog.internal.utils.isPreCertificateSigningCert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Make sure the correct info about certificates is provided.  */
@RunWith(JUnit4::class)
class CertificateInfoTest {
    @Test
    fun correctlyIdentifiesPreCertificateSigningCert() {
        val preCertificateSigningCert = loadCertificates(PRE_CERT_SIGNING_CERT)[0]
        val ordinaryCaCert = loadCertificates(ROOT_CA_CERT)[0]

        assertTrue(preCertificateSigningCert.isPreCertificateSigningCert())
        assertFalse(ordinaryCaCert.isPreCertificateSigningCert())
    }

    @Test
    fun correctlyIdentifiesPreCertificates() {
        val regularCert = loadCertificates(TEST_CERT)[0]
        val preCertificate = loadCertificates(TEST_PRE_CERT)[0]

        assertTrue(preCertificate.isPreCertificate())
        assertFalse(regularCert.isPreCertificate())
    }
}
