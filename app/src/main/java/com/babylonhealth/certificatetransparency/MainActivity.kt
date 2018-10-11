package com.babylonhealth.certificatetransparency

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.certificatetransparency.ctlog.okhttp.CertificateTransparencyInterceptor
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            val client = OkHttpClient.Builder().addNetworkInterceptor(CertificateTransparencyInterceptor()).build()

            val request = Request.Builder()
                .url("https://app.babylonhealth.com/")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("onFailure")
                    //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onResponse(call: Call, response: Response) {
                    println("onResponse")
                    //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            })
        }
    }
}
