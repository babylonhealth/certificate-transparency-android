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

import com.babylon.certificatetransparency.internal.loglist.LogListJsonBadFormat
import com.babylon.certificatetransparency.internal.loglist.LogServerInvalidKey
import com.babylon.certificatetransparency.internal.loglist.model.LogList
import com.babylon.certificatetransparency.internal.utils.Base64
import com.babylon.certificatetransparency.internal.utils.PublicKeyFactory
import com.babylon.certificatetransparency.loglist.LogListResult
import com.babylon.certificatetransparency.loglist.LogServer
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException

internal class LogListJsonParserV1 : LogListJsonParser {

    override fun parseJson(logListJson: String): LogListResult {
        val logList = try {
            GsonBuilder().setLenient().create().fromJson(logListJson, LogList::class.java)
        } catch (e: JsonParseException) {
            return LogListJsonBadFormat(e)
        }

        return buildLogServerList(logList)
    }

    @Suppress("ReturnCount")
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
