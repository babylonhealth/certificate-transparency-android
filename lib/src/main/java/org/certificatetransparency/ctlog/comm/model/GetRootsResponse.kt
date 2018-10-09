package org.certificatetransparency.ctlog.comm.model

import com.google.gson.annotations.SerializedName

data class GetRootsResponse(
    @SerializedName("certificates") val certificates: List<String>
)
