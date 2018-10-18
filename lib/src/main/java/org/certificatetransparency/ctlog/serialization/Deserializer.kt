package org.certificatetransparency.ctlog.serialization

import org.certificatetransparency.ctlog.Base64
import org.certificatetransparency.ctlog.domain.logclient.model.DigitallySigned
import org.certificatetransparency.ctlog.domain.logclient.model.LogEntry
import org.certificatetransparency.ctlog.domain.logclient.model.LogEntryType
import org.certificatetransparency.ctlog.domain.logclient.model.LogId
import org.certificatetransparency.ctlog.domain.logclient.model.MerkleAuditProof
import org.certificatetransparency.ctlog.domain.logclient.model.MerkleTreeLeaf
import org.certificatetransparency.ctlog.domain.logclient.model.ParsedLogEntry
import org.certificatetransparency.ctlog.domain.logclient.model.ParsedLogEntryWithProof
import org.certificatetransparency.ctlog.domain.logclient.model.PreCertificate
import org.certificatetransparency.ctlog.domain.logclient.model.SignedCertificateTimestamp
import org.certificatetransparency.ctlog.domain.logclient.model.SignedEntry
import org.certificatetransparency.ctlog.domain.logclient.model.TimestampedEntry
import org.certificatetransparency.ctlog.domain.logclient.model.Version
import java.io.IOException
import java.io.InputStream

/** Converting binary data to CT structures.  */
internal object Deserializer {
    private const val TIMESTAMPED_ENTRY_LEAF_TYPE = 0

    /**
     * Parses a SignedCertificateTimestamp from binary encoding.
     *
     * @param inputStream byte stream of binary encoding.
     * @return Built SignedCertificateTimestamp
     * @throws SerializationException if the data stream is too short.
     */
    @JvmStatic
    fun parseSctFromBinary(inputStream: InputStream): SignedCertificateTimestamp {
        val version = Version.forNumber(readNumber(inputStream, 1 /* single byte */).toInt())
        if (version != Version.V1) {
            throw SerializationException("Unknown version: $version")
        }

        val keyId = readFixedLength(inputStream, CTConstants.KEY_ID_LENGTH)

        val timestamp = readNumber(inputStream, CTConstants.TIMESTAMP_LENGTH)

        val extensions = readVariableLength(inputStream, CTConstants.MAX_EXTENSIONS_LENGTH)

        val signature = parseDigitallySignedFromBinary(inputStream)

        return SignedCertificateTimestamp(
            version = version,
            id = LogId(keyId),
            timestamp = timestamp,
            extensions = extensions,
            signature = signature
        )
    }

    /**
     * Parses a DigitallySigned from binary encoding.
     *
     * @param inputStream byte stream of binary encoding.
     * @return Built DigitallySigned
     * @throws SerializationException if the data stream is too short.
     */
    @JvmStatic
    fun parseDigitallySignedFromBinary(inputStream: InputStream): DigitallySigned {
        val hashAlgorithmByte = readNumber(inputStream, 1 /* single byte */).toInt()
        val hashAlgorithm = DigitallySigned.HashAlgorithm.forNumber(hashAlgorithmByte)
            ?: throw SerializationException("Unknown hash algorithm: ${hashAlgorithmByte.toString(16)}")

        val signatureAlgorithmByte = readNumber(inputStream, 1 /* single byte */).toInt()
        val signatureAlgorithm = DigitallySigned.SignatureAlgorithm.forNumber(signatureAlgorithmByte)
            ?: throw SerializationException("Unknown signature algorithm: ${signatureAlgorithmByte.toString(16)}")

        val signature = readVariableLength(inputStream, CTConstants.MAX_SIGNATURE_LENGTH)

        return DigitallySigned(
            hashAlgorithm = hashAlgorithm,
            signatureAlgorithm = signatureAlgorithm,
            signature = signature
        )
    }

    /**
     * Parses an entry retrieved from Log and it's audit proof.
     *
     * @param entry ParsedLogEntry instance.
     * @param proof An array of base64-encoded Merkle Tree nodes proving the inclusion of the chosen
     * certificate.
     * @param leafIndex The index of the desired entry.
     * @param treeSize The tree size of the tree for which the proof is desired.
     * @return [ParsedLogEntryWithProof]
     */
    @JvmStatic
    fun parseLogEntryWithProof(entry: ParsedLogEntry, proof: List<String>, leafIndex: Long, treeSize: Long): ParsedLogEntryWithProof {
        return ParsedLogEntryWithProof(entry, parseAuditProof(proof, leafIndex, treeSize))
    }

