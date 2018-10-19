/*
 * Copyright 2018 Babylon Healthcare Services Limited
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

package org.certificatetransparency.ctlog.logclient.model

/**
 * @property timestamp UTC time in milliseconds, since January 1, 1970, 00:00.
 */
data class SignedCertificateTimestamp(
    val version: Version = Version.UNKNOWN_VERSION,
    val id: LogId,
    val timestamp: Long,
    val signature: DigitallySigned,
    val extensions: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignedCertificateTimestamp

        if (version != other.version) return false
        if (id != other.id) return false
        if (timestamp != other.timestamp) return false
        if (signature != other.signature) return false
        if (!extensions.contentEquals(other.extensions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + signature.hashCode()
        result = 31 * result + extensions.contentHashCode()
        return result
    }
}
