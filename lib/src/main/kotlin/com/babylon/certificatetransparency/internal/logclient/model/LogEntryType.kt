/*
 * Copyright 2019 Babylon Partners Limited
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

package com.babylon.certificatetransparency.internal.logclient.model

/**
 * [LogEntryType] is the type of this entry. Future revisions of this protocol version may add new LogEntryType values.
 * @property UNKNOWN_ENTRY_TYPE Not part of the I-D, and outside the valid range.
 */
// Numbers part of specification
@Suppress("MagicNumber")
internal enum class LogEntryType(val number: Int) {
    /**
     * Type specifying [LogEntry] is [LogEntry.X509ChainEntry]
     */
    X509_ENTRY(0),

    /**
     * Type specifying [LogEntry] is [LogEntry.PreCertificateChainEntry]
     */
    PRE_CERTIFICATE_ENTRY(1),
    UNKNOWN_ENTRY_TYPE(65536);

    companion object {
        fun forNumber(number: Int) = values().firstOrNull { it.number == number } ?: UNKNOWN_ENTRY_TYPE
    }
}