    /**
     * Parses the audit proof retrieved from Log.
     *
     * @param proof An array of base64-encoded Merkle Tree nodes proving the inclusion of the chosen
     * certificate.
     * @param leafIndex The index of the desired entry.
     * @param treeSize The tree size of the tree for which the proof is desired.
     * @return [MerkleAuditProof]
     */
    @JvmStatic
    fun parseAuditProof(proof: List<String>, leafIndex: Long, treeSize: Long) =
        MerkleAuditProof(Version.V1, treeSize, leafIndex, proof.map(Base64::decode))

    /**
     * Parses an entry retrieved from Log.
     *
     * @param merkleTreeLeaf MerkleTreeLeaf structure, byte stream of binary encoding.
     * @param extraData extra data, byte stream of binary encoding.
     * @return [ParsedLogEntry]
     */
    @JvmStatic
    fun parseLogEntry(merkleTreeLeaf: InputStream, extraData: InputStream): ParsedLogEntry {
        val treeLeaf = parseMerkleTreeLeaf(merkleTreeLeaf)

        val logEntry = when (treeLeaf.timestampedEntry.signedEntry) {
            is SignedEntry.X509 -> {
                parseX509ChainEntry(extraData, treeLeaf.timestampedEntry.signedEntry.x509)
            }
            is SignedEntry.PreCertificate -> {
                parsePreCertificateChainEntry(extraData, treeLeaf.timestampedEntry.signedEntry.preCertificate)
            }
        }

        return ParsedLogEntry(treeLeaf, logEntry)
    }

    /**
     * Parses a [MerkleTreeLeaf] from binary encoding.
     *
     * @param inputStream byte stream of binary encoding.
     * @return Built [MerkleTreeLeaf].
     * @throws SerializationException if the data stream is too short.
     */
    private fun parseMerkleTreeLeaf(inputStream: InputStream): MerkleTreeLeaf {
        val version = readNumber(inputStream, CTConstants.VERSION_LENGTH).toInt()
        if (version != Version.V1.number) {
            throw SerializationException("Unknown version: $version")
        }

        val leafType = readNumber(inputStream, 1).toInt()
        if (leafType != TIMESTAMPED_ENTRY_LEAF_TYPE) {
            throw SerializationException("Unknown entry type: $leafType")
        }

        return MerkleTreeLeaf(Version.forNumber(version), parseTimestampedEntry(inputStream))
    }

    /**
     * Parses a [TimestampedEntry] from binary encoding.
     *
     * @param inputStream byte stream of binary encoding.
     * @return Built [TimestampedEntry].
     * @throws SerializationException if the data stream is too short.
     */
    private fun parseTimestampedEntry(inputStream: InputStream): TimestampedEntry {
        val timestamp = readNumber(inputStream, CTConstants.TIMESTAMP_LENGTH)

        val entryType = readNumber(inputStream, CTConstants.LOG_ENTRY_TYPE_LENGTH).toInt()
        val logEntryType = LogEntryType.forNumber(entryType)

        val signedEntry = when (logEntryType) {
            LogEntryType.X509_ENTRY -> {
                val length = readNumber(inputStream, 3).toInt()
                SignedEntry.X509(readFixedLength(inputStream, length))
            }
            LogEntryType.PRE_CERTIFICATE_ENTRY -> {
                val issuerKeyHash = readFixedLength(inputStream, 32)

                val length = readNumber(inputStream, 2).toInt()
                val tbsCertificate = readFixedLength(inputStream, length)

                SignedEntry.PreCertificate(
                    PreCertificate(
                        issuerKeyHash = issuerKeyHash,
                        tbsCertificate = tbsCertificate))
            }
            else -> throw SerializationException("Unknown entry type: $entryType")
        }

        return TimestampedEntry(
            timestamp = timestamp,
            signedEntry = signedEntry)
    }

    /**
     * Parses X509ChainEntry structure.
     *
     * @param inputStream X509ChainEntry structure, byte stream of binary encoding.
     * @param x509Cert leaf certificate.
     * @throws SerializationException if an I/O error occurs.
     * @return [LogEntry.X509ChainEntry] object.
     */
    private fun parseX509ChainEntry(inputStream: InputStream, x509Cert: ByteArray?): LogEntry.X509ChainEntry {
        val certificateChain = mutableListOf<ByteArray>()

        try {
            if (readNumber(inputStream, 3) != inputStream.available().toLong()) {
                throw SerializationException("Extra data corrupted.")
            }
            while (inputStream.available() > 0) {
                val length = readNumber(inputStream, 3).toInt()
                certificateChain.add(readFixedLength(inputStream, length))
            }
        } catch (e: IOException) {
            throw SerializationException("Cannot parse xChainEntry. ${e.localizedMessage}")
        }

        return LogEntry.X509ChainEntry(
            leafCertificate = x509Cert,
            certificateChain = certificateChain.toList()
        )
    }

