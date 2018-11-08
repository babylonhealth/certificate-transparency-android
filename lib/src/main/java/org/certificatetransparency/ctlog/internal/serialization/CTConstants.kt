package org.certificatetransparency.ctlog.internal.serialization

/** Constants used for serializing and de-serializing.  */
internal object CTConstants {
    // All in bytes.
    const val MAX_EXTENSIONS_LENGTH = (1 shl 16) - 1
    const val MAX_SIGNATURE_LENGTH = (1 shl 16) - 1
    const val KEY_ID_LENGTH = 32
    const val TIMESTAMP_LENGTH = 8
    const val VERSION_LENGTH = 1
    const val LOG_ENTRY_TYPE_LENGTH = 2
    const val MAX_CERTIFICATE_LENGTH = (1 shl 24) - 1

    // Useful OIDs
    const val PRECERTIFICATE_SIGNING_OID = "1.3.6.1.4.1.11129.2.4.4"
    const val POISON_EXTENSION_OID = "1.3.6.1.4.1.11129.2.4.3"
    const val SCT_CERTIFICATE_OID = "1.3.6.1.4.1.11129.2.4.2"
}
