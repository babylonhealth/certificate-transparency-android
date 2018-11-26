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

package org.certificatetransparency.ctlog.utils

import org.bouncycastle.util.io.pem.PemReader
import org.certificatetransparency.ctlog.internal.utils.PublicKeyFactory
import java.io.File
import java.io.FileReader
import java.security.PublicKey

/**
 * Load EC or RSA [PublicKey] from a PEM file.
 *
 * @receiver [File] containing the key.
 * @return [PublicKey] represented by this [File].
 */
fun File.readPemFile(): PublicKey {
    return PemReader(FileReader(this)).use {
        PublicKeyFactory.fromByteArray(it.readPemObject().content)
    }
}
