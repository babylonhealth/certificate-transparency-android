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

package com.babylon.certificaterevocation

/**
 * A [CRLogger] object is used to log the results of performing certificate revocation for a particular host
 */
interface CRLogger {
    /**
     * Log a [host] and its [result] from performing certificate revocation
     */
    fun log(host: String, result: RevocationResult) = Unit
}
