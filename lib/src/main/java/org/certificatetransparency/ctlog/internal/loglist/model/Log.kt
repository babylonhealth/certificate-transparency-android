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

package org.certificatetransparency.ctlog.internal.loglist.model

import com.google.gson.annotations.SerializedName

/**
 * @property key Base64 encoded public key
 * @property maximumMergeDelay Maximum merge delay (MMD) in seconds; often 86400 = 24 hours
 * @property operatedBy List of log operators - containing Operator numeric id
 * @property dnsApiEndpoint DNS API endpoint for the log
 */
internal data class Log(
    val description: String,
    val key: String,
    val url: String,
    @SerializedName("maximum_merge_delay") val maximumMergeDelay: Long,
    @SerializedName("operated_by") val operatedBy: List<Int>,
    @SerializedName("dns_api_endpoint") val dnsApiEndpoint: String?
)
