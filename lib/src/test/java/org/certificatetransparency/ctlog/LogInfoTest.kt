package org.certificatetransparency.ctlog

import org.bouncycastle.util.encoders.Base64
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/** Mostly for verifying the log info calculates the log ID correctly.  */
@RunWith(JUnit4::class)
class LogInfoTest {

    @Test
    fun testCalculatesLogIdCorrectly() {
        val logInfo = LogInfo(getKey(PUBLIC_KEY, "EC"))
        assertTrue(logInfo.isSameLogId(LOG_ID))
    }

    @Test
    fun testCalculatesLogIdCorrectlyRSA() {
        val logInfo = LogInfo(getKey(PUBLIC_KEY_RSA, "RSA"))
        assertTrue(logInfo.isSameLogId(LOG_ID_RSA))
    }

    companion object {
        /** EC log key  */
        val PUBLIC_KEY: ByteArray = Base64.decode(
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEfahLEimAoz2t01p3uMziiLOl/fHTDM0YDOhBRuiBARsV" + "4UvxG2LdNgoIGLrtCzWE0J5APC2em4JlvR8EEEFMoA==")

        val LOG_ID: ByteArray = Base64.decode("pLkJkLQYWBSHuxOizGdwCjw1mAT5G9+443fNDsgN3BA=")

        /** RSA log key  */
        val PUBLIC_KEY_RSA: ByteArray = Base64.decode(
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3tyLdYQYM+K+1jGlLUTJ"
                + "lNFTeNJM4LN5ctwAwXDhoKCFJrGAayZaXJsYtKHf+RH2Y6pqbtE4Ln/4HgXXzFQi"
                + "BuyTed/ooAafYkDPQsrg51/DxV4WZG66WzFjbFtBPKVfSnLqmbhRlr99PEY92bDt"
                + "8YUOCfEikqHIDZaieJHQQlIx5yjOYbRnsBT0HDitTuvM1or589k+wnYVyNEtU9Np"
                + "NA+37kBD0SM7LipYCCSrb0zh5yTriNQS/LmdUWE1G5v8VR+acttDl5zPKetocNMg"
                + "7NIa/zvrXizld9DQqt2UiC49KcD9x2shxEgp64K0S0546kU0lKYnY7NimDkVRCOe"
                + "3wIDAQAB")

        val LOG_ID_RSA: ByteArray = Base64.decode("oCQsumIkVhezsKvGJ+spTJIM9H+jy/OdvSGDIX0VsgY=")

        internal fun getKey(keyBytes: ByteArray, keyAlg: String): PublicKey {
            val spec = X509EncodedKeySpec(keyBytes)
            try {
                val kf = KeyFactory.getInstance(keyAlg)
                return kf.generatePublic(spec)
            } catch (e: InvalidKeySpecException) {
                throw RuntimeException(e)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
        }
    }
}
