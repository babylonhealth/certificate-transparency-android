package org.certificatetransparency.ctlog.serialization

import org.certificatetransparency.ctlog.TestData
import org.certificatetransparency.ctlog.Base64
import org.certificatetransparency.ctlog.serialization.model.DigitallySigned
import org.certificatetransparency.ctlog.serialization.model.LogID
import org.certificatetransparency.ctlog.serialization.model.SignedCertificateTimestamp
import org.certificatetransparency.ctlog.serialization.model.Version
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test serialization.  */
@RunWith(JUnit4::class)
class TestSerializer {

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
            id = LogID(Base64.decode(keyIdBase64)),
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
