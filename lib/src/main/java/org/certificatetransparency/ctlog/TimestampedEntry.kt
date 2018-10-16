package org.certificatetransparency.ctlog

data class TimestampedEntry(
    val timestamp: Long = 0,
    val signedEntry: SignedEntry
)
