package org.certificatetransparency.ctlog.comm.model

import com.google.gson.annotations.SerializedName

data class GetEntriesResponse(
    @SerializedName("entries") val entries: List<Entry>
) {
    data class Entry(
        @SerializedName("leaf_input") val leafInput: String,
        @SerializedName("extra_data") val extraData: String
    )
}
