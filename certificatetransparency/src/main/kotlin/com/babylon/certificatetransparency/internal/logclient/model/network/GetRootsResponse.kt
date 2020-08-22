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

package com.babylon.certificatetransparency.internal.logclient.model.network

import com.babylon.certificatetransparency.internal.exceptions.CertificateTransparencyException
import com.babylon.certificatetransparency.internal.utils.Base64
import com.google.gson.annotations.SerializedName
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory

/**
 * @property certificates An array of base64-encoded root certificates that are acceptable to the log.
 */
internal data class GetRootsResponse(
    @SerializedName("certificates") val certificates: List<String>
) {

    /**
     * Parses the response from "get-roots" GET method.
     *
     * @return a list of root certificates.
     */
    fun toRootCertificates(): List<Certificate> {
        return certificates.map {
            try {
                CertificateFactory.getInstance("X509").generateCertificate(Base64.decode(it).inputStream())
            } catch (e: CertificateException) {
                throw CertificateTransparencyException("Malformed data from a CT log have been received: ${e.localizedMessage}", e)
            }
        }
    }
}
