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

package org.certificatetransparency.ctlog.internal.loglist.model.v2beta

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.certificatetransparency.ctlog.internal.loglist.deserializer.StateDeserializer

/**
 * @property timestamp The time at which the log entered this state.
 */
@JsonAdapter(StateDeserializer::class)
sealed class State {
    abstract val timestamp: String

    data class Pending(override val timestamp: String) : State()

    data class Qualified(override val timestamp: String) : State()

    data class Usable(override val timestamp: String) : State()

    /**
     * @property finalTreeHead The tree head (tree size and root hash) at which the log was frozen.
     */
    data class Frozen(
        override val timestamp: String,
        @SerializedName("final_tree_head") val finalTreeHead: FinalTreeHead
    ) : State()

    data class Retired(override val timestamp: String) : State()

    data class Rejected(override val timestamp: String) : State()
}
