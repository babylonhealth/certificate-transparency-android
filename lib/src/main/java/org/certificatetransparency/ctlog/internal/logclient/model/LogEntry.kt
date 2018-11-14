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

internal sealed class LogEntry {
    /**
     * @property leafCertificate [leafCertificate] is the end-entity certificate submitted for auditing. For V1 this entry just includes the
     * certificate in the [leafCertificate] field
     * @property certificateChain [certificateChain] is a chain of additional certificates required to verify the end-entity certificate.  The
     * first certificate MUST certify the end-entity certificate.  Each following certificate MUST directly certify the one preceding it.  The
     * final certificate MUST be a root certificate accepted by the log. A chain from the leaf to a trusted root (excluding leaf and possibly
     * root).
     */
    data class X509ChainEntry(
        val leafCertificate: ByteArray? = null,
        val certificateChain: List<ByteArray> = emptyList()
    ) : LogEntry() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as X509ChainEntry

            if (leafCertificate != null) {
                if (other.leafCertificate == null) return false
                if (!leafCertificate.contentEquals(other.leafCertificate)) return false
            } else if (other.leafCertificate != null) return false
            if (certificateChain != other.certificateChain) return false

            return true
        }

        override fun hashCode(): Int {
            var result = leafCertificate?.contentHashCode() ?: 0
            result = 31 * result + certificateChain.hashCode()
            return result
        }
    }

    /**
     * @property preCertificateChain [preCertificateChain] is a chain of additional certificates required to verify the Precertificate
     * submission.  The first certificate MAY be a valid Precertificate Signing Certificate and MUST certify the first certificate.  Each
     * following certificate MUST directly certify the one preceding it.  The final certificate MUST be a root certificate accepted by the log.
     * @property preCertificate [preCertificate] is the [PreCertificate] submitted for auditing.
     */
    data class PreCertificateChainEntry(
        // The chain certifying the pre-certificate, as submitted by the CA.
        val preCertificateChain: List<ByteArray> = emptyList(),

        // Pre-certificate input to the SCT. Can be computed from the above.
        // Store it alongside the entry data so that the signers don't have to parse certificates to recompute it.
        val preCertificate: PreCertificate
    ) : LogEntry()
}
