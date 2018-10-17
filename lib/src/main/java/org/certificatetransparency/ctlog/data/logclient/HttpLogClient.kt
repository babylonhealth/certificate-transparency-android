package org.certificatetransparency.ctlog.data.logclient

import org.certificatetransparency.ctlog.Base64
import org.certificatetransparency.ctlog.CertificateTransparencyException
import org.certificatetransparency.ctlog.data.logclient.model.SignedTreeHead
import org.certificatetransparency.ctlog.data.logclient.model.network.AddChainRequest
import org.certificatetransparency.ctlog.data.logclient.model.network.AddChainResponse
import org.certificatetransparency.ctlog.data.logclient.model.network.GetEntriesResponse
import org.certificatetransparency.ctlog.data.logclient.model.network.GetRootsResponse
import org.certificatetransparency.ctlog.data.logclient.model.network.GetSthConsistencyResponse
import org.certificatetransparency.ctlog.data.logclient.model.network.GetSthResponse
import org.certificatetransparency.ctlog.domain.logclient.LogClient
import org.certificatetransparency.ctlog.domain.logclient.model.LogId
import org.certificatetransparency.ctlog.domain.logclient.model.MerkleAuditProof
import org.certificatetransparency.ctlog.domain.logclient.model.ParsedLogEntry
import org.certificatetransparency.ctlog.domain.logclient.model.ParsedLogEntryWithProof
import org.certificatetransparency.ctlog.domain.logclient.model.SignedCertificateTimestamp
import org.certificatetransparency.ctlog.domain.logclient.model.Version
import org.certificatetransparency.ctlog.isPreCertificate
import org.certificatetransparency.ctlog.isPreCertificateSigningCert
import org.certificatetransparency.ctlog.serialization.Deserializer
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
internal class HttpLogClient(private val ctService: LogClientService) : LogClient {

    /**
     * Retrieves Latest Signed Tree Head from the log. The signature of the Signed Tree Head component
     * is not verified.
     *
     * @return latest STH
     */
    internal val logSth: SignedTreeHead by lazy {
        ctService.getSth().execute()?.body()!!.toSignedTreeHead()
    }

