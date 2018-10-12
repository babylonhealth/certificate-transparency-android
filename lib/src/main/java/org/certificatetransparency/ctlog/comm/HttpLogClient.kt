package org.certificatetransparency.ctlog.comm

import org.bouncycastle.util.encoders.Base64
import org.certificatetransparency.ctlog.CertificateTransparencyException
import org.certificatetransparency.ctlog.MerkleAuditProof
import org.certificatetransparency.ctlog.ParsedLogEntry
import org.certificatetransparency.ctlog.ParsedLogEntryWithProof
import org.certificatetransparency.ctlog.SignedTreeHead
import org.certificatetransparency.ctlog.comm.model.AddChainRequest
import org.certificatetransparency.ctlog.comm.model.AddChainResponse
import org.certificatetransparency.ctlog.comm.model.GetEntriesResponse
import org.certificatetransparency.ctlog.comm.model.GetRootsResponse
import org.certificatetransparency.ctlog.comm.model.GetSthConsistencyResponse
import org.certificatetransparency.ctlog.comm.model.GetSthResponse
import org.certificatetransparency.ctlog.isPreCertificate
import org.certificatetransparency.ctlog.isPreCertificateSigningCert
import org.certificatetransparency.ctlog.serialization.Deserializer
import org.certificatetransparency.ctlog.serialization.model.LogID
import org.certificatetransparency.ctlog.serialization.model.SignedCertificateTimestamp
import org.certificatetransparency.ctlog.serialization.model.Version
import java.io.ByteArrayInputStream
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory

/** A CT HTTP client. Abstracts away the json encoding necessary for the server.
 *
 * @constructor New HttpLogClient.
 * @property ctService CtService pointing to CT Log's full URL, e.g. "http://ct.googleapis.com/pilot/ct/v1/"
 */
class HttpLogClient(private val ctService: CtService) {

    /**
     * Retrieves Latest Signed Tree Head from the log. The signature of the Signed Tree Head component
     * is not verified.
     *
     * @return latest STH
     */
    val logSTH: SignedTreeHead
        get() {
            val response = ctService.getSth().execute()?.body()!!
            return parseSTHResponse(response)
        }

    /**
     * Retrieves accepted Root Certificates.
     *
     * @return a list of root certificates.
     */
    val logRoots: List<Certificate>
        get() {
            val response = ctService.getRoots().execute()?.body()!!
            return parseRootCertsResponse(response)
        }

    /**
     * JSON-encodes the list of certificates into a JSON object.
     *
     * @param certs Certificates to encode.
     * @return A JSON object with one field, "chain", holding a JSON array of base64-encoded certs.
     */
    internal fun encodeCertificates(certs: List<Certificate>): AddChainRequest {
        try {
            return AddChainRequest(certs.map {
                Base64.toBase64String(it.encoded)
            })
        } catch (e: CertificateEncodingException) {
            throw CertificateTransparencyException("Error encoding certificate", e)
        }
    }

    /**
     * Adds a certificate to the log.
     *
     * @param certificatesChain The certificate chain to add.
     * @return SignedCertificateTimestamp if the log added the chain successfully.
     */
    fun addCertificate(certificatesChain: List<Certificate>): SignedCertificateTimestamp? {
        require(!certificatesChain.isEmpty()) { "Must have at least one certificate to submit." }

        val isPreCertificate = certificatesChain[0].isPreCertificate()
        if (isPreCertificate && certificatesChain[1].isPreCertificateSigningCert()) {
            require(certificatesChain.size >= 3) {
                "When signing a PreCertificate with a PreCertificate Signing Cert, the issuer certificate must follow."
            }
        }

        return addCertificate(certificatesChain, isPreCertificate)
    }

