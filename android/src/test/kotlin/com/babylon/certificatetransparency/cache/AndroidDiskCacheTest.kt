package com.babylon.certificatetransparency.cache

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.babylon.certificatetransparency.loglist.RawLogListResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class AndroidDiskCacheTest {

    @Test
    fun cachedLogListRetrievedWithinExpiryDate() {
        val result = RawLogListResult.Success("bogo", ByteArray(10) {
            it.toByte()
        })

        // given a disk cache with a non-expiring cache policy
        val diskCache = AndroidDiskCache(
            ApplicationProvider.getApplicationContext(),
            givenDiskCachePolicy(false)
        )

        // when I write the result
        runBlocking {
            diskCache.set(result)
        }

        // and I read the result back
        val actual = runBlocking {
            diskCache.get()
        }

        // then the retrieved result matches the original
        assertEquals(result, actual)
    }

    @Test
    fun cachedLogListNotRetrievedOverExpiryDate() {
        val result = RawLogListResult.Success("bogo", ByteArray(10) {
            it.toByte()
        })

        // given a disk cache with an always-expired cache policy
        val diskCache = AndroidDiskCache(
            ApplicationProvider.getApplicationContext(),
            givenDiskCachePolicy(true)
        )

        // when I write the result
        runBlocking {
            diskCache.set(result)
        }

        // and I read the result back
        val actual = runBlocking {
            diskCache.get()
        }

        // then the retrieved result is null
        assertEquals(null, actual)
    }

    private fun givenDiskCachePolicy(expiring: Boolean) = object : DiskCachePolicy {
        override fun isExpired(lastWriteDate: Date, currentDate: Date): Boolean {
            return expiring
        }
    }
}