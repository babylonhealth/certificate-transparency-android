package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.serialization.model.Version

data class MerkleTreeLeaf(val version: Version, val timestampedEntry: TimestampedEntry)
