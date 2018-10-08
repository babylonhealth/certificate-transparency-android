package org.certificatetransparency.ctlog.comm

import com.google.common.base.Function
import com.google.common.base.Preconditions
import com.google.common.collect.Lists
import com.google.protobuf.ByteString
import org.apache.commons.codec.binary.Base64
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.certificatetransparency.ctlog.CertificateTransparencyException
import org.certificatetransparency.ctlog.MerkleAuditProof
import org.certificatetransparency.ctlog.ParsedLogEntry
import org.certificatetransparency.ctlog.ParsedLogEntryWithProof
import org.certificatetransparency.ctlog.SignedTreeHead
import org.certificatetransparency.ctlog.isPreCertificate
import org.certificatetransparency.ctlog.isPreCertificateSigningCert
import org.certificatetransparency.ctlog.proto.Ct
import org.certificatetransparency.ctlog.serialization.Deserializer
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import java.io.ByteArrayInputStream
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.util.ArrayList

/** A CT HTTP client. Abstracts away the json encoding necessary for the server.  */
class HttpLogClient
/**
 * For testing specify an HttpInvoker
 *
 * @constructor New HttpLogClient.
 * @param logUrl CT Log's full URL, e.g. "http://ct.googleapis.com/pilot/ct/v1/"
 * @param postInvoker HttpInvoker instance to use.
 */
