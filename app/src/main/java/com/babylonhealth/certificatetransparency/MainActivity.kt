package com.babylonhealth.certificatetransparency

import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.certificatetransparency.ctlog.okhttp.CertificateTransparencyInterceptor
import java.io.IOException
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {

            val interceptor = CertificateTransparencyInterceptor.Builder().build()
            val client = OkHttpClient.Builder().apply {
                addNetworkInterceptor(interceptor)

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {

                    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    trustManagerFactory.init(null as KeyStore?)

                    val x509TrustManager = trustManagerFactory.trustManagers.first { it is X509TrustManager } as X509TrustManager

                    val sslContext = SSLContext.getInstance("TLSv1.2").apply { init(null, trustManagerFactory.trustManagers, null) }
                    sslSocketFactory(Tls12SocketFactory(sslContext.socketFactory), x509TrustManager)
                    connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
                }
            }.build()

            val request = Request.Builder()
                .url("https://app.babylonhealth.com/")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("onFailure")
                    e.printStackTrace()
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
