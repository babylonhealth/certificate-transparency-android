package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.serialization.model.Version

class MerkleTreeLeaf(var version: Version, var timestampedEntry: TimestampedEntry)
