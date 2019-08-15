/*
 * Copyright 2019 Babylon Partners Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
