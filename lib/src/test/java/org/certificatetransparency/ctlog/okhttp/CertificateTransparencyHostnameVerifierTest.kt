package org.certificatetransparency.ctlog.okhttp

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.tls.OkHostnameVerifier
import org.certificatetransparency.ctlog.CertificateTransparencyFactory
import org.junit.Ignore
import org.junit.Test
import javax.net.ssl.SSLPeerUnverifiedException

class CertificateTransparencyHostnameVerifierTest {

    companion object {
        val hostnameVerifier = CertificateTransparencyFactory.hostnameVerifier(OkHostnameVerifier.INSTANCE).build()

        val networkInterceptor = CertificateTransparencyFactory.okHttpInterceptor().build()
    }

    @Test
    fun verifyRuby() {
        val client = OkHttpClient.Builder().hostnameVerifier(hostnameVerifier).build()

        val request = Request.Builder()
            .url("https://app.babylonpartners.com")
            .build()

        println(client.newCall(request).execute().body().toString())
    }

    @Test
    fun verifyRuby2() {
        val client = OkHttpClient.Builder().addNetworkInterceptor(networkInterceptor).build()

        val request = Request.Builder()
            .url("https://app.babylonpartners.com")
            .build()

        println(client.newCall(request).execute().body().toString())
    }

    @Test(expected = SSLPeerUnverifiedException::class)
    @Ignore
    fun verifyBlog2() {
        val client = OkHttpClient.Builder().addNetworkInterceptor(networkInterceptor).build()

        val request = Request.Builder()
            .url("https://blog.babylonhealth.com/")
            .build()

        client.newCall(request).execute()
    }

    @Test
    fun verifyPhp() {
        val client = OkHttpClient.Builder().hostnameVerifier(hostnameVerifier).build()

        //client.sslSocketFactory()

        val request = Request.Builder()
            .url("https://app2.babylonpartners.com/")
            .build()

        println(client.newCall(request).execute().body().toString())
    }

    @Test
    fun verifyAi() {
        val client = OkHttpClient.Builder().hostnameVerifier(hostnameVerifier).build()

        val request = Request.Builder()
            .url("https://services.babylonpartners.com/")
            .build()

        println(client.newCall(request).execute().body().toString())
    }

    @Test
    fun verifyWebApp() {
        val client = OkHttpClient.Builder().hostnameVerifier(hostnameVerifier).build()

        val request = Request.Builder()
            .url("https://online.babylonhealth.com/")
            .build()

        client.newCall(request).execute()
    }

    @Test(expected = SSLPeerUnverifiedException::class)
    @Ignore
    fun verifyBlog() {
        val client = OkHttpClient.Builder().hostnameVerifier(hostnameVerifier).build()

        val request = Request.Builder()
            .url("https://blog.babylonhealth.com/")
            .build()

        client.newCall(request).execute()
    }
}
