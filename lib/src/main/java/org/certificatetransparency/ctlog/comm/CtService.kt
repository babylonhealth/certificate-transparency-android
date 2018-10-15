/*
 * Copyright 2018 Babylon Healthcare Services Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.certificatetransparency.ctlog.comm

import org.certificatetransparency.ctlog.comm.model.AddChainRequest
import org.certificatetransparency.ctlog.comm.model.AddChainResponse
import org.certificatetransparency.ctlog.comm.model.GetEntriesResponse
import org.certificatetransparency.ctlog.comm.model.GetEntryAndProofResponse
import org.certificatetransparency.ctlog.comm.model.GetRootsResponse
import org.certificatetransparency.ctlog.comm.model.GetSthConsistencyResponse
import org.certificatetransparency.ctlog.comm.model.GetSthResponse
import org.certificatetransparency.ctlog.comm.model.ProofByHashResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CtService {
    @GET("get-proof-by-hash")
    fun getProofByHash(@Query("tree_size") treeSize: Long, @Query("hash") hash: String): Call<ProofByHashResponse>

    @GET("get-entry-and-proof")
    fun getEntryAndProof(@Query("leaf_index") leafIndex: Long, @Query("tree_size") treeSize: Long): Call<GetEntryAndProofResponse>

    @GET("get-roots")
    fun getRoots(): Call<GetRootsResponse>

    @GET("get-sth")
    fun getSth(): Call<GetSthResponse>

    @GET("get-entries")
    fun getEntries(@Query("start") start: Long, @Query("end") end: Long): Call<GetEntriesResponse>

    @GET("get-sth-consistency")
    fun getSthConsistency(@Query("first") first: Long, @Query("second") second: Long): Call<GetSthConsistencyResponse>

    @POST("add-pre-chain")
    fun addPreChain(@Body addChainRequest: AddChainRequest): Call<AddChainResponse>

    @POST("add-chain")
    fun addChain(@Body addChainRequest: AddChainRequest): Call<AddChainResponse>
}
