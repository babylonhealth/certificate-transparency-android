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
 *
 * Derived from https://github.com/appmattus/layercache/
 */

package com.babylon.certificatetransparency.cache

import com.babylon.certificatetransparency.datasource.*
import com.babylon.certificatetransparency.loglist.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

/**
 * A disk cache which stores and retrieves raw log list data
 */
interface DiskCache : DataSource<RawLogListResult> {

    /**
     * Return the value associated with this data source or null if not present
     */
    override suspend fun get(): RawLogListResult? = null

    /**
     * Save the [RawLogListResult] to this data source
     */
    override suspend fun set(value: RawLogListResult) = Unit

    override suspend fun isValid(value: RawLogListResult?): Boolean = value != null

    override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext
}
