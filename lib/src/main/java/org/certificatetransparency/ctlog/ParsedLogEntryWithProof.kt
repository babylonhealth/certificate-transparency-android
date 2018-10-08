package org.certificatetransparency.ctlog

/** ParsedLogEntry data type contains an entry retrieved from Log with it's audit proof.  */
class ParsedLogEntryWithProof private constructor(
    val parsedLogEntry: ParsedLogEntry,
    val auditProof: MerkleAuditProof
) {

    companion object {
        @JvmStatic
        fun newInstance(logEntry: ParsedLogEntry, proof: MerkleAuditProof) = ParsedLogEntryWithProof(logEntry, proof)
    }
}
