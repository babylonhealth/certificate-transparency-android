package org.certificatetransparency.ctlog.domain.logclient.model

class MerkleAuditProof(var version: Version, var treeSize: Long, var leafIndex: Long) {
    var pathNode = mutableListOf<ByteArray>()
}