@JvmOverloads constructor(private val logUrl: String, private val postInvoker: HttpInvoker = HttpInvoker()) {

    /**
     * Retrieves Latest Signed Tree Head from the log. The signature of the Signed Tree Head component
     * is not verified.
     *
     * @return latest STH
     */
    val logSTH: SignedTreeHead
        get() {
            val response = postInvoker.makeGetRequest(logUrl + GET_STH_PATH)
            return parseSTHResponse(response)
        }

    /**
     * Retrieves accepted Root Certificates.
     *
     * @return a list of root certificates.
     */
    val logRoots: List<Certificate>
        get() {
            val response = postInvoker.makeGetRequest(logUrl + GET_ROOTS_PATH)

            return parseRootCertsResponse(response)
        }

    private val jsonToLogEntry = Function<JSONObject, ParsedLogEntry> { entry ->
        val leaf = entry!!["leaf_input"] as String
        val extra = entry["extra_data"] as String

        Deserializer.parseLogEntry(
            ByteArrayInputStream(Base64.decodeBase64(leaf)),
            ByteArrayInputStream(Base64.decodeBase64(extra)))
    }

    /**
     * JSON-encodes the list of certificates into a JSON object.
     *
     * @param certs Certificates to encode.
     * @return A JSON object with one field, "chain", holding a JSON array of base64-encoded certs.
     */
    internal // Because JSONObject, JSONArray extend raw types.
    fun encodeCertificates(certs: List<Certificate>): JSONObject {
        val retObject = JSONArray()

        try {
            for (cert in certs) {
                retObject.add(Base64.encodeBase64String(cert.encoded))
            }
        } catch (e: CertificateEncodingException) {
            throw CertificateTransparencyException("Error encoding certificate", e)
        }

        val jsonObject = JSONObject()
        jsonObject["chain"] = retObject
        return jsonObject
    }

    /**
     * Adds a certificate to the log.
     *
     * @param certificatesChain The certificate chain to add.
     * @return SignedCertificateTimestamp if the log added the chain successfully.
     */
    fun addCertificate(certificatesChain: List<Certificate>): Ct.SignedCertificateTimestamp? {
        Preconditions.checkArgument(
            !certificatesChain.isEmpty(), "Must have at least one certificate to submit.")

        val isPreCertificate = certificatesChain[0].isPreCertificate()
        if (isPreCertificate && certificatesChain[1].isPreCertificateSigningCert()) {
            Preconditions.checkArgument(
                certificatesChain.size >= 3,
                "When signing a PreCertificate with a PreCertificate Signing Cert," + " the issuer certificate must follow.")
        }

        return addCertificate(certificatesChain, isPreCertificate)
    }

    private fun addCertificate(
        certificatesChain: List<Certificate>, isPreCertificate: Boolean): Ct.SignedCertificateTimestamp? {
        val jsonPayload = encodeCertificates(certificatesChain).toJSONString()
        val methodPath = if (isPreCertificate) ADD_PRE_CHAIN_PATH else ADD_CHAIN_PATH

        val response = postInvoker.makePostRequest(logUrl + methodPath, jsonPayload)
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
        Preconditions.checkArgument(start in 0..end)

        val params = createParamsList("start", "end", java.lang.Long.toString(start), java.lang.Long.toString(end))

        val response = postInvoker.makeGetRequest(logUrl + GET_ENTRIES, params)
        return parseLogEntries(response)
    }

    /**
     * Retrieve Merkle Consistency Proof between Two Signed Tree Heads.
     *
     * @param first The tree_size of the first tree, in decimal.
     * @param second The tree_size of the second tree, in decimal.
     * @return A list of base64 decoded Merkle Tree nodes serialized to ByteString objects.
     */
    fun getSTHConsistency(first: Long, second: Long): List<ByteString> {
        Preconditions.checkArgument(first in 0..second)

        val params = createParamsList("first", "second", java.lang.Long.toString(first), java.lang.Long.toString(second))

        val response = postInvoker.makeGetRequest(logUrl + GET_STH_CONSISTENCY, params)
        return parseConsistencyProof(response)
    }

    /**
     * Retrieve Entry+Merkle Audit Proof from Log.
     *
     * @param leafindex The index of the desired entry.
     * @param treeSize The tree_size of the tree for which the proof is desired.
     * @return ParsedLog entry object with proof.
     */
    fun getLogEntryAndProof(leafindex: Long, treeSize: Long): ParsedLogEntryWithProof {
        Preconditions.checkArgument(leafindex in 0..treeSize)

        val params = createParamsList(
            "leaf_index", "tree_size", java.lang.Long.toString(leafindex), java.lang.Long.toString(treeSize))

        val response = postInvoker.makeGetRequest(logUrl + GET_ENTRY_AND_PROOF, params)
        val entry = JSONValue.parse(response) as JSONObject
        val auditPath = entry["audit_path"] as JSONArray

        return Deserializer.parseLogEntryWithProof(jsonToLogEntry.apply(entry)!!, auditPath, leafindex, treeSize)
    }

    /**
     * Retrieve Merkle Audit Proof from Log by Merkle Leaf Hash.
     *
     * @param leafHash sha256 hash of MerkleTreeLeaf.
     * @return MerkleAuditProof object.
     */
    fun getProofByHash(leafHash: ByteArray): MerkleAuditProof {
        Preconditions.checkArgument(leafHash.isNotEmpty())
        val encodedMerkleLeafHash = Base64.encodeBase64String(leafHash)
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
        Preconditions.checkArgument(encodedMerkleLeafHash.isNotEmpty())
        val params = createParamsList("tree_size", "hash", java.lang.Long.toString(treeSize), encodedMerkleLeafHash)
        val response = postInvoker.makeGetRequest(logUrl + GET_PROOF_BY_HASH, params)
        val entry = JSONValue.parse(response) as JSONObject
        val auditPath = entry["audit_path"] as JSONArray
        val leafindex = java.lang.Long.valueOf(entry["leaf_index"].toString())
        return Deserializer.parseAuditProof(auditPath, leafindex, treeSize)
    }

    /**
     * Creates a list of NameValuePair objects.
     *
     * @param firstParamName The first parameter name.
     * @param firstParamValue The first parameter value.
     * @param secondParamName The second parameter name.
     * @param secondParamValue The second parameter value.
     * @return A list of NameValuePair objects.
     */
    private fun createParamsList(
        firstParamName: String,
        secondParamName: String,
        firstParamValue: String,
        secondParamValue: String): List<NameValuePair> {
        val params = ArrayList<NameValuePair>()
        params.add(BasicNameValuePair(firstParamName, firstParamValue))
        params.add(BasicNameValuePair(secondParamName, secondParamValue))
        return params
    }

    /**
     * Parses CT log's response for "get-entries" into a list of [ParsedLogEntry] objects.
     *
     * @param response Log response to pars.
     * @return list of Log's entries.
     */
    private fun parseLogEntries(response: String): List<ParsedLogEntry> {
        Preconditions.checkNotNull(response, "Log entries response from Log should not be null.")

        val responseJson = JSONValue.parse(response) as JSONObject
        val arr = responseJson["entries"] as JSONArray

        // JSONArray is guaranteed to be a list of JSONObjects
        @Suppress("UNCHECKED_CAST")
        return Lists.transform<JSONObject, ParsedLogEntry>(arr as List<JSONObject>, jsonToLogEntry)
    }

    /**
     * Parses CT log's response for the "get-sth-consistency" request.
     *
     * @param response JsonObject containing an array of Merkle Tree nodes.
     * @return A list of base64 decoded Merkle Tree nodes serialized to ByteString objects.
     */
    private fun parseConsistencyProof(response: String): List<ByteString> {
        Preconditions.checkNotNull(response, "Merkle Consistency response should not be null.")

        val responseJson = JSONValue.parse(response) as JSONObject
        val arr = responseJson["consistency"] as JSONArray

        val proof = ArrayList<ByteString>()
        for (node in arr) {
            proof.add(ByteString.copyFrom(Base64.decodeBase64(node as String)))
        }
        return proof
    }

    /**
     * Parses CT log's response for "get-sth" into a proto object.
     *
     * @param sthResponse Log response to parse
     * @return a proto object of SignedTreeHead type.
     */
    private fun parseSTHResponse(sthResponse: String): SignedTreeHead {
        Preconditions.checkNotNull(
            sthResponse, "Sign Tree Head response from a CT log should not be null")

        val response = JSONValue.parse(sthResponse) as JSONObject
        val treeSize = response["tree_size"] as Long
        val timestamp = response["timestamp"] as Long
        if (treeSize < 0 || timestamp < 0) {
            throw CertificateTransparencyException(
                String.format(
                    "Bad response. Size of tree or timestamp cannot be a negative value. " + "Log Tree size: %d Timestamp: %d",
                    treeSize, timestamp))
        }
        val base64Signature = response["tree_head_signature"] as String
        val sha256RootHash = response["sha256_root_hash"] as String

        val sth = SignedTreeHead(Ct.Version.V1)
        sth.treeSize = treeSize
        sth.timestamp = timestamp
        sth.sha256RootHash = Base64.decodeBase64(sha256RootHash)
        sth.signature = Deserializer.parseDigitallySignedFromBinary(
            ByteArrayInputStream(Base64.decodeBase64(base64Signature)))

        if (sth.sha256RootHash!!.size != 32) {
            throw CertificateTransparencyException(
                String.format(
                    "Bad response. The root hash of the Merkle Hash Tree must be 32 bytes. " + "The size of the root hash is %d",
                    sth.sha256RootHash!!.size))
        }
        return sth
    }

    /**
     * Parses the response from "get-roots" GET method.
     *
     * @param response JSONObject with certificates to parse.
     * @return a list of root certificates.
     */
    private fun parseRootCertsResponse(response: String): List<Certificate> {
        val certs = ArrayList<Certificate>()

        val entries = JSONValue.parse(response) as JSONObject
        val entriesArray = entries["certificates"] as JSONArray

        for (i in entriesArray) {
            // We happen to know that JSONArray contains strings.
            val `in` = Base64.decodeBase64(i as String)
            try {
                certs.add(
                    CertificateFactory.getInstance("X509")
                        .generateCertificate(ByteArrayInputStream(`in`)))
            } catch (e: CertificateException) {
                throw CertificateTransparencyException(
                    "Malformed data from a CT log have been received: " + e.localizedMessage, e)
            }
        }
        return certs
    }

    companion object {
        private const val ADD_PRE_CHAIN_PATH = "add-pre-chain"
        private const val ADD_CHAIN_PATH = "add-chain"
        private const val GET_STH_PATH = "get-sth"
        private const val GET_ROOTS_PATH = "get-roots"
        private const val GET_ENTRIES = "get-entries"
        private const val GET_STH_CONSISTENCY = "get-sth-consistency"
        private const val GET_ENTRY_AND_PROOF = "get-entry-and-proof"
        private const val GET_PROOF_BY_HASH = "get-proof-by-hash"

        /**
         * Parses the CT Log's json response into a proper proto.
         *
         * @param responseBody Response string to parse.
         * @return SCT filled from the JSON input.
         */
        internal fun parseServerResponse(responseBody: String?): Ct.SignedCertificateTimestamp? {
            if (responseBody == null) {
                return null
            }

            val parsedResponse = JSONValue.parse(responseBody) as JSONObject
            val builder = Ct.SignedCertificateTimestamp.newBuilder()

            val numericVersion = (parsedResponse["sct_version"] as Number).toInt()
            val version = Ct.Version.forNumber(numericVersion) ?: throw IllegalArgumentException(
                String.format("Input JSON has an invalid version: %d", numericVersion))
            builder.version = version
            val logIdBuilder = Ct.LogID.newBuilder()
            logIdBuilder.keyId = ByteString.copyFrom(Base64.decodeBase64(parsedResponse["id"] as String))
            builder.id = logIdBuilder.build()
            builder.timestamp = (parsedResponse["timestamp"] as Number).toLong()
            val extensions = parsedResponse["extensions"] as String
            if (!extensions.isEmpty()) {
                builder.extensions = ByteString.copyFrom(Base64.decodeBase64(extensions))
            }

            val base64Signature = parsedResponse["signature"] as String
            builder.signature = Deserializer.parseDigitallySignedFromBinary(
                ByteArrayInputStream(Base64.decodeBase64(base64Signature)))
            return builder.build()
        }
    }
}
