/*
 * Copyright 2018 Babylon Healthcare Services Limited
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

package org.certificatetransparency.ctlog.internal.loglist

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.certificatetransparency.ctlog.datasource.DataSource
import org.certificatetransparency.ctlog.internal.loglist.model.LogList
import org.certificatetransparency.ctlog.internal.utils.Base64
import org.certificatetransparency.ctlog.internal.utils.PublicKeyFactory
import org.certificatetransparency.ctlog.loglist.LogListResult
import org.certificatetransparency.ctlog.loglist.LogServer
import java.io.IOException
import java.security.*
import java.security.spec.InvalidKeySpecException

// Collection of CT logs that are trusted for the purposes of this test from https://www.gstatic.com/ct/log_list/log_list.json
internal class LogListNetworkDataSource(
    private val logService: LogListService,
    private val publicKey: PublicKey = GoogleLogListPublicKey
) : DataSource<LogListResult> {

    override val coroutineContext = GlobalScope.coroutineContext

    override suspend fun get(): LogListResult {
        val logListJob = async { logService.getLogList().execute().body()?.string() }
        val signatureJob = async { logService.getLogListSignature().execute().body()?.bytes() }

        val logListJson = try {
            logListJob.await() ?: return LogListJsonFailedLoading
        } catch (e: IOException) {
            return LogListJsonFailedLoadingWithException(e)
        }

        val signature = try {
            signatureJob.await() ?: return LogListSigFailedLoading
        } catch (e: IOException) {
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
            GsonBuilder().setLenient().create().fromJson(logListJson, LogList::class.java)
        } catch (e: JsonParseException) {
            return JsonFormat(e)
        }

        return buildLogServerList(logList)
    }

    private fun buildLogServerList(logList: LogList): LogListResult {
        return logList.logs.map {
            val keyBytes = Base64.decode(it.key)
            val validUntil = it.disqualifiedAt ?: it.finalSignedTreeHead?.timestamp

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
