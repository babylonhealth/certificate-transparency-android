package com.babylon.certificaterevocation

import android.util.Log

class BasicAndroidCRLogger(private val isDebugMode: Boolean) : CRLogger {
    override fun log(host: String, result: RevocationResult) {
        if (isDebugMode) {
            Log.i("CertificateRevocation", "$host $result")
        }
    }
}
