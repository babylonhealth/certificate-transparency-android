package org.certificatetransparency.ctlog.data.verifier

import org.certificatetransparency.ctlog.data.logclient.model.SignedTreeHead
import org.certificatetransparency.ctlog.equalsVerifier
import org.junit.Test

class IssuerInformationTest {

    @Test
    fun verifyEquals() {
        equalsVerifier<SignedTreeHead>()
    }
}