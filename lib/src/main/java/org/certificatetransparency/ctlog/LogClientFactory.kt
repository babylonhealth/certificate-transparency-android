package org.certificatetransparency.ctlog

import okhttp3.OkHttpClient
import org.certificatetransparency.ctlog.internal.logclient.HttpLogClient
import org.certificatetransparency.ctlog.internal.logclient.LogClientService
import org.certificatetransparency.ctlog.logclient.LogClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object LogClientFactory {
    fun create(baseUrl: String): LogClient {
        val client = OkHttpClient.Builder().build()
        val retrofit = Retrofit.Builder().client(client).addConverterFactory(GsonConverterFactory.create()).baseUrl(baseUrl).build()
        val logClientService = retrofit.create(LogClientService::class.java)

        return HttpLogClient(logClientService)
    }
}
