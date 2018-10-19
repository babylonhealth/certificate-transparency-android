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

data class PreCertificate(
    val issuerKeyHash: ByteArray? = null,
    val tbsCertificate: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreCertificate

        if (issuerKeyHash != null) {
            if (other.issuerKeyHash == null) return false
            if (!issuerKeyHash.contentEquals(other.issuerKeyHash)) return false
        } else if (other.issuerKeyHash != null) return false
        if (tbsCertificate != null) {
            if (other.tbsCertificate == null) return false
            if (!tbsCertificate.contentEquals(other.tbsCertificate)) return false
        } else if (other.tbsCertificate != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = issuerKeyHash?.contentHashCode() ?: 0
        result = 31 * result + (tbsCertificate?.contentHashCode() ?: 0)
        return result
    }
}
