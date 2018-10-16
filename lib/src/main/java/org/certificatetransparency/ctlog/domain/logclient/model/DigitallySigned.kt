package org.certificatetransparency.ctlog.domain.logclient.model

data class DigitallySigned(
    val hashAlgorithm: HashAlgorithm = HashAlgorithm.NONE,
    val signatureAlgorithm: SignatureAlgorithm = SignatureAlgorithm.ANONYMOUS,
    val signature: ByteArray
) {
    enum class HashAlgorithm(val number: Int) {
        NONE(0),
        MD5(1),
        SHA1(2),
        SHA224(3),
        SHA256(4),
        SHA384(5),
        SHA512(6);

        companion object {
            fun forNumber(number: Int) = HashAlgorithm.values().firstOrNull { it.number == number }
        }
    }

    enum class SignatureAlgorithm(val number: Int) {
        ANONYMOUS(0),
        RSA(1),
        DSA(2),
        ECDSA(3);

        companion object {
            fun forNumber(number: Int) = SignatureAlgorithm.values().firstOrNull { it.number == number }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DigitallySigned

        if (hashAlgorithm != other.hashAlgorithm) return false
        if (signatureAlgorithm != other.signatureAlgorithm) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hashAlgorithm.hashCode()
        result = 31 * result + signatureAlgorithm.hashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}
