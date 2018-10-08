package org.certificatetransparency.ctlog.comm.model

import com.google.gson.annotations.SerializedName

data class GetEntryAndProofResponse(
    @SerializedName("leaf_input") val leafInput: String,
    @SerializedName("extra_data") val extraData: String,
    @SerializedName("audit_path") val auditPath: List<String>
)
