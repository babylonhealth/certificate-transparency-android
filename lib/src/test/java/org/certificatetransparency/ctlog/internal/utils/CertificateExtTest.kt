package org.certificatetransparency.ctlog.internal.utils

import org.certificatetransparency.ctlog.utils.TestData.PRE_CERT_SIGNING_CERT
import org.certificatetransparency.ctlog.utils.TestData.ROOT_CA_CERT
import org.certificatetransparency.ctlog.utils.TestData.TEST_CERT
import org.certificatetransparency.ctlog.utils.TestData.TEST_PRE_CERT
import org.certificatetransparency.ctlog.utils.TestData.loadCertificates
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Make sure the correct info about certificates is provided.  */
class CertificateExtTest {
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
