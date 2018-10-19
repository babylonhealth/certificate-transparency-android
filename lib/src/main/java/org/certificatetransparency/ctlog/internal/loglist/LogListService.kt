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
 */

package org.certificatetransparency.ctlog.internal.loglist

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

internal interface LogListService {
    @GET("log_list.json")
    fun getLogList(): Call<ResponseBody>

    @GET("log_list.sig")
    fun getLogListSignature(): Call<ResponseBody>
}
