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
