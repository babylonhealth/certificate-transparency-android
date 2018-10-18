package org.certificatetransparency.ctlog.data.loglist

import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import org.certificatetransparency.ctlog.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import javax.net.ssl.SSLPeerUnverifiedException

class LogListNetworkDataSourceTest {

    private val mockInterceptor = mock<Interceptor>()

    private val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(mockInterceptor).build()
    private val retrofit = Retrofit.Builder().client(client).baseUrl("http://ctlog/").build()
    private val logListService: LogListService = retrofit.create(LogListService::class.java)

    private fun expectInterceptor(url: String, jsonResponse: String) {
        whenever(mockInterceptor.intercept(argThat { request().url().toString() == url })).then {

            val chain = it.arguments[0] as Interceptor.Chain

            Response.Builder()
                .body(ResponseBody.create(MediaType.parse("application/json"), jsonResponse))
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("")
                .build()
        }
    }

    private fun expectInterceptorHttpNotFound(url: String) {
        whenever(mockInterceptor.intercept(argThat { request().url().toString() == url })).then {

            val chain = it.arguments[0] as Interceptor.Chain

            Response.Builder()
                .body(ResponseBody.create(MediaType.parse("application/octet-stream"), ByteArray(0)))
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(404)
                .message("")
                .build()
        }
    }

    private fun expectInterceptorSSLException(url: String) {
        whenever(mockInterceptor.intercept(argThat { request().url().toString() == url })).then {
            throw SSLPeerUnverifiedException("Mock throwing exception")
        }
    }

    private fun expectInterceptor(url: String, byteResponse: ByteArray) {
        whenever(mockInterceptor.intercept(argThat { request().url().toString() == url })).then {

            val chain = it.arguments[0] as Interceptor.Chain

            Response.Builder()
                .body(ResponseBody.create(MediaType.parse("application/octet-stream"), byteResponse))
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("")
                .build()
        }
    }

    @Test
    fun verifiesSignature() = runBlocking {
        // given we have a valid json file and signature
        expectInterceptor("http://ctlog/log_list.json", json)
        expectInterceptor("http://ctlog/log_list.sig", sig)

        // when we ask for data
        val result = LogListNetworkDataSource(logListService).get()

        // then 32 items are returned
        requireNotNull(result)
        assertEquals(32, result.size)
        assertEquals("pFASaQVaFVReYhGrN7wQP2KuVXakXksXFEU+GyIQaiU=", result[0].logId)
    }

    @Test
    fun returnsNullIfJsonIncomplete() = runBlocking {
        // given we have a valid json file and signature
        expectInterceptor("http://ctlog/log_list.json", jsonIncomplete)
        expectInterceptor("http://ctlog/log_list.sig", sig)

        // when we ask for data
        val result = LogListNetworkDataSource(logListService).get()

        // then null is returned
        assertNull(result?.size)
    }

    @Test
    fun returnsNullIfSignatureInvalid() = runBlocking {
        // given we have a valid json file and signature
        expectInterceptor("http://ctlog/log_list.json", json)
        expectInterceptor("http://ctlog/log_list.sig", ByteArray(512))

        // when we ask for data
        val result = LogListNetworkDataSource(logListService).get()

        // then null is returned
        assertNull(result?.size)
    }

    @Test
    fun returnsNullIfSignatureCorrupt() = runBlocking {
        // given we have a valid json file and signature
        expectInterceptor("http://ctlog/log_list.json", json)
        expectInterceptor("http://ctlog/log_list.sig", ByteArray(32))

        // when we ask for data
        val result = LogListNetworkDataSource(logListService).get()

        // then null is returned
        assertNull(result?.size)
    }

    @Test
    fun returnsNullWhenLogListNotFound() = runBlocking {
        // given we have a valid json file and signature
        expectInterceptorHttpNotFound("http://ctlog/log_list.json")
        expectInterceptor("http://ctlog/log_list.sig", sig)

        // when we ask for data
        val result = LogListNetworkDataSource(logListService).get()

        // then null is returned
        assertNull(result?.size)
    }

    @Test
    fun returnsNullWhenSignatureNotFound() = runBlocking {
        // given we have a valid json file and signature
        expectInterceptor("http://ctlog/log_list.json", json)
        expectInterceptorHttpNotFound("http://ctlog/log_list.sig")

        // when we ask for data
        val result = LogListNetworkDataSource(logListService).get()

        // then null is returned
        assertNull(result?.size)
    }

    @Test
    fun returnsNullWhenLogListSslException() = runBlocking {
        // given we have a valid signature and an exception when accessing the log list
        expectInterceptorSSLException("http://ctlog/log_list.json")
        expectInterceptor("http://ctlog/log_list.sig", sig)

        // when we ask for data
        val result = LogListNetworkDataSource(logListService).get()

        // then null is returned
        assertNull(result?.size)
    }

    @Test
    fun returnsNullWhenSignatureSslException() = runBlocking {
        // given we have a valid json file and an exception when accessing the signature
        expectInterceptor("http://ctlog/log_list.json", json)
        expectInterceptorSSLException("http://ctlog/log_list.sig")

        // when we ask for data
        val result = LogListNetworkDataSource(logListService).get()

        // then null is returned
        assertNull(result?.size)
    }

    companion object {
        private val json = TestData.file(TestData.TEST_LOG_LIST_JSON).readText()
        private val sig = TestData.file(TestData.TEST_LOG_LIST_SIG).readBytes()

        private val jsonIncomplete = TestData.file(TestData.TEST_LOG_LIST_JSON_INCOMPLETE).readText()
    }
}
