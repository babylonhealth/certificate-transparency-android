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

package org.certificatetransparency.ctlog.internal.verifier.model

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.Extension

internal data class IssuerInformation(
    val name: X500Name? = null,
    val keyHash: ByteArray,
    val x509authorityKeyIdentifier: Extension? = null,
    val issuedByPreCertificateSigningCert: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IssuerInformation

        if (name != other.name) return false
        if (!keyHash.contentEquals(other.keyHash)) return false
        if (x509authorityKeyIdentifier != other.x509authorityKeyIdentifier) return false
        if (issuedByPreCertificateSigningCert != other.issuedByPreCertificateSigningCert) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + keyHash.contentHashCode()
        result = 31 * result + (x509authorityKeyIdentifier?.hashCode() ?: 0)
        result = 31 * result + issuedByPreCertificateSigningCert.hashCode()
        return result
    }
}
