package com.babylon.certificatetransparency.loglist

/**
 * Class representing the raw log list data
 */
sealed class RawLogListResult {

    /**
     * Class representing raw log list data loading successfully
     */
    data class Success(
            val logList: String,
            val signature: ByteArray
    ) : RawLogListResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Success

            if (logList != other.logList) return false
            if (!signature.contentEquals(other.signature)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = logList.hashCode()
            result = 31 * result + signature.contentHashCode()
            return result
        }
    }

    /**
     * Class representing raw log list data loading failed
     */
    abstract class Failure : RawLogListResult()
}
