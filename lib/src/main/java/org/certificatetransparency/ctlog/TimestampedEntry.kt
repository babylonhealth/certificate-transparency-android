package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.proto.Ct

class TimestampedEntry {
    var timestamp: Long = 0
    var entryType: Ct.LogEntryType? = null
    var signedEntry: SignedEntry? = null
}
