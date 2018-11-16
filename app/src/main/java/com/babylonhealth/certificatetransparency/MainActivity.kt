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

package com.babylonhealth.certificatetransparency

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.certificatetransparency.ctlog.Logger
import org.certificatetransparency.ctlog.VerificationResult
import org.certificatetransparency.ctlog.certificateTransparencyInterceptor
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {

            val interceptor = certificateTransparencyInterceptor {
                +"*.babylonhealth.com"

                logger = object : Logger {
                    override fun log(host: String, result: VerificationResult) {
                        println("$host -> $result")
                    }
                }
            }

            val client = OkHttpClient.Builder().apply {
                addNetworkInterceptor(interceptor)
            }.build()

            val request = Request.Builder()
                .url("https://app.babylonhealth.com/")
                .build()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Snackbar.make(container, "Failure", Snackbar.LENGTH_LONG).run {
                        view.setBackgroundColor(Color.parseColor("#FF5252"))
                        show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    Snackbar.make(container, "Success", Snackbar.LENGTH_LONG).run {
                        view.setBackgroundColor(Color.parseColor("#4CAF50"))
                        show()
                    }
                }
            })
        }
    }
}
