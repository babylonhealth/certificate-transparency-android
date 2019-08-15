package com.babylon.certificatetransparency.cache

import org.junit.*
import org.junit.Assert.*
import java.util.*

class DefaultDiskCachePolicyTest {

    @Test
    fun dateDifferenceBelow24HrsReturnsFalse() {
        val now = Date()
        val calendar = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 23)
            add(Calendar.MINUTE, 55)
        }
        val offset = calendar.time

        // given a default disk cache policy
        val diskCachePolicy = DefaultDiskCachePolicy()

        // when I check the cache expiry
        val result = diskCachePolicy.isExpired(now, offset)

        // then the result is false
        assertFalse(result)
    }

    @Test
    fun dateDifferenceAbove24HrsReturnsTrue() {
        val now = Date()
        val calendar = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 24)
            add(Calendar.MINUTE, 5)
        }
        val offset = calendar.time

        // given a default disk cache policy
        val diskCachePolicy = DefaultDiskCachePolicy()

        // when I check the cache expiry
        val result = diskCachePolicy.isExpired(now, offset)

        // then the result is true
        assertTrue(result)
    }
}