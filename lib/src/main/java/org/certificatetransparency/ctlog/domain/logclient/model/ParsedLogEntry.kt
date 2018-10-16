package org.certificatetransparency.ctlog.domain.logclient.model

/** ParsedLogEntry data type contains an entry retrieved from Log.  */
data class ParsedLogEntry(
    val merkleTreeLeaf: MerkleTreeLeaf,
    val logEntry: LogEntry
)
