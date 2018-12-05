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
 *
 * Code derived from https://github.com/google/certificate-transparency-java
 */

package com.babylon.certificatetransparency.internal.serialization

/** Constants used for serializing and de-serializing.  */
internal object CTConstants {
    // All in bytes.
    const val MAX_EXTENSIONS_LENGTH = (1 shl 16) - 1
    const val MAX_SIGNATURE_LENGTH = (1 shl 16) - 1
    const val KEY_ID_LENGTH = 32
    const val TIMESTAMP_LENGTH = 8
    const val VERSION_LENGTH = 1
    const val LOG_ENTRY_TYPE_LENGTH = 2
    const val MAX_CERTIFICATE_LENGTH = (1 shl 24) - 1

    // Useful OIDs
    const val PRECERTIFICATE_SIGNING_OID = "1.3.6.1.4.1.11129.2.4.4"
    const val POISON_EXTENSION_OID = "1.3.6.1.4.1.11129.2.4.3"
    const val SCT_CERTIFICATE_OID = "1.3.6.1.4.1.11129.2.4.2"
}
