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

package com.babylon.certificatetransparency

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog

@RunWith(AndroidJUnit4::class)
class BasicAndroidCTLoggerTest {

    @Test
    fun logsInDebugMode() {
        // given a basic logger
        val logger = BasicAndroidCTLogger(true)

        // when we log
        logger.log("a.b.c", VerificationResult.Failure.NoCertificates)

        // then a message is output
        assertEquals("a.b.c Failure: No certificates", logEntries.first().msg)
    }

    @Test
    fun nothingLoggedInReleaseMode() {
        // given a basic logger
        val logger = BasicAndroidCTLogger(false)

        // when we log
        logger.log("a.b.c", VerificationResult.Failure.NoCertificates)

        // then nothing is output
        assertEquals(0, logEntries.size)
    }

    private val logEntries
        get() = ShadowLog.getLogsForTag("CertificateTransparency")

}
