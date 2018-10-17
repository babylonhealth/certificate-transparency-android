package org.certificatetransparency.ctlog.data.logclient.model

import org.certificatetransparency.ctlog.equalsVerifier
import org.junit.Test

class SignedTreeHeadTest {

    @Test
    fun verifyEquals() {
        equalsVerifier<SignedTreeHead>()
    }
}
