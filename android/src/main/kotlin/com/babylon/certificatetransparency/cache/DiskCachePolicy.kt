package com.babylon.certificatetransparency.cache

import java.util.*

/**
 * Disk cache expiry policy
 */
interface DiskCachePolicy {
    fun isExpired(lastWriteDate: Date, currentDate: Date): Boolean
}