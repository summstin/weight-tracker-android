package com.example.weighttrackerapp.domain

import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.math.roundToLong

// Pure validation functions
// Username/password/phone checks return an error message or null

object InputValidator {
    const val MIN_USERNAME_LENGTH = 3
    const val MAX_USERNAME_LENGTH = 32
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 128
    const val MIN_WEIGHT = 20.0
    const val MAX_WEIGHT = 1000.0

    private val USERNAME_PATTERN = Regex("^[A-Za-z0-9_.-]+\$")
    private val PHONE_PATTERN = Regex("^\\+?[0-9]{7,15}\$")
    private val PHONE_SEPARATORS = Regex("[\\s().-]")

    fun validateUsername(username: String): String? = when {
        username.length < MIN_USERNAME_LENGTH ->
            "Username must be at least $MIN_USERNAME_LENGTH characters"
        username.length > MAX_USERNAME_LENGTH ->
            "Username must be at most $MAX_USERNAME_LENGTH characters"
        !USERNAME_PATTERN.matches(username) ->
            "Username may only use letters, numbers, dots, dashes and underscores"
        else -> null
    }

    fun validatePassword(password: String): String? = when {
        password.length < MIN_PASSWORD_LENGTH ->
            "Password must be at least $MIN_PASSWORD_LENGTH characters"
        password.length > MAX_PASSWORD_LENGTH ->
            "Password must be at most $MAX_PASSWORD_LENGTH characters"
        else -> null
    }

    // Parses a weight in lbs, rounded to one decimal place.
    fun parseWeight(text: String): Outcome<Double> {
        val message = "Weight must be a number between ${MIN_WEIGHT.toInt()} and ${MAX_WEIGHT.toInt()}"
        val value = text.trim().toDoubleOrNull()
        if (value == null || !value.isFinite() || value < MIN_WEIGHT || value > MAX_WEIGHT) {
            return Outcome.Failure(message)
        }
        return Outcome.Success((value * 10).roundToLong() / 10.0)
    }

    // Parses a strict ISO date that is not in the future
    fun parseDate(text: String, today: LocalDate = LocalDate.now()): Outcome<LocalDate> {
        val date = try {
            LocalDate.parse(text.trim())
        } catch (e: DateTimeParseException) {
            return Outcome.Failure("Date must be a real date in YYYY-MM-DD format")
        }
        if (date.isAfter(today)) return Outcome.Failure("Date can't be in the future")
        return Outcome.Success(date)
    }

    // A blank phone number is allowed; returns null in that case
    fun parsePhone(text: String): Outcome<String?> {
        if (text.isBlank()) return Outcome.Success(null)
        val cleaned = text.replace(PHONE_SEPARATORS, "")
        if (!PHONE_PATTERN.matches(cleaned)) {
            return Outcome.Failure("Enter a phone number with 7 to 15 digits")
        }
        return Outcome.Success(cleaned)
    }
}