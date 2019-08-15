package com.babylon.certificatetransparency.internal.loglist.parser

import com.babylon.certificatetransparency.internal.loglist.*
import com.babylon.certificatetransparency.internal.loglist.model.*
import com.babylon.certificatetransparency.internal.utils.*
import com.babylon.certificatetransparency.loglist.*
import com.google.gson.*
import java.security.*
import java.security.spec.*

internal class LogListJsonParserV1 : LogListJsonParser {

    override fun parseJson(logListJson: String): LogListResult {
        val logList = try {
            GsonBuilder().setLenient().create().fromJson(logListJson, LogList::class.java)
        } catch (e: JsonParseException) {
            return JsonFormat(e)
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