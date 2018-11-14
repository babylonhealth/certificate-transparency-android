package org.certificatetransparency.ctlog.internal.serialization

import org.certificatetransparency.ctlog.internal.exceptions.SerializationException
import org.certificatetransparency.ctlog.internal.utils.Base64
import org.certificatetransparency.ctlog.internal.logclient.model.DigitallySigned
import org.certificatetransparency.ctlog.internal.logclient.model.LogEntry
import org.certificatetransparency.ctlog.internal.logclient.model.LogEntryType
import org.certificatetransparency.ctlog.internal.logclient.model.LogId
import org.certificatetransparency.ctlog.internal.logclient.model.MerkleAuditProof
import org.certificatetransparency.ctlog.internal.logclient.model.MerkleTreeLeaf
import org.certificatetransparency.ctlog.internal.logclient.model.ParsedLogEntry
import org.certificatetransparency.ctlog.internal.logclient.model.ParsedLogEntryWithProof
import org.certificatetransparency.ctlog.internal.logclient.model.PreCertificate
import org.certificatetransparency.ctlog.internal.logclient.model.SignedCertificateTimestamp
import org.certificatetransparency.ctlog.internal.logclient.model.SignedEntry
import org.certificatetransparency.ctlog.internal.logclient.model.TimestampedEntry
import org.certificatetransparency.ctlog.internal.logclient.model.Version
import java.io.IOException
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.log2

/** Converting binary data to CT structures.  */
//
@Suppress("TooManyFunctions")
internal object Deserializer {
    private const val TIMESTAMPED_ENTRY_LEAF_TYPE = 0

    /**
     * Parses a SignedCertificateTimestamp from binary encoding.
     *
     * @param inputStream byte stream of binary encoding.
     * @return Built SignedCertificateTimestamp
     * @throws SerializationException if the data stream is too short.
     * TODO IOException
     */
    fun parseSctFromBinary(inputStream: InputStream): SignedCertificateTimestamp {
        val version = Version.forNumber(inputStream.readNumber(1 /* single byte */).toInt())
        if (version != Version.V1) {
            throw SerializationException("Unknown version: $version")
        }

        val keyId = inputStream.readFixedLength(CTConstants.KEY_ID_LENGTH)

        val timestamp = inputStream.readNumber(CTConstants.TIMESTAMP_LENGTH)

        val extensions = inputStream.readVariableLength(CTConstants.MAX_EXTENSIONS_LENGTH)

        val signature = parseDigitallySignedFromBinary(inputStream)

        return SignedCertificateTimestamp(
            sctVersion = version,
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
     * TODO IOException
     */
    fun parseDigitallySignedFromBinary(inputStream: InputStream): DigitallySigned {
        val hashAlgorithmByte = inputStream.readNumber(1 /* single byte */).toInt()
        val hashAlgorithm = DigitallySigned.HashAlgorithm.forNumber(hashAlgorithmByte)
            ?: throw SerializationException("Unknown hash algorithm: ${hashAlgorithmByte.toString(HEX_RADIX)}")

        val signatureAlgorithmByte = inputStream.readNumber(1 /* single byte */).toInt()
        val signatureAlgorithm = DigitallySigned.SignatureAlgorithm.forNumber(signatureAlgorithmByte)
            ?: throw SerializationException("Unknown signature algorithm: ${signatureAlgorithmByte.toString(HEX_RADIX)}")

        val signature = inputStream.readVariableLength(CTConstants.MAX_SIGNATURE_LENGTH)

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
    fun parseLogEntryWithProof(entry: ParsedLogEntry, proof: List<String>, leafIndex: Long, treeSize: Long): ParsedLogEntryWithProof {
        return ParsedLogEntryWithProof(
            entry,
            parseAuditProof(proof, leafIndex, treeSize)
        )
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
    fun parseAuditProof(proof: List<String>, leafIndex: Long, treeSize: Long) =
        MerkleAuditProof(
            Version.V1,
            treeSize,
            leafIndex,
            proof.map(Base64::decode)
        )

    /**
     * Parses an entry retrieved from Log.
     *
     * @param merkleTreeLeaf MerkleTreeLeaf structure, byte stream of binary encoding.
     * @param extraData extra data, byte stream of binary encoding.
     * @return [ParsedLogEntry]
     */
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
        val version = inputStream.readNumber(CTConstants.VERSION_LENGTH).toInt()
        if (version != Version.V1.number) {
            throw SerializationException("Unknown version: $version")
        }

        val leafType = inputStream.readNumber(1).toInt()
        if (leafType != TIMESTAMPED_ENTRY_LEAF_TYPE) {
            throw SerializationException("Unknown entry type: $leafType")
        }

        return MerkleTreeLeaf(
            Version.forNumber(
                version
            ), parseTimestampedEntry(inputStream)
        )
    }

    /**
     * Parses a [TimestampedEntry] from binary encoding.
     *
     * @param inputStream byte stream of binary encoding.
     * @return Built [TimestampedEntry].
     * @throws SerializationException if the data stream is too short.
     */
    private fun parseTimestampedEntry(inputStream: InputStream): TimestampedEntry {
        val timestamp = inputStream.readNumber(CTConstants.TIMESTAMP_LENGTH)

        val entryType = inputStream.readNumber(CTConstants.LOG_ENTRY_TYPE_LENGTH).toInt()
        val logEntryType = LogEntryType.forNumber(entryType)

        val signedEntry = when (logEntryType) {
            LogEntryType.X509_ENTRY -> {
                val length = inputStream.readNumber(THREE_BYTES).toInt()
                SignedEntry.X509(inputStream.readFixedLength(length))
            }
            LogEntryType.PRE_CERTIFICATE_ENTRY -> {
                val issuerKeyHash = inputStream.readFixedLength(THIRTY_TWO_BYTES)

                val length = inputStream.readNumber(2).toInt()
                val tbsCertificate = inputStream.readFixedLength(length)

                SignedEntry.PreCertificate(
                    PreCertificate(
                        issuerKeyHash = issuerKeyHash,
                        tbsCertificate = tbsCertificate
                    )
                )
            }
            else -> throw SerializationException("Unknown entry type: $entryType")
        }

        return TimestampedEntry(
            timestamp = timestamp,
            signedEntry = signedEntry
        )
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
            if (inputStream.readNumber(THREE_BYTES) != inputStream.available().toLong()) {
                throw SerializationException("Extra data corrupted.")
            }
            while (inputStream.available() > 0) {
                val length = inputStream.readNumber(THREE_BYTES).toInt()
                certificateChain.add(inputStream.readFixedLength(length))
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
            if (inputStream.readNumber(THREE_BYTES) != inputStream.available().toLong()) {
                throw SerializationException("Extra data corrupted.")
            }
            while (inputStream.available() > 0) {
                val length = inputStream.readNumber(THREE_BYTES).toInt()
                preCertificateChain.add(inputStream.readFixedLength(length))
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
     * Calculates the number of bytes needed to hold the given number: ceil(log2(maxDataLength)) / 8
     *
     * @param maxDataLength the number that needs to be represented as bytes
     * @return Number of bytes needed to represent the given number
     */
    fun bytesForDataLength(maxDataLength: Int): Int {
        return (ceil(log2(maxDataLength.toDouble())) / BITS_IN_BYTE).toInt()
    }

    private const val HEX_RADIX = 16
    private const val THREE_BYTES = 3
    private const val THIRTY_TWO_BYTES = 32
    private const val BITS_IN_BYTE = 8
}
