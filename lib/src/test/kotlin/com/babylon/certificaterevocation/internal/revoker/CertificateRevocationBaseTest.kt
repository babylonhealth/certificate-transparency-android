/*
 * Copyright 2019 Babylon Partners Limited
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
 */

package com.babylon.certificaterevocation.internal.revoker

import com.babylon.certificaterevocation.RevocationResult
import com.babylon.certificatetransparency.utils.TestData
import com.babylon.certificatetransparency.utils.assertIsA
import org.junit.Test

class CertificateRevocationBaseTest {

    @Test
    fun chainAllowedWhenNoRevocationsInPlace() {
        val ctb = CertificateRevocationBase()

        val certsToCheck = TestData.loadCertificates(TestData.TEST_GITHUB_CHAIN)

        assertIsA<RevocationResult.Success.Trusted>(ctb.verifyCertificateRevocation("www.github.com", certsToCheck))
    }

    @Test
    fun chainAllowedWhenRevocationDoesNotMatch() {
        val certsToCheck = TestData.loadCertificates(TestData.TEST_GITHUB_CHAIN)

        val randomCert = TestData.loadCertificates(TestData.ROOT_CA_CERT)[0]
        val ctb = CertificateRevocationBase(
            crlSet = setOf(CrlItem(randomCert.issuerX500Principal, listOf(randomCert.serialNumber)))
        )

        assertIsA<RevocationResult.Success.Trusted>(ctb.verifyCertificateRevocation("www.github.com", certsToCheck))
    }

    @Test
    fun chainRejectedWhenFirstCertMatches() {
        val certsToCheck = TestData.loadCertificates(TestData.TEST_GITHUB_CHAIN)

        val ctb = CertificateRevocationBase(
            crlSet = setOf(CrlItem(certsToCheck[0].issuerX500Principal, listOf(certsToCheck[0].serialNumber)))
        )

        assertIsA<RevocationResult.Failure.CertificateRevoked>(ctb.verifyCertificateRevocation("www.github.com", certsToCheck))
    }

    @Test
    fun chainRejectedWhenLastCertMatches() {
        val certsToCheck = TestData.loadCertificates(TestData.TEST_GITHUB_CHAIN)

        val ctb = CertificateRevocationBase(
            crlSet = setOf(CrlItem(certsToCheck[1].issuerX500Principal, listOf(certsToCheck[1].serialNumber)))
        )

        assertIsA<RevocationResult.Failure.CertificateRevoked>(ctb.verifyCertificateRevocation("www.github.com", certsToCheck))
    }
}
