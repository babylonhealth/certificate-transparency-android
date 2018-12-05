package com.babylon.certificatetransparency.internal.loglist

import com.babylon.certificatetransparency.internal.utils.stringStackTrace
import com.babylon.certificatetransparency.loglist.LogListResult
import com.google.gson.JsonParseException
import java.io.IOException

internal data class SignatureVerificationFailed(val signatureResult: LogServerSignatureResult.Invalid) : LogListResult.Invalid()

internal object NoLogServers : LogListResult.Invalid() {
    override fun toString() = "log-list.json contains no log servers"
}

internal object LogListJsonFailedLoading : LogListResult.Invalid() {
    override fun toString() = "log-list.json failed to load"
}

internal object LogListSigFailedLoading : LogListResult.Invalid() {
    override fun toString() = "log-list.sig failed to load"
}

internal data class LogListJsonFailedLoadingWithException(val exception: IOException) : LogListResult.Invalid() {
    override fun toString() = "log-list.json failed to load with ${exception.stringStackTrace()}"
}

internal data class LogListSigFailedLoadingWithException(val exception: IOException) : LogListResult.Invalid() {
    override fun toString() = "log-list.sig failed to load with ${exception.stringStackTrace()}"
}

internal data class JsonFormat(val exception: JsonParseException) : LogListResult.Invalid() {
    override fun toString() = "log-list.json badly formatted with ${exception.stringStackTrace()}"
}

internal data class LogServerInvalidKey(val exception: Exception, val key: String) : LogListResult.Invalid() {
    override fun toString() = "Public key for log server $key cannot be used with ${exception.stringStackTrace()}"
}
