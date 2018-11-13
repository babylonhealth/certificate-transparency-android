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

package org.certificatetransparency.ctlog

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
