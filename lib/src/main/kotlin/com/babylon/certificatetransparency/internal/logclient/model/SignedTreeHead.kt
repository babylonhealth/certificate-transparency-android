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

import java.util.Arrays

internal data class SignedTreeHead(
    val version: Version,
    val timestamp: Long = 0,
    val treeSize: Long = 0,
    val sha256RootHash: ByteArray? = null,
    val signature: DigitallySigned? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignedTreeHead

        if (version != other.version) return false
        if (timestamp != other.timestamp) return false
        if (treeSize != other.treeSize) return false
        if (!Arrays.equals(sha256RootHash, other.sha256RootHash)) return false
        if (signature != other.signature) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + treeSize.hashCode()
        result = 31 * result + (sha256RootHash?.let { Arrays.hashCode(it) } ?: 0)
        result = 31 * result + (signature?.hashCode() ?: 0)
        return result
    }
}
