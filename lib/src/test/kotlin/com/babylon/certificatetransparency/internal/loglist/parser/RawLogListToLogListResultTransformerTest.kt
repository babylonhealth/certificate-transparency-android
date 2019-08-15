package com.babylon.certificatetransparency.internal.loglist.parser

import com.babylon.certificatetransparency.internal.loglist.JsonFormat
import com.babylon.certificatetransparency.internal.loglist.LogListJsonFailedLoadingWithException
import com.babylon.certificatetransparency.internal.loglist.LogListSigFailedLoadingWithException
import com.babylon.certificatetransparency.internal.loglist.RawLogListJsonFailedLoadingWithException
import com.babylon.certificatetransparency.internal.loglist.RawLogListSigFailedLoadingWithException
import com.babylon.certificatetransparency.internal.loglist.SignatureVerificationFailed
import com.babylon.certificatetransparency.internal.utils.Base64
import com.babylon.certificatetransparency.loglist.LogListResult
import com.babylon.certificatetransparency.loglist.RawLogListResult
import com.babylon.certificatetransparency.utils.TestData
import com.babylon.certificatetransparency.utils.assertIsA
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import javax.net.ssl.SSLException

class RawLogListToLogListResultTransformerTest {

    @Test
    fun `verifies signature`() = runBlocking {
        // given we have a valid json file and signature

        // when we ask for data
        val result = RawLogListToLogListResultTransformer().transform(
            RawLogListResult.Success(
                json,
                sig
            )
        )

        // then 32 items are returned
        require(result is LogListResult.Valid)
        assertEquals(32, result.servers.size)
        assertEquals("pFASaQVaFVReYhGrN7wQP2KuVXakXksXFEU+GyIQaiU=", Base64.toBase64String(result.servers[0].id))
    }

    @Test
    fun `returns Invalid if json incomplete`() = runBlocking {
        // given we have an invalid json file and valid signature
        val keyPair = generateKeyPair()
        val signature = calculateSignature(keyPair.private, jsonIncomplete.toByteArray())

        // when we ask for data
        val result = RawLogListToLogListResultTransformer(
            logListVerifier = LogListVerifier(keyPair.public)
        ).transform(
            RawLogListResult.Success(
                jsonIncomplete, signature
            )
        )

        // then invalid is returned
        assertIsA<JsonFormat>(result)
    }

    @Test
    fun `returns Invalid if signature invalid`() = runBlocking {
        // given we have a valid json file and na invalid signature

        // when we ask for data
        val result = RawLogListToLogListResultTransformer().transform(
            RawLogListResult.Success(
                json, ByteArray(512)
            )
        )

        // then invalid is returned
        assertIsA<SignatureVerificationFailed>(result)
    }

    @Test
    fun `returns Invalid if signature corrupt`() = runBlocking {
        // given we have a valid json file and signature

        // when we ask for data
        val result = RawLogListToLogListResultTransformer().transform(
            RawLogListResult.Success(
                json, ByteArray(32)
            )
        )

        // then invalid is returned
        assertIsA<SignatureVerificationFailed>(result)
    }

    @Test
    fun `returns Invalid when log_list json not found`() = runBlocking {
        // given we have a valid json file and signature

        // when we ask for data
        val result = RawLogListToLogListResultTransformer().transform(
            RawLogListJsonFailedLoadingWithException(IOException("bogo"))
        )

        // then invalid is returned
        assertIsA<LogListJsonFailedLoadingWithException>(result)
    }

    @Test
    fun `returns Invalid when log_list sig not found`() = runBlocking {
        // given we have a valid json file and signature

        // when we ask for data
        val result = RawLogListToLogListResultTransformer().transform(
            RawLogListSigFailedLoadingWithException(IOException("bogo"))
        )

        // then invalid is returned
        assertIsA<LogListSigFailedLoadingWithException>(result)
    }

    @Test
    fun `returns Invalid when log_list json has SslException`() = runBlocking {
        // given we have a valid signature and an exception when accessing the log list

        // when we ask for data
        val result = RawLogListToLogListResultTransformer().transform(
            RawLogListJsonFailedLoadingWithException(SSLException("bogo"))
        )

        // then invalid is returned
        assertIsA<LogListJsonFailedLoadingWithException>(result)
    }

    @Test
    fun `returns Invalid when log_list sig has SslException`() = runBlocking {
        // given we have a valid json file and an exception when accessing the signature

        // when we ask for data
        val result = RawLogListToLogListResultTransformer().transform(
            RawLogListSigFailedLoadingWithException(SSLException("bogo"))
        )

        // then invalid is returned
        assertIsA<LogListSigFailedLoadingWithException>(result)
    }

    @Test
    fun `validUntil null when not disqualified or no FinalTreeHead`() = runBlocking {
        // given we have a valid json file and signature
        val keyPair = generateKeyPair()
        val signature = calculateSignature(keyPair.private, jsonValidUntil.toByteArray())

        // when we ask for data
        val result = RawLogListToLogListResultTransformer(
            logListVerifier = LogListVerifier(keyPair.public)
        ).transform(
            RawLogListResult.Success(
                jsonValidUntil, signature
            )
        )

        // then validUntil is set to the the STH timestamp
        require(result is LogListResult.Valid)
        val logServer = result.servers[0]
        assertNull(logServer.validUntil)
    }

    @Test
    fun `validUntil set from Sth`() = runBlocking {
        // given we have a valid json file and signature
        val keyPair = generateKeyPair()
        val signature = calculateSignature(keyPair.private, jsonValidUntil.toByteArray())

        // when we ask for data
        val result = RawLogListToLogListResultTransformer(
            logListVerifier = LogListVerifier(keyPair.public)
        ).transform(
            RawLogListResult.Success(
                jsonValidUntil, signature
            )
        )

        // then validUntil is set to the the STH timestamp
        require(result is LogListResult.Valid)
        val logServer = result.servers[2]
        assertNotNull(logServer.validUntil)
        assertEquals(1480512258330, logServer.validUntil)
    }

    @Test
    fun `validUntil set from disqualified`() = runBlocking {
        // given we have a valid json file and signature
        val keyPair = generateKeyPair()
        val signature = calculateSignature(keyPair.private, jsonValidUntil.toByteArray())

        // when we ask for data
        val result = RawLogListToLogListResultTransformer(
            logListVerifier = LogListVerifier(keyPair.public)
        ).transform(
            RawLogListResult.Success(
                jsonValidUntil, signature
            )
        )

        // then validUntil is set to the the STH timestamp
        require(result is LogListResult.Valid)
        val logServer = result.servers[1]
        assertNotNull(logServer.validUntil)
        assertEquals(1475637842000, logServer.validUntil)
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
        private val json = TestData.file(TestData.TEST_LOG_LIST_JSON).readText()
        private val sig = TestData.file(TestData.TEST_LOG_LIST_SIG).readBytes()

        private val jsonIncomplete = TestData.file(TestData.TEST_LOG_LIST_JSON_INCOMPLETE).readText()
        private val jsonValidUntil = TestData.file(TestData.TEST_LOG_LIST_JSON_VALID_UNTIL).readText()
    }
}