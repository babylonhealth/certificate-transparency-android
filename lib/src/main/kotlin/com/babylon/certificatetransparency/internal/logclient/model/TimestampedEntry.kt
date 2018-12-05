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

package com.babylon.certificatetransparency.internal.logclient.model

/**
 * @property timestamp [timestamp] is the timestamp of the corresponding SCT issued for this certificate.
 * @property signedEntry [signedEntry] is the [SignedEntry] of the corresponding SCT.
 */
internal data class TimestampedEntry(
    val timestamp: Long = 0,
    val signedEntry: SignedEntry
)
