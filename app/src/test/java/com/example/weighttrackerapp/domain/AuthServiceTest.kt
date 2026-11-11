package com.example.weighttrackerapp.domain

import com.example.weighttrackerapp.security.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthServiceTest {
    private val hasher = PasswordHasher(iterations = 1_000)
    private val repo = FakeWeightRepository()
    private val auth = AuthService(repo, hasher)

    @Test
    fun registerStoresAHashNeverThePassword() {
        val id = (auth.register("tyler", "my secret pw") as Outcome.Success).value
        val stored = repo.findUser("tyler")!!
        assertEquals(id, stored.id)
        assertNotEquals("my secret pw", stored.passwordHash)
        assertTrue(hasher.verify("my secret pw", stored.passwordHash))
    }

    @Test
    fun registerRejectsDuplicateUsernamesIgnoringCase() {
        auth.register("tyler", "password-one")
        assertTrue(auth.register("TYLER", "password-two") is Outcome.Failure)
    }

    @Test
    fun registerRejectsWeakInput() {
        assertTrue(auth.register("ty", "password-one") is Outcome.Failure)
        assertTrue(auth.register("tyler", "short") is Outcome.Failure)
    }

    @Test
    fun loginWorksWithCorrectCredentials() {
        val id = (auth.register("tyler", "my secret pw") as Outcome.Success).value
        assertEquals(Outcome.Success(id), auth.login("tyler", "my secret pw"))
    }

    @Test
    fun wrongPasswordAndUnknownUserGetTheSameMessage() {
        auth.register("tyler", "my secret pw")
        val wrongPassword = auth.login("tyler", "not my pw") as Outcome.Failure
        val unknownUser = auth.login("nobody", "my secret pw") as Outcome.Failure
        assertEquals(AuthService.INVALID_LOGIN, wrongPassword.message)
        assertEquals(wrongPassword.message, unknownUser.message)
    }

    @Test
    fun passwordsAreNotTrimmed() {
        auth.register("tyler", "  spaced password  ")
        assertTrue(auth.login("tyler", "  spaced password  ") is Outcome.Success)
        assertTrue(auth.login("tyler", "spaced password") is Outcome.Failure)
    }

    @Test
    fun loginUpgradesHashesMadeWithAWeakerWorkFactor() {
        val oldHash = PasswordHasher(iterations = 500).hash("legacy password")
        val id = repo.createUser("legacy", oldHash)!!

        assertTrue(auth.login("legacy", "legacy password") is Outcome.Success)

        val upgraded = repo.findUser("legacy")!!.passwordHash
        assertNotEquals(oldHash, upgraded)
        assertFalse(hasher.needsRehash(upgraded))
        assertTrue(hasher.verify("legacy password", upgraded))
        assertEquals(id, repo.findUser("legacy")!!.id)
    }
}