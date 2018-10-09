package org.certificatetransparency.ctlog.serialization

import com.google.protobuf.ByteString
import org.bouncycastle.util.encoders.Base64
import org.certificatetransparency.ctlog.LogEntry
import org.certificatetransparency.ctlog.MerkleAuditProof
import org.certificatetransparency.ctlog.MerkleTreeLeaf
import org.certificatetransparency.ctlog.ParsedLogEntry
import org.certificatetransparency.ctlog.ParsedLogEntryWithProof
import org.certificatetransparency.ctlog.PreCert
import org.certificatetransparency.ctlog.PrecertChainEntry
import org.certificatetransparency.ctlog.SignedEntry
import org.certificatetransparency.ctlog.TimestampedEntry
import org.certificatetransparency.ctlog.X509ChainEntry
import org.certificatetransparency.ctlog.proto.Ct
import java.io.IOException
import java.io.InputStream

/** Converting binary data to CT structures.  */
object Deserializer {
    private const val TIMESTAMPED_ENTRY_LEAF_TYPE = 0

    /**
     * Parses a SignedCertificateTimestamp from binary encoding.
     *
     * @param inputStream byte stream of binary encoding.
     * @return Built CT.SignedCertificateTimestamp
     * @throws SerializationException if the data stream is too short.
     */
    @JvmStatic
    fun parseSCTFromBinary(inputStream: InputStream): Ct.SignedCertificateTimestamp {
        val sctBuilder = Ct.SignedCertificateTimestamp.newBuilder()

        val version = readNumber(inputStream, 1 /* single byte */).toInt()
        if (version != Ct.Version.V1.number) {
            throw SerializationException("Unknown version: $version")
        }
        sctBuilder.version = Ct.Version.forNumber(version)

        val keyId = readFixedLength(inputStream, CTConstants.KEY_ID_LENGTH)
        sctBuilder.id = Ct.LogID.newBuilder().setKeyId(ByteString.copyFrom(keyId)).build()

        val timestamp = readNumber(inputStream, CTConstants.TIMESTAMP_LENGTH)
        sctBuilder.timestamp = timestamp

        val extensions = readVariableLength(inputStream, CTConstants.MAX_EXTENSIONS_LENGTH)
        sctBuilder.extensions = ByteString.copyFrom(extensions)

        sctBuilder.signature = parseDigitallySignedFromBinary(inputStream)
        return sctBuilder.build()
    }

    /**
     * Parses a Ct.DigitallySigned from binary encoding.
     *
     * @param inputStream byte stream of binary encoding.
     * @return Built Ct.DigitallySigned
     * @throws SerializationException if the data stream is too short.
     */
    @JvmStatic
    fun parseDigitallySignedFromBinary(inputStream: InputStream): Ct.DigitallySigned {
        val builder = Ct.DigitallySigned.newBuilder()
        val hashAlgorithmByte = readNumber(inputStream, 1 /* single byte */).toInt()
        val hashAlgorithm = Ct.DigitallySigned.HashAlgorithm.forNumber(hashAlgorithmByte)
            ?: throw SerializationException("Unknown hash algorithm: ${hashAlgorithmByte.toString(16)}")
        builder.hashAlgorithm = hashAlgorithm

        val signatureAlgorithmByte = readNumber(inputStream, 1 /* single byte */).toInt()
        val signatureAlgorithm = Ct.DigitallySigned.SignatureAlgorithm.forNumber(signatureAlgorithmByte)
            ?: throw SerializationException("Unknown signature algorithm: ${signatureAlgorithmByte.toString(16)}")
        builder.sigAlgorithm = signatureAlgorithm

        val signature = readVariableLength(inputStream, CTConstants.MAX_SIGNATURE_LENGTH)
        builder.signature = ByteString.copyFrom(signature)

        return builder.build()
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
        val auditProof = MerkleAuditProof(Ct.Version.V1, treeSize, leafIndex)
        proof.asSequence().map(Base64::decode).forEach { node -> auditProof.pathNode.add(node) }
        return ParsedLogEntryWithProof.newInstance(entry, auditProof)
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
    fun parseAuditProof(proof: List<String>, leafIndex: Long, treeSize: Long): MerkleAuditProof {
        val auditProof = MerkleAuditProof(Ct.Version.V1, treeSize, leafIndex)
        proof.forEach { node -> auditProof.pathNode.add(Base64.decode(node)) }
        return auditProof
    }

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
        val logEntry = LogEntry()

        val entryType = treeLeaf.timestampedEntry.entryType

        when (entryType) {
            Ct.LogEntryType.X509_ENTRY -> {
                val x509EntryChain = parseX509ChainEntry(extraData, treeLeaf.timestampedEntry.signedEntry!!.x509)
                logEntry.x509Entry = x509EntryChain
            }
            Ct.LogEntryType.PRECERT_ENTRY -> {
                val preCertChain = parsePrecertChainEntry(extraData, treeLeaf.timestampedEntry.signedEntry!!.preCert)
                logEntry.precertEntry = preCertChain
            }
            else -> throw SerializationException("Unknown entry type: $entryType")
        }

        return ParsedLogEntry.newInstance(treeLeaf, logEntry)
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
        if (version != Ct.Version.V1.number) {
            throw SerializationException("Unknown version: $version")
        }

        val leafType = readNumber(inputStream, 1).toInt()
        if (leafType != TIMESTAMPED_ENTRY_LEAF_TYPE) {
            throw SerializationException("Unknown entry type: $leafType")
        }

        return MerkleTreeLeaf(Ct.Version.forNumber(version), parseTimestampedEntry(inputStream))
    }