    /**
     * Parses PreCertificateChainEntry structure.
     *
     * @param inputStream PreCertificateChainEntry structure, byte stream of binary encoding.
     * @param preCertificate Pre-certificate.
     * @return [LogEntry.PreCertificateChainEntry] object.
     */
    private fun parsePreCertificateChainEntry(inputStream: InputStream, preCertificate: PreCertificate): LogEntry.PreCertificateChainEntry {
        val preCertificateChain = mutableListOf<ByteArray>()

        try {
            if (readNumber(inputStream, 3) != inputStream.available().toLong()) {
                throw SerializationException("Extra data corrupted.")
            }
            while (inputStream.available() > 0) {
                val length = readNumber(inputStream, 3).toInt()
                preCertificateChain.add(readFixedLength(inputStream, length))
            }
        } catch (e: IOException) {
            throw SerializationException("Cannot parse PrecertEntryChain.${e.localizedMessage}")
        }

        return LogEntry.PreCertificateChainEntry(
            preCertificate = preCertificate,
            preCertificateChain = preCertificateChain.toList()
        )
    }

    /**
     * Reads a variable-length byte array with a maximum length. The length is read (based on the
     * number of bytes needed to represent the max data length) then the byte array itself.
     *
     * @param inputStream byte stream of binary encoding.
     * @param maxDataLength Maximal data length.
     * @return read byte array.
     * @throws SerializationException if the data stream is too short.
     */
    private fun readVariableLength(inputStream: InputStream, maxDataLength: Int): ByteArray {
        val bytesForDataLength = bytesForDataLength(maxDataLength)
        val dataLength = readNumber(inputStream, bytesForDataLength)

        val rawData = ByteArray(dataLength.toInt())
        val bytesRead: Int
        try {
            bytesRead = inputStream.read(rawData)
        } catch (e: IOException) {
            //Note: A finer-grained exception type should be thrown if the client
            // ever cares to handle transient I/O errors.
            throw SerializationException("Error while reading variable-length data", e)
        }

        if (bytesRead.toLong() != dataLength) {
            throw SerializationException("Incomplete data. Expected $dataLength bytes, had $bytesRead.")
        }

        return rawData
    }

    /**
     * Reads a fixed-length byte array.
     *
     * @param inputStream byte stream of binary encoding.
     * @param dataLength exact data length.
     * @return read byte array.
     * @throws SerializationException if the data stream is too short.
     */
    private fun readFixedLength(inputStream: InputStream, dataLength: Int): ByteArray {
        val toReturn = ByteArray(dataLength)
        try {
            val bytesRead = inputStream.read(toReturn)
            if (bytesRead < dataLength) {
                throw SerializationException("Not enough bytes: Expected $dataLength, got $bytesRead.")
            }
            return toReturn
        } catch (e: IOException) {
            throw SerializationException("Error while reading fixed-length buffer", e)
        }
    }

    /**
     * Calculates the number of bytes needed to hold the given number: ceil(log2(maxDataLength)) / 8
     *
     * @param maxDataLength the number that needs to be represented as bytes
     * @return Number of bytes needed to represent the given number
     */
    @JvmStatic
    fun bytesForDataLength(maxDataLength: Int): Int {
        return (Math.ceil(Math.log(maxDataLength.toDouble()) / Math.log(2.0)) / 8).toInt()
    }

    /**
     * Read a number of numBytes bytes (Assuming MSB first).
     *
     * @param inputStream byte stream of binary encoding.
     * @param numBytes exact number of bytes representing this number.
     * @return a number of at most 2^numBytes
     */
    private fun readNumber(inputStream: InputStream, numBytes: Int): Long {
        require(numBytes <= 8) { "Could not read a number of more than 8 bytes." }

        var toReturn: Long = 0
        try {
            for (i in 0 until numBytes) {
                val valRead = inputStream.read()
                if (valRead < 0) {
                    throw SerializationException("Missing length bytes: Expected $numBytes, got $i.")
                }
                toReturn = toReturn shl 8 or valRead.toLong()
            }
            return toReturn
        } catch (e: IOException) {
            throw SerializationException("IO Error when reading number", e)
        }
    }
}
