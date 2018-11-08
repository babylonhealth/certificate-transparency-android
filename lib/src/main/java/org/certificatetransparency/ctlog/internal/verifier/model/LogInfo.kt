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
 *
 * Code derived from https://github.com/google/certificate-transparency-java
 */

package org.certificatetransparency.ctlog.internal.verifier.model

import org.certificatetransparency.ctlog.internal.utils.hash
import org.certificatetransparency.ctlog.internal.verifier.LogSignatureVerifier
import org.certificatetransparency.ctlog.loglist.LogServer
import org.certificatetransparency.ctlog.verifier.SignatureVerifier
import java.security.PublicKey

/**
 * Holds information about the log: Mainly, its public key and log ID (which is calculated from the
 * Log ID). Ideally created from a file with the Log's public key in PEM encoding.
 *
 * @constructor C'tor.
 * @property key Public key of the log.
 */
internal data class LogInfo(val key: PublicKey, override val validUntil: Long? = null) : LogServer {

    // A CT log's Id is created by hashing the CT log public key
    override val id: ByteArray = key.hash()

    fun isSameLogId(idToCheck: ByteArray): Boolean = id.contentEquals(idToCheck)

    override val signatureVerifier: SignatureVerifier by lazy {
        LogSignatureVerifier(this)
    }


    companion object
}