    /**
     * Retrieves accepted Root Certificates.
     *
     * @return a list of root certificates.
     */
    internal val logRoots: List<Certificate> by lazy {
        ctService.getRoots().execute()?.body()!!.toRootCertificates()
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
    override fun addCertificate(certificatesChain: List<Certificate>): SignedCertificateTimestamp {
        require(!certificatesChain.isEmpty()) { "Must have at least one certificate to submit." }

        val isPreCertificate = certificatesChain[0].isPreCertificate()
        if (isPreCertificate && certificatesChain[1].isPreCertificateSigningCert()) {
            require(certificatesChain.size >= 3) {
                "When signing a PreCertificate with a PreCertificate Signing Cert, the issuer certificate must follow."
            }
        }

        return addCertificate(certificatesChain, isPreCertificate)
    }

    private fun addCertificate(certificatesChain: List<Certificate>, isPreCertificate: Boolean): SignedCertificateTimestamp {
        val jsonPayload = encodeCertificates(certificatesChain)

        val call = if (isPreCertificate) ctService.addPreChain(jsonPayload) else ctService.addChain(jsonPayload)

        return call.execute()?.body()!!.toSignedCertificateTimestamp()
    }

    /**
     * Retrieve Entries from Log.
     *
     * @param start 0-based index of first entry to retrieve, in decimal.
     * @param end 0-based index of last entry to retrieve, in decimal.
     * @return list of Log's entries.
     */
    override fun getLogEntries(start: Long, end: Long): List<ParsedLogEntry> {
        require(start in 0..end)

        return ctService.getEntries(start, end).execute()?.body()!!.toParsedLogEntries()
    }

    /**
     * Retrieve Merkle Consistency Proof between Two Signed Tree Heads.
     *
     * @param first The tree_size of the first tree, in decimal.
     * @param second The tree_size of the second tree, in decimal.
     * @return A list of base64 decoded Merkle Tree nodes serialized to ByteString objects.
     */
    override fun getSthConsistency(first: Long, second: Long): List<ByteArray> {
        require(first in 0..second)

        return ctService.getSthConsistency(first, second).execute()?.body()!!.toMerkleTreeNodes()
    }

    /**
     * Retrieve Entry+Merkle Audit Proof from Log.
     *
     * @param leafIndex The index of the desired entry.
     * @param treeSize The tree_size of the tree for which the proof is desired.
     * @return ParsedLog entry object with proof.
     */
    override fun getLogEntryAndProof(leafIndex: Long, treeSize: Long): ParsedLogEntryWithProof {
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
    override fun getProofByHash(leafHash: ByteArray): MerkleAuditProof {
        require(leafHash.isNotEmpty())
        val encodedMerkleLeafHash = Base64.toBase64String(leafHash)
        val sth = logSth
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
    override fun getProofByEncodedHash(encodedMerkleLeafHash: String, treeSize: Long): MerkleAuditProof {
        require(encodedMerkleLeafHash.isNotEmpty())
        val response = ctService.getProofByHash(treeSize, encodedMerkleLeafHash).execute()?.body()!!
        return Deserializer.parseAuditProof(response.auditPath, response.leafIndex, treeSize)
    }

    /**
     * Parses CT log's response for "get-entries" into a list of [ParsedLogEntry] objects.
     *
     * @return list of Log's entries.
     */
    private fun GetEntriesResponse.toParsedLogEntries(): List<ParsedLogEntry> {
        requireNotNull(this) { "Log entries response from Log should not be null." }

        return entries.map {
            Deserializer.parseLogEntry(
                ByteArrayInputStream(Base64.decode(it.leafInput)),
                ByteArrayInputStream(Base64.decode(it.extraData)))
        }
    }

    /**
     * Parses CT log's response for the "get-sth-consistency" request.
     *
     * @return A list of base64 decoded Merkle Tree nodes serialized to ByteString objects.
     */
    private fun GetSthConsistencyResponse.toMerkleTreeNodes(): List<ByteArray> {
        requireNotNull(this) { "Merkle Consistency response should not be null." }

        return consistency.map { Base64.decode(it) }
    }

    /**
     * Parses CT log's response for "get-sth" into a signed tree head object.
     *
     * @return A SignedTreeHead object.
     */
    private fun GetSthResponse.toSignedTreeHead(): SignedTreeHead {
        requireNotNull(this) { "Sign Tree Head response from a CT log should not be null" }

        val treeSize = treeSize
        val timestamp = timestamp
        if (treeSize < 0 || timestamp < 0) {
            throw CertificateTransparencyException("Bad response. Size of tree or timestamp cannot be a negative value. Log Tree size: " +
                "$treeSize Timestamp: $timestamp")
        }
        val base64Signature = treeHeadSignature
        val sha256RootHash = Base64.decode(sha256RootHash)

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
    }

    /**
     * Parses the response from "get-roots" GET method.
     *
     * @return a list of root certificates.
     */
    private fun GetRootsResponse.toRootCertificates(): List<Certificate> {
        return certificates.map {
            val inputStream = Base64.decode(it)
            try {
                CertificateFactory.getInstance("X509").generateCertificate(ByteArrayInputStream(inputStream))
            } catch (e: CertificateException) {
                throw CertificateTransparencyException("Malformed data from a CT log have been received: ${e.localizedMessage}", e)
            }
        }
    }

    /**
     * Parses the CT Log's json response into a SignedCertificateTimestamp.
     *
     * @return SCT filled from the JSON input.
     */
    private fun AddChainResponse.toSignedCertificateTimestamp(): SignedCertificateTimestamp {
        return SignedCertificateTimestamp(
            version = Version.forNumber(sctVersion),
            id = LogId(Base64.decode(id)),
            timestamp = timestamp,
            extensions = if (extensions.isNotEmpty()) Base64.decode(extensions) else ByteArray(0),
            signature = Deserializer.parseDigitallySignedFromBinary(ByteArrayInputStream(Base64.decode(signature)))
        )
    }
}
