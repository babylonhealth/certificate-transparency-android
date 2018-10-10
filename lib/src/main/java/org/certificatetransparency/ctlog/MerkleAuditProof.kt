package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.serialization.model.Version

class MerkleAuditProof(var version: Version, var treeSize: Long, var leafIndex: Long) {
    var pathNode = mutableListOf<ByteArray>()
}
