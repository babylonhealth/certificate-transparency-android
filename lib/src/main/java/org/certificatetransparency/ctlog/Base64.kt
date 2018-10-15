package org.certificatetransparency.ctlog

import org.bouncycastle.util.encoders.Base64

object Base64 {
    fun decode(data: String): ByteArray = Base64.decode(data)

    fun toBase64String(data: ByteArray?): String = Base64.toBase64String(data)
}
