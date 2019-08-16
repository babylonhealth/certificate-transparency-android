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
 *
 * Code derived from https://github.com/google/certificate-transparency-java
 */

package com.babylon.certificatetransparency.internal.serialization

import com.babylon.certificatetransparency.internal.logclient.model.DigitallySigned
import com.babylon.certificatetransparency.internal.logclient.model.LogId
import com.babylon.certificatetransparency.internal.logclient.model.SignedCertificateTimestamp
import com.babylon.certificatetransparency.internal.logclient.model.Version
import com.babylon.certificatetransparency.internal.utils.Base64
import com.babylon.certificatetransparency.utils.TestData
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

/** Test serialization.  */
class OutputStreamExtTest {

    @Test
    fun serializeSct() {
        val keyIdBase64 = "3xwuwRUAlFJHqWFoMl3cXHlZ6PfG04j8AC4LvT9012Q="

        val signatureBase64 = "MEUCIGBuEK5cLVobCu1J3Ek39I3nGk6XhOnCCN+/6e9TbPfyAiEAvrKcctfQbWHQa9s4oGlGmqhv4S4Yu3zEVomiwBh+9aU="

        val signature = DigitallySigned(
            hashAlgorithm = DigitallySigned.HashAlgorithm.SHA256,
            signatureAlgorithm = DigitallySigned.SignatureAlgorithm.ECDSA,
            signature = Base64.decode(signatureBase64)
        )

        val sct = SignedCertificateTimestamp(
            sctVersion = Version.V1,
            timestamp = 1365181456089L,
            id = LogId(Base64.decode(keyIdBase64)),
            signature = signature,
            extensions = ByteArray(0)
        )

        val generatedBytes = serializeSctToBinary(sct)
        val readBytes = TestData.file(TEST_CERT_SCT).readBytes()
        assertArrayEquals(readBytes, generatedBytes)
    }

    private fun serializeSctToBinary(sct: SignedCertificateTimestamp): ByteArray {
        return ByteArrayOutputStream().use {
            it.writeUint(sct.sctVersion.number.toLong(), CTConstants.VERSION_LENGTH)
            it.write(sct.id.keyId)
            it.writeUint(sct.timestamp, CTConstants.TIMESTAMP_LENGTH)
            it.writeVariableLength(sct.extensions, CTConstants.MAX_EXTENSIONS_LENGTH)
            it.writeUint(sct.signature.hashAlgorithm.number.toLong(), HASH_ALG_LENGTH)
            it.writeUint(sct.signature.signatureAlgorithm.number.toLong(), SIGNATURE_ALG_LENGTH)
            it.writeVariableLength(sct.signature.signature, CTConstants.MAX_SIGNATURE_LENGTH)

            it.toByteArray()
        }
    }

    companion object {
        const val TEST_CERT_SCT = "/testdata/test-cert.proof"

        const val HASH_ALG_LENGTH = 1
        const val SIGNATURE_ALG_LENGTH = 1
    }
}
