/*
 * Copyright 2020 Babylon Partners Limited
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

package com.babylon.certificatetransparency.internal.loglist

import com.babylon.certificatetransparency.cache.DiskCache
import com.babylon.certificatetransparency.datasource.DataSource
import com.babylon.certificatetransparency.internal.loglist.parser.RawLogListToLogListResultTransformer
import com.babylon.certificatetransparency.internal.utils.ByteArrayConverterFactory
import com.babylon.certificatetransparency.internal.utils.MaxSizeInterceptor
import com.babylon.certificatetransparency.loglist.LogListResult
import com.babylon.certificatetransparency.loglist.RawLogListResult
import okhttp3.OkHttpClient
import retrofit2.Retrofit

internal object LogListDataSourceFactory {

    fun create(diskCache: DiskCache? = null): DataSource<LogListResult> {
        val okHttpClient = OkHttpClient.Builder().addInterceptor(MaxSizeInterceptor()).cache(null).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.gstatic.com/ct/log_list/v2/")
            .addConverterFactory(ByteArrayConverterFactory())
            .client(okHttpClient)
            .build()

        val logService = retrofit.create(LogListService::class.java)
        val transformer = RawLogListToLogListResultTransformer()

        return InMemoryCache()
            .run {
                diskCache?.let(::compose) ?: this
            }
            .compose(LogListZipNetworkDataSource(logService))
            .compose(LogListNetworkDataSource(logService))
            .oneWayTransform { transformer.transform(it) }
            .reuseInflight()
    }

    private class InMemoryCache : InMemoryDataSource<RawLogListResult>() {
        override suspend fun isValid(value: RawLogListResult?) = value is RawLogListResult.Success
    }
}
