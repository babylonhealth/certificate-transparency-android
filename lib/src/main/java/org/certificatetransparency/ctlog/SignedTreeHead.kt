package org.certificatetransparency.ctlog

import org.certificatetransparency.ctlog.serialization.model.DigitallySigned
import org.certificatetransparency.ctlog.serialization.model.Version

class SignedTreeHead(var version: Version) {
    var timestamp: Long = 0
    var treeSize: Long = 0
    var sha256RootHash: ByteArray? = null
    var signature: DigitallySigned? = null
}
