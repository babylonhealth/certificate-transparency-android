package org.certificatetransparency.ctlog.serialization.model

/**
 * @property UNKNOWN_VERSION Not part of the I-D, and outside the valid range.
 */
enum class Version(val number: Int) {
    V1(0),
    UNKNOWN_VERSION(256);

    companion object {
        fun forNumber(number: Int) = Version.values().firstOrNull { it.number == number } ?: UNKNOWN_VERSION
    }
}
