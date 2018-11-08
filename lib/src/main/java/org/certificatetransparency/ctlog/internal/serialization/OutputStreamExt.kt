package org.certificatetransparency.ctlog.internal.serialization

import java.io.IOException
import java.io.OutputStream
import kotlin.math.pow

private const val BITS_IN_BYTE = 8

/**
 * Write a numeric value of numBytes bytes, MSB first.
 *
 * @receiver stream to write to.
 * @param value number to write. Must be non-negative.
 * @param numBytes number to bytes to write for the value.
 * @throws IOException
 */
fun OutputStream.writeUint(value: Long, numBytes: Int) {
    require(value >= 0)
    @Suppress("MagicNumber")
    require(value < 256.0.pow(numBytes.toDouble())) { "Value $value cannot be stored in $numBytes bytes" }
    var numBytesRemaining = numBytes
    while (numBytesRemaining > 0) {
        // MSB first.
        val shiftBy = (numBytesRemaining - 1) * BITS_IN_BYTE
        @Suppress("MagicNumber")
        val mask = 0xff.toLong() shl shiftBy
        write((value and mask shr shiftBy).toByte().toInt())
        numBytesRemaining--
    }
}

/**
 * Write a variable-length array to the output stream.
 *
 * @receiver stream to write to.
 * @param data data to write.
 * @param maxDataLength Maximal data length. Used for calculating the number of bytes needed to
 * store the length of the data.
 * @throws IOException
 */
fun OutputStream.writeVariableLength(data: ByteArray, maxDataLength: Int) {
    require(data.size <= maxDataLength)
    val bytesForDataLength = Deserializer.bytesForDataLength(maxDataLength)
    writeUint(data.size.toLong(), bytesForDataLength)
    write(data, 0, data.size)
}
