package com.babylon.certificatetransparency

import android.util.Log

class BasicAndroidCTLogger : CTLogger {
    override fun log(host: String, result: VerificationResult) {
        if (BuildConfig.DEBUG) {
            Log.i("CertificateTransparency", "$host $result")
        }
    }
}
