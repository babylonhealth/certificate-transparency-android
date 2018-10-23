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

package org.certificatetransparency.ctlog.internal.loglist.model_v2_beta

/**
 * @property email CT log operator email addresses. The log operator can be contacted using any of these email addresses. (format: email)
 * @property logs Details of Certificate Transparency logs run by this operator.
 */
data class Operator(
    val email: List<String>,
    val logs: Map<String, Log>
) {
    init {
        require(email.isNotEmpty())
        require(logs.isNotEmpty())
    }
}
