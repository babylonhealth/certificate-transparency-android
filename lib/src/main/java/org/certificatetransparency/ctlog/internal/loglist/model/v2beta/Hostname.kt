package org.certificatetransparency.ctlog.internal.loglist.model.v2beta

import com.google.gson.annotations.JsonAdapter
import okhttp3.HttpUrl
import org.certificatetransparency.ctlog.internal.loglist.deserializer.HostnameDeserializer

@JsonAdapter(HostnameDeserializer::class)
data class Hostname(
    val value: String
) {
    init {
        HttpUrl.parse("http://$value")?.host() ?: throw IllegalArgumentException("$value is not a well-formed hostname")
    }
}
