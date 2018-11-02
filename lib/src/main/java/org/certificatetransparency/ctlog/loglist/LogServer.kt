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

package org.certificatetransparency.ctlog.loglist

import org.certificatetransparency.ctlog.verifier.SignatureVerifier

/**
 * @property validUntil Timestamp denoting when a log server is valid until, or null if it is valid
 * for all time
 */
interface LogServer {
    val id: ByteArray
    val signatureVerifier: SignatureVerifier
    val validUntil: Long?
}
