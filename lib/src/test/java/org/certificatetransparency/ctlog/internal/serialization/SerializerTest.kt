package org.certificatetransparency.ctlog.internal.serialization

import org.certificatetransparency.ctlog.internal.utils.Base64
import org.certificatetransparency.ctlog.logclient.model.DigitallySigned
import org.certificatetransparency.ctlog.logclient.model.LogId
import org.certificatetransparency.ctlog.logclient.model.SignedCertificateTimestamp
import org.certificatetransparency.ctlog.logclient.model.Version
import org.certificatetransparency.ctlog.utils.TestData
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test serialization.  */
@RunWith(JUnit4::class)
class SerializerTest {

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
            version = Version.V1,
            timestamp = 1365181456089L,
            id = LogId(Base64.decode(keyIdBase64)),
            signature = signature,
            extensions = ByteArray(0)
        )

        val generatedBytes = Serializer.serializeSctToBinary(sct)
        val readBytes = TestData.file(TEST_CERT_SCT).readBytes()
        assertArrayEquals(readBytes, generatedBytes)
    }

    companion object {
        const val TEST_CERT_SCT = "/testdata/test-cert.proof"
    }
}
