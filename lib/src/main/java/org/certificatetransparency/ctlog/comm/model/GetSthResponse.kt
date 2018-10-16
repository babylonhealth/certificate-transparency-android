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

package org.certificatetransparency.ctlog.comm.model

import com.google.gson.annotations.SerializedName

/**
 * @property treeSize The size of the tree, in entries, in decimal.
 * @property timestamp The timestamp, in decimal.
 * @property sha256RootHash The Merkle Tree Hash of the tree, in base64.
 * @property treeHeadSignature A TreeHeadSignature for the above data.
 */
internal data class GetSthResponse(
    @SerializedName("tree_size") val treeSize: Long,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("sha256_root_hash") val sha256RootHash: String,
    @SerializedName("tree_head_signature") val treeHeadSignature: String
)
