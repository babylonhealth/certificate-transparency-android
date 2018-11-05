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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.certificatetransparency.ctlog.datasource.DataSource
import org.certificatetransparency.ctlog.internal.loglist.model.v2beta.LogListV2Beta
import org.certificatetransparency.ctlog.internal.loglist.model.v2beta.State
import org.certificatetransparency.ctlog.internal.utils.Base64
import org.certificatetransparency.ctlog.internal.utils.PublicKeyFactory
import org.certificatetransparency.ctlog.internal.verifier.model.LogInfo
import org.certificatetransparency.ctlog.loglist.LogServer
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.spec.InvalidKeySpecException

// Collection of CT logs that are trusted for the purposes of this test from https://www.gstatic.com/ct/log_list/log_list.json
internal class LogListNetworkDataSourceV2(
    private val logService: LogListService,
    private val publicKey: PublicKey = GoogleLogListPublicKey
) : DataSource<List<LogServer>> {

    override val coroutineContext = GlobalScope.coroutineContext

    override suspend fun get(): List<LogServer>? {
        try {
            val logListJob = async { logService.getLogList().execute().body()?.string() }
            val signatureJob = async { logService.getLogListSignature().execute().body()?.bytes() }

            val logListJson = requireNotNull(logListJob.await())
            val signature = requireNotNull(signatureJob.await())

            if (verify(logListJson, signature, publicKey)) {
                val logList = GsonBuilder().setLenient().create().fromJson(logListJson, LogListV2Beta::class.java)
                return buildLogServerList(logList)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            println("Exception loading log-list.json from network. ${e.message}")
        }

        return null
    }

    override suspend fun set(value: List<LogServer>) = Unit

    private fun verify(message: String, signature: ByteArray, publicKey: PublicKey): Boolean {
        return try {
            Signature.getInstance("SHA256WithRSA").apply {
                initVerify(publicKey)
                update(message.toByteArray())
            }.verify(signature)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            println("Exception loading signature")
            e.printStackTrace()
            false
        }
    }

    /**
     * Construct LogSignatureVerifiers for each of the trusted CT logs.
     *
     * @throws InvalidKeySpecException the CT log key isn't RSA or EC, the key is probably corrupt.
     * @throws NoSuchAlgorithmException the crypto provider couldn't supply the hashing algorithm
     * or the key algorithm. This probably means you are using an ancient or bad crypto provider.
     */
    @Throws(InvalidKeySpecException::class, NoSuchAlgorithmException::class)
    private fun buildLogServerList(logList: LogListV2Beta): List<LogServer> {
        return logList.operators
            .flatMap { it.value.logs.values }
            // null, PENDING, REJECTED -> An SCT associated with this log server would be treated as untrusted
            .filterNot { it.state == null || it.state is State.Pending || it.state is State.Rejected }
            .map {
                val key = Base64.decode(it.key)

                // FROZEN, RETIRED -> Validate SCT against this if it was issued before the state timestamp, otherwise SCT is untrusted
                // QUALIFIED, USABLE -> Validate SCT against this (any timestamp okay)
                val validUntil = if (it.state is State.Retired || it.state is State.Frozen) it.state.timestamp else null

                LogInfo(PublicKeyFactory.fromByteArray(key), validUntil)
            }
    }
}

