package org.certificatetransparency.ctlog.comm.model

import com.google.gson.annotations.SerializedName

data class GetSthResponse(
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("tree_head_signature") val treeHeadSignature: String,
    @SerializedName("sha256_root_hash") val sha256RootHash: String,
    @SerializedName("tree_size") val treeSize: Long
)
