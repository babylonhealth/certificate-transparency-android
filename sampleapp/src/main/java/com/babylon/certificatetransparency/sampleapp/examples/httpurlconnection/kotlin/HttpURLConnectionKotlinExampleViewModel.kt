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

package com.babylon.certificatetransparency.sampleapp.examples.httpurlconnection.kotlin

import android.content.Context
import com.babylon.certificatetransparency.Logger
import com.babylon.certificatetransparency.certificateTransparencyHostnameVerifier
import com.babylon.certificatetransparency.sampleapp.examples.BaseExampleViewModel
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class HttpURLConnectionKotlinExampleViewModel(private val applicationContext: Context) : BaseExampleViewModel() {

    override val sampleCodeTemplate
        get() = "httpurlconnection-kotlin.txt"

    private fun HttpURLConnection.enableCertificateTransparencyChecks(
        hosts: Set<String>,
        isFailOnError: Boolean,
        defaultLogger: Logger
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

    override fun openConnection(connectionHost: String, hosts: Set<String>, isFailOnError: Boolean, defaultLogger: Logger) {
        // Quick and dirty way to push the network call onto a background thread, don't do this is a real app
        Thread {
            try {
                val connection = URL("https://$connectionHost").openConnection() as HttpURLConnection

                connection.enableCertificateTransparencyChecks(hosts, isFailOnError, defaultLogger)

                connection.connect()
            } catch (e: IOException) {
                sendException(e)
            }
        }.start()
    }
}
