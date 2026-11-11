package com.example.weighttrackerapp.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    // Low iteration count keeps tests fast
    private val hasher = PasswordHasher(iterations = 1_000)

    @Test
    fun hashIsNotPlaintextAndVerifies() {
        val stored = hasher.hash("correct horse battery")
        assertFalse(stored.contains("correct horse battery"))
        assertTrue(hasher.verify("correct horse battery", stored))
    }

    @Test
    fun wrongPasswordFails() {
        val stored = hasher.hash("correct horse battery")
        assertFalse(hasher.verify("Correct horse battery", stored))
        assertFalse(hasher.verify("", stored))
    }

    @Test
    fun samePasswordGetsDifferentSalts() {
        assertNotEquals(hasher.hash("same-password"), hasher.hash("same-password"))
    }

    @Test
    fun malformedStoredValuesFailInsteadOfThrowing() {
        assertFalse(hasher.verify("x", ""))
        assertFalse(hasher.verify("x", "plaintext-password"))
        assertFalse(hasher.verify("x", "pbkdf2_sha256\$notanumber\$AAAA\$AAAA"))
        assertFalse(hasher.verify("x", "pbkdf2_sha256\$1000\$!!!\$!!!"))
        assertFalse(hasher.verify("x", "other_algo\$1000\$AAAA\$AAAA"))
    }

    @Test
    fun needsRehashWhenWorkFactorWasLower() {
        val old = PasswordHasher(iterations = 500).hash("pw-for-rehash")
        assertTrue(hasher.needsRehash(old))
        assertFalse(hasher.needsRehash(hasher.hash("pw-for-rehash")))
        assertTrue(hasher.verify("pw-for-rehash", old))   // old hashes still verify
    }
}