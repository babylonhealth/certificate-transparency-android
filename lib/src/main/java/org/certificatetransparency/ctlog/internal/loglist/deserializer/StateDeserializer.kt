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

package org.certificatetransparency.ctlog.internal.loglist.deserializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import org.certificatetransparency.ctlog.internal.loglist.model_v2_beta.State
import java.lang.reflect.Type

class StateDeserializer : JsonDeserializer<State> {

    override fun deserialize(
        jsonElement: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ): State {
        // Schema specifies there is exactly 1 element
        val (stateType, data) = jsonElement.asJsonObject.entrySet().first()

        val stateClass = when (stateType) {
            "pending" -> State.Pending::class
            "qualified" -> State.Qualified::class
            "usable" -> State.Usable::class
            "frozen" -> State.Frozen::class
            "retired" -> State.Retired::class
            "rejected" -> State.Rejected::class
            else -> throw IllegalStateException("Unknown state")
        }

        return context.deserialize<State>(data, stateClass.java)
    }
}
