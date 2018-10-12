package org.certificatetransparency.ctlog.okhttp.model

internal data class LogList(
    val logs: List<Log>,
    val operators: List<Operator>
)
