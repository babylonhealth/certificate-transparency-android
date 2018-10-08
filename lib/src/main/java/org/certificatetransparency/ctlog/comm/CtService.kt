package org.certificatetransparency.ctlog.comm

import org.certificatetransparency.ctlog.comm.model.GetEntryAndProofResponse
import org.certificatetransparency.ctlog.comm.model.ProofByHashResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CtService {
    @GET("get-proof-by-hash")
    fun getProofByHash(@Query("tree_size") treeSize: Long, @Query("hash") hash: String): Call<ProofByHashResponse>

    @GET("get-entry-and-proof")
    fun getEntryAndProof(@Query("leaf_index") leafIndex: Long, @Query("tree_size") treeSize: Long): Call<GetEntryAndProofResponse>
}
