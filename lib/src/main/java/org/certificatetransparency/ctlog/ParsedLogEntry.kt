package org.certificatetransparency.ctlog

/** ParsedLogEntry data type contains an entry retrieved from Log.  */
class ParsedLogEntry private constructor(val merkleTreeLeaf: MerkleTreeLeaf, val logEntry: LogEntry) {

    companion object {
        @JvmStatic
        fun newInstance(merkleTreeLeaf: MerkleTreeLeaf, logEntry: LogEntry) = ParsedLogEntry(merkleTreeLeaf, logEntry)
    }
}
