package com.example.weighttrackerapp.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// Salted password hashing with PBKDF2-HMAC-SHA256.
class PasswordHasher(private val iterations: Int = DEFAULT_ITERATIONS) {

    private val random = SecureRandom()

    init {
        require(iterations > 0) { "iterations must be positive" }
    }

    // Hashes password with a fresh random salt
    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = derive(password, salt, iterations, HASH_BYTES)
        return listOf(ALGORITHM_TAG, iterations.toString(), encode(salt), encode(hash))
            .joinToString(SEPARATOR)
    }

    // Returns true only if password produces the stored hash
    fun verify(password: String, stored: String): Boolean {
        val parsed = parse(stored) ?: return false
        val actual = derive(password, parsed.salt, parsed.iterations, parsed.hash.size)
        // Constant-time comparison so timing doesn't reveal how many bytes matched.
        return MessageDigest.isEqual(parsed.hash, actual)
    }

    // True if stored was created with a weaker work factor than this hasher uses now
    fun needsRehash(stored: String): Boolean {
        val parsed = parse(stored) ?: return true
        return parsed.iterations < iterations
    }

    private class Parsed(val iterations: Int, val salt: ByteArray, val hash: ByteArray)

    private fun parse(stored: String): Parsed? {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 4 || parts[0] != ALGORITHM_TAG) return null
        val iter = parts[1].toIntOrNull() ?: return null
        if (iter !in 1..MAX_ACCEPTED_ITERATIONS) return null
        return try {
            val salt = decode(parts[2])
            val hash = decode(parts[3])
            if (salt.isEmpty() || hash.isEmpty()) null else Parsed(iter, salt, hash)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int, hashBytes: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, hashBytes * 8)
        try {
            return SecretKeyFactory.getInstance(KDF_NAME).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    companion object {
        const val DEFAULT_ITERATIONS = 600_000
        private const val MAX_ACCEPTED_ITERATIONS = 10_000_000
        private const val SALT_BYTES = 16
        private const val HASH_BYTES = 32
        private const val ALGORITHM_TAG = "pbkdf2_sha256"
        private const val KDF_NAME = "PBKDF2WithHmacSHA256"
        private const val SEPARATOR = "$"
    }
}