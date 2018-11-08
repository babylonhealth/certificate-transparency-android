package org.certificatetransparency.ctlog.internal.serialization

import org.certificatetransparency.ctlog.internal.utils.Base64
import org.certificatetransparency.ctlog.logclient.model.DigitallySigned
import org.certificatetransparency.ctlog.logclient.model.LogId
import org.certificatetransparency.ctlog.logclient.model.SignedCertificateTimestamp
import org.certificatetransparency.ctlog.logclient.model.Version
import org.certificatetransparency.ctlog.utils.TestData
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
            version = Version.V1,
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
            it.writeUint(sct.version.number.toLong(), CTConstants.VERSION_LENGTH)
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
