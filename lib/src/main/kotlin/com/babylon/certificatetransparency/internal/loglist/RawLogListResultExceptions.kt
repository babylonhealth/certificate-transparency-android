package com.babylon.certificatetransparency.internal.loglist

import com.babylon.certificatetransparency.internal.utils.stringStackTrace
import com.babylon.certificatetransparency.loglist.*
import com.google.gson.JsonParseException

internal object RawLogListJsonFailedLoading : RawLogListResult.Failure() {
    override fun toString() = "log-list.json failed to load"
}

internal object RawLogListSigFailedLoading : RawLogListResult.Failure() {
    override fun toString() = "log-list.sig failed to load"
}

internal data class RawLogListJsonFailedLoadingWithException(val exception: Exception) : RawLogListResult.Failure() {
    override fun toString() = "log-list.json failed to load with ${exception.stringStackTrace()}"
}

internal data class RawLogListSigFailedLoadingWithException(val exception: Exception) : RawLogListResult.Failure() {
    override fun toString() = "log-list.sig failed to load with ${exception.stringStackTrace()}"
}
