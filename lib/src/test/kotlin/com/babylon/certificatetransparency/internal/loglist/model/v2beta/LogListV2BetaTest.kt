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

package com.babylon.certificatetransparency.internal.loglist.model.v2beta

import com.babylon.certificatetransparency.utils.TestData
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogListV2BetaTest {

    @Test
    fun verifyGsonParser() {
        val json = TestData.file(TestData.TEST_LOG_LIST_JSON_V2_BETA).readText()

        val logList = GsonBuilder().setLenient().create().fromJson(json, LogListV2Beta::class.java)

        val google = logList.operators.first { it.name == "Google" }
        val cloudflare = logList.operators.first { it.name == "Cloudflare" }
        val certly = logList.operators.first { it.name == "Certly" }

        assertEquals(3, google.logs.size)
        assertEquals(1, cloudflare.logs.size)
        assertEquals(1, certly.logs.size)

        val xenonLog = google.logs.first { it.description == "Google 'Xenon2018' log" }
        assertEquals(
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1syJvwQdrv0a8dM2VAnK/SmHJNw/+FxC+CncFcnXMX2jNH9Xs7Q56FiV3taG5G2CokMsizhpcm7xXzuR3IHmag==",
            xenonLog.key
        )
        assertTrue(xenonLog.state is State.Pending)

        val aviatorLog = google.logs.first { it.description == "Google 'Aviator' log" }
        assertTrue(aviatorLog.state is State.ReadOnly)
        assertEquals(46466472, (aviatorLog.state as State.ReadOnly).finalTreeHead.treeSize)

        val nimbusLog = cloudflare.logs.first { it.description == "Cloudflare 'Nimbus2018' Log" }
        assertEquals(LogType.PROD, nimbusLog.logType)
        assertEquals(86400, nimbusLog.maximumMergeDelay)
        assertEquals(1534095762000, nimbusLog.state?.timestamp)
    }
}
