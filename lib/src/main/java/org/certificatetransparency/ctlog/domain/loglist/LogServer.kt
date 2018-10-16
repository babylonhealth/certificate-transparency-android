package org.certificatetransparency.ctlog.domain.loglist

import org.certificatetransparency.ctlog.data.verifier.LogSignatureVerifier

data class LogServer(
    val logId: String,
    val verifier: LogSignatureVerifier
)
