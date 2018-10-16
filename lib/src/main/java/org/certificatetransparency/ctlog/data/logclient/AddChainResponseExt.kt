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

package org.certificatetransparency.ctlog.data.logclient

import org.certificatetransparency.ctlog.Base64
import org.certificatetransparency.ctlog.data.logclient.model.network.AddChainResponse
import org.certificatetransparency.ctlog.serialization.Deserializer
import org.certificatetransparency.ctlog.domain.logclient.model.LogId
import org.certificatetransparency.ctlog.domain.logclient.model.SignedCertificateTimestamp
import org.certificatetransparency.ctlog.domain.logclient.model.Version
import java.io.ByteArrayInputStream

/**
 * Parses the CT Log's json response into a SignedCertificateTimestamp.
 *
 * @return SCT filled from the JSON input.
 */
internal fun AddChainResponse.toSignedCertificateTimestamp(): SignedCertificateTimestamp {
    return SignedCertificateTimestamp(
        version = Version.forNumber(sctVersion),
        id = LogId(Base64.decode(id)),
        timestamp = timestamp,
        extensions = if (extensions.isNotEmpty()) Base64.decode(extensions) else ByteArray(0),
        signature = Deserializer.parseDigitallySignedFromBinary(ByteArrayInputStream(Base64.decode(signature)))
    )
}
