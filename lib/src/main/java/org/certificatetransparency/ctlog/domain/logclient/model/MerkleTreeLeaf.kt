package org.certificatetransparency.ctlog.domain.logclient.model

data class MerkleTreeLeaf(
    val version: Version,
    val timestampedEntry: TimestampedEntry
)
