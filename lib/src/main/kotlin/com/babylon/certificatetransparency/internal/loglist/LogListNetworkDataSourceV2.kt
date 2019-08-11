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

package com.babylon.certificatetransparency.internal.loglist

import com.babylon.certificatetransparency.datasource.DataSource
import com.babylon.certificatetransparency.internal.loglist.model.v2beta.LogListV2Beta
import com.babylon.certificatetransparency.internal.loglist.model.v2beta.State
import com.babylon.certificatetransparency.internal.utils.Base64
import com.babylon.certificatetransparency.internal.utils.PublicKeyFactory
import com.babylon.certificatetransparency.loglist.LogListResult
import com.babylon.certificatetransparency.loglist.LogServer
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException

// Collection of CT logs that are trusted for the purposes of this test from https://www.gstatic.com/ct/log_list/log_list.json
internal class LogListNetworkDataSourceV2(
    private val logService: LogListService,
    private val publicKey: PublicKey = GoogleLogListPublicKey
) : DataSource<LogListResult> {

    override val coroutineContext = GlobalScope.coroutineContext

    @Suppress("ReturnCount")
    override suspend fun get(): LogListResult {
        val logListJob = async { logService.getLogList().string() }
        val signatureJob = async { logService.getLogListSignature().bytes() }

        val logListJson = try {
            logListJob.await() ?: return LogListJsonFailedLoading
        } catch (e: Exception) {
            return LogListJsonFailedLoadingWithException(e)
        }

        val signature = try {
            signatureJob.await() ?: return LogListSigFailedLoading
        } catch (e: Exception) {
            return LogListSigFailedLoadingWithException(e)
        }

        return when (val signatureResult = verify(logListJson, signature, publicKey)) {
            is LogServerSignatureResult.Valid -> parseJson(logListJson)
            is LogServerSignatureResult.Invalid -> SignatureVerificationFailed(signatureResult)
        }
    }

    override suspend fun isValid(value: LogListResult?) = value is LogListResult.Valid

    override suspend fun set(value: LogListResult) = Unit

    private fun verify(message: String, signature: ByteArray, publicKey: PublicKey): LogServerSignatureResult {
        return try {
            if (Signature.getInstance("SHA256WithRSA").apply {
                    initVerify(publicKey)
                    update(message.toByteArray())
                }.verify(signature)) {
                LogServerSignatureResult.Valid
            } else {
                LogServerSignatureResult.Invalid.SignatureFailed
            }
        } catch (e: SignatureException) {
            LogServerSignatureResult.Invalid.SignatureNotValid(e)
        } catch (e: InvalidKeyException) {
            LogServerSignatureResult.Invalid.PublicKeyNotValid(e)
        } catch (e: NoSuchAlgorithmException) {
            LogServerSignatureResult.Invalid.NoSuchAlgorithm(e)
        }
    }

    private fun parseJson(logListJson: String): LogListResult {
        val logList = try {
            GsonBuilder().setLenient().create().fromJson(logListJson, LogListV2Beta::class.java)
        } catch (e: JsonParseException) {
            return JsonFormat(e)
        }

        return buildLogServerList(logList)
    }

    @Suppress("ReturnCount")
    private fun buildLogServerList(logList: LogListV2Beta): LogListResult {
        return logList.operators
            .flatMap { it.logs }
            // null, PENDING, REJECTED -> An SCT associated with this log server would be treated as untrusted
            .filterNot { it.state == null || it.state is State.Pending || it.state is State.Rejected }
            .map {
                val keyBytes = Base64.decode(it.key)

                // FROZEN, RETIRED -> Validate SCT against this if it was issued before the state timestamp, otherwise SCT is untrusted
                // QUALIFIED, USABLE -> Validate SCT against this (any timestamp okay)
                val validUntil = if (it.state is State.Retired || it.state is State.ReadOnly) it.state.timestamp else null

                val key = try {
                    PublicKeyFactory.fromByteArray(keyBytes)
                } catch (e: InvalidKeySpecException) {
                    return LogServerInvalidKey(e, it.key)
                } catch (e: NoSuchAlgorithmException) {
                    return LogServerInvalidKey(e, it.key)
                } catch (e: IllegalArgumentException) {
                    return LogServerInvalidKey(e, it.key)
                }

                LogServer(key, validUntil)
            }.let(LogListResult::Valid)
    }
}
