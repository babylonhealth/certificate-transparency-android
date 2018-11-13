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

package org.certificatetransparency.ctlog.verifier

sealed class SctResult {
    object Valid : SctResult()


    sealed class Invalid : SctResult() {
        object FailedVerification : Invalid()

        object NoVerifierFound : Invalid()

        data class FutureTimestamp(val timestamp: Long, val now: Long) : Invalid() {
            override fun toString() = "SCT timestamp, $timestamp, is in the future, current timestamp is $now."
        }

        data class LogServerUntrusted(val timestamp: Long, val logServerValidUntil: Long) : Invalid() {
            override fun toString() = "SCT timestamp, $timestamp, is greater than the log server validity, $logServerValidUntil."
        }

        abstract class Generic : Invalid()

        abstract class Exception : Invalid() {
            abstract val exception: kotlin.Exception?
        }
    }
}
