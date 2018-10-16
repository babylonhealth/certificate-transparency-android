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

package org.certificatetransparency.ctlog.comm

import org.certificatetransparency.ctlog.MerkleAuditProof
import org.certificatetransparency.ctlog.ParsedLogEntry
import org.certificatetransparency.ctlog.ParsedLogEntryWithProof
import org.certificatetransparency.ctlog.serialization.model.SignedCertificateTimestamp
import java.security.cert.Certificate

interface LogClient {
    /**
     * Adds a certificate to the log.
     *
     * @param certificatesChain The certificate chain to add.
     * @return SignedCertificateTimestamp if the log added the chain successfully.
     */
    fun addCertificate(certificatesChain: List<Certificate>): SignedCertificateTimestamp

    /**
     * Retrieve Entries from Log.
     *
     * @param start 0-based index of first entry to retrieve, in decimal.
     * @param end 0-based index of last entry to retrieve, in decimal.
     * @return list of Log's entries.
     */
    fun getLogEntries(start: Long, end: Long): List<ParsedLogEntry>

    /**
     * Retrieve Merkle Consistency Proof between Two Signed Tree Heads.
     *
     * @param first The tree_size of the first tree, in decimal.
     * @param second The tree_size of the second tree, in decimal.
     * @return A list of base64 decoded Merkle Tree nodes serialized to ByteString objects.
     */
    fun getSthConsistency(first: Long, second: Long): List<ByteArray>

    /**
     * Retrieve Entry+Merkle Audit Proof from Log.
     *
     * @param leafIndex The index of the desired entry.
     * @param treeSize The tree_size of the tree for which the proof is desired.
     * @return ParsedLog entry object with proof.
     */
    fun getLogEntryAndProof(leafIndex: Long, treeSize: Long): ParsedLogEntryWithProof

    /**
     * Retrieve Merkle Audit Proof from Log by Merkle Leaf Hash.
     *
     * @param leafHash sha256 hash of MerkleTreeLeaf.
     * @return MerkleAuditProof object.
     */
    fun getProofByHash(leafHash: ByteArray): MerkleAuditProof

    /**
     * Retrieve Merkle Audit Proof from Log by Merkle Leaf Hash.
     *
     * @param encodedMerkleLeafHash Base64 encoded of sha256 hash of MerkleTreeLeaf.
     * @param treeSize The tree_size of the tree for which the proof is desired. It can be obtained
     * from latest STH.
     * @return MerkleAuditProof object.
     */
    fun getProofByEncodedHash(encodedMerkleLeafHash: String, treeSize: Long): MerkleAuditProof
}
