package org.certificatetransparency.ctlog.comm.model

import com.google.gson.annotations.SerializedName

data class AddChainResponse(
    @SerializedName("sct_version") val sctVersion: Int,
    @SerializedName("id") val id: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("extensions") val extensions: String,
    @SerializedName("signature") val signature: String
)
