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

import com.babylon.certificatetransparency.internal.loglist.JsonFormat
import com.babylon.certificatetransparency.internal.utils.Base64
import com.babylon.certificatetransparency.loglist.LogListResult
import com.babylon.certificatetransparency.utils.TestData
import com.babylon.certificatetransparency.utils.assertIsA
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LogListJsonParserV2Test {

    @Test
    fun `verifies signature`() = runBlocking {
        // given we have a valid json file and signature

        // when we ask for data
        val result = LogListJsonParserV2().parseJson(json)

        // then 3 items are returned (many ignored as invalid states)
        require(result is LogListResult.Valid)
        assertEquals(3, result.servers.size)
        assertEquals("aPaY+B9kgr46jO65KB1M/HFRXWeT1ETRCmesu09P+8Q=", Base64.toBase64String(result.servers[0].id))
    }

    @Test
    fun `returns Invalid if json incomplete`() = runBlocking {
        // given we have a valid json file and signature

        // when we ask for data
        val result = LogListJsonParserV2().parseJson(jsonIncomplete)

        // then invalid is returned
        assertIsA<JsonFormat>(result)
    }

    @Test
    fun `validUntil null when not frozen or retired`() = runBlocking {
        // given we have a valid json file and signature

        // when we ask for data
        val result = LogListJsonParserV2().parseJson(json)

        // then validUntil is set to the the STH timestamp
        require(result is LogListResult.Valid)
        val logServer = result.servers[1]
        assertNull(logServer.validUntil)
    }

    @Test
    fun `validUntil set from Frozen`() = runBlocking {
        // given we have a valid json file and signature

        // when we ask for data
        val result = LogListJsonParserV2().parseJson(json)

        // then validUntil is set to the the STH timestamp
        require(result is LogListResult.Valid)
        val logServer = result.servers[0]
        assertNotNull(logServer.validUntil)
        assertEquals(1480424940000, logServer.validUntil)
    }

    @Test
    fun `validUntil set from Retired`() = runBlocking {
        // given we have a valid json file and signature

        // when we ask for data
        val result = LogListJsonParserV2().parseJson(json)

        // then validUntil is set to the the STH timestamp
        require(result is LogListResult.Valid)
        val logServer = result.servers[2]
        assertNotNull(logServer.validUntil)
        assertEquals(1460678400000, logServer.validUntil)
    }

    companion object {
        private val json = TestData.file(TestData.TEST_LOG_LIST_JSON_V2_BETA).readText()
        private val jsonIncomplete = TestData.file(TestData.TEST_LOG_LIST_JSON_INCOMPLETE).readText()
    }
}