/*
 * Copyright 2019 Babylon Partners Limited
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

package com.babylon.certificatetransparency.internal.logclient.model.network

import com.google.gson.annotations.SerializedName

/**
 * @property chain An array of base64-encoded certificates. The first element is the end-entity certificate; the second chains to the first
 * and so on to the last, which is either the root certificate or a certificate that chains to a known root certificate.
 */
internal data class AddChainRequest(
    @SerializedName("chain") val chain: List<String>
)
