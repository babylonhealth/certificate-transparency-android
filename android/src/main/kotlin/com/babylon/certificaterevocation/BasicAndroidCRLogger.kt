package com.babylon.certificaterevocation

import android.util.Log
import com.babylon.certificatetransparency.BuildConfig

class BasicAndroidCRLogger : CRLogger {
    override fun log(host: String, result: RevocationResult) {
        if (BuildConfig.DEBUG) {
            Log.i("CertificateRevocation", "$host $result")
        }
    }
}
