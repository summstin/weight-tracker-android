package com.example.weighttrackerapp.domain

import com.example.weighttrackerapp.data.WeightRepository
import com.example.weighttrackerapp.security.PasswordHasher

/** Account creation and login. Passwords are only ever stored and compared as salted hashes. */
class AuthService(
    private val repository: WeightRepository,
    private val hasher: PasswordHasher
) {
    // Used to spend the same hashing time when a username doesn't exist
    private val dummyHash by lazy { hasher.hash("not-a-real-password") }

    fun register(username: String, password: String): Outcome<Long> {
        val cleanName = username.trim()
        InputValidator.validateUsername(cleanName)?.let { return Outcome.Failure(it) }
        // The password is deliberately not trimmed
        InputValidator.validatePassword(password)?.let { return Outcome.Failure(it) }

        val id = repository.createUser(cleanName, hasher.hash(password))
            ?: return Outcome.Failure("That username is already taken")
        return Outcome.Success(id)
    }

    fun login(username: String, password: String): Outcome<Long> {
        val cleanName = username.trim()
        if (cleanName.isEmpty() || password.isEmpty()) {
            return Outcome.Failure("Enter username and password")
        }

        val user = repository.findUser(cleanName)
        if (user == null) {
            hasher.verify(password, dummyHash)
            return Outcome.Failure(INVALID_LOGIN)
        }
        if (!hasher.verify(password, user.passwordHash)) return Outcome.Failure(INVALID_LOGIN)

        // Upgrade hashes created with an older, weaker work factor now that we have the password
        if (hasher.needsRehash(user.passwordHash)) {
            repository.updatePasswordHash(user.id, hasher.hash(password))
        }
        return Outcome.Success(user.id)
    }

    companion object {
        // Same message for "no such user" and "wrong password"
        const val INVALID_LOGIN = "Invalid username or password"
    }
}