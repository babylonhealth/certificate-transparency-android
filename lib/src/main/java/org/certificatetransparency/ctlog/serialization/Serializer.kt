package org.certificatetransparency.ctlog.serialization

import org.certificatetransparency.ctlog.proto.Ct
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

/** Serializes common structure to binary format.  */
object Serializer {
    /**
     * Write a numeric value of numBytes bytes, MSB first.
     *
     * @param outputStream stream to write to.
     * @param value number to write. Must be non-negative.
     * @param numBytes number to bytes to write for the value.
     */
    @JvmStatic
    fun writeUint(outputStream: OutputStream, value: Long, numBytes: Int) {
        require(value >= 0)
        require(value < Math.pow(256.0, numBytes.toDouble())) { "Value $value cannot be stored in $numBytes bytes" }
        var numBytes = numBytes
        try {
            while (numBytes > 0) {
                // MSB first.
                val shiftBy = (numBytes - 1) * 8
                val mask = 0xff.toLong() shl shiftBy
                outputStream.write((value and mask shr shiftBy).toByte().toInt())
                numBytes--
            }
        } catch (e: IOException) {
            throw SerializationException("Failure while writing number $value", e)
        }
    }

    /**
     * Write a variable-length array to the output stream.
     *
     * @param outputStream stream to write to.
     * @param data data to write.
     * @param maxDataLength Maximal data length. Used for calculating the number of bytes needed to
     * store the length of the data.
     */
    @JvmStatic
    fun writeVariableLength(outputStream: OutputStream, data: ByteArray, maxDataLength: Int) {
        require(data.size <= maxDataLength)
        val bytesForDataLength = Deserializer.bytesForDataLength(maxDataLength)
        writeUint(outputStream, data.size.toLong(), bytesForDataLength)
        try {
            outputStream.write(data, 0, data.size)
        } catch (e: IOException) {
            throw SerializationException("Failure while writing byte array.", e)
        }
    }

    @JvmStatic
    fun writeFixedBytes(outputStream: OutputStream, data: ByteArray) {
        try {
            outputStream.write(data)
        } catch (e: IOException) {
            throw SerializationException(
                String.format("Failure while writing fixed data buffer of length %d", data.size))
        }
    }

    @JvmStatic
    fun serializeSctToBinary(sct: Ct.SignedCertificateTimestamp): ByteArray {
        val bos = ByteArrayOutputStream()
        if (sct.version != Ct.Version.V1) {
            throw SerializationException("Cannot serialize unknown SCT version: " + sct.version)
        }
        writeUint(bos, sct.version.number.toLong(), CTConstants.VERSION_LENGTH)
        writeFixedBytes(bos, sct.id.keyId.toByteArray())
        writeUint(bos, sct.timestamp, CTConstants.TIMESTAMP_LENGTH)
        writeVariableLength(bos, sct.extensions.toByteArray(), CTConstants.MAX_EXTENSIONS_LENGTH)
        writeUint(bos, sct.signature.hashAlgorithm.number.toLong(), CTConstants.HASH_ALG_LENGTH)
        writeUint(
            bos, sct.signature.sigAlgorithm.number.toLong(), CTConstants.SIGNATURE_ALG_LENGTH)
        writeVariableLength(
            bos, sct.signature.signature.toByteArray(), CTConstants.MAX_SIGNATURE_LENGTH)

        return bos.toByteArray()
    }
}
