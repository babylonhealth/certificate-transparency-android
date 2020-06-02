package com.babylon.certificatetransparency.internal.verifier.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HostTest {

    @Test
    fun identicalPatternsEqual() {
        val host1 = Host("example.com")
        val host2 = Host("example.com")

        assertEquals(host1, host2)
        assertEquals(host1.hashCode(), host2.hashCode())
    }

    @Test
    fun identicalWildcardPatternsEqual() {
        val host1 = Host("*.example.com")
        val host2 = Host("*.example.com")

        assertEquals(host1, host2)
        assertEquals(host1.hashCode(), host2.hashCode())
    }

    @Test
    fun canonicalPatternsEqual() {
        val host1 = Host("example.com")
        val host2 = Host("eXaMpLe.CoM")

        assertEquals(host1, host2)
        assertEquals(host1.hashCode(), host2.hashCode())
    }

    @Test
    fun subdomainsDoNotEqual() {
        val host1 = Host("sub1.example.com")
        val host2 = Host("sub2.example.com")

        assertNotEquals(host1, host2)
        assertNotEquals(host1.hashCode(), host2.hashCode())
    }

    @Test
    fun baseAndSubDoNotEqual() {
        val host1 = Host("example.com")
        val host2 = Host("sub.example.com")

        assertNotEquals(host1, host2)
        assertNotEquals(host1.hashCode(), host2.hashCode())
    }

    @Test
    fun baseAndWildcardDoNotEqual() {
        val host1 = Host("example.com")
        val host2 = Host("*.example.com")

        assertNotEquals(host1, host2)
        assertNotEquals(host1.hashCode(), host2.hashCode())
    }

    @Test
    fun subAndWildcardDoNotEqual() {
        val host1 = Host("sub.example.com")
        val host2 = Host("*.example.com")

        assertNotEquals(host1, host2)
        assertNotEquals(host1.hashCode(), host2.hashCode())
    }

    @Test
    fun baseDomainMatches() {
        val host = Host("example.com")

        assertTrue(host.matches("example.com"))
    }

    @Test
    fun subdomainMatches() {
        val host = Host("sub.example.com")

        assertTrue(host.matches("sub.example.com"))
    }

    @Test
    fun subdomainMatchesCanonical() {
        val host = Host("SuB.eXaMpLe.CoM")

        assertTrue(host.matches("sub.example.com"))
    }

    @Test
    fun differentSubdomainsDoNotMatch() {
        val host = Host("sub1.example.com")

        assertFalse(host.matches("sub2.example.com"))
    }

    @Test
    fun wildcardMatchesSubdomain() {
        val host = Host("*.example.com")

        assertTrue(host.matches("sub.example.com"))
    }

    @Test
    fun wildcardDoesNotMatchBase() {
        val host = Host("*.example.com")

        assertFalse(host.matches("example.com"))
    }

    @Test
    fun baseDomainMatchesCanonical() {
        val host = Host("eXaMpLe.CoM")

        assertTrue(host.matches("example.com"))
    }

    @Test
    fun baseDoesNotMatchSubdomain() {
        val host = Host("example.com")

        assertFalse(host.matches("sub.example.com"))
    }

    @Test
    fun allBaseDomainMatches() {
        val host = Host("*.*")

        assertTrue(host.matches("example.com"))
    }

    @Test
    fun allSubdomainMatches() {
        val host = Host("*.*")

        assertTrue(host.matches("sub.example.com"))
    }
}
