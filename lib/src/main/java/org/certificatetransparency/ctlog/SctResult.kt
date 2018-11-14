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

/**
 * Abstract class providing the results of verifying a Signed Certificate Timestamp
 */
sealed class SctResult {
    /**
     * Signed Certificate Timestamp checks passed
     */
    object Valid : SctResult()

    /**
     * Abstract class representing Signed Certificate Timestamp checks failed
     */
    sealed class Invalid : SctResult() {
        /**
         * Signed Certificate Timestamp checks failed as the signature could not be verified
         */
        object FailedVerification : Invalid()

        /**
         * Signed Certificate Timestamp checks failed as there was no log server we trust in the log-list.json
         */
        object NoLogServerFound : Invalid()

        /**
         * Signed Certificate Timestamp checks failed as the [timestamp] of the SCT is in the future
         * @param timestamp The timestamp of the SCT
         * @param now The time now
         */
        data class FutureTimestamp(val timestamp: Long, val now: Long) : Invalid() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "SCT timestamp, $timestamp, is in the future, current timestamp is $now."
        }

        /**
         * Signed Certificate Timestamp checks failed as the log server is no longer trusted
         * @param timestamp The timestamp of the SCT
         * @param logServerValidUntil The time the log server was valid till
         */
        data class LogServerUntrusted(val timestamp: Long, val logServerValidUntil: Long) : Invalid() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "SCT timestamp, $timestamp, is greater than the log server validity, $logServerValidUntil."
        }

        /**
         * Signed Certificate Timestamp checks failed for an unspecified reason
         */
        abstract class Generic : Invalid()

        /**
         * Signed Certificate Timestamp checks failed as an [exception] was detected
         */
        abstract class Exception : Invalid() {
            /**
             * The [Exception] that occurred
             */
            abstract val exception: kotlin.Exception?
        }
    }
}
