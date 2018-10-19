package org.certificatetransparency.ctlog.internal

import okhttp3.OkHttpClient
import okhttp3.Request
import org.certificatetransparency.ctlog.certificateTransparencyHostnameVerifier
import org.certificatetransparency.ctlog.utils.LogListDataSourceTestFactory
import org.junit.Test
import javax.net.ssl.SSLPeerUnverifiedException

class CertificateTransparencyHostnameVerifierIntegrationTest {

    companion object {
        val hostnameVerifier = certificateTransparencyHostnameVerifier {
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
        val client = OkHttpClient.Builder().hostnameVerifier(hostnameVerifier).build()

        val request = Request.Builder()
            .url("https://www.babylonhealth.com")
            .build()

        client.newCall(request).execute()
    }

    @Test
    fun letsEncryptAllowed() {
        val client = OkHttpClient.Builder().hostnameVerifier(hostnameVerifier).build()

        val request = Request.Builder()
            .url("https://letsencrypt.org")
            .build()

        client.newCall(request).execute()
    }

    @Test(expected = SSLPeerUnverifiedException::class)
    fun invalidDisallowedWithException() {
        val client = OkHttpClient.Builder().hostnameVerifier(hostnameVerifier).build()

        val request = Request.Builder()
            .url("https://invalid-expected-sct.badssl.com/")
            .build()

        client.newCall(request).execute()
    }

    @Test
    fun invalidAllowedWhenSctNotChecked() {
        val client = OkHttpClient.Builder().hostnameVerifier(certificateTransparencyHostnameVerifier {
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
