package org.certificatetransparency.ctlog.comm.model

import com.google.gson.annotations.SerializedName

data class ProofByHashResponse(
    @SerializedName("leaf_index") val leafIndex: Long,
    @SerializedName("audit_path") val auditPath: List<String>
)
