package org.certificatetransparency.ctlog.comm.model

import com.google.gson.annotations.SerializedName

data class GetSthConsistencyResponse(
    @SerializedName("consistency") val consistency: List<String>
)
