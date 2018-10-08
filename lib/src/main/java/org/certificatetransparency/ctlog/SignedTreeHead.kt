package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.proto.Ct

class SignedTreeHead(var version: Ct.Version) {
    var timestamp: Long = 0
    var treeSize: Long = 0
    var sha256RootHash: ByteArray? = null
    var signature: Ct.DigitallySigned? = null
}
