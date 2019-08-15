package com.babylon.certificatetransparency.cache

import java.util.*

/**
 * A default disk cache expiry policy. The log list expires after 24 hours since it was last saved.
 */
class DefaultDiskCachePolicy : DiskCachePolicy {

    override fun isExpired(lastWriteDate: Date, currentDate: Date): Boolean {
        val expiryCalendar = Calendar.getInstance().apply {
            time = lastWriteDate
            add(Calendar.DAY_OF_MONTH, 1)
        }

        return currentDate.after(expiryCalendar.time)
    }
}