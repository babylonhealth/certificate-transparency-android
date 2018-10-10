package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.serialization.model.LogEntryType

class TimestampedEntry {
    var timestamp: Long = 0
    var entryType: LogEntryType? = null
    var signedEntry: SignedEntry? = null
}
