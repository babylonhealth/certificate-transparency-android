package org.certificatetransparency.ctlog.okhttp.model

internal data class Log(
    val description: String,
    val key: String,
    val url: String,
    val maximum_merge_delay: Long,
    val operated_by: List<Int>,
    val dns_api_endpoint: String
)
