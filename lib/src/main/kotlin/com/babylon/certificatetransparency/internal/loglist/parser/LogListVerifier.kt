package com.babylon.certificatetransparency.internal.loglist.parser

import com.babylon.certificatetransparency.internal.loglist.*
import com.babylon.certificatetransparency.internal.loglist.LogServerSignatureResult
import java.security.*

internal class LogListVerifier(
        private val publicKey: PublicKey = GoogleLogListPublicKey
) {

    fun verify(message: String, signature: ByteArray): LogServerSignatureResult {
        return try {
            if (Signature.getInstance("SHA256WithRSA").apply {
                        initVerify(publicKey)
                        update(message.toByteArray())
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