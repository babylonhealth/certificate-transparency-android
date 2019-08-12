package com.babylon.certificatetransparency

import android.util.Log

class BasicAndroidCTLogger(private val isDebugMode: Boolean) : CTLogger {
    override fun log(host: String, result: VerificationResult) {
        if (isDebugMode) {
            Log.i("CertificateTransparency", "$host $result")
        }
    }
}
