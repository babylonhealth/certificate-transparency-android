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
