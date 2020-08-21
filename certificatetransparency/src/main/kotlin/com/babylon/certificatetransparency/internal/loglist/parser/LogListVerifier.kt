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

package com.babylon.certificatetransparency.internal.loglist.parser

import com.babylon.certificatetransparency.internal.loglist.GoogleLogListPublicKey
import com.babylon.certificatetransparency.internal.loglist.LogServerSignatureResult
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException

internal class LogListVerifier(
    private val publicKey: PublicKey = GoogleLogListPublicKey
) {

    fun verify(message: ByteArray, signature: ByteArray): LogServerSignatureResult {
        return try {
            if (Signature.getInstance("SHA256withRSA").apply {
                    initVerify(publicKey)
                    update(message)
                }.verify(signature)) {
                LogServerSignatureResult.Valid
            } else {
                LogServerSignatureResult.Invalid.SignatureFailed
            }
        } catch (e: SignatureException) {
            LogServerSignatureResult.Invalid.SignatureNotValid(e)
        } catch (e: InvalidKeyException) {
            LogServerSignatureResult.Invalid.PublicKeyNotValid(e)
        } catch (e: NoSuchAlgorithmException) {
            LogServerSignatureResult.Invalid.NoSuchAlgorithm(e)
        }
    }
}
