package org.certificatetransparency.ctlog.domain.logclient.model

/** ParsedLogEntry data type contains an entry retrieved from Log with it's audit proof.  */
data class ParsedLogEntryWithProof(
    val parsedLogEntry: ParsedLogEntry,
    val auditProof: MerkleAuditProof
)
