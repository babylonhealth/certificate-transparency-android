package org.certificatetransparency.ctlog.okhttp

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

internal interface LogService {
    @GET("log_list.json")
    fun getLogList(): Call<ResponseBody>

    @GET("log_list.sig")
    fun getLogListSignature(): Call<ResponseBody>
}
