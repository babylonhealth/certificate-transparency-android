package org.certificatetransparency.ctlog.okhttp

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.tls.OkHostnameVerifier
import org.junit.Test
import javax.net.ssl.SSLPeerUnverifiedException

class CertificateTransparencyHostnameVerifierTest {

    @Test
    fun verifyRuby() {
        val client = OkHttpClient.Builder().hostnameVerifier(CertificateTransparencyHostnameVerifier(OkHostnameVerifier.INSTANCE)).build()

        val request = Request.Builder()
            .url("https://app.babylonpartners.com")
            .build()

        println(client.newCall(request).execute().body().toString())
    }

    @Test
    fun verifyRuby2() {
        val client = OkHttpClient.Builder().addNetworkInterceptor(CertificateTransparencyInterceptor()).build()

        val request = Request.Builder()
            .url("https://app.babylonpartners.com")
            .build()

        println(client.newCall(request).execute().body().toString())
    }

    @Test(expected = SSLPeerUnverifiedException::class)
    fun verifyBlog2() {
        val client = OkHttpClient.Builder().addNetworkInterceptor(CertificateTransparencyInterceptor()).build()

        val request = Request.Builder()
            .url("https://blog.babylonhealth.com/")
            .build()

        client.newCall(request).execute()
    }

    @Test
    fun verifyPhp() {
        val client = OkHttpClient.Builder().hostnameVerifier(CertificateTransparencyHostnameVerifier(OkHostnameVerifier.INSTANCE)).build()

        //client.sslSocketFactory()

        val request = Request.Builder()
            .url("https://app2.babylonpartners.com/")
            .build()

        println(client.newCall(request).execute().body().toString())
    }

    @Test
    fun verifyAi() {
        val client = OkHttpClient.Builder().hostnameVerifier(CertificateTransparencyHostnameVerifier(OkHostnameVerifier.INSTANCE)).build()

        val request = Request.Builder()
            .url("https://services.babylonpartners.com/")
            .build()

        println(client.newCall(request).execute().body().toString())
    }

    @Test
    fun verifyWebApp() {
        val client = OkHttpClient.Builder().hostnameVerifier(CertificateTransparencyHostnameVerifier(OkHostnameVerifier.INSTANCE)).build()

        val request = Request.Builder()
            .url("https://online.babylonhealth.com/")
            .build()

        client.newCall(request).execute()
    }

    @Test(expected = SSLPeerUnverifiedException::class)
    fun verifyBlog() {
        val client = OkHttpClient.Builder().hostnameVerifier(CertificateTransparencyHostnameVerifier(OkHostnameVerifier.INSTANCE)).build()

        val request = Request.Builder()
            .url("https://blog.babylonhealth.com/")
            .build()

        client.newCall(request).execute()
    }
}
