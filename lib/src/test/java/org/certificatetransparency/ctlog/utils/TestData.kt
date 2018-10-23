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

package org.certificatetransparency.ctlog.utils

import org.junit.Ignore
import java.io.File
import java.security.cert.Certificate

/** Constants for tests.  */
@Ignore
object TestData {
    private const val DATA_ROOT = "/testdata/"
    // Public log key
    const val TEST_LOG_KEY = DATA_ROOT + "ct-server-key-public.pem"
    const val TEST_LOG_KEY_RSA = DATA_ROOT + "rsa/ct-server-key-public-rsa.pem"
    const val TEST_LOG_KEY_PILOT = DATA_ROOT + "google-ct-pilot-server-key-public.pem"
    const val TEST_LOG_KEY_SKYDIVER = DATA_ROOT + "google-ct-skydiver-server-key-public.pem"
    const val TEST_LOG_KEY_DIGICERT = DATA_ROOT + "digicert-ct-server-key-public.pem"
    // Root CA cert.
    const val ROOT_CA_CERT = DATA_ROOT + "ca-cert.pem"
    // Ordinary cert signed by ca-cert, with SCT served separately.
    const val TEST_CERT = DATA_ROOT + "test-cert.pem"
    const val TEST_CERT_SCT = DATA_ROOT + "test-cert.proof"
    const val TEST_CERT_SCT_RSA = DATA_ROOT + "rsa/test-cert-rsa.proof"
    // PreCertificate signed by ca-cert.
    const val TEST_PRE_CERT = DATA_ROOT + "test-embedded-pre-cert.pem"
    const val TEST_PRE_SCT = DATA_ROOT + "test-embedded-pre-cert.proof"
    const val TEST_PRE_SCT_RSA = DATA_ROOT + "rsa/test-embedded-pre-cert-rsa.proof"
    // PreCertificate Signing cert, signed by ca-cert.pem
    const val PRE_CERT_SIGNING_CERT = DATA_ROOT + "ca-pre-cert.pem"
    // PreCertificate signed by the PreCertificate Signing Cert above.
    const val TEST_PRE_CERT_SIGNED_BY_PRECA_CERT = DATA_ROOT + "test-embedded-with-preca-pre-cert.pem"
    const val TEST_PRE_CERT_PRECA_SCT = DATA_ROOT + "test-embedded-with-preca-pre-cert.proof"
    // intermediate CA cert signed by ca-cert
    const val INTERMEDIATE_CA_CERT = DATA_ROOT + "intermediate-cert.pem"
    // Certificate signed by intermediate CA.
    const val TEST_INTERMEDIATE_CERT = DATA_ROOT + "test-intermediate-cert.pem"
    const val TEST_INTERMEDIATE_CERT_SCT = DATA_ROOT + "test-intermediate-cert.proof"

    const val TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE = DATA_ROOT + "test-embedded-with-intermediate-pre-cert.pem"
    const val TEST_PRE_CERT_SIGNED_BY_INTERMEDIATE_SCT = DATA_ROOT + "test-embedded-with-intermediate-pre-cert.proof"

    const val PRE_CERT_SIGNING_BY_INTERMEDIATE = DATA_ROOT + "intermediate-pre-cert.pem"
    const val TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE = DATA_ROOT + "test-embedded-with-intermediate-preca-pre-cert.pem"
    const val TEST_PRE_CERT_SIGNED_BY_PRECA_INTERMEDIATE_SCT = DATA_ROOT + "test-embedded-with-intermediate-preca-pre-cert.proof"
    const val TEST_ROOT_CERTS = DATA_ROOT + "test-root-certs"
    const val TEST_GITHUB_CHAIN = DATA_ROOT + "github-chain.pem"

    const val TEST_LOG_LIST_JSON = DATA_ROOT + "loglist/log_list.json"
    const val TEST_LOG_LIST_JSON_INCOMPLETE = DATA_ROOT + "loglist/log_list_incomplete.json"
    const val TEST_LOG_LIST_SIG = DATA_ROOT + "loglist/log_list.sig"

    const val TEST_LOG_LIST_JSON_V2_BETA = DATA_ROOT + "loglist_v2_beta/log_list_example.json"

    const val TEST_MITMPROXY_ROOT_CERT = DATA_ROOT + "mitmproxy-ca-cert.pem"
    const val TEST_MITMPROXY_ATTACK_CHAIN = DATA_ROOT + "mitmproxy-attack-chain.pem"
    const val TEST_MITMPROXY_ORIGINAL_CHAIN = DATA_ROOT + "mitmproxy-original-chain.pem"

    fun loadCertificates(filename: String): List<Certificate> {
        val file = File(TestData::class.java.getResource(filename)!!.file)
        return CryptoDataLoader.certificatesFromFile(file)
    }

    fun file(name: String): File {
        return File(TestData::class.java.getResource(name)!!.file)
    }

    fun fileName(name: String): String {
        println(name)
        return TestData::class.java.getResource(name)!!.file
    }
}
