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

package org.certificatetransparency.ctlog.internal

import okhttp3.OkHttpClient
import okhttp3.Request
import org.certificatetransparency.ctlog.certificateTransparencyInterceptor
import org.certificatetransparency.ctlog.utils.LogListDataSourceTestFactory
import org.junit.Test
import java.net.SocketException

class CertificateTransparencyInterceptorIntegrationTest {

    companion object {
        val networkInterceptor = certificateTransparencyInterceptor {
            +"*.babylonhealth.com"
            +"letsencrypt.org"
            +"invalid-expected-sct.badssl.com"

            logListDataSource {
                LogListDataSourceTestFactory.logListDataSource
            }
        }
    }

    @Test
    fun babylonHealthAllowed() {
        val client = OkHttpClient.Builder().addNetworkInterceptor(networkInterceptor).build()

        val request = Request.Builder()
            .url("https://www.babylonhealth.com")
            .build()

        client.newCall(request).execute()
    }

    @Test
    fun letsEncryptAllowed() {
        val client = OkHttpClient.Builder().addNetworkInterceptor(networkInterceptor).build()

        val request = Request.Builder()
            .url("https://letsencrypt.org")
            .build()

        client.newCall(request).execute()
    }

    @Test(expected = SocketException::class)
    fun invalidDisallowedWithException() {
        val client = OkHttpClient.Builder().addNetworkInterceptor(networkInterceptor).build()

        val request = Request.Builder()
            .url("https://invalid-expected-sct.badssl.com/")
            .build()

        client.newCall(request).execute()
    }

    @Test
    fun invalidAllowedWhenSctNotChecked() {
        val client = OkHttpClient.Builder().addNetworkInterceptor(certificateTransparencyInterceptor {
            +"*.babylonhealth.com"

            logListDataSource {
                LogListDataSourceTestFactory.logListDataSource
            }

        }).build()

        val request = Request.Builder()
            .url("https://invalid-expected-sct.badssl.com/")
            .build()

        client.newCall(request).execute()
    }
}
