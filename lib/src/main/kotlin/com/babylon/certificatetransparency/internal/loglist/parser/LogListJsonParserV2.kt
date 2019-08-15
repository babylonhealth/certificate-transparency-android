package com.babylon.certificatetransparency.internal.loglist.parser

import com.babylon.certificatetransparency.internal.loglist.*
import com.babylon.certificatetransparency.internal.loglist.model.v2beta.*
import com.babylon.certificatetransparency.internal.utils.*
import com.babylon.certificatetransparency.loglist.*
import com.google.gson.*
import java.security.*
import java.security.spec.*

internal class LogListJsonParserV2 : LogListJsonParser {

    override fun parseJson(logListJson: String): LogListResult {
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