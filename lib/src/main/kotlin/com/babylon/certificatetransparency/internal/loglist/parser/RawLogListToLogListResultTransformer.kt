/*
 * Copyright 2019 Babylon Partners Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.babylon.certificatetransparency.internal.loglist.parser

import com.babylon.certificatetransparency.internal.loglist.LogListJsonFailedLoading
import com.babylon.certificatetransparency.internal.loglist.LogListJsonFailedLoadingWithException
import com.babylon.certificatetransparency.internal.loglist.LogListSigFailedLoading
import com.babylon.certificatetransparency.internal.loglist.LogListSigFailedLoadingWithException
import com.babylon.certificatetransparency.internal.loglist.LogServerSignatureResult
import com.babylon.certificatetransparency.internal.loglist.RawLogListJsonFailedLoading
import com.babylon.certificatetransparency.internal.loglist.RawLogListJsonFailedLoadingWithException
import com.babylon.certificatetransparency.internal.loglist.RawLogListSigFailedLoading
import com.babylon.certificatetransparency.internal.loglist.RawLogListSigFailedLoadingWithException
import com.babylon.certificatetransparency.internal.loglist.SignatureVerificationFailed
import com.babylon.certificatetransparency.loglist.LogListResult
import com.babylon.certificatetransparency.loglist.RawLogListResult

internal class RawLogListToLogListResultTransformer(
    private val logListVerifier: LogListVerifier = LogListVerifier(),
    private val logListJsonParser: LogListJsonParser = LogListJsonParserV2()
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
            is LogServerSignatureResult.Valid -> logListJsonParser.parseJson(logListJson.toString(Charsets.UTF_8))
            is LogServerSignatureResult.Invalid -> SignatureVerificationFailed(signatureResult)
        }
    }
}
