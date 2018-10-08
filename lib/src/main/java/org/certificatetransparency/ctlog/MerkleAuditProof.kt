package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.proto.Ct

class MerkleAuditProof(var version: Ct.Version, var treeSize: Long, var leafIndex: Long) {
    var pathNode = mutableListOf<ByteArray>()
}
