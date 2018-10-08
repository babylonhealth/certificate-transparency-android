package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.proto.Ct

class MerkleTreeLeaf(var version: Ct.Version, var timestampedEntry: TimestampedEntry)
