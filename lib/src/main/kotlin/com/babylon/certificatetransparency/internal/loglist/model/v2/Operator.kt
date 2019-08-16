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

package com.babylon.certificatetransparency.internal.loglist.model.v2

import com.google.gson.annotations.SerializedName

/**
 * @property name Name of this log operator
 * @property email CT log operator email addresses. The log operator can be contacted using any of these email addresses. (format: email)
 * @property logs Details of Certificate Transparency logs run by this operator.
 */
internal data class Operator(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: List<String>,
    @SerializedName("logs") val logs: List<Log>
) {
    init {
        require(name.isNotEmpty())
        require(email.isNotEmpty())
        require(logs.isNotEmpty())
    }
}
