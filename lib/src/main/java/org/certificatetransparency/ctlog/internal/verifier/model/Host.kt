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

package org.certificatetransparency.ctlog.internal.verifier.model

import okhttp3.HttpUrl

/**
 * @property pattern A hostname like `example.com` or a pattern like `*.example.com`.
 */
internal data class Host(
    val pattern: String
) {
    /**
     * The canonical hostname, i.e. `EXAMPLE.com` becomes `example.com`.
     */
    private val canonicalHostname: String

    init {
        this.canonicalHostname = if (pattern.startsWith(WILDCARD)) {
            HttpUrl.parse("http://" + pattern.substring(WILDCARD.length))?.host()
        } else {
            HttpUrl.parse("http://$pattern")?.host()
        } ?: throw IllegalArgumentException("$pattern is not a well-formed URL")
    }

    fun matches(hostname: String): Boolean {
        if (pattern.startsWith(WILDCARD)) {
            val firstDot = hostname.indexOf('.')
            return hostname.length - firstDot - 1 == canonicalHostname.length && hostname.regionMatches(firstDot + 1, canonicalHostname, 0,
                canonicalHostname.length, ignoreCase = false)
        }

        return hostname == canonicalHostname
    }

    companion object {

        private const val WILDCARD = "*."
    }
}
