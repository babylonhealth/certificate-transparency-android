package com.babylon.certificaterevocation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.babylon.certificatetransparency.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog

@RunWith(AndroidJUnit4::class)
class BasicAndroidCRLoggerTest {

    @Test
    fun logsInDebugMode() {
        if (BuildConfig.DEBUG) {
            // given a basic logger
            val logger = BasicAndroidCRLogger()

            // when we log
            logger.log("a.b.c", RevocationResult.Failure.NoCertificates)

            // then a message is output
            assertEquals("a.b.c Failure: No certificates", logEntries.first().msg)
        }
    }

    @Test
    fun nothingIsLoggedInReleaseMode() {
        if (!BuildConfig.DEBUG) {
            // given a basic logger
            val logger = BasicAndroidCRLogger()

            // when we log
            logger.log("a.b.c", RevocationResult.Failure.NoCertificates)

            // then nothing is output
            assertEquals(0, logEntries.size)
        }
    }

    private val logEntries
        get() = ShadowLog.getLogsForTag("CertificateRevocation")
}
