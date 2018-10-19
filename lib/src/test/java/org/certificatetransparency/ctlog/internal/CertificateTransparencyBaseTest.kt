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
 */

package org.certificatetransparency.ctlog.internal

import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import org.certificatetransparency.ctlog.internal.utils.Base64
import org.certificatetransparency.ctlog.internal.verifier.model.Host
import org.certificatetransparency.ctlog.utils.TestData
import org.certificatetransparency.ctlog.utils.TestData.TEST_MITMPROXY_ATTACK_CHAIN
import org.certificatetransparency.ctlog.utils.TestData.TEST_MITMPROXY_ORIGINAL_CHAIN
import org.certificatetransparency.ctlog.utils.TestData.TEST_MITMPROXY_ROOT_CERT
import org.certificatetransparency.ctlog.internal.verifier.CertificateTransparencyBase
import org.certificatetransparency.ctlog.internal.serialization.CTConstants
import org.certificatetransparency.ctlog.utils.LogListDataSourceTestFactory
import org.certificatetransparency.ctlog.utils.TrustedSocketFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
            logListDataSource = LogListDataSourceTestFactory.logListDataSource)

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ATTACK_CHAIN)

        assertFalse(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test
    fun mitmAttackAllowedWhenHostNotChecked() {
        val trustManager = mitmProxyTrustManager()

        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.random.com")),
            trustManager = trustManager,
            logListDataSource = LogListDataSourceTestFactory.logListDataSource)

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ATTACK_CHAIN)

        assertTrue(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test
    fun originalChainAllowedWhenHostNotChecked() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.random.com")),
            logListDataSource = LogListDataSourceTestFactory.logListDataSource)

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ORIGINAL_CHAIN)

        assertTrue(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test
    fun originalChainAllowedWhenHostChecked() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.logListDataSource)

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ORIGINAL_CHAIN)

        assertTrue(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test(expected = SSLPeerUnverifiedException::class)
    fun untrustedCertificateThrowsException() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.logListDataSource)

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ATTACK_CHAIN)

        println(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test(expected = IllegalArgumentException::class)
    fun noHostsDefinedThrowsException() {
        CertificateTransparencyBase(hosts = emptySet())
    }

    @Test
    fun originalChainDisallowedWhenEmptyLogs() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.emptySource)

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ORIGINAL_CHAIN)

        assertFalse(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test
    fun originalChainDisallowedWhenNullLogs() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.nullSource)

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ORIGINAL_CHAIN)

        assertFalse(ctb.verifyCertificateTransparency("www.babylonhealth.com", certsToCheck))
    }

    @Test
    fun originalChainDisallowedWhenOnlyOneSct() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.logListDataSource)

        val certsToCheck = TestData.loadCertificates(TEST_MITMPROXY_ORIGINAL_CHAIN)

        val certWithSingleSct = singleSctOnly(certsToCheck.first() as X509Certificate)

        val filtered = listOf(certWithSingleSct, *certsToCheck.drop(1).toTypedArray())

        assertFalse(ctb.verifyCertificateTransparency("www.babylonhealth.com", filtered))
    }

    @Test
    fun noCertificatesDisallowed() {
        val ctb = CertificateTransparencyBase(
            hosts = setOf(Host("*.babylonhealth.com")),
            logListDataSource = LogListDataSourceTestFactory.nullSource)

        assertFalse(ctb.verifyCertificateTransparency("www.babylonhealth.com", emptyList()))
    }

    private fun mitmProxyTrustManager(): X509TrustManager {
        val rootCerts = TestData.loadCertificates(TEST_MITMPROXY_ROOT_CERT)
        val trustManager = TrustedSocketFactory().create(rootCerts).trustManager
        return trustManager
    }

    private fun singleSctOnly(cert: X509Certificate) = spy(cert).apply {
        whenever(getExtensionValue(CTConstants.SCT_CERTIFICATE_OID)).thenAnswer {
            Base64.decode("BHwEegB4AHYAu9nfvB+KcbWTlCOXqpJ7RzhXlQqrUugakJZkNo4e0YUAAAFj7ztQ3wAABAMARzBFAiEA53gntK6Dnr6ROwYGBjqjt5dS4tWM6Zw/TtxIxOvobW8CIF3n4XjIX7/w66gThQD47iF7YmxelwgUQgPzEWNlHQiu")
        }
    }
}
