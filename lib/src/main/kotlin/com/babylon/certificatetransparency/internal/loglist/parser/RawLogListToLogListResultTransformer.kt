package com.babylon.certificatetransparency.internal.loglist.parser

import com.babylon.certificatetransparency.internal.loglist.*
import com.babylon.certificatetransparency.loglist.*

internal class RawLogListToLogListResultTransformer(
        private val logListVerifier: LogListVerifier = LogListVerifier(),
        private val logListJsonParser: LogListJsonParser = LogListJsonParserV1()
) {
    fun transform(rawLogListResult: RawLogListResult) =
            when (rawLogListResult) {
                is RawLogListResult.Success -> transformSuccess(rawLogListResult)
                is RawLogListResult.Failure -> transformFailure(rawLogListResult)
            }

    private fun transformFailure(rawLogListResult: RawLogListResult.Failure) =
            when (rawLogListResult) {
                is RawLogListJsonFailedLoading -> LogListJsonFailedLoading
                is RawLogListJsonFailedLoadingWithException ->
                    LogListJsonFailedLoadingWithException(rawLogListResult.exception)
                is RawLogListSigFailedLoading -> LogListSigFailedLoading
                is RawLogListSigFailedLoadingWithException ->
                    LogListSigFailedLoadingWithException(rawLogListResult.exception)
                else -> LogListJsonFailedLoading
            }

    private fun transformSuccess(rawLogListResult: RawLogListResult.Success): LogListResult {
        val (logListJson, signature) = rawLogListResult
        return when (val signatureResult = logListVerifier.verify(logListJson, signature)) {
            is LogServerSignatureResult.Valid -> logListJsonParser.parseJson(logListJson)
            is LogServerSignatureResult.Invalid -> SignatureVerificationFailed(signatureResult)
        }
    }
}