    /**
     * Parses a [TimestampedEntry] from binary encoding.
     *
     * @param inputStream byte stream of binary encoding.
     * @return Built [TimestampedEntry].
     * @throws SerializationException if the data stream is too short.
     */
    private fun parseTimestampedEntry(inputStream: InputStream): TimestampedEntry {
        val timestampedEntry = TimestampedEntry()

        timestampedEntry.timestamp = readNumber(inputStream, CTConstants.TIMESTAMP_LENGTH)

        val entryType = readNumber(inputStream, CTConstants.LOG_ENTRY_TYPE_LENGTH).toInt()
        timestampedEntry.entryType = Ct.LogEntryType.forNumber(entryType)

        val signedEntry = SignedEntry()
        when (entryType) {
            Ct.LogEntryType.X509_ENTRY_VALUE -> {
                val length = readNumber(inputStream, 3).toInt()
                signedEntry.x509 = readFixedLength(inputStream, length)
            }
            Ct.LogEntryType.PRECERT_ENTRY_VALUE -> {
                val preCert = PreCert()

                preCert.issuerKeyHash = readFixedLength(inputStream, 32)

                // set tbs certificate
                val length = readNumber(inputStream, 2).toInt()
                preCert.tbsCertificate = readFixedLength(inputStream, length)

                signedEntry.preCert = preCert
            }
            else -> throw SerializationException("Unknown entry type: $entryType")
        }
        timestampedEntry.signedEntry = signedEntry

        return timestampedEntry
    }

    /**
     * Parses X509ChainEntry structure.
     *
     * @param inputStream X509ChainEntry structure, byte stream of binary encoding.
     * @param x509Cert leaf certificate.
     * @throws SerializationException if an I/O error occurs.
     * @return [X509ChainEntry] object.
     */
    private fun parseX509ChainEntry(inputStream: InputStream, x509Cert: ByteArray?): X509ChainEntry {
        val x509EntryChain = X509ChainEntry()
        x509EntryChain.leafCertificate = x509Cert

        try {
            if (readNumber(inputStream, 3) != inputStream.available().toLong()) {
                throw SerializationException("Extra data corrupted.")
            }
            while (inputStream.available() > 0) {
                val length = readNumber(inputStream, 3).toInt()
                x509EntryChain.certificateChain.add(readFixedLength(inputStream, length))
            }
        } catch (e: IOException) {
            throw SerializationException("Cannot parse xChainEntry. ${e.localizedMessage}")
        }

        return x509EntryChain
    }

    /**
     * Parses PrecertChainEntry structure.
     *
     * @param inputStream PrecertChainEntry structure, byte stream of binary encoding.
     * @param preCert Precertificate.
     * @return [PrecertChainEntry] object.
     */
    private fun parsePrecertChainEntry(inputStream: InputStream, preCert: PreCert?): PrecertChainEntry {
        val preCertChain = PrecertChainEntry()
        preCertChain.preCert = preCert

        try {
            if (readNumber(inputStream, 3) != inputStream.available().toLong()) {
                throw SerializationException("Extra data corrupted.")
            }
            while (inputStream.available() > 0) {
                val length = readNumber(inputStream, 3).toInt()
                preCertChain.precertificateChain.add(readFixedLength(inputStream, length))
            }
        } catch (e: IOException) {
            throw SerializationException("Cannot parse PrecertEntryChain.${e.localizedMessage}")
        }

        return preCertChain
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
