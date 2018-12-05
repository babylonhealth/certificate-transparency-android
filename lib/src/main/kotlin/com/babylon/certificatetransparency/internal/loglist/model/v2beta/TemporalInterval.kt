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

package com.babylon.certificatetransparency.internal.loglist.model.v2beta

import com.babylon.certificatetransparency.internal.loglist.deserializer.Rfc3339Deserializer
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * @property startInclusive All certificates must expire on this date or later. (format: date-time)
 * @property endExclusive All certificates must expire before this date. (format: date-time)
 */
internal data class TemporalInterval(
    @JsonAdapter(Rfc3339Deserializer::class) @SerializedName("start_inclusive") val startInclusive: Long,
    @JsonAdapter(Rfc3339Deserializer::class) @SerializedName("end_exclusive") val endExclusive: Long
)
