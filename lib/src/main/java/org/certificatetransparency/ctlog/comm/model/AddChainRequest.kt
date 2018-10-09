package org.certificatetransparency.ctlog.comm.model

import com.google.gson.annotations.SerializedName

data class AddChainRequest(
    @SerializedName("chain") val chain: List<String>
)
