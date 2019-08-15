package com.babylon.certificatetransparency.internal.loglist.parser

import com.babylon.certificatetransparency.internal.loglist.*
import com.babylon.certificatetransparency.utils.*
import kotlinx.coroutines.*
import org.junit.*
import java.security.*

class LogListVerifierTest {


    @Test
    fun `verifies signature`() = runBlocking {
        // given we have a valid json file and signature
        val keyPair = generateKeyPair()
        val signature = calculateSignature(keyPair.private, json.toByteArray())

        // when we ask for data
        val result = LogListVerifier(keyPair.public).verify(json, signature)

        // then 3 items are returned (many ignored as invalid states)
        require(result is LogServerSignatureResult.Valid)
    }

    @Test
    fun `returns Invalid if signature invalid`() = runBlocking {
        // given we have a valid json file and invalid signature

        // when we ask for data
        val result = LogListVerifier().verify(json, ByteArray(512) { it.toByte() })

        // then invalid is returned
        assertIsA<LogServerSignatureResult.Invalid.SignatureFailed>(result)
    }

    @Test
    fun `returns Invalid if signature corrupt`() = runBlocking {
        // given we have a valid json file and invalid signature

        // when we ask for data
        val result = LogListVerifier().verify(json, ByteArray(32) { it.toByte() })

        // then invalid is returned
        assertIsA<LogServerSignatureResult.Invalid.SignatureNotValid>(result)
    }

    private fun calculateSignature(privateKey: PrivateKey, data: ByteArray): ByteArray {
        return Signature.getInstance("SHA256WithRSA").apply {
            initSign(privateKey)
            update(data)
        }.sign()
    }

    private fun generateKeyPair(): KeyPair {
        return KeyPairGenerator.getInstance("RSA").apply {
            initialize(1024)
        }.generateKeyPair()
    }

    companion object {
        private val json = TestData.file(TestData.TEST_LOG_LIST_JSON_V2_BETA).readText()
        private val sig = TestData.file(TestData.TEST_LOG_LIST_SIG).readBytes()
    }
}