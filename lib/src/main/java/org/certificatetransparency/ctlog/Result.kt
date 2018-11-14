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

import org.certificatetransparency.ctlog.loglist.LogServer
import java.io.IOException

/**
 * Abstract class providing the results of performing certificate transparency checks
 */
sealed class Result {
    /**
     * Abstract class representing certificate transparency checks passed
     */
    sealed class Success : Result() {

        /**
         * Certificate transparency checks passed as [host] is not being verified
         * @param host The host certificate transparency is not enabled for
         */
        data class DisabledForHost(val host: String) : Success() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "Success: SCT not enabled for $host"
        }

        /**
         * Certificate transparency checks passed with the provided [scts]
         * @param scts List of [SctResult] showing the results of checking each Signed Certificate Timestamp
         */
        data class Trusted(val scts: List<SctResult>) : Success() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "Success: SCT trusted logs $scts"
        }
    }

    /**
     * Abstract class representing certificate transparency checks failed
     */
    sealed class Failure : Result() {

        /**
         * Certificate transparency checks failed as no certificates are present
         */
        object NoCertificates : Failure() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "Failure: No certificates"
        }

        /**
         * Certificate transparency checks failed as no [LogServer] are present. This can occur if there are network problems loading
         * the log-list.json file
         */
        object NoLogServers : Failure() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "Failure: No log servers to check against"
        }

        /**
         * Certificate transparency checks failed as no Signed Certificate Timestamps have been found in the X.509 extensions. This can occur
         * if your server relies on providing SCTs through TLS extensions or OCSP stapling instead.
         */
        object NoScts : Failure() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "Failure: This certificate does not have any Signed Certificate Timestamps in it."
        }

        /**
         * Certificate transparency checks failed as there are not enough valid Signed Certificate Timestamps
         * @param scts List of [SctResult] stating which SCTs passed or failed checks
         * @param minSctCount The number of valid SCTs required for trust to be established
         */
        data class TooFewSctsTrusted(val scts: List<SctResult>, val minSctCount: Int) : Failure() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() =
                "Failure: Too few trusted SCTs present, required $minSctCount, found ${scts.count { it is SctResult.Valid }} in $scts"
        }

        /**
         * Certificate transparency checks failed due to an unknown [IOException]
         * @param ioException The [IOException] that occurred
         */
        data class UnknownIoException(val ioException: IOException) : Failure() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "Failure: IOException $ioException"
        }
    }
}
