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

package org.certificatetransparency.ctlog.internal.logclient.model

internal sealed class SignedEntry {

    data class X509(val x509: ByteArray) : SignedEntry() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as X509

            if (!x509.contentEquals(other.x509)) return false

            return true
        }

        override fun hashCode(): Int {
            return x509.contentHashCode()
        }
    }

    data class PreCertificate(val preCertificate: org.certificatetransparency.ctlog.internal.logclient.model.PreCertificate) : SignedEntry()
}
