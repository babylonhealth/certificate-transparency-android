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

package org.certificatetransparency.ctlog.internal.loglist.model.v2beta

import com.google.gson.GsonBuilder
import org.certificatetransparency.ctlog.utils.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogListV2BetaTest {

    @Test
    fun verifyGsonParser() {
        val json = TestData.file(TestData.TEST_LOG_LIST_JSON_V2_BETA).readText()

        val logList = GsonBuilder().setLenient().create().fromJson(json, LogListV2Beta::class.java)


        assertEquals(3, logList.operators["Google"]!!.logs.size)
        assertEquals(1, logList.operators["Cloudflare"]!!.logs.size)
        assertEquals(1, logList.operators["Certly"]!!.logs.size)

        val xenonLog = logList.operators["Google"]!!.logs["google_xenon2018"]!!
        assertEquals(
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1syJvwQdrv0a8dM2VAnK/SmHJNw/+FxC+CncFcnXMX2jNH9Xs7Q56FiV3taG5G2CokMsizhpcm7xXzuR3IHmag==",
            xenonLog.key
        )
        assertTrue(xenonLog.state is State.Pending)

        val aviatorLog = logList.operators["Google"]!!.logs["google_aviator"]!!
        assertTrue(aviatorLog.state is State.Frozen)
        assertEquals(46466472, (aviatorLog.state as State.Frozen).finalTreeHead.treeSize)


        val nimbusLog = logList.operators["Cloudflare"]!!.logs["cloudflare_nimbus2018"]!!
        assertEquals(LogType.PROD, nimbusLog.logType)
        assertEquals(86400, nimbusLog.maximumMergeDelay)
    }
}
