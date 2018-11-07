package org.certificatetransparency.ctlog.internal.verifier.model

import org.certificatetransparency.ctlog.verifier.SctResult
import java.io.IOException

sealed class Result {
    sealed class Success : Result() {

        data class DisabledForHost(val host: String) : Success() {
            override fun toString() = "Success: SCT not enabled for $host"
        }

        data class Trusted(val scts: List<SctResult>) : Success() {
            override fun toString() = "Success: SCT trusted logs $scts"
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

        data class TooFewSctsTrusted(val scts: List<SctResult>, val minSctCount: Int) : Failure() {
            override fun toString() =
                "Failure: Too few trusted SCTs present, required $minSctCount, found ${scts.count { it is SctResult.Valid }} in $scts"
        }

        data class UnknownIoException(val ioException: IOException) : Failure() {
            override fun toString() = "Failure: IOException $ioException"
        }
    }
}
