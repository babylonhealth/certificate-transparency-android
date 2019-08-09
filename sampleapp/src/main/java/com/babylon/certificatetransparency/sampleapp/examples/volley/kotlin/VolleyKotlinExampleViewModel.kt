/*
 * Copyright 2019 Babylon Partners Limited
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

package com.babylon.certificatetransparency.sampleapp.examples.volley.kotlin

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.babylon.certificatetransparency.CTLogger
import com.babylon.certificatetransparency.certificateTransparencyHostnameVerifier
import com.babylon.certificatetransparency.sampleapp.examples.BaseExampleViewModel
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class VolleyKotlinExampleViewModel(private val applicationContext: Context) : BaseExampleViewModel() {

    override val sampleCodeTemplate
        get() = "volley-kotlin.txt"

    private fun HttpURLConnection.enableCertificateTransparencyChecks(
        hosts: Set<String>,
        isFailOnError: Boolean,
        defaultLogger: CTLogger
    ) {
        if (this is HttpsURLConnection) {
            // Create a hostname verifier wrapping the original
            hostnameVerifier = certificateTransparencyHostnameVerifier(hostnameVerifier) {
                hosts.forEach {
                    +it
                }
                failOnError = isFailOnError
                logger = defaultLogger
            }
        }
    }

    // A normal client would create this ahead of time and share it between network requests
    // We create it dynamically as we allow the user to set the hosts for certificate transparency
    private fun createRequestQueue(hosts: Set<String>, isFailOnError: Boolean, defaultLogger: CTLogger): RequestQueue {
        return Volley.newRequestQueue(applicationContext,
            object : HurlStack() {
                override fun createConnection(url: URL): HttpURLConnection {
                    return super.createConnection(url).apply {
                        enableCertificateTransparencyChecks(hosts, isFailOnError, defaultLogger)
                    }
                }
            })
    }

    override fun openConnection(connectionHost: String, hosts: Set<String>, isFailOnError: Boolean, defaultLogger: CTLogger) {
        val queue = createRequestQueue(hosts, isFailOnError, defaultLogger)

        val request = StringRequest(Request.Method.GET, "https://$connectionHost",
            Response.Listener<String> { response ->
                // Success. Reason will have been sent to the logger
                println(response)
            },
            Response.ErrorListener {
                // Failure. Send message to the UI as logger won't catch generic network exceptions
                sendException(it)
            })

        // Explicitly disable cache so we always call the interceptor and thus see the certificate transparency results
        request.setShouldCache(false)

        // Add the request to the RequestQueue.
        queue.add(request)
    }
}
