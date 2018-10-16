package org.certificatetransparency.ctlog.domain.logclient.model

data class TimestampedEntry(
    val timestamp: Long = 0,
    val signedEntry: SignedEntry
)