    private fun addCertificate(
        certificatesChain: List<Certificate>, isPreCertificate: Boolean): SignedCertificateTimestamp? {
        val jsonPayload = encodeCertificates(certificatesChain)//.toJSONString()

        //val methodPath = if (isPreCertificate) ADD_PRE_CHAIN_PATH else ADD_CHAIN_PATH

        val call = if (isPreCertificate) ctService.addPreChain(jsonPayload) else ctService.addChain(jsonPayload)

        val response = call.execute()?.body()!!


        return parseServerResponse(response)
    }

    /**
     * Retrieve Entries from Log.
     *
     * @param start 0-based index of first entry to retrieve, in decimal.
     * @param end 0-based index of last entry to retrieve, in decimal.
     * @return list of Log's entries.
     */
    fun getLogEntries(start: Long, end: Long): List<ParsedLogEntry> {
        require(start in 0..end)

        val response = ctService.getEntries(start, end).execute()?.body()!!
        return parseLogEntries(response)
    }

    /**
     * Retrieve Merkle Consistency Proof between Two Signed Tree Heads.
     *
     * @param first The tree_size of the first tree, in decimal.
     * @param second The tree_size of the second tree, in decimal.
     * @return A list of base64 decoded Merkle Tree nodes serialized to ByteString objects.
     */
    fun getSTHConsistency(first: Long, second: Long): List<ByteArray> {
        require(first in 0..second)

        val response = ctService.getSthConsistency(first, second).execute()?.body()!!
        return parseConsistencyProof(response)
    }

    /**
     * Retrieve Entry+Merkle Audit Proof from Log.
     *
     * @param leafIndex The index of the desired entry.
     * @param treeSize The tree_size of the tree for which the proof is desired.
     * @return ParsedLog entry object with proof.
     */
    fun getLogEntryAndProof(leafIndex: Long, treeSize: Long): ParsedLogEntryWithProof {
        require(leafIndex in 0..treeSize)

        val response = ctService.getEntryAndProof(leafIndex, treeSize).execute()?.body()!!

        val logEntry = Deserializer.parseLogEntry(
            ByteArrayInputStream(Base64.decode(response.leafInput)),
            ByteArrayInputStream(Base64.decode(response.extraData)))

        return Deserializer.parseLogEntryWithProof(logEntry, response.auditPath, leafIndex, treeSize)
    }

    /**
     * Retrieve Merkle Audit Proof from Log by Merkle Leaf Hash.
     *
     * @param leafHash sha256 hash of MerkleTreeLeaf.
     * @return MerkleAuditProof object.
     */
    fun getProofByHash(leafHash: ByteArray): MerkleAuditProof {
        require(leafHash.isNotEmpty())
        val encodedMerkleLeafHash = Base64.toBase64String(leafHash)
        val sth = logSTH
        return getProofByEncodedHash(encodedMerkleLeafHash, sth.treeSize)
    }

    /**
     * Retrieve Merkle Audit Proof from Log by Merkle Leaf Hash.
     *
     * @param encodedMerkleLeafHash Base64 encoded of sha256 hash of MerkleTreeLeaf.
     * @param treeSize The tree_size of the tree for which the proof is desired. It can be obtained
     * from latest STH.
     * @return MerkleAuditProof object.
     */
    fun getProofByEncodedHash(encodedMerkleLeafHash: String, treeSize: Long): MerkleAuditProof {
        require(encodedMerkleLeafHash.isNotEmpty())
        val response = ctService.getProofByHash(treeSize, encodedMerkleLeafHash).execute()?.body()!!
        return Deserializer.parseAuditProof(response.auditPath, response.leafIndex, treeSize)
    }

    /**
     * Parses CT log's response for "get-entries" into a list of [ParsedLogEntry] objects.
     *
     * @param response Log response to pars.
     * @return list of Log's entries.
     */
    private fun parseLogEntries(response: GetEntriesResponse): List<ParsedLogEntry> {
        requireNotNull(response) { "Log entries response from Log should not be null." }

        return response.entries.map {
            Deserializer.parseLogEntry(
                ByteArrayInputStream(Base64.decode(it.leafInput)),
                ByteArrayInputStream(Base64.decode(it.extraData)))
        }
    }

