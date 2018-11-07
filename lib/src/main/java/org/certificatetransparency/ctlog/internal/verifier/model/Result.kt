package org.certificatetransparency.ctlog.internal.verifier.model

import java.io.IOException

sealed class Result {
    sealed class Success : Result() {

        data class DisabledForHost(val host: String) : Success() {
            override fun toString() = "Success: SCT not enabled for $host"
        }

        data class Trusted(val trustedLogIds: List<String>) : Success() {
            override fun toString() = "Success: SCT trusted logs $trustedLogIds"
        }
    }

    sealed class Failure : Result() {

        object NoCertificates : Failure() {
            override fun toString() = "Failure: No certificates"
        }

        object NoVerifiers : Failure() {
            override fun toString() = "Failure: No verifiers to check against"
        }

        object NoScts : Failure() {
            override fun toString() = "Failure: This certificate does not have any Signed Certificate Timestamps in it."
        }

        data class TooFewSctsPresent(val sctCount: Int, val minSctCount: Int) : Failure() {
            override fun toString() = "Failure: Too few SCTs are present, I want at least $minSctCount CT logs to be nominated."
        }

        data class TooFewSctsTrusted(val trustedSctCount: Int, val minSctCount: Int) : Failure() {
            override fun toString() = "Failure: Too few trusted SCTs are present, I want at least $minSctCount trusted CT logs."
        }

        data class UnknownIoException(val ioException: IOException) : Failure() {
            override fun toString() = "Failure: IOException $ioException"
        }
    }
}
