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

package org.certificatetransparency.ctlog.internal.verifier

import org.certificatetransparency.ctlog.internal.utils.stringStackTrace
import org.certificatetransparency.ctlog.verifier.SctResult
import java.io.IOException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.security.cert.CertificateParsingException

internal data class UnsupportedSignatureAlgorithm(
    val algorithm: String,
    override val exception: NoSuchAlgorithmException? = null
) : SctResult.Invalid.Exception() {

    override fun toString() = if (exception != null) {
        "Unsupported signature algorithm $algorithm with: ${exception.stringStackTrace()}"
    } else {
        "Unsupported signature algorithm $algorithm"
    }
}

internal data class LogPublicKeyNotValid(override val exception: InvalidKeyException) : SctResult.Invalid.Exception() {
    override fun toString() = "Log's public key cannot be used with ${exception.stringStackTrace()}"
}

internal data class SignatureNotValid(override val exception: SignatureException) : SctResult.Invalid.Exception() {
    override fun toString() =
        "Signature object not properly initialized or signature from SCT is improperly encoded with: ${exception.stringStackTrace()}"
}

internal data class LogIdMismatch(val sctLogId: String, val logServerId: String) : SctResult.Invalid.Generic() {
    override fun toString() = "Log ID of SCT, $sctLogId, does not match this log's ID, $logServerId"
}

internal object NoIssuer : SctResult.Invalid.Generic() {
    override fun toString() = "Chain with PreCertificate or Certificate must contain issuer"
}

internal object NoIssuerWithPreCert : SctResult.Invalid.Generic() {
    override fun toString() = "Chain with PreCertificate signed by PreCertificate Signing Cert must contain issuer"
}

internal data class CertificateEncodingFailed(override val exception: kotlin.Exception) : SctResult.Invalid.Exception() {
    override fun toString() = "Certificate could not be encoded with: ${exception.stringStackTrace()}"
}


internal data class CertificateParsingFailed(override val exception: CertificateParsingException) : SctResult.Invalid.Exception() {
    override fun toString() = "Error parsing cert with: ${exception.stringStackTrace()}"
}

internal data class ASN1ParsingFailed(override val exception: IOException) : SctResult.Invalid.Exception() {
    override fun toString() = "Error during ASN.1 parsing of certificate with: ${exception.stringStackTrace()}"
}