    /**
     * Parses CT log's response for the "get-sth-consistency" request.
     *
     * @param response JsonObject containing an array of Merkle Tree nodes.
     * @return A list of base64 decoded Merkle Tree nodes serialized to ByteString objects.
     */
    private fun parseConsistencyProof(response: GetSthConsistencyResponse): List<ByteArray> {
        requireNotNull(response) { "Merkle Consistency response should not be null." }

        return response.consistency.map { Base64.decode(it) }
    }

    /**
     * Parses CT log's response for "get-sth" into a proto object.
     *
     * @param sthResponse Log response to parse
     * @return a proto object of SignedTreeHead type.
     */
    private fun parseSTHResponse(sthResponse: GetSthResponse): SignedTreeHead {
        requireNotNull(sthResponse) { "Sign Tree Head response from a CT log should not be null" }

        val treeSize = sthResponse.treeSize
        val timestamp = sthResponse.timestamp
        if (treeSize < 0 || timestamp < 0) {
            throw CertificateTransparencyException("Bad response. Size of tree or timestamp cannot be a negative value. Log Tree size: " +
                "$treeSize Timestamp: $timestamp")
        }
        val base64Signature = sthResponse.treeHeadSignature
        val sha256RootHash = Base64.decode(sthResponse.sha256RootHash)

        if (sha256RootHash.size != 32) {
            throw CertificateTransparencyException("Bad response. The root hash of the Merkle Hash Tree must be 32 bytes. The size of the " +
                "root hash is ${sha256RootHash.size}")
        }

        return SignedTreeHead(
            version = Version.V1,
            treeSize = treeSize,
            timestamp = timestamp,
            sha256RootHash = sha256RootHash,
            signature = Deserializer.parseDigitallySignedFromBinary(ByteArrayInputStream(Base64.decode(base64Signature)))
        )

        /*val sth = SignedTreeHead(Version.V1)
        sth.treeSize = treeSize
        sth.timestamp = timestamp
        sth.sha256RootHash = sha256RootHash
        sth.signature = Deserializer.parseDigitallySignedFromBinary(
            ByteArrayInputStream(Base64.decode(base64Signature)))

        if (sth.sha256RootHash?.size != 32) {
            throw CertificateTransparencyException("Bad response. The root hash of the Merkle Hash Tree must be 32 bytes. The size of the " +
                "root hash is ${sth.sha256RootHash?.size}")
        }
        return sth*/
    }

    /**
     * Parses the response from "get-roots" GET method.
     *
     * @param response GetRootsResponse with certificates to parse.
     * @return a list of root certificates.
     */
    private fun parseRootCertsResponse(response: GetRootsResponse): List<Certificate> {
        return response.certificates.map {
            val inputStream = Base64.decode(it)
            try {
                CertificateFactory.getInstance("X509").generateCertificate(ByteArrayInputStream(inputStream))
            } catch (e: CertificateException) {
                throw CertificateTransparencyException("Malformed data from a CT log have been received: ${e.localizedMessage}", e)
            }
        }
    }

    companion object {
        /**
         * Parses the CT Log's json response into a SignedCertificateTimestamp.
         *
         * @param responseBody Response string to parse.
         * @return SCT filled from the JSON input.
         */
        internal fun parseServerResponse(responseBody: AddChainResponse?): SignedCertificateTimestamp? {
            if (responseBody == null) {
                return null
            }

            return SignedCertificateTimestamp(
                version = Version.forNumber(responseBody.sctVersion),
                id = LogID(Base64.decode(responseBody.id)),
                timestamp = responseBody.timestamp,
                extensions = if (responseBody.extensions.isNotEmpty()) Base64.decode(responseBody.extensions) else ByteArray(0),
                signature = Deserializer.parseDigitallySignedFromBinary(ByteArrayInputStream(Base64.decode(responseBody.signature)))
            )
        }
    }
}
