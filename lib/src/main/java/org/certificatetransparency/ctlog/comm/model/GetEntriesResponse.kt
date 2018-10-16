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

package org.certificatetransparency.ctlog.comm.model

import com.google.gson.annotations.SerializedName
import org.certificatetransparency.ctlog.comm.model.GetEntriesResponse.Entry

/**
 * Note that this message is not signed -- the retrieved data can be verified by constructing the Merkle Tree Hash corresponding to a
 * retrieved STH.  All leaves MUST be v1.  However, a compliant v1 client MUST NOT construe an unrecognized MerkleLeafType or LogEntryType
 * value as an error.  This means it may be unable to parse some entries, but note that each client can inspect the entries it does recognize
 * as well as verify the integrity of the data by treating unrecognized leaves as opaque input to the tree.
 *
 * @property entries An array of [Entry] objects
 */
data class GetEntriesResponse(
    @SerializedName("entries") val entries: List<Entry>
) {
    /**
     * @property leafInput The base64-encoded MerkleTreeLeaf structure.
     * @property extraData The base64-encoded unsigned data pertaining to the log entry.  In the case of an
     * [org.certificatetransparency.ctlog.LogEntry.X509], this is the "certificate_chain".  In the case of a
     * [org.certificatetransparency.ctlog.LogEntry.PreCertificate], this is the whole "PreCertificateChainEntry".
     */
    data class Entry(
        @SerializedName("leaf_input") val leafInput: String,
        @SerializedName("extra_data") val extraData: String
    )
}
