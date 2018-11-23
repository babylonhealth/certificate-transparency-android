package org.certificatetransparency.ctlog.internal.loglist

import org.certificatetransparency.ctlog.internal.utils.stringStackTrace
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException

internal sealed class LogServerSignatureResult {
    object Valid : LogServerSignatureResult() {
        override fun toString() = "Valid signature"
    }

    sealed class Invalid : LogServerSignatureResult() {
        object SignatureFailed : Invalid() {
            override fun toString() = "Invalid signature"
        }

        data class SignatureNotValid(val exception: SignatureException) : Invalid() {
            override fun toString() = "Invalid signature (public key) with ${exception.stringStackTrace()}"
        }

        data class PublicKeyNotValid(val exception: InvalidKeyException) : Invalid() {
            override fun toString() = "Invalid signature (public key) with ${exception.stringStackTrace()}"
        }

        data class NoSuchAlgorithm(val exception: NoSuchAlgorithmException) : Invalid() {
            override fun toString() = "Invalid signature (public key) with ${exception.stringStackTrace()}"
        }
    }
}
