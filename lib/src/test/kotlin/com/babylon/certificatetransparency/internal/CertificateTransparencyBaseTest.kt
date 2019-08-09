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

package com.babylon.certificatetransparency.internal

import com.babylon.certificatetransparency.SctVerificationResult
import com.babylon.certificatetransparency.VerificationResult
import com.babylon.certificatetransparency.internal.serialization.CTConstants
import com.babylon.certificatetransparency.internal.utils.Base64
import com.babylon.certificatetransparency.internal.verifier.CertificateTransparencyBase
import com.babylon.certificatetransparency.internal.verifier.model.Host
import com.babylon.certificatetransparency.utils.LogListDataSourceTestFactory
import com.babylon.certificatetransparency.utils.TestData
import com.babylon.certificatetransparency.utils.TestData.TEST_MITMPROXY_ATTACK_CHAIN
import com.babylon.certificatetransparency.utils.TestData.TEST_MITMPROXY_ORIGINAL_CHAIN
import com.babylon.certificatetransparency.utils.TestData.TEST_MITMPROXY_ROOT_CERT
import com.babylon.certificatetransparency.utils.TrustedSocketFactory
import com.babylon.certificatetransparency.utils.assertIsA
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.cert.X509Certificate
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.X509TrustManager

class CertificateTransparencyBaseTest {

    @Test
    fun mitmDisallowedWhenHostChecked() {
        val trustManager = mitmProxyTrustManager()

        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            trustManager = trustManager,
            logListDataSource = LogListDataSourceTestFactory.logListDataSource
        )

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ATTACK_CHAIN)

        assertIsA<VerificationResult.Failure.NoScts>(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test
    fun mitmAttackAllowedWhenHostNotChecked() {
        val trustManager = mitmProxyTrustManager()

        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.random.com")),
            trustManager = trustManager,
            logListDataSource = LogListDataSourceTestFactory.logListDataSource
        )

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ATTACK_CHAIN)

        assertIsA<VerificationResult.Success.DisabledForHost>(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test
    fun originalChainAllowedWhenHostNotChecked() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.random.com")),
            logListDataSource = LogListDataSourceTestFactory.logListDataSource
        )

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ORIGINAL_CHAIN)

        assertIsA<VerificationResult.Success.DisabledForHost>(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test
    fun originalChainAllowedWhenHostChecked() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.logListDataSource
        )

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ORIGINAL_CHAIN)

        val result = ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck)

        require(result is VerificationResult.Success.Trusted)
        assertEquals(2, result.scts.count { it.value is SctVerificationResult.Valid })
    }

    @Test(expected = SSLPeerUnverifiedException::class)
    fun untrustedCertificateThrowsException() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.logListDataSource
        )

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ATTACK_CHAIN)

        ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck)
    }

    @Test(expected = IllegalArgumentException::class)
    fun noHostsDefinedThrowsException() {
        CertificateTransparencyBase(hosts = emptySet())
    }

    @Test
    fun originalChainDisallowedWhenEmptyLogs() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.emptySource
        )

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ORIGINAL_CHAIN)

        assertIsA<VerificationResult.Failure.TooFewSctsTrusted>(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test
    fun originalChainDisallowedWhenNullLogs() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.nullSource
        )

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ORIGINAL_CHAIN)

        assertIsA<VerificationResult.Failure.LogServersFailed>(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test
    fun originalChainDisallowedWhenOnlyOneSct() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.logListDataSource
        )

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ORIGINAL_CHAIN)

        val certWithSingleSct = singleSctOnly(certsToCheck.first())

        val filtered = listOf(certWithSingleSct, *certsToCheck.drop(1).toTypedArray())

        assertIsA<VerificationResult.Failure.TooFewSctsTrusted>(ctb.verifyCertificateTransparency("www.babylonhealth.com", filtered))
    }

    @Test
    fun noCertificatesDisallowed() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.nullSource
        )

        assertIsA<VerificationResult.Failure.NoCertificates>(ctb.verifyCertificateTransparency("www.babylonhealth.com", emptyList()))
    }

    private fun mitmProxyTrustManager(): X509TrustManager {
        val rootCerts = TestData.loadCertificates(TEST_MITMPROXY_ROOT_CERT)
        return TrustedSocketFactory().create(rootCerts).trustManager
    }

    private fun singleSctOnly(cert: X509Certificate) = spy(cert).apply {
        whenever(getExtensionValue(CTConstants.SCT_CERTIFICATE_OID)).thenAnswer {
            @Suppress("MaxLineLength")
            Base64.decode("BHwEegB4AHYAu9nfvB+KcbWTlCOXqpJ7RzhXlQqrUugakJZkNo4e0YUAAAFj7ztQ3wAABAMARzBFAiEA53gntK6Dnr6ROwYGBjqjt5dS4tWM6Zw/TtxIxOvobW8CIF3n4XjIX7/w66gThQD47iF7YmxelwgUQgPzEWNlHQiu")
        }
    }
}
